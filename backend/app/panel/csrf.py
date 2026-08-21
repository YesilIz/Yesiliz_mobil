"""
CSRF kapısı — middleware, dependency değil (`docs/panel-plani.md §2`).

Dependency olsaydı her yazma route'unun onu **hatırlaması** gerekirdi ve
"hangi router'a bağladım" sorusu açık kalırdı. Middleware'de unutmak
fiziksel olarak mümkün değil: güvensiz metotların hepsi varsayılan olarak
reddediliyor ve muafiyet listesi boş.

İki ayak birlikte çalışıyor:

1. **Oturuma bağlı jeton** — `oturum.csrf_jetonu`, oturum özetinden HMAC ile
   türetiliyor. Başlıkta (`X-CSRF`, HTMX) ya da form alanında (`_csrf`).
2. **`Origin` / `Referer` denetimi** — panelin kendi adresiyle karşılaştırma.

`SameSite=Strict` çerezi üçüncü bir ayak; üçü birlikte tek bir tanesinin
atlatılmasına dayanıklı.
"""

from __future__ import annotations

import hmac
import logging
from urllib.parse import parse_qs, urlsplit

from fastapi import Request, status
from fastapi.responses import PlainTextResponse
from starlette.middleware.base import BaseHTTPMiddleware

from app.ayarlar import ayarlar
from app.guvenlik import jeton_ozetle
from app.panel.oturum import CEREZ_ADI, csrf_jetonu

gunluk = logging.getLogger(__name__)

GUVENSIZ_METOTLAR = frozenset({"POST", "PUT", "PATCH", "DELETE"})

BASLIK_ADI = "x-csrf"
FORM_ALANI = "_csrf"

#: Gövdeden jeton okunurken kabul edilen içerik türü. Çok parçalı gönderim
#: (dosya yükleme) geldiğinde jeton **başlıkta** taşınmak zorunda; sessizce
#: geçirilmiyor.
URLENCODED = "application/x-www-form-urlencoded"


class CsrfKapisi(BaseHTTPMiddleware):
    async def dispatch(self, istek: Request, cagir):
        if istek.method not in GUVENSIZ_METOTLAR:
            return await cagir(istek)

        hata = await self._sorun(istek)
        if hata:
            gunluk.warning(
                "CSRF reddi",
                extra={"yol": istek.url.path, "hata_turu": hata},
            )
            return PlainTextResponse(
                "Oturum doğrulaması başarısız. Sayfayı yenileyip tekrar deneyin.",
                status_code=status.HTTP_403_FORBIDDEN,
            )

        return await cagir(istek)

    async def _sorun(self, istek: Request) -> str | None:
        kaynak_sorunu = self._kaynak_sorunu(istek)
        if kaynak_sorunu:
            return kaynak_sorunu

        ham_cerez = istek.cookies.get(CEREZ_ADI)
        if not ham_cerez:
            # Oturumsuz güvensiz istek: giriş formu da buraya düşüyor.
            # Giriş POST'u oturum açılmadan geldiği için jeton taşıyamaz;
            # onu koruyan şey `Origin` denetimi (yukarıda) ve parolanın
            # kendisi.
            return None

        beklenen = csrf_jetonu(jeton_ozetle(ham_cerez))
        gelen = await self._jetonu_oku(istek)
        if not gelen or not hmac.compare_digest(gelen, beklenen):
            return "jeton"
        return None

    def _kaynak_sorunu(self, istek: Request) -> str | None:
        """
        `Origin` yoksa `Referer`'a düşülüyor; **ikisi de yoksa reddedilmiyor.**

        Başlığın yokluğu tek başına red sebebi değil, çünkü CSRF doğası
        gereği kurbanın tarayıcısını gerektiriyor ve tarayıcılar güvensiz
        metotlarda `Origin`'i her zaman gönderiyor. Başlıksız bir istek
        tarayıcıdan gelmiyor demektir; oturum çerezi de taşımıyorsa
        koruyacak bir şey yok, taşıyorsa jeton kontrolü (aşağıda) zaten
        devrede.

        Yokluğu red saymak somut bir zarar veriyordu: panele atılan her
        tarayıcı dışı POST — var olmayan bir yola atılan dahil — 404 yerine
        403 dönüyordu, yani rol ayrımının "cihaz ucu panelde yok" iddiası
        dışarıdan doğrulanamıyordu.
        """
        taban = ayarlar().panel_taban_url
        if not taban:
            # Yerel geliştirme: üretimde `dogrula` bunu zaten reddediyor.
            return None

        beklenen = urlsplit(taban).netloc
        for baslik in ("origin", "referer"):
            deger = istek.headers.get(baslik)
            if deger:
                return None if urlsplit(deger).netloc == beklenen else "kaynak"
        return None

    async def _jetonu_oku(self, istek: Request) -> str | None:
        basliktan = istek.headers.get(BASLIK_ADI)
        if basliktan:
            return basliktan

        tur = istek.headers.get("content-type", "")
        if not tur.startswith(URLENCODED):
            return None

        # Starlette gövdeyi önbelleğe alıyor (`_CachedRequest`), yani route
        # aynı gövdeyi yeniden okuyabiliyor. `form()` çağrılmıyor: çok
        # parçalı ayrıştırma burada tekrar oynatılamaz.
        govde = await istek.body()
        alanlar = parse_qs(govde.decode("utf-8", errors="replace"))
        degerler = alanlar.get(FORM_ALANI)
        return degerler[0] if degerler else None
