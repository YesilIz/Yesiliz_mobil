"""Günlük özet senkronu."""

from __future__ import annotations

import datetime as dt

from sqlalchemy import func, select

from app.modeller import GunlukOzet, Heartbeat, RiskSonuc


def _ozet(tarih: str, social=100.0, other=20.0):
    return {"tarih": tarih, "socialDk": social, "otherDk": other, "S": 42.0}


def test_ozet_yaziliyor(istemci, kullanici_kur, db):
    basliklar, _ = kullanici_kur()

    yanit = istemci.post("/ozet", json={"ozetler": [_ozet("2026-07-30")]}, headers=basliklar)

    assert yanit.status_code == 200
    assert yanit.json()["ozetSayisi"] == 1
    satir = db.scalar(select(GunlukOzet))
    assert satir.social_dk == 100.0
    assert satir.toplam_ekran_dk == 120.0


def test_ayni_gun_tekrar_gonderilince_satir_katlanmiyor(istemci, kullanici_kur, db):
    """
    Cihaz gecelik worker'da geriye dönük 7 günü de gönderiyor
    (`GunlukOzetWorker.TELAFI_GUN`), yani tekrar kaçınılmaz.
    """
    basliklar, _ = kullanici_kur()

    istemci.post("/ozet", json={"ozetler": [_ozet("2026-07-30", social=100.0)]}, headers=basliklar)
    istemci.post("/ozet", json={"ozetler": [_ozet("2026-07-30", social=180.0)]}, headers=basliklar)

    assert db.scalar(select(func.count()).select_from(GunlukOzet)) == 1
    assert db.scalar(select(GunlukOzet)).social_dk == 180.0


def test_risk_sonucu_yaziliyor(istemci, kullanici_kur, db):
    basliklar, _ = kullanici_kur()

    istemci.post(
        "/ozet",
        json={
            "riskler": [
                {
                    "tarih": "2026-07-30",
                    "S": 210.0,
                    "R": 55.0,
                    "gosterilenBant": "ORTA",
                    "trend": "SABIT",
                }
            ]
        },
        headers=basliklar,
    )

    satir = db.scalar(select(RiskSonuc))
    assert satir.gosterilen_bant == "ORTA"
    assert satir.r == 55.0


def test_ozet_kullaniciya_kapali(istemci, kullanici_kur):
    """Bir kullanıcı başkasının özetini göremez."""
    a_basliklar, _ = kullanici_kur(eposta="a@b.com")
    istemci.post("/ozet", json={"ozetler": [_ozet("2026-07-30")]}, headers=a_basliklar)

    b_basliklar, _ = kullanici_kur(ad="Başkası", eposta="b@b.com")
    yanit = istemci.get(
        "/ozet", params={"bas": "2026-07-01", "bit": "2026-08-01"}, headers=b_basliklar
    )

    assert yanit.status_code == 200
    assert yanit.json() == []


def test_ozet_geri_yukleme(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()
    istemci.post(
        "/ozet",
        json={"ozetler": [_ozet("2026-07-29"), _ozet("2026-07-30")]},
        headers=basliklar,
    )

    yanit = istemci.get(
        "/ozet", params={"bas": "2026-07-29", "bit": "2026-07-30"}, headers=basliklar
    )

    assert len(yanit.json()) == 2
    assert yanit.json()[0]["tarih"] == "2026-07-29"


def test_senkron_heartbeat_de_yaziyor(istemci, kullanici_kur, db):
    basliklar, _ = kullanici_kur()

    istemci.post("/ozet", json={"ozetler": [_ozet("2026-07-30")]}, headers=basliklar)

    assert db.scalar(select(func.count()).select_from(Heartbeat)) == 1


def test_heartbeat_ucu(istemci, kullanici_kur, db):
    basliklar, _ = kullanici_kur()

    assert istemci.post("/heartbeat", headers=basliklar).status_code == 200
    kayit = db.scalar(select(Heartbeat))
    assert kayit is not None


def test_haftalik_puan_geri_alinmiyor(istemci, kullanici_kur, db):
    """
    CLAUDE.md §4: ceza yok. Cihaz verisi silinip yeniden kurulsa ve düşük
    puan gönderse bile ligdeki puan geri alınmıyor.
    """
    basliklar, _ = kullanici_kur()
    istemci.post("/lig/katil", headers=basliklar)

    istemci.post("/ozet", json={"haftalikPuan": 500}, headers=basliklar)
    istemci.post("/ozet", json={"haftalikPuan": 10}, headers=basliklar)

    lig = istemci.get("/lig", headers=basliklar).json()
    assert lig["siralama"][0]["puan"] == 500


def test_senkron_negatif_sonsuz_ve_bir_gunden_fazla_veriyi_reddediyor(
    istemci, kullanici_kur
):
    basliklar, _ = kullanici_kur()

    gecersiz_ozetler = [
        _ozet("2026-08-10", social=-1),
        _ozet("2026-08-10", social="Infinity"),
        {
            "tarih": "2026-08-10",
            "socialDk": 800,
            "entertainmentDk": 800,
        },
        {"tarih": "2026-08-10", "gece": 1.01},
    ]

    for ozet in gecersiz_ozetler:
        yanit = istemci.post("/ozet", json={"ozetler": [ozet]}, headers=basliklar)
        assert yanit.status_code == 422


def test_senkron_liste_ve_deger_tavanlarini_reddediyor(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()
    sekiz_gun = [_ozet(f"2026-08-{gun:02d}") for gun in range(3, 11)]

    assert istemci.post("/ozet", json={"ozetler": sekiz_gun}, headers=basliklar).status_code == 422
    assert (
        istemci.post("/ozet", json={"haftalikPuan": 2001}, headers=basliklar).status_code
        == 422
    )
    assert (
        istemci.post(
            "/ozet",
            json={
                "riskler": [
                    {
                        "tarih": "2026-08-10",
                        "S": 100,
                        "R": 101,
                        "gosterilenBant": "ORTA",
                        "trend": "SABIT",
                    }
                ]
            },
            headers=basliklar,
        ).status_code
        == 422
    )


def test_senkron_uzak_tarihi_reddediyor(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()

    yanit = istemci.post(
        "/ozet", json={"ozetler": [_ozet("2030-01-01")]}, headers=basliklar
    )

    assert yanit.status_code == 422


def test_ozet_geri_yukleme_tarih_araligini_sinirliyor(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()

    ters = istemci.get(
        "/ozet", params={"bas": "2026-08-10", "bit": "2026-08-01"}, headers=basliklar
    )
    cok_genis = istemci.get(
        "/ozet", params={"bas": "2020-01-01", "bit": "2026-08-01"}, headers=basliklar
    )

    assert ters.status_code == 422
    assert cok_genis.status_code == 422
