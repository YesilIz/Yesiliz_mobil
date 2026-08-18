"""
Gösterge metrikleri — **toplu ve anonim** (`docs/panel-plani.md §5.1`).

Buradaki hiçbir sorgu tekil kullanıcı döndürmüyor. P9: admin varsayılan
olarak bir kullanıcının davranış verisini görmüyor; tekil veri ancak KVKK
talebi işlemi olarak, gerekçeyle ve denetim kaydıyla açılıyor.

Sorgular ayrı modülde çünkü router'dan bağımsız test edilebilmeleri
gerekiyor: bir metriğin yanlış sayması, panelde fark edilmesi en zor hata
türü — ekranda makul görünen bir sayı duruyor.
"""

from __future__ import annotations

import datetime as dt
from dataclasses import dataclass, field

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.modeller import (
    EbeveynBaglanti,
    GunlukOzet,
    Heartbeat,
    Kullanici,
    LigUyelik,
    RiskSonuc,
)
from app.zaman import yerel_bugun

#: Bant etiketleri — cihazdaki `Bant` enum'uyla aynı sıra.
BANTLAR = ("AZ", "ORTA", "COK")

BANT_ETIKETI = {"AZ": "Az riskli", "ORTA": "Orta", "COK": "Yüksek"}


@dataclass(frozen=True)
class Gosterge:
    kullanici_sayisi: int
    aktif_7: int
    aktif_30: int
    #: Bant → kullanıcı sayısı. Yalnızca `veri_durumu = HESAPLANDI`
    #: satırlardan; ölçülmemiş gün "az riskli" sayılmaz (CLAUDE.md §12 K30).
    bant_dagilimi: dict[str, int] = field(default_factory=dict)
    ortalama_ekran_dk: int = 0
    lig_katilimi: int = 0
    ebeveyn_eslesmesi: int = 0
    #: Ölçüm penceresi içinde hiç `HESAPLANDI` risk satırı olmayan kullanıcı.
    bandi_olmayan: int = 0

    @property
    def bant_toplami(self) -> int:
        return sum(self.bant_dagilimi.values())

    def bant_yuzdesi(self, bant: str) -> int:
        toplam = self.bant_toplami
        return round(100 * self.bant_dagilimi.get(bant, 0) / toplam) if toplam else 0


def _aktif_sayisi(db: Session, gun: int, simdi: dt.datetime) -> int:
    """
    Son `gun` gün içinde sunucuya sinyal gönderen cihaz.

    `heartbeat` kullanılıyor çünkü `gunluk_ozet` yalnızca "Yeşilay ile
    paylaşım" rızası açık kullanıcılardan geliyor (K19); ikisini karıştırmak
    aktifliği rızayla ölçmek olurdu. Heartbeat'in kendisi de rıza kapısında,
    yani bu sayı **rıza vermiş kullanıcıların** aktifliği — ekranda böyle
    yazıyor.
    """
    esik = simdi - dt.timedelta(days=gun)
    return db.scalar(
        select(func.count()).select_from(Heartbeat).where(Heartbeat.son_sinyal >= esik)
    ) or 0


def _bant_dagilimi(db: Session, bugun: dt.date, pencere_gun: int) -> dict[str, int]:
    """
    Kullanıcı başına **en son** hesaplanmış bandın dağılımı.

    Gün gün saymak, uygulamayı çok açan kullanıcıyı birden çok kez saymak
    olurdu. Her kullanıcının penceredeki en son `HESAPLANDI` satırı alınıyor.
    """
    bas = bugun - dt.timedelta(days=pencere_gun - 1)

    son_tarih = (
        select(
            RiskSonuc.kullanici_id.label("kid"),
            func.max(RiskSonuc.tarih).label("tarih"),
        )
        .where(
            RiskSonuc.tarih >= bas,
            RiskSonuc.tarih <= bugun,
            RiskSonuc.veri_durumu == "HESAPLANDI",
        )
        .group_by(RiskSonuc.kullanici_id)
        .subquery()
    )

    satirlar = db.execute(
        select(RiskSonuc.gosterilen_bant, func.count())
        .join(
            son_tarih,
            (RiskSonuc.kullanici_id == son_tarih.c.kid)
            & (RiskSonuc.tarih == son_tarih.c.tarih),
        )
        .where(RiskSonuc.veri_durumu == "HESAPLANDI")
        .group_by(RiskSonuc.gosterilen_bant)
    ).all()

    dagilim = {b: 0 for b in BANTLAR}
    for bant, sayi in satirlar:
        if bant in dagilim:
            dagilim[bant] = sayi
    return dagilim


def _ortalama_ekran_dk(db: Session, bugun: dt.date, pencere_gun: int) -> int:
    """
    Penceredeki günlük özetlerin ortalama toplam ekran süresi.

    `olayVarMi` karşılığı sunucuda yok; cihaz ölçülmemiş günü zaten
    göndermiyor (`SenkronDeposu` yalnızca dolu özetleri yolluyor). Yine de
    toplamı sıfır olan satırlar ortalamayı aşağı çekmesin diye eleniyor —
    "satır var ≠ veri var" (CLAUDE.md §9).
    """
    bas = bugun - dt.timedelta(days=pencere_gun - 1)
    toplam_ifade = (
        GunlukOzet.social_dk
        + GunlukOzet.entertainment_dk
        + GunlukOzet.games_dk
        + GunlukOzet.communication_dk
        + GunlukOzet.productivity_dk
        + GunlukOzet.education_dk
        + GunlukOzet.health_dk
        + GunlukOzet.other_dk
    )
    ortalama = db.scalar(
        select(func.avg(toplam_ifade)).where(
            GunlukOzet.tarih >= bas,
            GunlukOzet.tarih <= bugun,
            toplam_ifade > 0,
        )
    )
    return round(ortalama) if ortalama else 0


def gostergeyi_hesapla(
    db: Session,
    simdi: dt.datetime | None = None,
    pencere_gun: int = 7,
) -> Gosterge:
    """Panelin gösterge ekranını besleyen tek çağrı."""
    simdi = simdi or dt.datetime.now(dt.timezone.utc)
    bugun = yerel_bugun()

    kullanici_sayisi = db.scalar(select(func.count()).select_from(Kullanici)) or 0
    dagilim = _bant_dagilimi(db, bugun, pencere_gun)

    return Gosterge(
        kullanici_sayisi=kullanici_sayisi,
        aktif_7=_aktif_sayisi(db, 7, simdi),
        aktif_30=_aktif_sayisi(db, 30, simdi),
        bant_dagilimi=dagilim,
        ortalama_ekran_dk=_ortalama_ekran_dk(db, bugun, pencere_gun),
        lig_katilimi=db.scalar(
            select(func.count(func.distinct(LigUyelik.kullanici_id)))
        ) or 0,
        ebeveyn_eslesmesi=db.scalar(
            select(func.count()).select_from(EbeveynBaglanti)
        ) or 0,
        bandi_olmayan=max(0, kullanici_sayisi - sum(dagilim.values())),
    )
