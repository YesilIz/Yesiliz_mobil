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

from contextlib import asynccontextmanager

from fastapi import FastAPI, Response, status
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text

from app import gunluk
from app.ayarlar import SURUM, SunucuRolu, ayarlar
from app.uclar import ebeveyn, hesap, lig, odul, ozet
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
    yield


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

    # Panel router'ları F1'in ilerleyen commit'lerinde bağlanıyor.

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
