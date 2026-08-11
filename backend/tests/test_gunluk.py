"""Loglama — gizli değer sızmaması."""

from __future__ import annotations

import json
import logging

from app import gunluk
from app.ayarlar import ayarlar


def test_gizli_deger_log_metninden_siliniyor():
    anahtar = ayarlar().jwt_anahtari
    assert gunluk._gizle(f"jeton doğrulanamadı: {anahtar}") == "jeton doğrulanamadı: ***"


def test_veritabani_parolasi_log_metninden_siliniyor(monkeypatch):
    """SQLAlchemy bağlantı hatası URL'i parolasıyla istisnaya koyabiliyor."""
    monkeypatch.setattr(
        gunluk,
        "_gizli_degerler",
        lambda: ["cok-gizli-parola"],
    )
    metin = gunluk._gizle(
        "could not connect: postgresql+psycopg://yesiliz:cok-gizli-parola@db:5432/y"
    )
    assert "cok-gizli-parola" not in metin


def test_kayit_json_satiri_uretiyor():
    kayit = logging.LogRecord(
        name="yesiliz", level=logging.INFO, pathname=__file__, lineno=1,
        msg="istek", args=(), exc_info=None,
    )
    kayit.istek_id = "abc123"
    kayit.yol = "/lig"
    kayit.metot = "GET"
    kayit.durum = 200
    kayit.sure_ms = 12.3

    govde = json.loads(gunluk.JsonBicimlendirici().format(kayit))

    assert govde["istek_id"] == "abc123"
    assert govde["yol"] == "/lig"
    assert govde["metot"] == "GET"
    assert govde["durum"] == 200
    assert govde["sure_ms"] == 12.3
    assert govde["seviye"] == "INFO"


def test_govde_ve_basliklar_loglanmiyor(istemci, caplog):
    """Parola gövdede, JWT başlıkta — ikisi de log satırına girmemeli."""
    with caplog.at_level(logging.INFO, logger="yesiliz"):
        istemci.post(
            "/auth/kayit",
            json={"ad": "Turan", "eposta": "log@example.com", "sifre": "parola123"},
            headers={"Authorization": "Bearer sahte-jeton"},
        )
    metin = "\n".join(k.getMessage() + str(k.__dict__) for k in caplog.records)
    assert "parola123" not in metin
    assert "sahte-jeton" not in metin
    assert "log@example.com" not in metin


def test_istek_kimligi_yalnizca_guvenli_karakterleri_kabul_ediyor():
    assert gunluk._istek_kimligi("proxy_1-abc.xyz") == "proxy_1-abc.xyz"

    for gecersiz in ("", "boşluk var", "satir\nsonu", "a" * 65):
        uretilen = gunluk._istek_kimligi(gecersiz)
        assert uretilen != gecersiz
        assert gunluk.ISTEK_KIMLIGI_DESENI.fullmatch(uretilen)
