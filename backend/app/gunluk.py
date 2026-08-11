"""
Yapılandırılmış loglama.

Her istek tek bir JSON satırı üretiyor: istek kimliği, yol, metot, durum
kodu, süre, varsa hata türü. Sağlayıcı (Railway) stdout'u topluyor; JSON
satırı orada aranabilir hâle geliyor, serbest metin gelmiyor.

**Ne loglanmıyor:** başlıklar (yani `Authorization` ve dolayısıyla JWT),
gövde (yani parolalar ve günlük özet verisi), sorgu dizesi. Buna ek olarak
[_gizle] hata metinlerinde gizli değerleri arayıp siliyor — SQLAlchemy'nin
bağlantı hataları veritabanı URL'ini parolasıyla birlikte istisna mesajına
koyabiliyor ve o mesaj doğrudan loga giderdi.

Kişisel/sağlık verisi zaten uçların gövdesinde; gövde hiç loglanmadığı
için bu katmandan sızmıyor (CLAUDE.md §4).
"""

from __future__ import annotations

import json
import logging
import re
import sys
import time
import uuid
from typing import Any

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

from app.ayarlar import ayarlar

ISTEK_BASLIGI = "X-Istek-Id"
ISTEK_KIMLIGI_DESENI = re.compile(r"[A-Za-z0-9._-]{1,64}")

_gunlukcu = logging.getLogger("yesiliz")


class JsonBicimlendirici(logging.Formatter):
    def format(self, kayit: logging.LogRecord) -> str:
        govde: dict[str, Any] = {
            "zaman": self.formatTime(kayit, "%Y-%m-%dT%H:%M:%S%z"),
            "seviye": kayit.levelname,
            "mesaj": _gizle(kayit.getMessage()),
        }
        # Middleware'in eklediği alanlar `extra=` ile geliyor.
        for alan in ("istek_id", "yol", "metot", "durum", "sure_ms", "hata_turu"):
            deger = getattr(kayit, alan, None)
            if deger is not None:
                govde[alan] = deger
        return json.dumps(govde, ensure_ascii=False)


def _gizli_degerler() -> list[str]:
    a = ayarlar()
    degerler = [a.jwt_anahtari]
    # Veritabanı URL'inin parola kısmı: `sema://kullanici:PAROLA@sunucu/…`
    url = a.veritabani_url
    if "://" in url and "@" in url:
        kimlik = url.split("://", 1)[1].split("@", 1)[0]
        if ":" in kimlik:
            degerler.append(kimlik.split(":", 1)[1])
    degerler.append(url)
    return [d for d in degerler if d and len(d) > 3]


def _gizle(metin: str) -> str:
    for deger in _gizli_degerler():
        metin = metin.replace(deger, "***")
    return metin


def kur() -> None:
    """Kök logger'ı JSON'a bağlar. Uygulama açılışında bir kez çağrılıyor."""
    isleyici = logging.StreamHandler(sys.stdout)
    isleyici.setFormatter(JsonBicimlendirici())

    kok = logging.getLogger()
    kok.handlers = [isleyici]
    kok.setLevel(logging.INFO)

    # Uvicorn'un kendi erişim logu aynı isteği ikinci kez, biçimsiz
    # yazıyordu. Bizimki durum kodunu ve süreyi zaten taşıyor.
    logging.getLogger("uvicorn.access").handlers = []
    logging.getLogger("uvicorn.access").propagate = False
    for ad in ("uvicorn", "uvicorn.error"):
        logging.getLogger(ad).handlers = [isleyici]
        logging.getLogger(ad).propagate = False


class IstekGunlugu(BaseHTTPMiddleware):
    """İstek başına tek satır log + yanıta istek kimliği başlığı."""

    async def dispatch(self, istek: Request, sonraki) -> Response:  # noqa: ANN001
        istek_id = _istek_kimligi(istek.headers.get(ISTEK_BASLIGI))
        baslangic = time.perf_counter()
        alanlar = {
            "istek_id": istek_id,
            "yol": istek.url.path,
            "metot": istek.method,
        }

        try:
            yanit = await sonraki(istek)
        except Exception as hata:
            sure = round((time.perf_counter() - baslangic) * 1000, 1)
            _gunlukcu.exception(
                "istek başarısız",
                extra={**alanlar, "durum": 500, "sure_ms": sure,
                       "hata_turu": type(hata).__name__},
            )
            raise

        sure = round((time.perf_counter() - baslangic) * 1000, 1)
        # 5xx uyarı seviyesinde: sağlayıcı panelinde filtrelenebilsin.
        seviye = logging.ERROR if yanit.status_code >= 500 else logging.INFO
        _gunlukcu.log(
            seviye,
            "istek",
            extra={**alanlar, "durum": yanit.status_code, "sure_ms": sure},
        )
        yanit.headers[ISTEK_BASLIGI] = istek_id
        return yanit


def _istek_kimligi(aday: str | None) -> str:
    """Güvenilir zincir kimliğini korur; günlük/başlık girdisini sınırlar.

    İstemci başlığı iz sürmek için yararlı ama güven sınırının dışında.
    Sınırsız ya da denetim karakterli bir değer günlükleri şişirebilir,
    yanıt başlığını da geçersiz kılabilir. Geçersiz değeri yansıtmak yerine
    sunucuda kısa, güvenli bir kimlik üretiyoruz.
    """
    if aday is not None and ISTEK_KIMLIGI_DESENI.fullmatch(aday):
        return aday
    return uuid.uuid4().hex[:12]
