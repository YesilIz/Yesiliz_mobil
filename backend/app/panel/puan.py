"""
Puan düzeltmesi (`docs/panel-plani.md §9`, `CLAUDE.md §12 K56`).

**§4'ün "ceza puanı yok" kuralının tek istisnası** ve kuralın amacı
korunacak şekilde daraltılmış:

- **Otomatik ceza yok.** Hiçbir kural, worker ya da davranış puan
  düşürmüyor; yalnızca bir insanın tekil, gerekçeli işlemi.
- **Gerekçe zorunlu** ve kullanıcıya gösteriliyor. Gerekçesiz bir
  düşürme, §4'ün yasakladığı "ceza"nın ta kendisi olurdu.
- **Kazanılmış geri alınmıyor.** Ödül kademesi ve ağaç evresi cihazda
  yüksek-su-seviyesinden türüyor (U6); düzeltme bakiyeyi değiştiriyor,
  hak edilmişi değil.
- **İşlem anahtarı tek kullanımlık.** Aynı deltayı iki kez uygulamak geri
  dönülemez bir kullanıcı zararı; U2'nin (oturuma bağlı CSRF) bilinçli
  istisnası tam olarak burası.

Sunucu **toplam puanı hesaplamıyor**: cihaz hesaplıyor, sunucunun bildiği
değer bilgilendirici (U7, A13) ve panel formu bunu ekranda yazıyor.
"""

from __future__ import annotations

import datetime as dt
import secrets
from dataclasses import dataclass

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.modeller import (
    LigUyelik,
    Personel,
    PuanDuzeltme,
)

#: Tek işlemde yapılabilecek en büyük değişim.
#:
#: Sınır yoksa bir yazım hatası (fazladan sıfır) kullanıcının bakiyesini
#: anlamsız kılıyor ve geri alma yolu ikinci bir düzeltme olduğu için
#: denetim izi iki satırla kirleniyor. 5.000, en yüksek ödül kademesinin
#: yarısı — meşru bir telafi bunun altında kalıyor.
SINIR = 5_000


@dataclass(frozen=True)
class DuzeltmeSatiri:
    id: int
    delta: int
    gerekce: str
    zaman: dt.datetime
    personel_ad: str
    cihaz_uyguladi: bool


def islem_anahtari() -> str:
    """
    Forma gömülen tek kullanımlık anahtar.

    Sunucuda saklanmıyor: tekillik `puan_duzeltme.islem_anahtari` üzerinde
    ve ikinci gönderim orada çakışıyor. Ayrı bir "verilen anahtarlar"
    tablosu tutmak, temizliği gereken ikinci bir durum yaratırdı.
    """
    return secrets.token_hex(16)


def sunucudaki_haftalik_puan(db: Session, kullanici_id: int) -> int | None:
    """
    Panelin gösterdiği **bilgilendirici** değer.

    `LigUyelik.haftalik_puan` cihazın bildirdiği sayı (A13: istemci
    beyanlı) ve toplam puan değil — sunucu toplamı hiç bilmiyor. Form bu
    ayrımı ekranda yazıyor; yazmasaydı personel onu otoriteymiş gibi
    okurdu.
    """
    return db.scalar(
        select(func.sum(LigUyelik.haftalik_puan)).where(
            LigUyelik.kullanici_id == kullanici_id
        )
    )


def gecmis(db: Session, kullanici_id: int) -> list[DuzeltmeSatiri]:
    return [
        DuzeltmeSatiri(
            id=d.id,
            delta=d.delta,
            gerekce=d.gerekce,
            zaman=d.zaman,
            personel_ad=ad or "",
            cihaz_uyguladi=d.cihaz_uyguladi,
        )
        for d, ad in db.execute(
            select(PuanDuzeltme, Personel.ad)
            .outerjoin(Personel, Personel.id == PuanDuzeltme.personel_id)
            .where(PuanDuzeltme.kullanici_id == kullanici_id)
            .order_by(PuanDuzeltme.zaman.desc(), PuanDuzeltme.id.desc())
        ).all()
    ]


class DuzeltmeHatasi(ValueError):
    """Formun reddedilme sebebi; mesajı kullanıcıya (personele) gösteriliyor."""


def dogrula(delta: int) -> None:
    if delta == 0:
        # Sıfır bir düzeltme değil: denetim kaydına gerekçeli ama etkisiz
        # bir satır yazmak, izi okunmaz hâle getirir.
        raise DuzeltmeHatasi("Değişim sıfır olamaz")
    if abs(delta) > SINIR:
        raise DuzeltmeHatasi(f"Tek işlemde en fazla {SINIR} puan değiştirilebilir")


def uygula(
    db: Session,
    kullanici_id: int,
    delta: int,
    gerekce: str,
    personel_id: int,
    anahtar: str,
) -> PuanDuzeltme:
    """
    Düzeltmeyi yazar; commit etmez.

    Tekrar koruması **veritabanında**: aynı `islem_anahtari` ikinci kez
    yazılamıyor. Çağıran `IntegrityError`'ı yakalayıp kullanıcıya "bu
    işlem zaten uygulandı" demeli — sessizce ikinci bir satır yazmak,
    kullanıcının puanını iki kez değiştirmek olurdu.
    """
    dogrula(delta)
    kayit = PuanDuzeltme(
        kullanici_id=kullanici_id,
        delta=delta,
        gerekce=gerekce.strip()[:500],
        personel_id=personel_id,
        islem_anahtari=anahtar,
    )
    db.add(kayit)
    return kayit
