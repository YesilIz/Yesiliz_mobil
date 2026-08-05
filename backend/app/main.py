"""
Yeşilİz API.

CLAUDE.md §5: FastAPI (ASP.NET değil — ML katmanı Faz 3'te aynı dilde
olsun diye), PostgreSQL, Redis yok.

**Sunucu skor hesaplamıyor.** Risk çıkarımı ve skorlama cihazda kalıyor
(CLAUDE.md §4); buraya yalnızca sonuç geliyor. Bu, motorun tek bir
uygulaması olmasını da garanti ediyor — `core-risk` referans Python ile
birebir doğrulanmış durumda, ikinci bir sunucu uygulaması yazmak o
doğrulamayı bozardı.
"""

from contextlib import asynccontextmanager

from fastapi import FastAPI, Response, status
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text

from app import gunluk
from app.ayarlar import SURUM, ayarlar
from app.uclar import ebeveyn, hesap, lig, ozet
from app.veritabani import motor


@asynccontextmanager
async def yasam(_: FastAPI):
    """
    Açılış kapısı.

    `dogrula()` üretimde güvensiz yapılandırmayı reddediyor ve istisna
    burada fırlarsa uvicorn hiç dinlemeye başlamıyor — sağlayıcının sağlık
    kontrolü de düşüyor, yani hatalı sürüm devreye alınmıyor.
    """
    gunluk.kur()
    ayarlar().dogrula()
    yield


uygulama = FastAPI(
    title="Yeşilİz API",
    version=SURUM,
    description="Yeşilay · TEKNOFEST 2026 — ekran kullanımı destek uygulaması",
    lifespan=yasam,
)

uygulama.add_middleware(gunluk.IstekGunlugu)

# Mobil istemci CORS'a tabi değil (tarayıcı değil); bu ayar uzman/Yeşilay
# web paneli için. Liste boşken hiçbir kaynağa izin verilmiyor — açık
# uçlu `*` yazmak, kimlikli isteklerin herhangi bir siteden atılabilmesi
# demek olurdu.
if ayarlar().kaynaklar:
    uygulama.add_middleware(
        CORSMiddleware,
        allow_origins=ayarlar().kaynaklar,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

uygulama.include_router(hesap.yonlendirici)
uygulama.include_router(ozet.yonlendirici)
uygulama.include_router(lig.yonlendirici)
uygulama.include_router(ebeveyn.yonlendirici)


def _veritabani_ayakta() -> bool:
    try:
        with motor.connect() as baglanti:
            baglanti.execute(text("SELECT 1"))
        return True
    except Exception:
        # Gerekçe loglanmıyor: bağlantı istisnaları URL'i parolasıyla
        # taşıyabiliyor. Ayrıntı zaten `IstekGunlugu`'ndan çıkıyor.
        return False


@uygulama.get("/health", tags=["sistem"])
def health(yanit: Response) -> dict[str, str]:
    """
    Dağıtım platformunun sağlık kontrolü, CI doğrulaması ve smoke testi.

    Parola, bağlantı adresi ya da ortam değişkeni **döndürmüyor**; bu uç
    kimlik doğrulaması olmadan internete açık.
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


@uygulama.get("/saglik", tags=["sistem"])
def saglik() -> dict[str, str]:
    """
    Cihazın 'sunucu ayakta mı' yoklaması.

    `/health`'ten ayrı duruyor çünkü Android istemcisi bunu çağırıyor
    (`data/uzak/YesilizApi.kt`) ve kurulu APK'lar güncellenene kadar
    yanıt biçimi değişemez. Yeni iş `/health`'e gitmeli.
    """
    return {"durum": "ayakta"}
