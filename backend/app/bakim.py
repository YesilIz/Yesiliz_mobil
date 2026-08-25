"""
Bakım modu (`docs/panel-plani.md §5.7`).

Panelden açılınca **cihaz API'si** `503` + makine okunur gövde dönüyor;
uygulama bunu çevrimdışı saymıyor ve kullanıcıya bakımın sürdüğünü
söylüyor. Sessizce "sunucuya ulaşılamıyor" demek, geçici bir durumu
kalıcı bir arıza gibi göstermek olurdu (`CLAUDE.md §4`, K37 ailesi).

**Panel bu kapının dışında** — bakımı açan ve kapatan yer orası; kendini
kilitleyen bir anahtar işe yaramaz.

**`/health` de dışarıda:** sağlayıcının sağlık kontrolü bakım sırasında
kabı yeniden başlatmamalı. Bakım bir uygulama durumu, bir arıza değil.
"""

from __future__ import annotations

import logging
import time

from fastapi import Request, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from app import bayraklar
from app.veritabani import OturumFabrikasi

#: Bakım modunda da çalışan yollar.
MUAF_YOLLAR = ("/health", "/docs", "/redoc", "/openapi.json")

#: Cihazın çevrimdışıyla ayırdığı kod.
BAKIM_KODU = "bakim"

#: Bayrağın kaç saniye önbelleklendiği.
#:
#: `bayraklar.oku` bilinçli olarak önbelleklemiyor çünkü rıza ve yetki
#: kapılarında "ne zaman etkili olacak" belirsizliği kabul edilemez. Bakım
#: **onlardan biri değil**: bir erişim kararı değil bir servis durumu ve
#: her istekte veritabanına gitmek, her isteğe bir sorgu eklemek olurdu.
#: Beş saniye, kapatmanın gecikmesini sınırlıyor.
ONBELLEK_SN = 5.0

_son_okuma = 0.0
_son_deger = False

gunluk = logging.getLogger(__name__)


def _bakimda_mi() -> bool:
    """
    Bayrağı okur; okunamazsa **kapalı** sayar (fail-open).

    Bilinçli olarak fail-open: veritabanına ulaşılamadığında her isteği
    reddetmek, geçici bir tökezlemeyi tam bir kesintiye çevirirdi ve
    bakım modunun amacı bu değil. Gerçek bir veritabanı arızası zaten
    `/health` üzerinden görünüyor (`_veritabani_ayakta`).
    """
    global _son_okuma, _son_deger

    simdi = time.monotonic()
    if simdi - _son_okuma < ONBELLEK_SN:
        return _son_deger

    try:
        with OturumFabrikasi() as db:
            _son_deger = bayraklar.acik_mi(db, bayraklar.BAKIM_MODU)
    except Exception:  # noqa: BLE001 - kapı bir arızayı kesintiye çevirmemeli
        # DEBUG: testler kendi oturumlarını enjekte ettiği için burası
        # sık düşüyor ve WARNING gürültü yapardı. Üretimdeki gerçek arıza
        # `/health`'ten görünüyor.
        gunluk.debug("Bakım bayrağı okunamadı, kapalı sayılıyor", exc_info=True)
        _son_deger = False

    _son_okuma = simdi
    return _son_deger


def onbellegi_sifirla() -> None:
    """Testler için: bir sonraki okuma veritabanına gitsin."""
    global _son_okuma
    _son_okuma = 0.0


class BakimKapisi(BaseHTTPMiddleware):
    """
    Bakım modunda cihaz isteklerini `503` ile karşılayan middleware.

    Bayrak **her istekte** okunuyor (`bayraklar.oku` önbelleklemiyor):
    bakımı kapatan personel, cihazların ne zaman geri döneceğini
    bilmek zorunda kalmamalı.
    """

    async def dispatch(self, istek: Request, sonraki):
        if istek.url.path in MUAF_YOLLAR:
            return await sonraki(istek)

        if not _bakimda_mi():
            return await sonraki(istek)

        return JSONResponse(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            content={
                "hata": BAKIM_KODU,
                "mesaj": (
                    "Yeşilİz sunucusunda kısa bir bakım var. Verilerin "
                    "telefonunda duruyor ve uygulamayı kullanmaya devam "
                    "edebilirsin; senkron bakım bitince sürecek."
                ),
            },
        )
