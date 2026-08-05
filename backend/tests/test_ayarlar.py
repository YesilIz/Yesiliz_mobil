"""Ortam yapılandırması — açılış doğrulaması ve takma adlar."""

from __future__ import annotations

import pytest

from app.ayarlar import (
    VARSAYILAN_JWT_ANAHTARI,
    Ayarlar,
    Ortam,
    YapilandirmaHatasi,
)

GUVENLI_ANAHTAR = "x" * 48
PG = "postgresql+psycopg://k:p@sunucu:5432/yesiliz"

# Önekli adlar kullanılıyor: ortam değişkeni ile alan adı çakıştığında
# takma ad kazanıyor ve `conftest` zaten `YESILIZ_*` yazmış durumda.
ONEKLI = {
    "YESILIZ_ORTAM": Ortam.URETIM.value,
    "YESILIZ_JWT_ANAHTARI": GUVENLI_ANAHTAR,
    "YESILIZ_VERITABANI_URL": PG,
    "YESILIZ_IZINLI_KAYNAKLAR": "",
}


def _ayar(**ek) -> Ayarlar:
    return Ayarlar(**{**ONEKLI, **ek})


def test_uretim_gecerli_yapilandirmayla_basliyor():
    _ayar().dogrula()


def test_uretim_varsayilan_jwt_anahtariyla_baslamiyor():
    with pytest.raises(YapilandirmaHatasi) as hata:
        _ayar(YESILIZ_JWT_ANAHTARI=VARSAYILAN_JWT_ANAHTARI).dogrula()
    assert "JWT" in str(hata.value)


def test_uretim_kisa_jwt_anahtariyla_baslamiyor():
    with pytest.raises(YapilandirmaHatasi):
        _ayar(YESILIZ_JWT_ANAHTARI="kisa").dogrula()


def test_hata_mesaji_anahtarin_kendisini_yazmiyor():
    """Hata mesajı da bir log; gizli değer oraya girmemeli."""
    with pytest.raises(YapilandirmaHatasi) as hata:
        _ayar(YESILIZ_JWT_ANAHTARI="kisa-ama-gizli").dogrula()
    assert "kisa-ama-gizli" not in str(hata.value)


def test_uretim_sqlite_ile_baslamiyor():
    with pytest.raises(YapilandirmaHatasi) as hata:
        _ayar(YESILIZ_VERITABANI_URL="sqlite+pysqlite:///./yesiliz.db").dogrula()
    assert "SQLite" in str(hata.value)


def test_gelistirme_varsayilanlarla_basliyor():
    """Yerel geliştirme kapıya takılmamalı — kural yalnızca üretim için."""
    _ayar(
        YESILIZ_ORTAM=Ortam.GELISTIRME.value,
        YESILIZ_JWT_ANAHTARI=VARSAYILAN_JWT_ANAHTARI,
        YESILIZ_VERITABANI_URL="sqlite+pysqlite:///./yesiliz.db",
    ).dogrula()


@pytest.mark.parametrize(
    "gelen",
    ["postgresql://k:p@s:5432/d", "postgres://k:p@s:5432/d"],
)
def test_saglayicinin_url_semasi_psycopg3e_cevriliyor(gelen: str):
    """Railway `postgresql://` yazıyor; SQLAlchemy orada psycopg2 arıyor."""
    assert _ayar(YESILIZ_VERITABANI_URL=gelen).veritabani_url.startswith(
        "postgresql+psycopg://"
    )


def test_acikca_verilen_surucu_korunuyor():
    assert _ayar(YESILIZ_VERITABANI_URL=PG).veritabani_url == PG


def test_izinli_kaynaklar_virgulle_ayriliyor():
    a = _ayar(YESILIZ_IZINLI_KAYNAKLAR="https://a.example, https://b.example ,")
    assert a.izinli_kaynak_listesi == ["https://a.example", "https://b.example"]


def test_izinli_kaynak_yoksa_liste_bos():
    assert _ayar().izinli_kaynak_listesi == []


def test_standart_isimler_ortamdan_okunuyor(monkeypatch):
    """
    Sağlayıcı `DATABASE_URL`'i kendisi enjekte ediyor ve adı değiştirilemiyor.

    Bu test düşerse dağıtım sessizce SQLite'a düşer — üretim doğrulaması
    onu yakalar ama sebebi görünmez olur.
    """
    for ad in ONEKLI:
        monkeypatch.delenv(ad, raising=False)
    monkeypatch.setenv("ENVIRONMENT", "production")
    monkeypatch.setenv("DATABASE_URL", "postgresql://k:p@s:5432/d")
    monkeypatch.setenv("JWT_SECRET", GUVENLI_ANAHTAR)
    monkeypatch.setenv("ALLOWED_ORIGINS", "https://panel.example")
    monkeypatch.setenv("ACCESS_TOKEN_EXPIRE_MINUTES", "15")

    a = Ayarlar()

    assert a.uretim_mi
    assert a.veritabani_url == "postgresql+psycopg://k:p@s:5432/d"
    assert a.jwt_anahtari == GUVENLI_ANAHTAR
    assert a.erisim_dakika == 15
    assert a.izinli_kaynak_listesi == ["https://panel.example"]
    a.dogrula()


def test_onekli_isim_standart_ismi_yeniyor(monkeypatch):
    monkeypatch.setenv("DATABASE_URL", "postgresql://standart:p@s:5432/d")
    assert _ayar().veritabani_url == PG
