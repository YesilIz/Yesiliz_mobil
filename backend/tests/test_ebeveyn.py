"""Ebeveyn eşleştirme ve özet."""

from __future__ import annotations

import datetime as dt

from sqlalchemy import select

from app.modeller import EslestirmeKodu
from app.zaman import yerel_bugun


def _eslestir(istemci, kullanici_kur):
    """Çocuk kod üretir, ebeveyn girer. İki ayrı hesap gerekiyor."""
    cocuk_basliklar, _ = kullanici_kur(ad="Elif", eposta="elif@b.com")
    ebeveyn_basliklar, _ = kullanici_kur(ad="Anne", eposta="anne@b.com")

    kod = istemci.post("/ebeveyn/kod", json={"rol": "cocuk"}, headers=cocuk_basliklar).json()["kod"]
    yanit = istemci.post(
        "/ebeveyn/eslestir", json={"kod": kod, "rol": "ebeveyn"}, headers=ebeveyn_basliklar
    )
    return cocuk_basliklar, ebeveyn_basliklar, kod, yanit


def test_eslestirme_akisi(istemci, kullanici_kur):
    _, _, _, yanit = _eslestir(istemci, kullanici_kur)

    assert yanit.status_code == 200
    assert yanit.json()["durum"] == "eslesti"


def test_ebeveyn_eslesmeyi_kaldirinca_ozet_erisimi_biter(istemci, kullanici_kur):
    _, ebeveyn_basliklar, _, _ = _eslestir(istemci, kullanici_kur)

    kaldirma = istemci.delete("/ebeveyn/eslesme", headers=ebeveyn_basliklar)

    assert kaldirma.status_code == 200
    assert kaldirma.json()["durum"] == "kaldirildi"
    assert istemci.get("/ebeveyn/ozet", headers=ebeveyn_basliklar).status_code == 404


def test_cocuk_da_eslesmeyi_kaldirabilir_ve_islem_idempotenttir(istemci, kullanici_kur):
    cocuk_basliklar, ebeveyn_basliklar, _, _ = _eslestir(istemci, kullanici_kur)

    assert istemci.delete("/ebeveyn/eslesme", headers=cocuk_basliklar).status_code == 200
    assert istemci.delete("/ebeveyn/eslesme", headers=cocuk_basliklar).status_code == 200
    assert istemci.get("/ebeveyn/ozet", headers=ebeveyn_basliklar).status_code == 404


def test_kod_alti_haneli(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()

    kod = istemci.post("/ebeveyn/kod", json={"rol": "cocuk"}, headers=basliklar).json()["kod"]

    assert len(kod) == 6 and kod.isdigit()


def test_suresi_dolmus_kod_reddediliyor(istemci, kullanici_kur, db):
    """Tasarım 21: 'Kod 10 dakika geçerlidir' — gerçekten uygulanıyor."""
    cocuk_basliklar, _ = kullanici_kur(ad="Elif", eposta="elif@b.com")
    ebeveyn_basliklar, _ = kullanici_kur(ad="Anne", eposta="anne@b.com")
    kod = istemci.post("/ebeveyn/kod", json={"rol": "cocuk"}, headers=cocuk_basliklar).json()["kod"]

    kayit = db.scalar(select(EslestirmeKodu).where(EslestirmeKodu.kod == kod))
    kayit.son_gecerlilik = dt.datetime.now(dt.timezone.utc) - dt.timedelta(minutes=1)
    db.commit()

    yanit = istemci.post(
        "/ebeveyn/eslestir", json={"kod": kod, "rol": "ebeveyn"}, headers=ebeveyn_basliklar
    )
    assert yanit.status_code == 410


def test_kod_tek_kullanimlik(istemci, kullanici_kur):
    _, ebeveyn_basliklar, kod, _ = _eslestir(istemci, kullanici_kur)

    ikinci = istemci.post(
        "/ebeveyn/eslestir", json={"kod": kod, "rol": "ebeveyn"}, headers=ebeveyn_basliklar
    )
    assert ikinci.status_code == 404


def test_uydurma_kod_reddediliyor(istemci, kullanici_kur):
    """
    Cihazdaki eski hâli yalnızca '6 hane mi' diye bakıyordu; artık kodun
    gerçekten var olması gerekiyor.
    """
    basliklar, _ = kullanici_kur()

    yanit = istemci.post(
        "/ebeveyn/eslestir", json={"kod": "123456", "rol": "ebeveyn"}, headers=basliklar
    )
    assert yanit.status_code == 404


def test_kendi_kodunu_girmek_reddediliyor(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()
    kod = istemci.post("/ebeveyn/kod", json={"rol": "cocuk"}, headers=basliklar).json()["kod"]

    yanit = istemci.post(
        "/ebeveyn/eslestir", json={"kod": kod, "rol": "ebeveyn"}, headers=basliklar
    )
    assert yanit.status_code == 400


def test_ayni_rol_reddediliyor(istemci, kullanici_kur):
    a_basliklar, _ = kullanici_kur(ad="Bir", eposta="bir@b.com")
    b_basliklar, _ = kullanici_kur(ad="Iki", eposta="iki@b.com")
    kod = istemci.post("/ebeveyn/kod", json={"rol": "cocuk"}, headers=a_basliklar).json()["kod"]

    yanit = istemci.post(
        "/ebeveyn/eslestir", json={"kod": kod, "rol": "cocuk"}, headers=b_basliklar
    )
    assert yanit.status_code == 400


def test_yeni_kod_eskisini_iptal_ediyor(istemci, kullanici_kur):
    cocuk_basliklar, _ = kullanici_kur(ad="Elif", eposta="elif@b.com")
    ebeveyn_basliklar, _ = kullanici_kur(ad="Anne", eposta="anne@b.com")

    eski = istemci.post("/ebeveyn/kod", json={"rol": "cocuk"}, headers=cocuk_basliklar).json()["kod"]
    istemci.post("/ebeveyn/kod", json={"rol": "cocuk"}, headers=cocuk_basliklar)

    yanit = istemci.post(
        "/ebeveyn/eslestir", json={"kod": eski, "rol": "ebeveyn"}, headers=ebeveyn_basliklar
    )
    assert yanit.status_code == 404


def test_ozet_esleşme_yokken_404(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()

    assert istemci.get("/ebeveyn/ozet", headers=basliklar).status_code == 404


def test_ozet_cocugun_verisini_donduruyor(istemci, kullanici_kur):
    cocuk_basliklar, ebeveyn_basliklar, _, _ = _eslestir(istemci, kullanici_kur)
    bugun = dt.date.today().isoformat()

    istemci.post(
        "/ozet",
        json={
            "ozetler": [{"tarih": bugun, "socialDk": 200.0, "otherDk": 40.0, "S": 300.0}],
            "riskler": [
                {
                    "tarih": bugun,
                    "S": 300.0,
                    "R": 62.0,
                    "gosterilenBant": "ORTA",
                    "trend": "IYILESIYOR",
                }
            ],
        },
        headers=cocuk_basliklar,
    )

    ozet = istemci.get("/ebeveyn/ozet", headers=ebeveyn_basliklar).json()
    assert ozet["cocukAdi"] == "Elif"
    assert ozet["gunlukTarihler"] == [bugun]
    assert ozet["gunlukDakikalar"] == [240]
    assert ozet["ortalamaDk"] == 240
    assert ozet["bant"] == "ORTA"
    assert ozet["trend"] == "IYILESIYOR"


def test_ozet_ham_veri_icermiyor(istemci, kullanici_kur):
    """
    `docs/spec-veri.md`: ebeveyne ham veri değil ÖZET gösterilir.
    Yanıtta uygulama adı, oturum ya da kategori dökümü olmamalı.
    """
    cocuk_basliklar, ebeveyn_basliklar, _, _ = _eslestir(istemci, kullanici_kur)
    istemci.post(
        "/ozet",
        json={"ozetler": [{"tarih": dt.date.today().isoformat(), "socialDk": 200.0}]},
        headers=cocuk_basliklar,
    )

    ozet = istemci.get("/ebeveyn/ozet", headers=ebeveyn_basliklar).json()

    yasakli = {"socialDk", "paketAdi", "uygulamalar", "oturum", "kategoriler"}
    assert not (set(ozet.keys()) & yasakli)


def test_ozet_yedi_gunden_eski_riski_guncel_gibi_gostermez(istemci, kullanici_kur):
    cocuk_basliklar, ebeveyn_basliklar, _, _ = _eslestir(istemci, kullanici_kur)
    eski_gun = (yerel_bugun() - dt.timedelta(days=8)).isoformat()
    yanit = istemci.post(
        "/ozet",
        json={
            "riskler": [
                {
                    "tarih": eski_gun,
                    "S": 300.0,
                    "R": 62.0,
                    "gosterilenBant": "ORTA",
                    "trend": "IYILESIYOR",
                }
            ]
        },
        headers=cocuk_basliklar,
    )
    assert yanit.status_code == 200

    ozet = istemci.get("/ebeveyn/ozet", headers=ebeveyn_basliklar).json()

    assert ozet["bant"] is None
    assert ozet["trend"] is None


def test_bir_cihaz_ayni_anda_yalnizca_bir_eslesmede_olabilir(istemci, kullanici_kur):
    _, ebeveyn_basliklar, _, _ = _eslestir(istemci, kullanici_kur)
    ikinci_cocuk, _ = kullanici_kur(ad="Can", eposta="can@b.com")
    kod = istemci.post(
        "/ebeveyn/kod", json={"rol": "cocuk"}, headers=ikinci_cocuk
    ).json()["kod"]

    yanit = istemci.post(
        "/ebeveyn/eslestir", json={"kod": kod, "rol": "ebeveyn"}, headers=ebeveyn_basliklar
    )

    assert yanit.status_code == 409
    assert istemci.get("/ebeveyn/ozet", headers=ebeveyn_basliklar).json()["cocukAdi"] == "Elif"


def test_heartbeat_saat_hesabi(istemci, kullanici_kur):
    cocuk_basliklar, ebeveyn_basliklar, _, _ = _eslestir(istemci, kullanici_kur)
    istemci.post("/heartbeat", headers=cocuk_basliklar)

    ozet = istemci.get("/ebeveyn/ozet", headers=ebeveyn_basliklar).json()

    assert ozet["sonSinyalSaatOnce"] == 0
