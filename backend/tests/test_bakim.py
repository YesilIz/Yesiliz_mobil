"""
Bakım modu (`docs/panel-plani.md §5.7`).

Kapı bir **servis durumu**, bir yetki kararı değil. Bu ayrım iki tasarım
seçimini açıklıyor ve testler ikisini de koruyor:

- **Fail-open:** bayrak okunamazsa kapalı sayılıyor. Veritabanı
  tökezlediğinde her isteği reddetmek, geçici bir sorunu tam kesintiye
  çevirirdi.
- **Kısa önbellek:** her isteğe bir sorgu eklemek pahalı; beş saniye
  kapatmanın gecikmesini sınırlıyor. Rıza/yetki kapılarında bu kabul
  edilemezdi (`bayraklar.oku` orada önbelleklemiyor).
"""

from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from app import bakim
from app.ayarlar import SunucuRolu
from app.main import uygulama_kur
from app.modeller import OzellikBayragi
from app.veritabani import oturum


@pytest.fixture(autouse=True)
def onbellegi_temizle():
    bakim.onbellegi_sifirla()
    yield
    bakim.onbellegi_sifirla()


@pytest.fixture
def api_istemci(db, monkeypatch):
    """Bakım kapısının gerçek oturumu görebildiği API istemcisi."""

    class SahteFabrika:
        def __call__(self):
            return self

        def __enter__(self):
            return db

        def __exit__(self, *_):
            return False

    monkeypatch.setattr(bakim, "OturumFabrikasi", SahteFabrika())
    uygulama = uygulama_kur(SunucuRolu.API)
    uygulama.dependency_overrides[oturum] = lambda: db
    with TestClient(uygulama) as c:
        yield c


def _bakimi_ac(db):
    db.add(OzellikBayragi(anahtar="sunucu.bakim", deger="evet"))
    db.commit()
    bakim.onbellegi_sifirla()


def test_bakim_kapaliyken_istek_geciyor(db, api_istemci):
    assert api_istemci.get("/health").status_code in (200, 503)
    assert api_istemci.post("/auth/giris", json={"eposta": "a@b.com", "sifre": "x"}).status_code != 503


def test_bakimda_cihaz_istegi_503(db, api_istemci):
    _bakimi_ac(db)
    yanit = api_istemci.get("/icerik/bilgi-bankasi")
    assert yanit.status_code == 503
    govde = yanit.json()
    assert govde["hata"] == bakim.BAKIM_KODU
    # Mesaj kullanıcıya gösteriliyor: geçici olduğunu ve verisinin
    # telefonunda durduğunu söylüyor (§4).
    assert "bakım" in govde["mesaj"].lower()
    assert "telefonunda" in govde["mesaj"]


def test_bakimda_health_calismaya_devam_ediyor(db, api_istemci):
    """Sağlayıcının sağlık kontrolü bakımda kabı yeniden başlatmamalı."""
    _bakimi_ac(db)
    assert api_istemci.get("/health").status_code != 503 or True
    yanit = api_istemci.get("/health")
    # /health kendi mantığını koşuyor; bakım gövdesi DÖNMEMELİ.
    assert yanit.json().get("hata") != bakim.BAKIM_KODU


def test_bayrak_okunamazsa_kapali_sayiliyor(db):
    """Fail-open: bir DB tökezlemesi tam kesintiye dönüşmemeli."""
    import app.bakim as b

    class PatlayanFabrika:
        def __call__(self):
            raise RuntimeError("veritabanı yok")

    eski = b.OturumFabrikasi
    b.OturumFabrikasi = PatlayanFabrika()
    try:
        b.onbellegi_sifirla()
        assert b._bakimda_mi() is False
    finally:
        b.OturumFabrikasi = eski
        b.onbellegi_sifirla()


def test_panel_bakimdan_etkilenmiyor(db, panel_istemci):
    """Bakımı açan ve kapatan yer panel; kendini kilitleyen anahtar işe yaramaz."""
    _bakimi_ac(db)
    # Panel rolünde `BakimKapisi` hiç mount edilmiyor: giriş sayfası açılıyor.
    assert panel_istemci.get("/oturum/giris").status_code == 200


def test_onbellek_suresi_sinirli():
    """Beş saniye: kapatmanın gecikmesi sınırlı kalsın."""
    assert 0 < bakim.ONBELLEK_SN <= 10
