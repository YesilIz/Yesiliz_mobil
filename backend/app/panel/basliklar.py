"""
Panel güvenlik başlıkları.

Panel, personel kimliği ve (F3'ten sonra) sağlık verisine yakın bir yüzey
taşıyor; varsayılan tarayıcı davranışına bırakılamaz.
"""

from __future__ import annotations

from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware

from app.ayarlar import ayarlar

#: `'unsafe-inline'` YOK — satır içi `<style>` ve `hx-on` bu yüzden yasak.
#: `hx-headers` bir öznitelik, script değil; sorun çıkarmıyor. Sunucuda
#: üretilen SVG de element, script değil.
#:
#: `default-src 'none'`: beyaz liste yaklaşımı. Yeni bir kaynak türü
#: gerektiğinde açıkça eklenmek zorunda, sessizce yüklenemez.
CSP = (
    "default-src 'none'; "
    "script-src 'self'; "
    "style-src 'self'; "
    "img-src 'self' data:; "
    "font-src 'self'; "
    "connect-src 'self'; "
    "form-action 'self'; "
    "frame-ancestors 'none'; "
    "base-uri 'none'"
)

SABIT_BASLIKLAR = {
    "Content-Security-Policy": CSP,
    "X-Frame-Options": "DENY",
    "X-Content-Type-Options": "nosniff",
    # Panel URL'leri kullanıcı numarası ve başvuru numarası taşıyor; dış
    # bağlantıya `Referer` olarak sızmamalı. `same-origin` bunu tam olarak
    # veriyor: kendi sayfalarımız arasında tam `Referer`, dışarıya hiçbir şey.
    #
    # **`no-referrer` OLAMAZ ve bu bir tercih değil, CSRF kapısının şartı.**
    # Fetch standardı, `Origin` başlığını yazarken yönlendirici politikaya
    # bakıyor: politika `no-referrer` ise GET/HEAD dışındaki isteklerde
    # `Origin` **`null` olarak** gönderiliyor (Fetch §"Append a request
    # `Origin` header"). Yani panel `no-referrer` derse tarayıcı her form
    # POST'unda `Origin: null` + `Referer` yok gönderiyor, `CsrfKapisi` de
    # bunu haklı olarak reddediyor: panelin bütün yazma işlemleri gerçek
    # tarayıcıda 403 oluyor. Girişin kilitlenmesinin sebebi buydu.
    "Referrer-Policy": "same-origin",
}

#: Statik varlıklar `no-store` almıyor: htmx ve CSS her sayfa açılışında
#: yeniden indirilirdi. Kişisel veri taşıyan tek şey HTML.
STATIK_ONEKI = "/statik/"

#: Paylaşılan bir kurum bilgisayarının diskinde risk detayı kalmamalı.
#: Kolay unutulan ve önemli olan başlık bu.
HTML_ONBELLEGI = "no-store"


class GuvenlikBasliklari(BaseHTTPMiddleware):
    async def dispatch(self, istek: Request, cagir):
        yanit = await cagir(istek)
        for ad, deger in SABIT_BASLIKLAR.items():
            yanit.headers.setdefault(ad, deger)

        if not istek.url.path.startswith(STATIK_ONEKI):
            yanit.headers.setdefault("Cache-Control", HTML_ONBELLEGI)

        if ayarlar().uretim_mi:
            yanit.headers.setdefault(
                "Strict-Transport-Security",
                "max-age=31536000; includeSubDomains",
            )
        return yanit
