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

from fastapi import Depends, FastAPI, Response, status
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.ayarlar import ayarlar
from app.log import gunlukleyiciyi_kur, istek_gunlugu_ara_katmani
from app.uclar import ebeveyn, hesap, lig, odul, ozet
from app.veritabani import oturum

gunlukleyiciyi_kur()

uygulama = FastAPI(
    title="Yeşilİz API",
    version="0.1.0",
    description="Yeşilay · TEKNOFEST 2026 — ekran kullanımı destek uygulaması",
)

# İstek kimliği, endpoint, metot, durum kodu, süre — JSON log satırı
# (görev tanımı §8). Parola/JWT/DB parolası buraya hiç uğramıyor: gövdeye
# ya da başlıklara dokunmuyor, yalnızca yol/metot/süre/durum kodunu görüyor.
uygulama.middleware("http")(istek_gunlugu_ara_katmani)

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
def saglik(yanit: Response, db: Session = Depends(oturum)) -> dict[str, str]:
    """
    Docker/Render sağlık kontrolü ve cihazın "sunucu ayakta mı" yoklaması.

    Sabit `{"durum": "ayakta"}` dönüyordu — backend ayakta ama veritabanına
    hiç ulaşamasa bile "sağlıklı" derdi. Render'ın health check'i buna
    güvenip kopuk bir veritabanıyla çalışan bir sürümü "sağlıklı" sayabilirdi
    (görev tanımı §3). Artık gerçekten bir sorgu deniyor.

    Parola, bağlantı adresi gibi gizli bilgi dönmüyor — yalnızca bağlı olup
    olmadığı.
    """
    try:
        db.execute(text("SELECT 1"))
        veritabani_durumu = "connected"
    except Exception:
        veritabani_durumu = "disconnected"
        yanit.status_code = status.HTTP_503_SERVICE_UNAVAILABLE

    return {
        "status": "healthy" if veritabani_durumu == "connected" else "unhealthy",
        "database": veritabani_durumu,
        "version": uygulama.version,
    }
