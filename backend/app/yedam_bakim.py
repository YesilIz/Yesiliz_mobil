"""
Rıza süresi bakımı — arka plan döngüsü.

**Niçin ayrı bir iş gerekiyor.** Erişim zaten her okumada kapanıyor
(`yedam_riza_yonetimi.gecerli_riza`), yani danışman süresi dolmuş bir
rızada veri göremiyor. Ama rıza metni "**silinir**" diyor, "artık
göstermiyoruz" demiyor: gerekçesi biten veri sunucuda durmamalı. Bu iş o
silmeyi yapıyor.

İkisi ayrı sorumluluk ve ikisi de gerekli: iş koşmazsa erişim yine kapalı
kalır (güvenli taraf), okuma kontrolü olmazsa iş koşana kadar veri
görünür kalırdı (güvensiz taraf).

**Neden ayrı bir zamanlayıcı kütüphanesi yok.** Tek bir günlük görev için
Celery/APScheduler getirmek, ikinci bir süreç, ikinci bir dağıtım hedefi
ve ikinci bir izleme yüzeyi demek (K5'in grafik kütüphanesi gerekçesiyle
aynı). `asyncio` döngüsü uygulamanın kendi sürecinde yaşıyor.

**Yalnızca API rolünde koşuyor.** İki servis aynı imajı paylaşıyor (K57);
ikisi birden koşarsa aynı satırları iki kez işlemeye çalışırlar. Silme
idempotent olduğu için zarar vermezdi ama gereksiz ve log'u kirletirdi.
"""

from __future__ import annotations

import asyncio
import logging

from app.veritabani import OturumFabrikasi
from app.yedam_riza_yonetimi import suresi_dolanlari_kapat

gunluk = logging.getLogger(__name__)

#: Bakım aralığı. Rızanın süresi gün çözünürlüklü; saatte bir koşmanın
#: kazancı yok, günde bir koşmak da 24 saate kadar gecikme demek.
#: Altı saat ortada duruyor ve erişim zaten anında kapanıyor.
ARALIK_SN = 6 * 60 * 60

#: Açılıştan sonraki ilk turun gecikmesi.
#:
#: Sıfır olamaz: açılışta yapılacak iş trafiği karşılamak, bakım değil —
#: hazır olmayan bir veritabanına ilk saniyede gitmek gereksiz bir hata
#: logluyor. Uzun da olamaz: sık yeniden başlayan bir sunucu bakımı hiç
#: koşturmazdı.
ILK_GECIKME_SN = 60


def bir_tur() -> int:
    """
    Tek bakım turu; kendi oturumunu açar ve commit eder.

    Ayrı fonksiyon çünkü test edilebilir olmalı ve döngü beklemekten
    ibaret. Hata yutulmuyor — çağıran karar veriyor.
    """
    with OturumFabrikasi() as db:
        kapanan = suresi_dolanlari_kapat(db)
        if kapanan:
            db.commit()
            gunluk.info("Süresi dolan YEDAM rızası kapatıldı", extra={"adet": kapanan})
        return kapanan


async def dongu(aralik_sn: int = ARALIK_SN) -> None:
    """
    Süresiz bakım döngüsü. İlk tur [ILK_GECIKME_SN] sonra koşuyor.

    Hata **yutulmuyor ama döngüyü öldürmüyor**: geçici bir veritabanı
    kesintisi yüzünden bakımın kalıcı olarak durması, sessizce birikin
    veri demek olurdu. Her hata loglanıyor (§9: yutulan istisnanın izi
    kalsın).
    """
    await asyncio.sleep(ILK_GECIKME_SN)
    while True:
        try:
            await asyncio.to_thread(bir_tur)
        except asyncio.CancelledError:
            raise
        except Exception:
            gunluk.exception("YEDAM bakım turu başarısız")
        await asyncio.sleep(aralik_sn)
