"""Yapılandırılmış (JSON) istek logu.

Görev tanımı §8: istek kimliği, endpoint, HTTP metodu, durum kodu, işlem
süresi, hata türü, zaman bilgisi — parola/JWT/veritabanı bağlantı parolası
**kesinlikle yok**. Bu modül yalnızca yol/metot/süre/durum kodunu görüyor;
gövdeye (`Authorization` başlığı dahil) hiç dokunmuyor, o yüzden yanlışlıkla
gizli bir alanı loglama riski yapısal olarak yok.
"""

from __future__ import annotations

import json
import logging
import sys
import time
import uuid
from collections.abc import Awaitable, Callable

from fastapi import Request, Response

_kayitci = logging.getLogger("yesiliz.istek")


def gunlukleyiciyi_kur() -> None:
    """Uygulama başlarken bir kez çağrılır."""
    if _kayitci.handlers:
        return  # Testlerde birden çok kez import edilebiliyor.

    tasiyici = logging.StreamHandler(sys.stdout)
    tasiyici.setFormatter(_JsonBicimlendirici())
    _kayitci.addHandler(tasiyici)
    _kayitci.setLevel(logging.INFO)
    _kayitci.propagate = False


class _JsonBicimlendirici(logging.Formatter):
    def format(self, kayit: logging.LogRecord) -> str:
        satir = {
            "zaman": self.formatTime(kayit, "%Y-%m-%dT%H:%M:%S"),
            "seviye": kayit.levelname,
            "mesaj": kayit.getMessage(),
        }
        satir.update(getattr(kayit, "alanlar", {}))
        return json.dumps(satir, ensure_ascii=False)


async def istek_gunlugu_ara_katmani(
    istek: Request,
    sonraki: Callable[[Request], Awaitable[Response]],
) -> Response:
    """Her isteği tek satır JSON olarak loglayan FastAPI ara katmanı."""
    istek_id = str(uuid.uuid4())
    baslangic = time.perf_counter()

    try:
        yanit = await sonraki(istek)
    except Exception as hata:
        sure_ms = (time.perf_counter() - baslangic) * 1000
        _kayitci.info(
            "istek",
            extra={
                "alanlar": {
                    "istek_id": istek_id,
                    "endpoint": istek.url.path,
                    "metot": istek.method,
                    "durum_kodu": 500,
                    "sure_ms": round(sure_ms, 1),
                    "hata_turu": type(hata).__name__,
                },
            },
        )
        raise

    sure_ms = (time.perf_counter() - baslangic) * 1000
    _kayitci.info(
        "istek",
        extra={
            "alanlar": {
                "istek_id": istek_id,
                "endpoint": istek.url.path,
                "metot": istek.method,
                "durum_kodu": yanit.status_code,
                "sure_ms": round(sure_ms, 1),
            },
        },
    )
    yanit.headers["X-Istek-Id"] = istek_id
    return yanit
