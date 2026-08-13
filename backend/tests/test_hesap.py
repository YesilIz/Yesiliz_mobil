"""Kimlik doğrulama uçları."""

from __future__ import annotations

import datetime as dt
from types import SimpleNamespace

import pytest
from fastapi import HTTPException
from sqlalchemy import select

from app.guvenlik import jeton_ozetle, kullaniciyi_kilitle
from app.modeller import GunlukOzet, Kullanici, OdulSecimi, SifreSifirlama, YenilemeJetonu
from app.uclar import hesap


def test_kayit_jeton_donduruyor(istemci):
    yanit = istemci.post(
        "/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"}
    )

    assert yanit.status_code == 201
    veri = yanit.json()
    assert veri["erisim"] and veri["yenileme"]
    assert veri["ad"] == "Turan"


def test_sifre_duz_metin_saklanmiyor(istemci, db):
    istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"})

    kullanici = db.scalar(select(Kullanici).where(Kullanici.eposta == "a@b.com"))
    assert "parola123" not in kullanici.sifre_ozet
    assert kullanici.sifre_ozet.startswith("$argon2")


def test_ayni_eposta_ikinci_kez_kabul_edilmiyor(istemci):
    istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"})
    yanit = istemci.post(
        "/auth/kayit", json={"ad": "Başkası", "eposta": "a@b.com", "sifre": "parola456"}
    )

    assert yanit.status_code == 409


def test_eposta_buyuk_kucuk_harf_ayirmiyor(istemci):
    istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"})
    yanit = istemci.post("/auth/giris", json={"eposta": "A@B.COM", "sifre": "parola123"})

    assert yanit.status_code == 200


def test_yanlis_sifre_reddediliyor(istemci):
    istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"})
    yanit = istemci.post("/auth/giris", json={"eposta": "a@b.com", "sifre": "yanlis123"})

    assert yanit.status_code == 401


def test_olmayan_kullanici_ile_yanlis_sifre_ayni_mesaji_veriyor(istemci):
    """Hangi e-postaların kayıtlı olduğu sızmamalı."""
    istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"})

    yok = istemci.post("/auth/giris", json={"eposta": "yok@b.com", "sifre": "parola123"})
    yanlis = istemci.post("/auth/giris", json={"eposta": "a@b.com", "sifre": "yanlis123"})

    assert yok.status_code == yanlis.status_code == 401
    assert yok.json()["detail"] == yanlis.json()["detail"]


def test_olmayan_kullanici_da_argon2_dogrulamasindan_geciyor(istemci, monkeypatch):
    """Aynı yanıtın yanında hesaplama yolu da zamanlama sızıntısı vermemeli."""
    gorulen_ozetler: list[str | None] = []

    def izle(ozet: str | None, _sifre: str) -> bool:
        gorulen_ozetler.append(ozet)
        return False

    monkeypatch.setattr(hesap, "sifre_dogrula", izle)

    yanit = istemci.post(
        "/auth/giris",
        json={"eposta": "olmayan@example.com", "sifre": "parola123"},
    )

    assert yanit.status_code == 401
    assert gorulen_ozetler == [None]


def test_kisa_sifre_reddediliyor(istemci):
    yanit = istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "kisa"})

    assert yanit.status_code == 422


def test_bos_ad_ve_asiri_uzun_giris_sifresi_reddediliyor(istemci):
    bos_ad = istemci.post(
        "/auth/kayit", json={"ad": "   ", "eposta": "a@b.com", "sifre": "parola123"}
    )
    uzun_sifre = istemci.post(
        "/auth/giris", json={"eposta": "a@b.com", "sifre": "x" * 129}
    )

    assert bos_ad.status_code == 422
    assert uzun_sifre.status_code == 422


def test_yenileme_jetonu_tek_kullanimlik(istemci):
    kayit = istemci.post(
        "/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"}
    ).json()

    ilk = istemci.post("/auth/yenile", json={"yenileme": kayit["yenileme"]})
    ikinci = istemci.post("/auth/yenile", json={"yenileme": kayit["yenileme"]})

    assert ilk.status_code == 200
    # Çalınan jeton ikinci kez kullanılamıyor.
    assert ikinci.status_code == 401


def test_cikis_tum_yenileme_jetonlarini_iptal_ediyor(istemci):
    ilk = istemci.post(
        "/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"}
    ).json()
    # İkinci cihaz/oturum gibi ikinci bir yenileme jetonu oluştur.
    ikinci = istemci.post(
        "/auth/giris", json={"eposta": "a@b.com", "sifre": "parola123"}
    ).json()

    yanit = istemci.post(
        "/auth/cikis", headers={"Authorization": f"Bearer {ikinci['erisim']}"}
    )

    assert yanit.status_code == 200
    assert istemci.post("/auth/yenile", json={"yenileme": ilk["yenileme"]}).status_code == 401
    assert istemci.post("/auth/yenile", json={"yenileme": ikinci["yenileme"]}).status_code == 401


def test_suresi_dolmus_yenileme_reddediliyor(istemci, db):
    kayit = istemci.post(
        "/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"}
    ).json()

    jeton = db.scalar(
        select(YenilemeJetonu).where(
            YenilemeJetonu.jeton == jeton_ozetle(kayit["yenileme"])
        )
    )
    jeton.son_gecerlilik = dt.datetime.now(dt.timezone.utc) - dt.timedelta(days=1)
    db.commit()

    assert istemci.post("/auth/yenile", json={"yenileme": kayit["yenileme"]}).status_code == 401


def test_jetonsuz_istek_401(istemci):
    assert istemci.get("/lig").status_code == 401


def test_gecersiz_jeton_401(istemci):
    yanit = istemci.get("/lig", headers={"Authorization": "Bearer uydurma"})

    assert yanit.status_code == 401


def test_sifre_sifirlama_kayitsiz_epostada_da_202(istemci):
    """Hangi adreslerin kayıtlı olduğu sızmamalı."""
    yanit = istemci.post("/auth/sifre-sifirla", json={"eposta": "yok@b.com"})

    assert yanit.status_code == 202


def test_sifre_sifirlama_uretimde_eposta_yokken_basari_taklidi_yapmiyor(
    istemci, db, monkeypatch
):
    istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"})
    monkeypatch.setattr(
        hesap,
        "ayarlar",
        lambda: SimpleNamespace(uretim_mi=True, sifirlama_saat=1),
    )

    yanit = istemci.post("/auth/sifre-sifirla", json={"eposta": "a@b.com"})

    assert yanit.status_code == 503
    assert db.scalar(select(SifreSifirlama)) is None


def test_sifre_yenileme_akisi(istemci, db, monkeypatch):
    ham_jeton = "sifirlama-icin-yalnizca-test-jetonu"
    uretilenler = iter(("kayit-yenileme-test-jetonu", ham_jeton, "giris-yenileme-test-jetonu"))
    monkeypatch.setattr(hesap, "rastgele_jeton", lambda: next(uretilenler))
    istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"})
    istemci.post("/auth/sifre-sifirla", json={"eposta": "a@b.com"})

    kayit = db.scalar(select(SifreSifirlama))
    assert kayit.jeton == jeton_ozetle(ham_jeton)
    assert kayit.jeton != ham_jeton
    yanit = istemci.post(
        "/auth/sifre-yenile", json={"jeton": ham_jeton, "yeniSifre": "yeniparola1"}
    )

    assert yanit.status_code == 200
    assert istemci.post("/auth/giris", json={"eposta": "a@b.com", "sifre": "yeniparola1"}).status_code == 200
    assert istemci.post("/auth/giris", json={"eposta": "a@b.com", "sifre": "parola123"}).status_code == 401


def test_sifirlama_jetonu_tek_kullanimlik(istemci, db, monkeypatch):
    ham_jeton = "tek-kullanim-icin-test-jetonu"
    uretilenler = iter(("kayit-yenileme-test-jetonu", ham_jeton))
    monkeypatch.setattr(hesap, "rastgele_jeton", lambda: next(uretilenler))
    istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"})
    istemci.post("/auth/sifre-sifirla", json={"eposta": "a@b.com"})

    istemci.post("/auth/sifre-yenile", json={"jeton": ham_jeton, "yeniSifre": "yeniparola1"})
    ikinci = istemci.post(
        "/auth/sifre-yenile", json={"jeton": ham_jeton, "yeniSifre": "baskasi123"}
    )

    assert ikinci.status_code == 400


def test_sifre_degisince_oturumlar_kapaniyor(istemci, db, monkeypatch):
    uretilenler = iter(("ilk-yenileme-test-jetonu", "sifirlama-test-jetonu"))
    monkeypatch.setattr(hesap, "rastgele_jeton", lambda: next(uretilenler))
    kayit = istemci.post(
        "/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"}
    ).json()
    istemci.post("/auth/sifre-sifirla", json={"eposta": "a@b.com"})
    sifirlama = db.scalar(select(SifreSifirlama))
    assert sifirlama.jeton == jeton_ozetle("sifirlama-test-jetonu")
    istemci.post(
        "/auth/sifre-yenile",
        json={"jeton": "sifirlama-test-jetonu", "yeniSifre": "yeniparola1"},
    )

    # Hesabı ele geçiren birinin elindeki yenileme jetonu da geçersizleşmeli.
    assert istemci.post("/auth/yenile", json={"yenileme": kayit["yenileme"]}).status_code == 401


def test_yenileme_jetonu_veritabaninda_duz_metin_degil(istemci, db):
    yanit = istemci.post(
        "/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"}
    ).json()

    saklanan = db.scalar(select(YenilemeJetonu)).jeton
    assert saklanan == jeton_ozetle(yanit["yenileme"])
    assert saklanan != yanit["yenileme"]


def test_hesap_silinince_kullanici_ve_ozetler_gidiyor(istemci, kullanici_kur, db):
    baslik, _ = kullanici_kur()
    istemci.post(
        "/ozet",
        headers=baslik,
        json={
            "ozetler": [
                {
                    "tarih": "2026-07-30",
                    "toplamDk": 180.0,
                    "acilisSayisi": 40,
                    "geceKullanimDk": 20.0,
                    "sosyalDk": 60.0,
                    "eglenceDk": 40.0,
                }
            ],
            "riskler": [],
            "haftalikPuan": 100,
        },
    )
    kullanici = db.scalar(select(Kullanici))
    db.add(
        OdulSecimi(
            kullanici_id=kullanici.id,
            kademe_id="kupon_market",
            secenek_id="fidan_bagisi",
        )
    )
    db.commit()
    assert db.scalar(select(GunlukOzet)) is not None
    assert db.scalar(select(OdulSecimi)) is not None

    yanit = istemci.delete("/auth/hesap", headers=baslik)

    assert yanit.status_code == 200
    assert db.scalar(select(Kullanici)) is None
    assert db.scalar(select(GunlukOzet)) is None
    assert db.scalar(select(OdulSecimi)) is None
    # Yenileme jetonu da gitmeli: kalsaydı silinen hesaba jeton üretilebilirdi.
    assert db.scalar(select(YenilemeJetonu)) is None


def test_profil_ad_ve_avatar_sunucuda_guncelleniyor(istemci, kullanici_kur, db):
    baslik, _ = kullanici_kur()

    yanit = istemci.patch(
        "/auth/profil", json={"ad": "Turan K", "avatar": "YAPRAK"}, headers=baslik
    )

    assert yanit.status_code == 200, yanit.text
    assert yanit.json() == {"ad": "Turan K", "eposta": "turan@example.com", "avatar": "YAPRAK"}
    kullanici = db.scalar(select(Kullanici).where(Kullanici.eposta == "turan@example.com"))
    db.refresh(kullanici)
    assert (kullanici.ad, kullanici.avatar) == ("Turan K", "YAPRAK")


def test_profil_avatari_sonraki_giriste_geri_geliyor(istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    istemci.patch("/auth/profil", json={"ad": "Turan", "avatar": "AGAC"}, headers=baslik)

    giris = istemci.post(
        "/auth/giris", json={"eposta": "turan@example.com", "sifre": "parola123"}
    ).json()

    assert giris["avatar"] == "AGAC"


def test_profilden_sifre_degistirince_yeni_sifre_geciyor(istemci, kullanici_kur):
    baslik, _ = kullanici_kur()

    yanit = istemci.patch(
        "/auth/profil",
        json={"ad": "Turan", "mevcutSifre": "parola123", "yeniSifre": "yeniparola1"},
        headers=baslik,
    )

    assert yanit.status_code == 200, yanit.text
    assert istemci.post(
        "/auth/giris", json={"eposta": "turan@example.com", "sifre": "yeniparola1"}
    ).status_code == 200
    assert istemci.post(
        "/auth/giris", json={"eposta": "turan@example.com", "sifre": "parola123"}
    ).status_code == 401


def test_sifre_degistirmek_mevcut_sifreyi_istiyor(istemci, kullanici_kur):
    baslik, _ = kullanici_kur()

    eksik = istemci.patch(
        "/auth/profil", json={"ad": "Turan", "yeniSifre": "yeniparola1"}, headers=baslik
    )
    yanlis = istemci.patch(
        "/auth/profil",
        json={"ad": "Turan", "mevcutSifre": "baskasifre", "yeniSifre": "yeniparola1"},
        headers=baslik,
    )

    assert eksik.status_code == 400
    assert yanlis.status_code == 401
    # Reddedilen istek adı da değiştirmemeli: güncelleme ya hep ya hiç.
    assert istemci.post(
        "/auth/giris", json={"eposta": "turan@example.com", "sifre": "parola123"}
    ).status_code == 200


def test_profil_guncelleme_oturum_istiyor(istemci):
    istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"})

    assert istemci.patch("/auth/profil", json={"ad": "Başkası"}).status_code == 401


def test_profil_ucu_epostayi_degistirmiyor(istemci, kullanici_kur, db):
    """E-posta doğrulaması yok (A15); değiştirilmesi hesabı kilitleyebilirdi."""
    baslik, _ = kullanici_kur()

    istemci.patch(
        "/auth/profil",
        json={"ad": "Turan", "eposta": "yeni@example.com"},
        headers=baslik,
    )

    assert db.scalar(select(Kullanici).where(Kullanici.eposta == "yeni@example.com")) is None
    assert db.scalar(select(Kullanici).where(Kullanici.eposta == "turan@example.com")) is not None


def test_hesap_silme_oturum_istiyor(istemci):
    istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"})

    assert istemci.delete("/auth/hesap").status_code == 401


def test_silinen_hesabin_jetonu_artik_calismiyor(istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    istemci.delete("/auth/hesap", headers=baslik)

    # Erişim jetonu hâlâ süresi içinde ama kullanıcı yok — 401 dönmeli.
    assert istemci.get("/ozet", headers=baslik).status_code == 401


def test_yazma_kilidi_arada_silinen_hesabi_401_ile_reddeder(db):
    """Eski kimlik nesnesiyle FK yazıp 500 üretmek yerine oturum kapanır."""
    kullanici = Kullanici(
        ad="Yarış",
        eposta="yaris@example.com",
        sifre_ozet="testte-dogrulanmayacak",
    )
    db.add(kullanici)
    db.commit()
    kullanici_id = kullanici.id
    db.delete(kullanici)
    db.commit()

    with pytest.raises(HTTPException) as yakalanan:
        kullaniciyi_kilitle(db, kullanici_id)

    assert yakalanan.value.status_code == 401
