"""/saglik — Render/Docker'ın health check'i buna güveniyor, gerçek DB kontrolü şart."""

from __future__ import annotations


def test_veritabani_baglantisi_varken_saglikli(istemci):
    yanit = istemci.get("/saglik")

    assert yanit.status_code == 200
    veri = yanit.json()
    assert veri["status"] == "healthy"
    assert veri["database"] == "connected"


def test_gizli_bilgi_donmuyor(istemci):
    """Parola, bağlantı adresi gibi hiçbir gizli bilgi yanıtta olmamalı."""
    veri = istemci.get("/saglik").json()

    assert set(veri.keys()) == {"status", "database", "version"}
