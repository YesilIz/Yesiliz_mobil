"""
Puan düzeltmesi — §4'ün "ceza puanı yok" kuralının tek istisnası (K56).

Kuralın **amacı** korunuyor ve testler o amacı bekliyor: otomatik ceza
yok, gerekçe zorunlu ve kullanıcıya görünür, aynı delta iki kez
uygulanamıyor. Sonuncusu geri dönülemez bir zarar — koruma veritabanında,
tek bir bayrakta değil.
"""

from __future__ import annotations

import datetime as dt

import pytest

from app.modeller import (
    DenetimKaydi,
    Kullanici,
    Personel,
    PersonelDurumu,
    PersonelOturum,
    PersonelRolu,
    PuanDuzeltme,
)
from app.panel.oturum import CEREZ_ADI
from app.panel.puan import SINIR

GEREKCE = "12 Ağustos etkinliği katılım telafisi olarak eklendi."


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


def _kid(db) -> int:
    return db.query(Kullanici).one().id


def _duzelt(panel_istemci, kid, delta=100, gerekce=GEREKCE, anahtar="anahtar-1"):
    return panel_istemci.post(
        f"/yonetim/kullanici/{kid}/puan-duzeltme",
        data={
            "delta": delta,
            "gerekce": gerekce,
            "islem_anahtari": anahtar,
            "_csrf": _csrf(panel_istemci),
        },
    )


# --- temel ---


def test_ekleme_yaziliyor(db, panel_istemci, girisli, kullanici_kur):
    girisli()
    kullanici_kur()
    kid = _kid(db)

    assert _duzelt(panel_istemci, kid, delta=250).status_code in (200, 303)

    kayit = db.query(PuanDuzeltme).one()
    assert kayit.delta == 250 and kayit.gerekce == GEREKCE
    assert kayit.cihaz_uyguladi is False


def test_dusurme_de_yaziliyor(db, panel_istemci, girisli, kullanici_kur):
    """K56: düşürme mümkün — ama insan kararlı ve gerekçeli."""
    girisli()
    kullanici_kur()
    kid = _kid(db)

    _duzelt(panel_istemci, kid, delta=-120)
    assert db.query(PuanDuzeltme).one().delta == -120


def test_gerekcesiz_duzeltilemiyor(db, panel_istemci, girisli, kullanici_kur):
    girisli()
    kullanici_kur()
    kid = _kid(db)

    assert _duzelt(panel_istemci, kid, gerekce="kısa").status_code == 422
    assert db.query(PuanDuzeltme).count() == 0


def test_sifir_degisim_reddediliyor(db, panel_istemci, girisli, kullanici_kur):
    """Etkisiz ama gerekçeli bir satır denetim izini okunmaz yapar."""
    girisli()
    kullanici_kur()
    assert _duzelt(panel_istemci, _kid(db), delta=0).status_code == 422


def test_sinir_asilamiyor(db, panel_istemci, girisli, kullanici_kur):
    """Fazladan bir sıfır, bakiyeyi anlamsız kılar."""
    girisli()
    kullanici_kur()
    kid = _kid(db)
    assert _duzelt(panel_istemci, kid, delta=SINIR + 1).status_code == 422
    assert _duzelt(panel_istemci, kid, delta=-(SINIR + 1), anahtar="a2").status_code == 422
    assert db.query(PuanDuzeltme).count() == 0


# --- tekrar koruması (U2 istisnası) ---


def test_ayni_anahtar_ikinci_kez_uygulanmiyor(db, panel_istemci, girisli, kullanici_kur):
    """Bir deltayı iki kez uygulamak geri dönülemez bir kullanıcı zararı."""
    girisli()
    kullanici_kur()
    kid = _kid(db)

    assert _duzelt(panel_istemci, kid, delta=100).status_code in (200, 303)
    ikinci = _duzelt(panel_istemci, kid, delta=100)

    assert ikinci.status_code == 409
    assert db.query(PuanDuzeltme).count() == 1


def test_farkli_anahtarla_ikinci_duzeltme_yazilabiliyor(
    db, panel_istemci, girisli, kullanici_kur
):
    girisli()
    kullanici_kur()
    kid = _kid(db)

    _duzelt(panel_istemci, kid, delta=100, anahtar="a1")
    _duzelt(panel_istemci, kid, delta=-50, anahtar="a2")
    assert db.query(PuanDuzeltme).count() == 2


def test_reddedilen_islem_denetim_kaydi_birakmiyor(
    db, panel_istemci, girisli, kullanici_kur
):
    girisli()
    kullanici_kur()
    _duzelt(panel_istemci, _kid(db), delta=0)
    assert db.query(DenetimKaydi).count() == 0


def test_basarili_islem_denetim_kaydi_biraliyor(db, panel_istemci, girisli, kullanici_kur):
    girisli()
    kullanici_kur()
    _duzelt(panel_istemci, _kid(db), delta=-300)

    kayit = db.query(DenetimKaydi).one()
    assert kayit.eylem == "kullanici.puan_duzeltme"
    assert kayit.gerekce == GEREKCE
    assert '"delta": -300' in kayit.detay


def test_gozlemci_puan_duzeltemiyor(db, panel_istemci, girisli, kullanici_kur):
    girisli(PersonelRolu.GOZLEMCI)
    kullanici_kur()
    assert _duzelt(panel_istemci, _kid(db)).status_code == 403
    assert db.query(PuanDuzeltme).count() == 0


# --- cihaz ucu ---


def test_cihaz_duzeltmeleri_cekiyor(db, istemci, panel_istemci, girisli, kullanici_kur):
    baslik, _ = kullanici_kur()
    kid = _kid(db)
    girisli()
    _duzelt(panel_istemci, kid, delta=-120)

    yanit = istemci.get("/puan/duzeltmeler", headers=baslik)
    assert yanit.status_code == 200
    kayitlar = yanit.json()
    assert len(kayitlar) == 1
    assert kayitlar[0]["delta"] == -120
    assert kayitlar[0]["gerekce"] == GEREKCE


def test_cihaz_yaniti_personel_adi_tasimiyor(
    db, istemci, panel_istemci, girisli, kullanici_kur
):
    """Kullanıcıya kimin yaptığı değil, ne yapıldığı ve niçin söyleniyor."""
    baslik, _ = kullanici_kur()
    kid = _kid(db)
    p = girisli()
    _duzelt(panel_istemci, kid, delta=100)

    yanit = istemci.get("/puan/duzeltmeler", headers=baslik)
    assert p.ad not in yanit.text
    assert "personel" not in yanit.text.lower()


def test_cekme_bayragi_isaretleniyor(db, istemci, panel_istemci, girisli, kullanici_kur):
    baslik, _ = kullanici_kur()
    kid = _kid(db)
    girisli()
    _duzelt(panel_istemci, kid, delta=100)

    istemci.get("/puan/duzeltmeler", headers=baslik)
    db.expire_all()
    assert db.query(PuanDuzeltme).one().cihaz_uyguladi is True


def test_tekrar_cekmek_kayitlari_cogaltmiyor(
    db, istemci, panel_istemci, girisli, kullanici_kur
):
    """Cihaz listeyi tekrar tekrar çekebilmeli; tekillik onun PK'sında."""
    baslik, _ = kullanici_kur()
    kid = _kid(db)
    girisli()
    _duzelt(panel_istemci, kid, delta=100)

    ilk = istemci.get("/puan/duzeltmeler", headers=baslik).json()
    ikinci = istemci.get("/puan/duzeltmeler", headers=baslik).json()
    assert ilk == ikinci
    assert db.query(PuanDuzeltme).count() == 1


def test_baskasinin_duzeltmesi_gorunmuyor(
    db, istemci, panel_istemci, girisli, kullanici_kur
):
    baslik, _ = kullanici_kur()
    baska = Kullanici(eposta="baska@ornek.com", ad="B", sifre_ozet="x")
    db.add(baska)
    db.flush()
    p = girisli()
    db.add(
        PuanDuzeltme(
            kullanici_id=baska.id,
            delta=999,
            gerekce=GEREKCE,
            personel_id=p.id,
            islem_anahtari="baskasi",
        )
    )
    db.commit()

    assert istemci.get("/puan/duzeltmeler", headers=baslik).json() == []
