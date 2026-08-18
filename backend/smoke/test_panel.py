"""
Panel servisi — canlı ortama karşı smoke.

`test_duman.py`'den ayrı dosya: o, cihaz API'sine karşı uçtan uca bir
kullanıcı akışı koşup hesabı siliyor. Panelin eşdeğeri yok — giriş TOTP
istiyor ve smoke testine personel sırrı vermek doğru değil. Buradaki
testler **kimlik doğrulaması gerektirmeyen** yüzeyi ve rol ayrımını
sınıyor.

En değerlisi `test_cihaz_ucu_panelde_yok` ve `test_panel_yolu_apide_yok`:
`docs/panel-plani.md` P8'in ("panel uçları API alan adında hiç mount
edilmiyor") canlı ortamdaki tek cümlelik kanıtı.
"""

from __future__ import annotations

import os

import httpx
import pytest

PANEL = os.environ.get("YESILIZ_SMOKE_PANEL_TABAN", "").rstrip("/")
API = os.environ.get("YESILIZ_SMOKE_TABAN", "").rstrip("/")

HTTP_TAMAM = os.environ.get("YESILIZ_SMOKE_HTTP_TAMAM") == "1"
ZAMAN_ASIMI = 20.0

pytestmark = pytest.mark.skipif(
    not PANEL,
    reason="YESILIZ_SMOKE_PANEL_TABAN tanımlı değil — panel adresi gerekiyor",
)


@pytest.fixture(scope="module")
def panel():
    with httpx.Client(base_url=PANEL, timeout=ZAMAN_ASIMI, follow_redirects=False) as c:
        yield c


def test_adres_https():
    if HTTP_TAMAM:
        pytest.skip("YESILIZ_SMOKE_HTTP_TAMAM=1 — yerel yığına karşı koşuluyor")
    assert PANEL.startswith("https://"), f"Panel adresi HTTPS olmalı: {PANEL}"


def test_health(panel):
    yanit = panel.get("/health")
    assert yanit.status_code == 200, yanit.text
    assert yanit.json()["database"] == "connected"


def test_kok_adres_girise_dusuyor(panel):
    yanit = panel.get("/")
    assert yanit.status_code == 303
    assert yanit.headers["location"] == "/oturum/giris"


def test_giris_sayfasi_aciliyor(panel):
    yanit = panel.get("/oturum/giris")
    assert yanit.status_code == 200
    assert "Giriş yap" in yanit.text


def test_oturumsuz_panel_girise_yonlendiriyor(panel):
    """200 dönerse yetki kapısı çalışmıyor demektir."""
    yanit = panel.get("/yonetim/gosterge")
    assert yanit.status_code == 303
    assert yanit.headers["location"] == "/oturum/giris"


def test_guvenlik_basliklari(panel):
    basliklar = panel.get("/oturum/giris").headers
    assert "unsafe-inline" not in basliklar["content-security-policy"]
    assert basliklar["x-frame-options"] == "DENY"
    assert basliklar["cache-control"] == "no-store"


def test_api_dokumani_kapali(panel):
    """Personel yüzeyinin uç listesi kimlik doğrulaması olmadan yayımlanmamalı."""
    for yol in ("/docs", "/openapi.json"):
        assert panel.get(yol).status_code == 404


def test_cihaz_ucu_panelde_yok(panel):
    """P8'in kanıtı — panel alan adında cihaz API'si mount edilmiyor."""
    assert panel.post("/auth/giris", json={}).status_code == 404


@pytest.mark.skipif(not API, reason="YESILIZ_SMOKE_TABAN tanımlı değil")
def test_panel_yolu_apide_yok():
    """P8'in diğer yönü — API alan adında panel girişi yok."""
    with httpx.Client(base_url=API, timeout=ZAMAN_ASIMI, follow_redirects=False) as c:
        assert c.get("/oturum/giris").status_code == 404
