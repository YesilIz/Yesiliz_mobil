"""
Servis rolü ayrımı — `YESILIZ_ROL` (CLAUDE.md §12 K57).

Panelin en temel güvenlik iddiası şu: **panel uçları API alan adında hiç
mount edilmiyor.** Bu dosya o iddiayı sınıyor. Sınanabilir olması için
`app/main.py`'nin uygulama fabrikasına çevrilmesi gerekti — modül import
anında `ayarlar()` okuduğu ve `ayarlar()` `lru_cache`'li olduğu sürece iki
rolü tek pytest oturumunda kurmak mümkün değildi.
"""

from __future__ import annotations

import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from app.ayarlar import (
    VARSAYILAN_PANEL_SIRRI,
    VARSAYILAN_PANEL_TOTP_ANAHTARI,
    Ayarlar,
    Ortam,
    SunucuRolu,
)
from app.main import uygulama_kur

GUVENLI_ANAHTAR = "x" * 48
PG = "postgresql+psycopg://k:p@sunucu:5432/yesiliz"

PANEL_URETIM = {
    "YESILIZ_ORTAM": Ortam.URETIM.value,
    "YESILIZ_ROL": SunucuRolu.PANEL.value,
    "YESILIZ_JWT_ANAHTARI": GUVENLI_ANAHTAR,
    "YESILIZ_VERITABANI_URL": PG,
    "YESILIZ_IZINLI_KAYNAKLAR": "",
    "YESILIZ_PANEL_SIRRI": GUVENLI_ANAHTAR,
    "YESILIZ_PANEL_TOTP_ANAHTARI": "a" * 44,
    "YESILIZ_PANEL_TABAN": "https://panel.ornek.com",
    # `conftest` bunu test ortamı için false yapıyor (TestClient http
    # konuşuyor); üretim kuralını sınayabilmek için açıkça geri alınıyor.
    "panel_cerez_guvenli": True,
}


def _panel_ayar(**ek) -> Ayarlar:
    return Ayarlar(**{**PANEL_URETIM, **ek})


def _yollar(rol: SunucuRolu) -> set[str]:
    return {r.path for r in uygulama_kur(rol).routes if hasattr(r, "path")}


# --- rol çözümlemesi ---


def test_varsayilan_rol_api():
    """Canlıdaki mevcut servis `YESILIZ_ROL` tanımlamıyor; davranışı değişmemeli."""
    a = Ayarlar()
    assert a.rol is SunucuRolu.API
    assert a.api_mi and not a.panel_mi


def test_taninmayan_rol_reddediliyor():
    """Yanlış yazılmış bir rol sessizce API'ye düşmemeli — servis hiç açılmamalı."""
    with pytest.raises(ValidationError):
        Ayarlar(YESILIZ_ROL="yonetim")


def test_rol_standart_takma_adla_da_okunuyor():
    assert Ayarlar(SERVER_ROLE="panel").rol is SunucuRolu.PANEL


# --- mount ayrımı ---


def test_panel_rolunde_cihaz_uclari_mount_edilmiyor():
    panel = _yollar(SunucuRolu.PANEL)
    for yol in ("/auth/giris", "/auth/kayit", "/ozet", "/lig", "/ebeveyn/ozet", "/odul/sec"):
        assert yol not in panel, f"{yol} panel alan adında görünüyor"


def test_panel_rolunde_cihaz_ucu_404_donuyor():
    """Mount edilmemek yalnızca liste iddiası değil; istek de 404 almalı."""
    with TestClient(uygulama_kur(SunucuRolu.PANEL)) as istemci:
        assert istemci.post("/auth/giris", json={}).status_code == 404


def test_api_rolunde_cihaz_uclari_duruyor():
    api = _yollar(SunucuRolu.API)
    for yol in ("/auth/giris", "/ozet", "/lig", "/ebeveyn/ozet", "/odul/sec", "/saglik"):
        assert yol in api


def test_health_iki_rolde_de_var():
    """Railway'in healthcheckPath ayarı iki serviste de `/health`."""
    assert "/health" in _yollar(SunucuRolu.API)
    assert "/health" in _yollar(SunucuRolu.PANEL)


def test_saglik_yalnizca_api_rolunde():
    """Cihaza ait bir sözleşme; panel alan adında bulunmasının gerekçesi yok."""
    assert "/saglik" in _yollar(SunucuRolu.API)
    assert "/saglik" not in _yollar(SunucuRolu.PANEL)


def test_panel_rolunde_api_dokumani_kapali():
    """Personel yüzeyinin uç listesi kimlik doğrulaması olmadan yayımlanmamalı."""
    panel = _yollar(SunucuRolu.PANEL)
    for yol in ("/docs", "/redoc", "/openapi.json"):
        assert yol not in panel


# --- üretim kapısı ---


def test_panel_uretimde_gecerli_yapilandirmayla_basliyor():
    _panel_ayar().dogrula()


@pytest.mark.parametrize(
    "ezme, beklenen",
    [
        ({"YESILIZ_PANEL_SIRRI": VARSAYILAN_PANEL_SIRRI}, "oturum sırrı"),
        ({"YESILIZ_PANEL_SIRRI": "kisa"}, "karakterden kısa"),
        ({"YESILIZ_PANEL_TOTP_ANAHTARI": VARSAYILAN_PANEL_TOTP_ANAHTARI}, "TOTP"),
        # Açık takma adı yok; doğrudan kurulumda alan adıyla geçiliyor.
        ({"panel_cerez_guvenli": False}, "Secure"),
        ({"YESILIZ_PANEL_TABAN": ""}, "Panel adresi tanımsız"),
        ({"YESILIZ_PANEL_TABAN": "http://panel.ornek.com"}, "https://"),
    ],
)
def test_panel_uretim_kapisi_guvensiz_yapilandirmayi_reddediyor(ezme, beklenen):
    from app.ayarlar import YapilandirmaHatasi

    with pytest.raises(YapilandirmaHatasi) as hata:
        _panel_ayar(**ezme).dogrula()
    assert beklenen in str(hata.value)


def test_panel_kapisi_api_rolunde_calismiyor():
    """
    API servisinin üretim kapısı bit-birebir aynı kalmalı.

    Canlıdaki servis panel ayarlarının hiçbirini tanımlamıyor; panel kuralları
    ona da uygulansaydı bu değişiklik mevcut dağıtımı düşürürdü.
    """
    _panel_ayar(YESILIZ_ROL=SunucuRolu.API.value).dogrula()


def test_panel_sirri_hata_mesajina_girmiyor():
    """Hata mesajı da bir log — sır oraya yazılmamalı (JWT kuralıyla aynı)."""
    from app.ayarlar import YapilandirmaHatasi

    sir = "b" * 20
    with pytest.raises(YapilandirmaHatasi) as hata:
        _panel_ayar(YESILIZ_PANEL_SIRRI=sir).dogrula()
    assert sir not in str(hata.value)
