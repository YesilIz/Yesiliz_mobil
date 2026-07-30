"""Lig gruplama ve arkadaşlık."""

from __future__ import annotations

import datetime as dt

from sqlalchemy import select

from app.ayarlar import ayarlar
from app.modeller import LigGrup, LigUyelik
from app.uclar.lig import hafta_baslangici


def test_hafta_baslangici_pazartesi():
    """`Aralik.HAFTA` ve `BildirimGrupla` ile aynı tanım — ISO haftası."""
    # 2026-07-31 Cuma → 2026-07-27 Pazartesi
    assert hafta_baslangici(dt.date(2026, 7, 31)) == dt.date(2026, 7, 27)
    assert hafta_baslangici(dt.date(2026, 7, 27)) == dt.date(2026, 7, 27)
    # Pazar hâlâ aynı haftada.
    assert hafta_baslangici(dt.date(2026, 8, 2)) == dt.date(2026, 7, 27)


def test_katilmamis_kullanici_bos_siralama_goruyor(istemci, kullanici_kur):
    """Tasarım 37 — boş durum. Sahte sıralama gösterilmiyor."""
    basliklar, _ = kullanici_kur()

    yanit = istemci.get("/lig", headers=basliklar).json()

    assert yanit["katildiMi"] is False
    assert yanit["siralama"] == []


def test_katilinca_siralamada_gorunuyor(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()

    yanit = istemci.post("/lig/katil", headers=basliklar).json()

    assert yanit["katildiMi"] is True
    assert len(yanit["siralama"]) == 1
    assert yanit["siralama"][0]["benMiyim"] is True


def test_yeni_kullanici_fidan_liginde(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()

    yanit = istemci.post("/lig/katil", headers=basliklar).json()

    assert yanit["ligAdi"] == "Fidan Ligi"


def test_iki_kez_katilmak_ikinci_uyelik_acmiyor(istemci, kullanici_kur, db):
    basliklar, _ = kullanici_kur()

    istemci.post("/lig/katil", headers=basliklar)
    istemci.post("/lig/katil", headers=basliklar)

    assert len(db.scalars(select(LigUyelik)).all()) == 1


def test_grup_dolunca_yeni_grup_aciliyor(istemci, kullanici_kur, db):
    boy = ayarlar().lig_grup_boyu

    for i in range(boy + 1):
        basliklar, _ = kullanici_kur(ad=f"Kisi{i}", eposta=f"k{i}@b.com")
        istemci.post("/lig/katil", headers=basliklar)

    gruplar = db.scalars(select(LigGrup)).all()
    assert len(gruplar) == 2


def test_siralama_puana_gore(istemci, kullanici_kur):
    a_basliklar, _ = kullanici_kur(ad="Az", eposta="az@b.com")
    b_basliklar, _ = kullanici_kur(ad="Cok", eposta="cok@b.com")
    istemci.post("/lig/katil", headers=a_basliklar)
    istemci.post("/lig/katil", headers=b_basliklar)

    istemci.post("/ozet", json={"haftalikPuan": 100}, headers=a_basliklar)
    istemci.post("/ozet", json={"haftalikPuan": 900}, headers=b_basliklar)

    siralama = istemci.get("/lig", headers=a_basliklar).json()["siralama"]
    assert siralama[0]["isim"] == "Cok"
    assert siralama[1]["isim"] == "Az"


def test_davet_kodu_kalici(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()

    ilk = istemci.get("/arkadas/kod", headers=basliklar).json()["kod"]
    ikinci = istemci.get("/arkadas/kod", headers=basliklar).json()["kod"]

    # Kod değişirse paylaşılmış davet geçersizleşir.
    assert ilk == ikinci
    assert ilk.startswith("YSZ-")


def test_davet_kodu_karistirilabilir_karakter_icermiyor(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()

    kod = istemci.get("/arkadas/kod", headers=basliklar).json()["kod"]

    # Cihazdaki `DavetKodu.kt` ile aynı alfabe: I, O, 0, 1 yok.
    govde = kod.removeprefix("YSZ-")
    assert not (set(govde) & {"I", "O", "0", "1"})


def test_davetle_arkadaslik_karsilikli(istemci, kullanici_kur):
    a_basliklar, _ = kullanici_kur(ad="Ali", eposta="ali@b.com")
    b_basliklar, _ = kullanici_kur(ad="Veli", eposta="veli@b.com")
    kod = istemci.get("/arkadas/kod", headers=a_basliklar).json()["kod"]

    istemci.post("/arkadas/davet", json={"kod": kod}, headers=b_basliklar)

    a_arkadaslar = istemci.get("/arkadas", headers=a_basliklar).json()
    b_arkadaslar = istemci.get("/arkadas", headers=b_basliklar).json()
    assert [a["isim"] for a in a_arkadaslar] == ["Veli"]
    assert [a["isim"] for a in b_arkadaslar] == ["Ali"]


def test_kendi_kodunu_kullanmak_reddediliyor(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()
    kod = istemci.get("/arkadas/kod", headers=basliklar).json()["kod"]

    yanit = istemci.post("/arkadas/davet", json={"kod": kod}, headers=basliklar)

    assert yanit.status_code == 400


def test_olmayan_kod_reddediliyor(istemci, kullanici_kur):
    basliklar, _ = kullanici_kur()

    yanit = istemci.post("/arkadas/davet", json={"kod": "YSZ-ZZZZ"}, headers=basliklar)

    assert yanit.status_code == 404


def test_arkadas_ligde_isaretleniyor(istemci, kullanici_kur):
    a_basliklar, _ = kullanici_kur(ad="Ali", eposta="ali@b.com")
    b_basliklar, _ = kullanici_kur(ad="Veli", eposta="veli@b.com")
    kod = istemci.get("/arkadas/kod", headers=a_basliklar).json()["kod"]
    istemci.post("/arkadas/davet", json={"kod": kod}, headers=b_basliklar)
    istemci.post("/lig/katil", headers=a_basliklar)
    istemci.post("/lig/katil", headers=b_basliklar)

    siralama = istemci.get("/lig", headers=a_basliklar).json()["siralama"]
    veli = next(s for s in siralama if s["isim"] == "Veli")
    assert veli["arkadasMi"] is True
