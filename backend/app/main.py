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

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.ayarlar import ayarlar
from app.uclar import ebeveyn, hesap, lig, odul, ozet

uygulama = FastAPI(
    title="Yeşilİz API",
    version="0.1.0",
    description="Yeşilay · TEKNOFEST 2026 — ekran kullanımı destek uygulaması",
)

# Android istemcisi Origin başlığı göndermiyor, bu yalnızca tarayıcı tabanlı
# istemcileri (uzman/Yeşilay paneli, Faz 3) ilgilendiriyor. Varsayılan boş
# liste = hiçbir origin'e izin yok; YESILIZ_IZINLI_KAYNAKLAR ile açılıyor.
uygulama.add_middleware(
    CORSMiddleware,
    allow_origins=ayarlar().izinli_kaynak_listesi,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

uygulama.include_router(hesap.yonlendirici)
uygulama.include_router(ozet.yonlendirici)
uygulama.include_router(lig.yonlendirici)
uygulama.include_router(ebeveyn.yonlendirici)
uygulama.include_router(odul.yonlendirici)


@uygulama.get("/saglik", tags=["sistem"])
def saglik() -> dict[str, str]:
    """Docker healthcheck ve cihazın 'sunucu ayakta mı' yoklaması."""
    return {"durum": "ayakta"}
