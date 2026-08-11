"""Ödül seçimi — market kuponu yerine fidan bağışı gibi alternatifler."""

from __future__ import annotations


def test_secim_kaydediliyor(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()

    yanit = istemci.post(
        "/odul/sec",
        json={"kademeId": "kupon_market", "secenekId": "fidan_bagisi"},
        headers=basliklar,
    )

    assert yanit.status_code == 201
    assert yanit.json() == {"kademeId": "kupon_market", "secenekId": "fidan_bagisi"}


def test_ayni_kademe_icin_ikinci_secim_reddediliyor(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()
    istemci.post(
        "/odul/sec",
        json={"kademeId": "kupon_market", "secenekId": "market_kuponu"},
        headers=basliklar,
    )

    yanit = istemci.post(
        "/odul/sec",
        json={"kademeId": "kupon_market", "secenekId": "fidan_bagisi"},
        headers=basliklar,
    )

    assert yanit.status_code == 409


def test_farkli_kademeler_ayri_secilebiliyor(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()
    istemci.post(
        "/odul/sec",
        json={"kademeId": "kupon_market", "secenekId": "market_kuponu"},
        headers=basliklar,
    )

    yanit = istemci.post(
        "/odul/sec",
        json={"kademeId": "kupon_spor", "secenekId": "fidan_bagisi"},
        headers=basliklar,
    )

    assert yanit.status_code == 201


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


def test_secimlerim_listesi(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()
    istemci.post(
        "/odul/sec",
        json={"kademeId": "kupon_market", "secenekId": "fidan_bagisi"},
        headers=basliklar,
    )

    yanit = istemci.get("/odul/secimlerim", headers=basliklar).json()

    assert yanit == [{"kademeId": "kupon_market", "secenekId": "fidan_bagisi"}]


def test_kullanicilar_arasinda_secim_karismiyor(istemci, kullanici_kur):
    a_basliklar, _ = kullanici_kur(ad="Ali", eposta="ali@b.com")
    b_basliklar, _ = kullanici_kur(ad="Veli", eposta="veli@b.com")
    istemci.post(
        "/odul/sec",
        json={"kademeId": "kupon_market", "secenekId": "fidan_bagisi"},
        headers=a_basliklar,
    )

    b_secimler = istemci.get("/odul/secimlerim", headers=b_basliklar).json()

    assert b_secimler == []


def test_oturumsuz_istek_reddediliyor(istemci):
    yanit = istemci.get("/odul/secimlerim")

    assert yanit.status_code == 401
