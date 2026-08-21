"""
Maddi ödül stoğu ve K28 kapısı (`docs/panel-plani.md §5.4`).

**A4'ün teknik ayağı burada çözülüyor.** K28 maddi ödülleri `PLANLANAN`
tutuyordu çünkü teslimatı karşılayacak bir tedarik entegrasyonu yoktu ve
olmayan bir teslimatı "kazandın" demek §4'ü ihlal ediyordu. Kapı artık
sabit bir `False` değil, **stoğun kendisi**: panelden parti yüklenince
kademe açılıyor, stok bitince kapanıyor.

Finansman ayağı hâlâ Yeşilay'da — panel yalnızca finansman geldiğinde
açılacak kapıyı hazır tutuyor.

**Seçim stoktan düşüyor.** Yoksa stok biterken aynı anda gelen iki seçim
ikisi de kabul edilir ve karşılanamayan bir vaat üretilirdi; K28'in
"geri alma yok" ilkesi bunu geri alınamaz kılardı.
"""

from __future__ import annotations

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.modeller import OdulStok

#: **Her seçenek stok istiyor — fidan bağışı dahil.**
#:
#: İlk yazımda fidan bağışı muaf tutulmuştu ("tedarik kalemi değil, bir
#: bağış") ama bu yanlış: bağış da bir maliyet ve A4'ün (finansman)
#: kapsamında. Muaf tutmak, finansman gelmeden fidan bağışını seçilebilir
#: yapardı — yani K28'in tam olarak engellediği şeyi.
#:
#: Stok satırı yoksa kapalı: "satır yok" "sınırsız" demek değil.


def acik_mi(db: Session, kademe_id: str, secenek_id: str) -> bool:
    """
    Bu seçenek şu an seçilebilir mi?

    Stok satırı **yoksa kapalı** — fail-closed (§4, K37). "Satır yok"
    "sınırsız" demek değil; hiç parti yüklenmemişse teslim edilecek bir
    şey de yok.
    """
    satir = db.scalar(
        select(OdulStok).where(
            OdulStok.kademe_id == kademe_id, OdulStok.secenek_id == secenek_id
        )
    )
    return satir is not None and satir.kalan > 0


def kademe_acik_mi(db: Session, kademe_id: str, secenekler: set[str]) -> bool:
    """Kademede **en az bir** seçenek açıksa kademe açık."""
    return any(acik_mi(db, kademe_id, s) for s in secenekler)


def dus(db: Session, kademe_id: str, secenek_id: str) -> bool:
    """
    Stoktan bir adet düşer; commit etmez. Stok yoksa `False`.

    Satır `with_for_update` ile kilitleniyor: aynı anda gelen iki seçim
    son adedi ikiye bölemez. K28 seçimi geri alınamaz kıldığı için burada
    yapılan bir fazla-satış düzeltilemez.
    """
    satir = db.scalar(
        select(OdulStok)
        .where(OdulStok.kademe_id == kademe_id, OdulStok.secenek_id == secenek_id)
        .with_for_update()
    )
    if satir is None or satir.kalan <= 0:
        return False

    satir.kalan -= 1
    return True


def geri_ver(db: Session, kademe_id: str, secenek_id: str) -> None:
    """
    İptal edilen teslimatta stoğu geri verir; commit etmez.

    **Yalnızca teslimat iptalinde.** Hesap silmede geri verilmiyor: kupon
    zaten ayrılmıştı ve havuza koymak teslim edilmiş bir ödülü ikinci kez
    dağıtmaya yol açabilirdi (`app/kullanici_silme.py`).
    """
    satir = db.scalar(
        select(OdulStok)
        .where(OdulStok.kademe_id == kademe_id, OdulStok.secenek_id == secenek_id)
        .with_for_update()
    )
    if satir is not None and satir.kalan < satir.toplam:
        satir.kalan += 1
