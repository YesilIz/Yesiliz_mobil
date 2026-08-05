"""İstek günlüğü ara katmanı — görev tanımı §8."""

from __future__ import annotations


def test_istek_id_basligi_donuyor(istemci):
    yanit = istemci.get("/saglik")

    assert "X-Istek-Id" in yanit.headers
    # Her istek farklı bir kimlik almalı.
    ikinci = istemci.get("/saglik")
    assert yanit.headers["X-Istek-Id"] != ikinci.headers["X-Istek-Id"]


def test_basarisiz_istek_de_calisiyor(istemci):
    """Ara katman var olmayan bir uca giden isteği de sorunsuz geçirmeli."""
    yanit = istemci.get("/olmayan-uc")

    assert yanit.status_code == 404
