"""Maddi ödül kapısı ve geçmiş seçimlerin hesap sınırı."""

from __future__ import annotations

from sqlalchemy import select

from app.modeller import Kullanici, OdulSecimi


def test_teslimat_yokken_yeni_secim_fail_closed(istemci, kullanici_kur, db):
    basliklar, _ = kullanici_kur()

    yanit = istemci.post(
        "/odul/sec",
        json={"kademeId": "kupon_market", "secenekId": "fidan_bagisi"},
        headers=basliklar,
    )

    assert yanit.status_code == 503
    assert "henüz etkin değil" in yanit.json()["detail"]
    assert db.scalar(select(OdulSecimi)) is None


def test_tanimsiz_kademe_ve_kademeye_ait_olmayan_secim_reddediliyor(
    istemci, kullanici_kur
):
    basliklar, _ = kullanici_kur()

    tanimsiz = istemci.post(
        "/odul/sec",
        json={"kademeId": "sinirsiz_kupon", "secenekId": "nakit"},
        headers=basliklar,
    )
    yanlis_eslesme = istemci.post(
        "/odul/sec",
        json={"kademeId": "kupon_market", "secenekId": "kupon_spor_kitap"},
        headers=basliklar,
    )

    assert tanimsiz.status_code == 400
    assert yanlis_eslesme.status_code == 400


def test_gecmis_secimler_listelenmeye_devam_ediyor(istemci, kullanici_kur, db):
    basliklar, _ = kullanici_kur()
    kullanici = db.scalar(select(Kullanici))
    db.add(
        OdulSecimi(
            kullanici_id=kullanici.id,
            kademe_id="kupon_market",
            secenek_id="fidan_bagisi",
        )
    )
    db.commit()

    yanit = istemci.get("/odul/secimlerim", headers=basliklar).json()

    assert yanit == [{"kademeId": "kupon_market", "secenekId": "fidan_bagisi"}]


def test_gecmis_secimler_kullanicilar_arasinda_karismiyor(istemci, kullanici_kur, db):
    a_basliklar, _ = kullanici_kur(ad="Ali", eposta="ali@b.com")
    b_basliklar, _ = kullanici_kur(ad="Veli", eposta="veli@b.com")
    ali = db.scalar(select(Kullanici).where(Kullanici.eposta == "ali@b.com"))
    db.add(
        OdulSecimi(
            kullanici_id=ali.id,
            kademe_id="kupon_market",
            secenek_id="fidan_bagisi",
        )
    )
    db.commit()

    assert istemci.get("/odul/secimlerim", headers=a_basliklar).status_code == 200
    assert istemci.get("/odul/secimlerim", headers=b_basliklar).json() == []


def test_secimlerim_oturum_istiyor(istemci):
    assert istemci.get("/odul/secimlerim").status_code == 401


def test_secim_yazmak_da_oturum_istiyor(istemci):
    yanit = istemci.post(
        "/odul/sec",
        json={"kademeId": "kupon_market", "secenekId": "fidan_bagisi"},
    )

    assert yanit.status_code == 401
