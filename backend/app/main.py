"""
Yeşilİz API.

CLAUDE.md §5: FastAPI (ASP.NET değil — ML katmanı Faz 3'te aynı dilde
olsun diye), PostgreSQL, Redis yok.

**Sunucu skor hesaplamıyor.** Risk çıkarımı ve skorlama cihazda kalıyor
(CLAUDE.md §4); buraya yalnızca sonuç geliyor. Bu, motorun tek bir
uygulaması olmasını da garanti ediyor — `core-risk` referans Python ile
birebir doğrulanmış durumda, ikinci bir sunucu uygulaması yazmak o
doğrulamayı bozardı.

**Tek imaj, iki servis** (CLAUDE.md §12 K57, `docs/panel-plani.md §4`).
[uygulama_kur] `YESILIZ_ROL`'e göre farklı router kümesi mount ediyor:

- `api`   → cihazın konuştuğu uçlar (varsayılan, canlıdaki mevcut davranış)
- `panel` → yönetim ve YEDAM danışman paneli (SSR)

Panel uçları API alan adında **hiç** mount edilmiyor. Yanlış yazılmış bir
yetki kontrolü bile internete açık olmuyor; yol 404 dönüyor.

**Fabrika olmasının ikinci sebebi test.** `ayarlar()` `lru_cache`'li ve bu
modül eskiden import anında okuyordu; iki rolü tek pytest oturumunda kurmak
mümkün değildi, yani "API alan adında panel yolu 404" iddiası hiç
sınanamıyordu. Fabrika rolü parametre olarak da alıyor.
"""

import asyncio
from contextlib import asynccontextmanager, suppress

from fastapi import FastAPI, Response, status
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text

from app import gunluk
from app.ayarlar import SURUM, SunucuRolu, ayarlar
from app.uclar import ebeveyn, hesap, icerik, lig, odul, ozet, puan, yedam
from app.veritabani import motor


@asynccontextmanager
async def yasam(_: FastAPI):
    """
    Açılış kapısı.

    Yapılandırma doğrulaması `ayarlar()` içinde koşuyor ve bu modül
    yüklenirken zaten çağrılıyor; üretimde güvensiz bir yapılandırma
    varsa uvicorn hiç dinlemeye başlamıyor, sağlayıcının sağlık kontrolü
    düşüyor ve hatalı sürüm devreye alınmıyor.
    """
    gunluk.kur()

    # YEDAM rıza bakımı yalnızca API rolünde: iki servis aynı imajı
    # paylaşıyor (K57) ve ikisi birden koşarsa aynı satırları iki kez
    # işlemeye çalışırlar. Silme idempotent, yani zarar vermezdi — ama
    # gereksiz ve log'u kirletirdi.
    bakim = None
    if ayarlar().rol is SunucuRolu.API:
        from app import yedam_bakim

        bakim = asyncio.create_task(yedam_bakim.dongu())

    try:
        yield
    finally:
        if bakim is not None:
            bakim.cancel()
            with suppress(asyncio.CancelledError):
                await bakim


def _veritabani_ayakta() -> bool:
    try:
        with motor.connect() as baglanti:
            baglanti.execute(text("SELECT 1"))
        return True
    except Exception:
        # Gerekçe loglanmıyor: bağlantı istisnaları URL'i parolasıyla
        # taşıyabiliyor. Ayrıntı zaten `IstekGunlugu`'ndan çıkıyor.
        return False


def uygulama_kur(rol: SunucuRolu | None = None) -> FastAPI:
    """
    Verilen role göre bir FastAPI uygulaması kurar.

    `rol` verilmezse `ayarlar().rol` okunuyor — uvicorn ve alembic bu yolu
    kullanıyor. Testler rolü açıkça geçiyor.
    """
    a = ayarlar()
    if rol is None:
        rol = a.rol

    uygulama = FastAPI(
        title="Yeşilİz API" if rol is SunucuRolu.API else "Yeşilİz Panel",
        version=SURUM,
        description="Yeşilay · TEKNOFEST 2026 — ekran kullanımı destek uygulaması",
        lifespan=yasam,
        # Panelde etkileşimli API dokümanı kapalı: personel yüzeyinin uç
        # listesini kimlik doğrulaması olmadan yayımlamanın gerekçesi yok.
        docs_url=None if rol is SunucuRolu.PANEL else "/docs",
        redoc_url=None if rol is SunucuRolu.PANEL else "/redoc",
        openapi_url=None if rol is SunucuRolu.PANEL else "/openapi.json",
    )

    # Middleware sırası: SON eklenen EN DIŞTA koşuyor. İstenen zincir
    #   CORS → IstekGunlugu → GuvenlikBasliklari → CsrfKapisi → route
    # yani CSRF reddi de loglanıyor ve güvenlik başlıklarını taşıyor.
    if rol is SunucuRolu.PANEL:
        from app.panel.basliklar import GuvenlikBasliklari
        from app.panel.csrf import CsrfKapisi

        uygulama.add_middleware(CsrfKapisi)
        uygulama.add_middleware(GuvenlikBasliklari)

    uygulama.add_middleware(gunluk.IstekGunlugu)

    # Android istemcisi Origin başlığı göndermiyor; panel de sunucu tarafında
    # render edildiği için çapraz kaynak istek yapmıyor. Yani liste iki rolde
    # de boş kalıyor. Varsayılan boş liste = hiçbir origin'e izin yok; açık
    # uçlu `*` yazmak kimlikli isteklerin herhangi bir siteden atılabilmesi
    # demek olurdu.
    uygulama.add_middleware(
        CORSMiddleware,
        allow_origins=a.izinli_kaynak_listesi,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    if rol is SunucuRolu.API:
        uygulama.include_router(hesap.yonlendirici)
        uygulama.include_router(ozet.yonlendirici)
        uygulama.include_router(lig.yonlendirici)
        uygulama.include_router(ebeveyn.yonlendirici)
        uygulama.include_router(odul.yonlendirici)
        uygulama.include_router(icerik.yonlendirici)
        uygulama.include_router(puan.yonlendirici)
        uygulama.include_router(yedam.yonlendirici)

        @uygulama.get("/saglik", tags=["sistem"])
        def saglik(yanit: Response) -> dict[str, str]:
            """
            Cihazın 'sunucu ayakta mı' yoklaması.

            `/health`'ten ayrı duruyor çünkü Android istemcisi bunu çağırıyor
            (`data/uzak/YesilizApi.kt`) ve kurulu APK'lar güncellenene kadar
            yanıt biçimi değişemez. Yeni iş `/health`'e gitmeli. Bununla
            birlikte yalnızca web sürecinin çalışması yeterli değil:
            uygulamanın kullandığı bütün kimlik/özet uçları veritabanına
            bağlı. Veritabanı yokken 200 + ``ayakta`` dönmek cihazda sahte
            bir başarı sinyali üretiyordu.

            **Yalnızca API rolünde var.** Cihaza ait bir sözleşme; panel
            alan adında bulunmasının gerekçesi yok.
            """
            baglandi = _veritabani_ayakta()
            if not baglandi:
                yanit.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
            return {"durum": "ayakta" if baglandi else "erisim_yok"}

    if rol is SunucuRolu.PANEL:
        from fastapi.responses import RedirectResponse
        from fastapi.staticfiles import StaticFiles

        from app.panel.sablon import STATIK_DIZINI
        from app.uclar import (
            panel_bayrak,
            panel_icerik,
            panel_odul,
            panel_oturum,
            panel_personel,
            panel_yedam,
            panel_yonetim,
        )

        uygulama.include_router(panel_oturum.yonlendirici)
        uygulama.include_router(panel_yonetim.yonlendirici)
        uygulama.include_router(panel_personel.yonlendirici)
        uygulama.include_router(panel_odul.yonlendirici)
        uygulama.include_router(panel_bayrak.yonlendirici)
        uygulama.include_router(panel_icerik.yonlendirici)
        uygulama.include_router(panel_yedam.yonlendirici)

        # CSS ve htmx depoya alınmış (`panel/statik/HTMX.md`): panel VPN
        # arkasında dışa çıkışsız olabiliyor ve CDN script'i `script-src
        # 'self'` CSP'sini gevşetmeyi zorunlu kılardı.
        uygulama.mount(
            "/statik", StaticFiles(directory=str(STATIK_DIZINI)), name="statik"
        )

        @uygulama.get("/", include_in_schema=False)
        def kok() -> RedirectResponse:
            """
            Kök adres girişe düşüyor.

            Rol bazlı hedef `panel_oturum.giris_sonrasi_yol`'da; buradan
            role bakmak, oturumu iki yerde çözmek demek olurdu.
            """
            return RedirectResponse(
                "/oturum/giris", status_code=status.HTTP_303_SEE_OTHER
            )


    @uygulama.get("/health", tags=["sistem"])
    def health(yanit: Response) -> dict[str, str]:
        """
        Dağıtım platformunun sağlık kontrolü, CI doğrulaması ve smoke testi.

        Parola, bağlantı adresi ya da ortam değişkeni **döndürmüyor**; bu uç
        kimlik doğrulaması olmadan internete açık. Rolü de dönmüyor: iki
        servisin hangi adreste koştuğunu dışarıya ilan etmenin faydası yok.

        Her iki rolde de var — Railway'in `healthcheckPath` ayarı ikisinde
        de `/health`.
        """
        baglandi = _veritabani_ayakta()
        if not baglandi:
            # 503: sağlayıcı bu sürümü devreye almasın, yük dengeleyici
            # trafiği buraya yönlendirmesin.
            yanit.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
        return {
            "status": "healthy" if baglandi else "unhealthy",
            "database": "connected" if baglandi else "unreachable",
            "version": SURUM,
        }

    return uygulama


#: uvicorn ve alembic bunu görüyor (`app.main:uygulama`).
uygulama = uygulama_kur()
