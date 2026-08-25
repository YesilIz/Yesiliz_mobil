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

#: Aynı makineyi gösteren adlar.
#:
#: Yerel geliştirmede panel hem `localhost:8001` hem `127.0.0.1:8001`
#: üzerinden açılıyor (yer imi, yönlendirme, elle yazım) ve ikisi **aynı
#: makine**. Kapı onları farklı sayınca giriş "Oturum doğrulaması
#: başarısız" ile reddediliyordu — gerçek tarayıcıda bulundu.
#:
#: Eşitlik **yalnızca iki taraf da loopback ise** kuruluyor: yapılandırılan
#: adres gerçek bir alan adıysa (üretim) hiçbir şey gevşemiyor.
LOOPBACK = frozenset({"localhost", "127.0.0.1", "::1", "[::1]"})

#: Tarayıcının "opak kaynak" dediği değer.
#:
#: **Kabul edilmiyor ve edilmemeli.** `null`, kaynağın kaybolduğu her yerden
#: geliyor: `file://` sayfası, `sandbox`'lı iframe, `data:` belge. Bir
#: saldırgan kendi sayfasına sandbox'lı bir iframe koyup panele POST
#: atabiliyor ve o istek de `Origin: null` taşıyor — yani `null`'ı geçirmek
#: kapıyı tam olarak korumak istediği saldırıya açardı.
#:
#: Kendi sayfalarımızın `null` göndermesinin **tek** sebebi yönlendirici
#: politikasıydı (`basliklar.py`); çözüm burayı gevşetmek değil, orayı
#: `same-origin` tutmak.
OPAK_KAYNAK = "null"


def _host_port(netloc: str) -> tuple[str, str]:
    """`localhost:8001` → `("localhost", "8001")`. IPv6 köşeli parantezli."""
    if netloc.startswith("["):
        kapanis = netloc.find("]")
        host = netloc[: kapanis + 1]
        kalan = netloc[kapanis + 1 :]
        return host, kalan[1:] if kalan.startswith(":") else ""
    if ":" in netloc:
        host, _, port = netloc.rpartition(":")
        return host, port
    return netloc, ""


def ayni_kaynak(gelen_netloc: str, beklenen_netloc: str) -> bool:
    """İki adres aynı kaynağı mı gösteriyor?"""
    if gelen_netloc == beklenen_netloc:
        return True
    gh, gp = _host_port(gelen_netloc)
    bh, bp = _host_port(beklenen_netloc)
    return gh in LOOPBACK and bh in LOOPBACK and gp == bp


class CsrfKapisi(BaseHTTPMiddleware):
    async def dispatch(self, istek: Request, cagir):
        if istek.method not in GUVENSIZ_METOTLAR:
            return await cagir(istek)

        hata = await self._sorun(istek)
        if hata:
            # **Ne görüldüğü de yazılıyor.** "kaynak" tek başına neyin
            # uyuşmadığını söylemiyor ve bu, üretimde de teşhis edilemez
            # bir 403 demek: ters vekil `Origin`'i değiştirmiş olabilir,
            # adres `localhost`/`127.0.0.1` ayrışmış olabilir. Değerler
            # gizli bilgi taşımıyor (host adı), jeton **yazılmıyor**.
            ayrinti = {"yol": istek.url.path, "hata_turu": hata}
            if hata.startswith("kaynak"):
                ayrinti["gelen_origin"] = istek.headers.get("origin", "")
                ayrinti["gelen_referer"] = istek.headers.get("referer", "")
                ayrinti["beklenen"] = ayarlar().panel_taban_url or ""
            gunluk.warning("CSRF reddi", extra=ayrinti)
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
            if not deger:
                continue
            if deger.strip().lower() == OPAK_KAYNAK:
                # Ayrı bir tür: günlükte "yabancı site POST'ladı" ile
                # "tarayıcı kaynağı düşürdü" birbirine karışmasın — ikisinin
                # teşhisi tamamen farklı yerde.
                return "kaynak_opak"
            return None if ayni_kaynak(urlsplit(deger).netloc, beklenen) else "kaynak"
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
