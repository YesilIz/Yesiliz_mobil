"""
Rızanın yaşam döngüsü: ebeveyn onayı, süre dolumu, silme.

Rıza metni (`docs/yedam-riza-metni-v1.md §3`) neyin silinip neyin
kalacağını tek tek yazıyor. Burada iki ayrı bitiş yolu var — kullanıcının
geri çekmesi ve 90 günün dolması — ve ikisi **aynı** silmeyi yapmak
zorunda: farklı davranırlarsa biri gerekçesi bitmiş veriyi sunucuda
bırakır.
"""

from __future__ import annotations

import datetime as dt

import pytest

from app.modeller import (
    AnketYanit,
    BasvuruDurumu,
    EbeveynBaglanti,
    ErisimKaydi,
    Kullanici,
    Personel,
    PersonelDurumu,
    PersonelRolu,
    RiskSurukleyici,
    YedamBasvuru,
    YedamRiza,
)
from app.yedam_riza_yonetimi import (
    GECERLILIK_GUN,
    gecerli_riza,
    suresi_dolanlari_kapat,
)

SIMDI = dt.datetime(2026, 8, 21, 12, 0, tzinfo=dt.timezone.utc)


@pytest.fixture
def kur(db):
    def yap(gun_once=0, geri_cekildi=False, kapsam="kimlik,risk,surukleyici,anket"):
        k = Kullanici(
            eposta=f"k{db.query(Kullanici).count()}@ornek.com", ad="K", sifre_ozet="x"
        )
        db.add(k)
        db.flush()
        verilme = SIMDI - dt.timedelta(days=gun_once)
        riza = YedamRiza(
            kullanici_id=k.id,
            metin_surumu="v1",
            kapsam=kapsam,
            verilme=verilme,
            son_gecerlilik=verilme + dt.timedelta(days=GECERLILIK_GUN),
            geri_cekme=SIMDI if geri_cekildi else None,
        )
        db.add(riza)
        db.flush()
        basvuru = YedamBasvuru(
            kullanici_id=k.id,
            riza_id=riza.id,
            iletisim_turu="telefon",
            iletisim_degeri="0555",
            olusturma=verilme,
        )
        db.add(basvuru)
        db.add_all(
            [
                RiskSurukleyici(
                    kullanici_id=k.id, tarih=dt.date(2026, 8, 20), faktor="social", sira=1
                ),
                AnketYanit(
                    kullanici_id=k.id,
                    tarih=dt.date(2026, 8, 1),
                    madde_puanlari="3,3,3,3,3,3,3,3,3,3",
                    ham_puan=30.0,
                    r_s=50.0,
                ),
            ]
        )
        db.commit()
        return k, riza, basvuru

    return yap


# --- geçerlilik ---


def test_taze_riza_gecerli(db, kur):
    k, riza, _ = kur(gun_once=1)
    assert gecerli_riza(db, k.id, SIMDI) is not None


def test_suresi_dolan_riza_gecerli_degil(db, kur):
    """Erişim, iş koşmasa da her okumada kapanmalı."""
    k, _, _ = kur(gun_once=GECERLILIK_GUN + 1)
    assert gecerli_riza(db, k.id, SIMDI) is None


def test_geri_cekilen_riza_gecerli_degil(db, kur):
    k, _, _ = kur(geri_cekildi=True)
    assert gecerli_riza(db, k.id, SIMDI) is None


def test_en_yeni_riza_seciliyor(db, kur):
    """Eski bir rızanın süresi dolsa da yenisi geçerliyse erişim açık."""
    k, eski, _ = kur(gun_once=GECERLILIK_GUN + 5)
    yeni = YedamRiza(
        kullanici_id=k.id,
        metin_surumu="v1",
        kapsam="kimlik",
        verilme=SIMDI,
        son_gecerlilik=SIMDI + dt.timedelta(days=GECERLILIK_GUN),
    )
    db.add(yeni)
    db.commit()

    secilen = gecerli_riza(db, k.id, SIMDI)
    assert secilen is not None and secilen.id == yeni.id


# --- süre dolumu ---


def test_suresi_dolan_veri_siliniyor(db, kur):
    k, _, _ = kur(gun_once=GECERLILIK_GUN + 1)

    assert suresi_dolanlari_kapat(db, SIMDI) == 1
    db.commit()
    db.expire_all()

    assert db.query(RiskSurukleyici).count() == 0
    assert db.query(AnketYanit).count() == 0
    assert db.query(YedamBasvuru).one().durum == BasvuruDurumu.SURESI_DOLDU.value


def test_taze_riza_kapanmiyor(db, kur):
    kur(gun_once=1)
    assert suresi_dolanlari_kapat(db, SIMDI) == 0
    db.commit()
    assert db.query(RiskSurukleyici).count() == 1


def test_is_iki_kez_kosunca_cogalmiyor(db, kur):
    kur(gun_once=GECERLILIK_GUN + 1)
    assert suresi_dolanlari_kapat(db, SIMDI) == 1
    db.commit()
    assert suresi_dolanlari_kapat(db, SIMDI) == 0


def test_geri_cekme_ve_sure_dolumu_ayirt_ediliyor(db, kur):
    """Biri kullanıcının kararı, öteki zamanın geçmesi."""
    from app.yedam_riza_yonetimi import rizayi_bitir

    k1, r1, _ = kur(gun_once=1)
    rizayi_bitir(db, r1, BasvuruDurumu.RIZA_GERI_CEKILDI, SIMDI)
    db.commit()

    k2, _, _ = kur(gun_once=GECERLILIK_GUN + 1)
    suresi_dolanlari_kapat(db, SIMDI)
    db.commit()
    db.expire_all()

    durumlar = {b.kullanici_id: b.durum for b in db.query(YedamBasvuru).all()}
    assert durumlar[k1.id] == "riza_geri_cekildi"
    assert durumlar[k2.id] == "suresi_doldu"


def test_kapanmis_basvurunun_durumu_ezilmiyor(db, kur):
    """Danışmanın kapattığı başvuru "süresi doldu"ya çevrilmemeli."""
    k, _, basvuru = kur(gun_once=GECERLILIK_GUN + 1)
    basvuru.durum = BasvuruDurumu.KAPANDI.value
    db.commit()

    suresi_dolanlari_kapat(db, SIMDI)
    db.commit()
    db.expire_all()
    assert db.query(YedamBasvuru).one().durum == "kapandi"
    # Veri yine de siliniyor: rızanın süresi doldu.
    assert db.query(RiskSurukleyici).count() == 0


def test_erisim_kaydi_sure_dolunca_da_kaliyor(db, kur):
    k, _, basvuru = kur(gun_once=GECERLILIK_GUN + 1)
    p = Personel(
        eposta="d@yesilay.org.tr",
        ad="D",
        sifre_ozet="x",
        rol=PersonelRolu.YEDAM_DANISMAN.value,
        durum=PersonelDurumu.AKTIF.value,
    )
    db.add(p)
    db.flush()
    db.add(ErisimKaydi(kullanici_id=k.id, personel_id=p.id, basvuru_id=basvuru.id))
    db.commit()

    suresi_dolanlari_kapat(db, SIMDI)
    db.commit()
    assert db.query(ErisimKaydi).count() == 1


# --- ebeveyn onayı ---


@pytest.fixture
def onay_kurulumu(db, istemci, kullanici_kur):
    """Çocuk başvurdu, ebeveyn onayı bekleniyor."""

    def yap():
        cocuk_baslik, _ = kullanici_kur(
            ad="Çocuk", eposta="cocuk@ornek.com", sifre="parola123"
        )
        ebeveyn_baslik, _ = kullanici_kur(
            ad="Anne", eposta="anne@ornek.com", sifre="parola123"
        )
        cocuk = db.query(Kullanici).filter_by(eposta="cocuk@ornek.com").one()
        ebeveyn = db.query(Kullanici).filter_by(eposta="anne@ornek.com").one()
        db.add(EbeveynBaglanti(ebeveyn_id=ebeveyn.id, cocuk_id=cocuk.id))
        db.commit()

        yanit = istemci.post(
            "/yedam/basvuru",
            json={
                "metinSurumu": "v1",
                "kapsam": ["kimlik", "risk"],
                "iletisimTuru": "telefon",
                "iletisimDegeri": "05551112233",
                "beyanYas": 15,
            },
            headers=cocuk_baslik,
        )
        assert yanit.json()["ebeveynOnayiBekleniyor"] is True
        riza_id = db.query(YedamRiza).one().id
        return cocuk_baslik, ebeveyn_baslik, riza_id

    return yap


def test_ebeveyn_onaylayinca_basvuru_havuza_giriyor(db, istemci, onay_kurulumu):
    _, ebeveyn_baslik, riza_id = onay_kurulumu()

    yanit = istemci.post(
        "/ebeveyn/yedam-onay", json={"rizaId": riza_id, "onay": True}, headers=ebeveyn_baslik
    )
    assert yanit.status_code == 200

    db.expire_all()
    assert db.query(YedamBasvuru).one().durum == "yeni"
    assert db.query(YedamRiza).one().ebeveyn_onay_tarihi is not None


def test_ebeveyn_reddedince_basvuru_kapaniyor(db, istemci, onay_kurulumu):
    """Belirsiz bekleyen kayıt bırakmak, çocuğun sorusunu cevapsız bırakırdı."""
    _, ebeveyn_baslik, riza_id = onay_kurulumu()

    yanit = istemci.post(
        "/ebeveyn/yedam-onay", json={"rizaId": riza_id, "onay": False}, headers=ebeveyn_baslik
    )
    assert yanit.json()["durum"] == "reddedildi"

    db.expire_all()
    assert db.query(YedamBasvuru).one().durum == "riza_geri_cekildi"
    assert db.query(YedamRiza).one().geri_cekme is not None


def test_baskasi_onaylayamiyor(db, istemci, onay_kurulumu, kullanici_kur):
    """Rızada adı yazan ebeveyn dışında kimse onaylayamaz."""
    _, _, riza_id = onay_kurulumu()
    yabanci_baslik, _ = kullanici_kur(
        ad="Yabancı", eposta="yabanci@ornek.com", sifre="parola123"
    )

    yanit = istemci.post(
        "/ebeveyn/yedam-onay", json={"rizaId": riza_id, "onay": True}, headers=yabanci_baslik
    )
    assert yanit.status_code == 404

    db.expire_all()
    assert db.query(YedamBasvuru).one().durum == "ebeveyn_onayi_bekliyor"


def test_cocuk_kendi_basvurusunu_onaylayamiyor(db, istemci, onay_kurulumu):
    cocuk_baslik, _, riza_id = onay_kurulumu()
    yanit = istemci.post(
        "/ebeveyn/yedam-onay", json={"rizaId": riza_id, "onay": True}, headers=cocuk_baslik
    )
    assert yanit.status_code == 404


def test_ikinci_onay_reddediliyor(db, istemci, onay_kurulumu):
    _, ebeveyn_baslik, riza_id = onay_kurulumu()
    istemci.post(
        "/ebeveyn/yedam-onay", json={"rizaId": riza_id, "onay": True}, headers=ebeveyn_baslik
    )
    ikinci = istemci.post(
        "/ebeveyn/yedam-onay", json={"rizaId": riza_id, "onay": True}, headers=ebeveyn_baslik
    )
    assert ikinci.status_code == 409
