"""
Ödül stoğu ve teslimat (`docs/panel-plani.md §5.4`, `CLAUDE.md §12 K28`).

**A4'ün teknik ayağı.** Kapı artık sabit bir `False` değil, stoğun
kendisi. Testlerin çoğu fail-closed davranışı koruyor: stok yoksa seçim
yok, çünkü olmayan bir teslimatı "kazandın" demek uygulamanın en somut
vaadini uydurma yapar.
"""

from __future__ import annotations

import datetime as dt

import pytest
from sqlalchemy import select

from app import odul_stogu
from app.modeller import (
    DenetimKaydi,
    Kullanici,
    OdulSecimi,
    OdulStok,
    OdulTeslimat,
    Personel,
    PersonelDurumu,
    PersonelOturum,
    PersonelRolu,
)
from app.panel.oturum import CEREZ_ADI

GEREKCE = "Ağustos partisi, tedarikçi sözleşmesi 2026-14."


@pytest.fixture
def girisli(db, panel_istemci):
    from app.guvenlik import jeton_ozetle, rastgele_jeton, sifre_ozetle

    def kur(rol=PersonelRolu.OPERASYON) -> Personel:
        p = Personel(
            eposta=f"{rol.value}@yesilay.org.tr",
            ad="Personel",
            sifre_ozet=sifre_ozetle("parola"),
            rol=rol.value,
            durum=PersonelDurumu.AKTIF.value,
            totp_sir="",
        )
        db.add(p)
        db.flush()
        ham = rastgele_jeton()
        db.add(
            PersonelOturum(
                personel_id=p.id,
                jeton_ozet=jeton_ozetle(ham),
                totp_dogrulandi=True,
                ip="127.0.0.1",
                tarayici="test",
                mutlak_bitis=dt.datetime.now(dt.timezone.utc) + dt.timedelta(hours=4),
                iptal=False,
            )
        )
        db.commit()
        panel_istemci.cookies.set(CEREZ_ADI, ham)
        return p

    return kur


def _csrf(panel_istemci) -> str:
    from app.guvenlik import jeton_ozetle
    from app.panel.oturum import csrf_jetonu

    return csrf_jetonu(jeton_ozetle(panel_istemci.cookies[CEREZ_ADI]))


def _post(panel_istemci, yol, **alanlar):
    return panel_istemci.post(yol, data={**alanlar, "_csrf": _csrf(panel_istemci)})


def _stok(db, adet=5, kademe="kupon_market", secenek="market_kuponu"):
    db.add(OdulStok(kademe_id=kademe, secenek_id=secenek, toplam=adet, kalan=adet))
    db.commit()


def _sec(istemci, baslik, kademe="kupon_market", secenek="market_kuponu"):
    return istemci.post(
        "/odul/sec", json={"kademeId": kademe, "secenekId": secenek}, headers=baslik
    )


# --- kapı: fail-closed ---


def test_stok_yoksa_secim_reddediliyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    yanit = _sec(istemci, baslik)
    assert yanit.status_code == 503
    assert db.scalar(select(OdulSecimi)) is None


def test_fidan_bagisi_da_stok_istiyor(db, istemci, kullanici_kur):
    """Bağış da bir maliyet ve A4'ün kapsamında; muaf tutmak K28'i delerdi."""
    baslik, _ = kullanici_kur()
    assert _sec(istemci, baslik, secenek="fidan_bagisi").status_code == 503


def test_durum_fidan_kademesini_de_bildiriyor(db, istemci):
    """
    `fidan` kademesi `SECILEBILIR_ODULLER`'de yoktu.

    K62 "fidan bağışı da stok istiyor" diyor ama sözlük onu tanımadığı için
    `/odul/durum` satır döndürmüyor, panel de ona parti yükleyemiyordu — yani
    kademe finansman gelse bile açılamıyordu. Cihaz kapıyı bu uçtan okuduğu
    için kademe kalıcı olarak "teslimat etkin değil" görünüyordu.
    """
    satirlar = istemci.get("/odul/durum").json()
    kademeler = {s["kademeId"] for s in satirlar}

    assert "fidan" in kademeler
    fidan = [s for s in satirlar if s["kademeId"] == "fidan"]
    assert [s["secenekId"] for s in fidan] == ["fidan_bagisi"]
    assert fidan[0]["acik"] is False, "Stok yokken kapalı (fail-closed)"

    # Dijital rozetin sunucuda karşılığı YOK ve olmamalı: tedarik zinciri
    # gerektirmiyor, kapı ona cihazda hiç uygulanmıyor.
    assert "rozet" not in kademeler


def test_fidan_stogu_yuklenince_kademe_aciliyor(db, panel_istemci, girisli, istemci):
    girisli()
    assert _post(
        panel_istemci,
        "/yonetim/odul/stok",
        kademe_id="fidan",
        secenek_id="fidan_bagisi",
        adet=5,
        gerekce=GEREKCE,
    ).status_code in (200, 303)

    satirlar = istemci.get("/odul/durum").json()
    fidan = next(s for s in satirlar if s["kademeId"] == "fidan")
    assert fidan["acik"] is True


def test_stok_sifirlaninca_kapaniyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    _stok(db, adet=0)
    assert _sec(istemci, baslik).status_code == 503


def test_stok_varsa_secim_kabul_ediliyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    _stok(db, adet=3)

    yanit = _sec(istemci, baslik)
    assert yanit.status_code == 201

    db.expire_all()
    assert db.scalar(select(OdulStok)).kalan == 2, "Seçim stoktan düşmeli"


def test_secimle_birlikte_teslimat_kaydi_aciliyor(db, istemci, kullanici_kur):
    """Panelin "kimin kuponu bekliyor" listesi bunun üzerinden."""
    baslik, _ = kullanici_kur()
    _stok(db, adet=1)
    _sec(istemci, baslik)

    teslimat = db.query(OdulTeslimat).one()
    assert teslimat.durum == "bekliyor"
    assert teslimat.odul_secimi_id == db.query(OdulSecimi).one().id


def test_son_adet_bitince_sonraki_kullanici_alamiyor(db, istemci, kullanici_kur):
    ilk, _ = kullanici_kur(eposta="a@ornek.com", sifre="parola123")
    _stok(db, adet=1)
    assert _sec(istemci, ilk).status_code == 201

    ikinci, _ = kullanici_kur(eposta="b@ornek.com", sifre="parola123")
    assert _sec(istemci, ikinci).status_code == 503
    assert db.query(OdulSecimi).count() == 1


def test_ayni_kademede_ikinci_secim_stok_dusurmuyor(db, istemci, kullanici_kur):
    """K28: bir kademe için seçim bir kez. Reddedilen istek stok yemez."""
    baslik, _ = kullanici_kur()
    _stok(db, adet=5)
    _sec(istemci, baslik)
    assert _sec(istemci, baslik, secenek="fidan_bagisi").status_code == 409

    db.expire_all()
    assert db.scalar(select(OdulStok)).kalan == 4


# --- panel: stok yükleme ---


def test_parti_yukleme_kademeyi_aciyor(db, istemci, panel_istemci, girisli, kullanici_kur):
    baslik, _ = kullanici_kur()
    assert _sec(istemci, baslik).status_code == 503

    girisli()
    assert _post(
        panel_istemci,
        "/yonetim/odul/stok",
        kademe_id="kupon_market",
        secenek_id="market_kuponu",
        adet=10,
        gerekce=GEREKCE,
    ).status_code in (200, 303)

    assert _sec(istemci, baslik).status_code == 201


def test_ikinci_parti_kalani_ezmiyor(db, panel_istemci, girisli):
    girisli()
    for _ in range(2):
        _post(
            panel_istemci,
            "/yonetim/odul/stok",
            kademe_id="kupon_market",
            secenek_id="market_kuponu",
            adet=10,
            gerekce=GEREKCE,
        )
    satir = db.scalar(select(OdulStok))
    assert satir.toplam == 20 and satir.kalan == 20


def test_gerekcesiz_parti_yuklenemiyor(db, panel_istemci, girisli):
    girisli()
    yanit = _post(
        panel_istemci,
        "/yonetim/odul/stok",
        kademe_id="kupon_market",
        secenek_id="market_kuponu",
        adet=10,
        gerekce="kısa",
    )
    assert yanit.status_code == 422
    assert db.scalar(select(OdulStok)) is None


def test_tanimsiz_secenek_yuklenemiyor(db, panel_istemci, girisli):
    girisli()
    assert _post(
        panel_istemci,
        "/yonetim/odul/stok",
        kademe_id="kupon_market",
        secenek_id="uydurma_odul",
        adet=10,
        gerekce=GEREKCE,
    ).status_code == 422


def test_parti_yukleme_denetim_kaydi_biraliyor(db, panel_istemci, girisli):
    girisli()
    _post(
        panel_istemci,
        "/yonetim/odul/stok",
        kademe_id="kupon_market",
        secenek_id="market_kuponu",
        adet=10,
        gerekce=GEREKCE,
    )
    kayit = db.query(DenetimKaydi).one()
    assert kayit.eylem == "odul.stok_yukle"
    assert '"adet": 10' in kayit.detay


def test_gozlemci_stok_yukleyemiyor(db, panel_istemci, girisli):
    girisli(PersonelRolu.GOZLEMCI)
    assert _post(
        panel_istemci,
        "/yonetim/odul/stok",
        kademe_id="kupon_market",
        secenek_id="market_kuponu",
        adet=10,
        gerekce=GEREKCE,
    ).status_code == 403


# --- panel: teslimat ---


def test_teslimat_durumu_ilerliyor(db, istemci, panel_istemci, girisli, kullanici_kur):
    baslik, _ = kullanici_kur()
    _stok(db, adet=1)
    _sec(istemci, baslik)
    girisli()
    tid = db.query(OdulTeslimat).one().id

    _post(panel_istemci, f"/yonetim/odul/teslimat/{tid}", durum="gonderildi", kod="ABC-123")

    db.expire_all()
    t = db.get(OdulTeslimat, tid)
    assert t.durum == "gonderildi" and t.kod == "ABC-123"
    assert t.gonderim_zamani is not None


def test_iptal_stogu_geri_veriyor_secimi_silmiyor(
    db, istemci, panel_istemci, girisli, kullanici_kur
):
    """İptal bir teslimat sorunu, kullanıcının hakkının kaybı değil (K28)."""
    baslik, _ = kullanici_kur()
    _stok(db, adet=3)
    _sec(istemci, baslik)
    db.expire_all()
    assert db.scalar(select(OdulStok)).kalan == 2

    girisli()
    tid = db.query(OdulTeslimat).one().id
    _post(panel_istemci, f"/yonetim/odul/teslimat/{tid}", durum="iptal")

    db.expire_all()
    assert db.scalar(select(OdulStok)).kalan == 3, "Stok geri verilmeli"
    assert db.query(OdulSecimi).count() == 1, "Seçim kalmalı"


def test_ikinci_iptal_stogu_ikinci_kez_geri_vermiyor(
    db, istemci, panel_istemci, girisli, kullanici_kur
):
    baslik, _ = kullanici_kur()
    _stok(db, adet=3)
    _sec(istemci, baslik)
    girisli()
    tid = db.query(OdulTeslimat).one().id

    _post(panel_istemci, f"/yonetim/odul/teslimat/{tid}", durum="iptal")
    _post(panel_istemci, f"/yonetim/odul/teslimat/{tid}", durum="iptal")

    db.expire_all()
    assert db.scalar(select(OdulStok)).kalan == 3


def test_stok_toplami_asilamiyor(db):
    """Geri verme, hiç yüklenmemiş bir adedi havuza koymamalı."""
    db.add(OdulStok(kademe_id="kupon_market", secenek_id="market_kuponu", toplam=2, kalan=2))
    db.commit()

    odul_stogu.geri_ver(db, "kupon_market", "market_kuponu")
    db.commit()
    assert db.scalar(select(OdulStok)).kalan == 2


def test_gecersiz_teslimat_durumu_reddediliyor(
    db, istemci, panel_istemci, girisli, kullanici_kur
):
    baslik, _ = kullanici_kur()
    _stok(db, adet=1)
    _sec(istemci, baslik)
    girisli()
    tid = db.query(OdulTeslimat).one().id
    assert _post(
        panel_istemci, f"/yonetim/odul/teslimat/{tid}", durum="belki"
    ).status_code == 422
