"""Kimlik doğrulama uçları."""

from __future__ import annotations

import datetime as dt

from sqlalchemy import select

from app.modeller import Kullanici, SifreSifirlama, YenilemeJetonu


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


def test_kisa_sifre_reddediliyor(istemci):
    yanit = istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "kisa"})

    assert yanit.status_code == 422


def test_yenileme_jetonu_tek_kullanimlik(istemci):
    kayit = istemci.post(
        "/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"}
    ).json()

    ilk = istemci.post("/auth/yenile", json={"yenileme": kayit["yenileme"]})
    ikinci = istemci.post("/auth/yenile", json={"yenileme": kayit["yenileme"]})

    assert ilk.status_code == 200
    # Çalınan jeton ikinci kez kullanılamıyor.
    assert ikinci.status_code == 401


def test_suresi_dolmus_yenileme_reddediliyor(istemci, db):
    kayit = istemci.post(
        "/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"}
    ).json()

    jeton = db.scalar(select(YenilemeJetonu).where(YenilemeJetonu.jeton == kayit["yenileme"]))
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


def test_sifre_yenileme_akisi(istemci, db):
    istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"})
    istemci.post("/auth/sifre-sifirla", json={"eposta": "a@b.com"})

    kayit = db.scalar(select(SifreSifirlama))
    yanit = istemci.post(
        "/auth/sifre-yenile", json={"jeton": kayit.jeton, "yeniSifre": "yeniparola1"}
    )

    assert yanit.status_code == 200
    assert istemci.post("/auth/giris", json={"eposta": "a@b.com", "sifre": "yeniparola1"}).status_code == 200
    assert istemci.post("/auth/giris", json={"eposta": "a@b.com", "sifre": "parola123"}).status_code == 401


def test_sifirlama_jetonu_tek_kullanimlik(istemci, db):
    istemci.post("/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"})
    istemci.post("/auth/sifre-sifirla", json={"eposta": "a@b.com"})
    kayit = db.scalar(select(SifreSifirlama))

    istemci.post("/auth/sifre-yenile", json={"jeton": kayit.jeton, "yeniSifre": "yeniparola1"})
    ikinci = istemci.post(
        "/auth/sifre-yenile", json={"jeton": kayit.jeton, "yeniSifre": "baskasi123"}
    )

    assert ikinci.status_code == 400


def test_sifre_degisince_oturumlar_kapaniyor(istemci, db):
    kayit = istemci.post(
        "/auth/kayit", json={"ad": "Turan", "eposta": "a@b.com", "sifre": "parola123"}
    ).json()
    istemci.post("/auth/sifre-sifirla", json={"eposta": "a@b.com"})
    sifirlama = db.scalar(select(SifreSifirlama))
    istemci.post("/auth/sifre-yenile", json={"jeton": sifirlama.jeton, "yeniSifre": "yeniparola1"})

    # Hesabı ele geçiren birinin elindeki yenileme jetonu da geçersizleşmeli.
    assert istemci.post("/auth/yenile", json={"yenileme": kayit["yenileme"]}).status_code == 401
