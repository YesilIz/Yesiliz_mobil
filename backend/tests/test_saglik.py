"""Sağlık kontrolü uçları."""

from __future__ import annotations

from app import main


def test_health_veritabani_ayaktayken_200(istemci):
    yanit = istemci.get("/health")
    assert yanit.status_code == 200
    govde = yanit.json()
    assert govde["status"] == "healthy"
    assert govde["database"] == "connected"
    assert govde["version"]


def test_health_veritabani_dusunce_503(istemci, monkeypatch):
    """Sağlayıcı bu sürümü devreye almamalı, yük dengeleyici yönlendirmemeli."""
    monkeypatch.setattr(main, "_veritabani_ayakta", lambda: False)
    yanit = istemci.get("/health")
    assert yanit.status_code == 503
    assert yanit.json()["database"] == "unreachable"


def test_health_gizli_bilgi_dondurmuyor(istemci):
    """Uç kimlik doğrulaması olmadan internete açık."""
    metin = istemci.get("/health").text.lower()
    for sizinti in ("password", "parola", "secret", "postgres", "sqlite", "@"):
        assert sizinti not in metin


def test_saglik_ucu_duruyor(istemci):
    """Kurulu APK'lar bunu çağırıyor (`data/uzak/YesilizApi.kt`)."""
    assert istemci.get("/saglik").json() == {"durum": "ayakta"}


def test_istek_kimligi_yaniti_isaretliyor(istemci):
    yanit = istemci.get("/saglik")
    assert yanit.headers["X-Istek-Id"]


def test_verilen_istek_kimligi_korunuyor(istemci):
    """Zincirdeki tek bir isteği baştan sona izleyebilmek için."""
    yanit = istemci.get("/saglik", headers={"X-Istek-Id": "abc123"})
    assert yanit.headers["X-Istek-Id"] == "abc123"
