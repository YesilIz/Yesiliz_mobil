"""
Denetim kaydı görüntüleyici sorguları (`docs/panel-plani.md §5.8`).

**Yalnızca okuma.** Bu modülde `insert`/`update`/`delete` yok ve olmayacak:
kaydın silinemezliği kod disiplini + veritabanı tetikleyicisi + F6'daki
ayrı DB rolüyle üç katmanlı korunuyor (`modeller.DenetimKaydi`).

Filtreler kesişimle çalışıyor (VE): denetim izinde arama, "şu personel şu
kullanıcıya ne yaptı" sorusuyla başlıyor ve iki kriter aynı anda gerekiyor.
"""

from __future__ import annotations

import datetime as dt
from dataclasses import dataclass

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.modeller import DenetimKaydi, Personel

SAYFA_BOYU = 50


@dataclass(frozen=True)
class KayitSatiri:
    id: int
    zaman: dt.datetime
    personel_ad: str
    personel_eposta: str
    eylem: str
    hedef_tur: str
    hedef_id: int | None
    gerekce: str
    ip: str
    detay: str


@dataclass(frozen=True)
class Filtre:
    personel_id: int | None = None
    kullanici_id: int | None = None
    eylem: str = ""
    baslangic: dt.date | None = None
    bitis: dt.date | None = None


def _kosullar(filtre: Filtre) -> list:
    kosullar = []
    if filtre.personel_id is not None:
        kosullar.append(DenetimKaydi.personel_id == filtre.personel_id)
    if filtre.kullanici_id is not None:
        # `hedef_tur` de aranıyor: `hedef_id` tablo başına ayrı bir sayı
        # uzayı, yani 7 numaralı kullanıcı ile 7 numaralı başvuru aynı
        # değil. İkisini karıştırmak yanlış kaydı gösterirdi.
        kosullar.append(DenetimKaydi.hedef_tur == "kullanici")
        kosullar.append(DenetimKaydi.hedef_id == filtre.kullanici_id)
    if filtre.eylem.strip():
        kosullar.append(DenetimKaydi.eylem.startswith(filtre.eylem.strip()))
    if filtre.baslangic is not None:
        kosullar.append(DenetimKaydi.zaman >= _gun_basi(filtre.baslangic))
    if filtre.bitis is not None:
        # Bitiş günü DAHİL: kullanıcı "18–20 Ağustos" derken 20'yi de
        # kastediyor. Ham `<= bitis` 20 Ağustos 00:00'dan sonrasını atardı.
        kosullar.append(DenetimKaydi.zaman < _gun_basi(filtre.bitis + dt.timedelta(days=1)))
    return kosullar


def _gun_basi(gun: dt.date) -> dt.datetime:
    return dt.datetime.combine(gun, dt.time.min, tzinfo=dt.timezone.utc)


def listele(db: Session, filtre: Filtre, sayfa: int = 0) -> tuple[list[KayitSatiri], int]:
    kosullar = _kosullar(filtre)

    toplam = (
        db.scalar(select(func.count()).select_from(DenetimKaydi).where(*kosullar)) or 0
    )

    satirlar = db.execute(
        select(DenetimKaydi, Personel.ad, Personel.eposta)
        .join(Personel, Personel.id == DenetimKaydi.personel_id)
        .where(*kosullar)
        # `id` eşitlik bozucu: aynı saniyeye düşen iki kayıtta SQLite sıra
        # garanti etmiyor (CLAUDE.md §9 "gün çözünürlüklü ORDER BY").
        .order_by(DenetimKaydi.zaman.desc(), DenetimKaydi.id.desc())
        .offset(sayfa * SAYFA_BOYU)
        .limit(SAYFA_BOYU)
    ).all()

    return [
        KayitSatiri(
            id=k.id,
            zaman=k.zaman,
            personel_ad=ad,
            personel_eposta=eposta,
            eylem=k.eylem,
            hedef_tur=k.hedef_tur,
            hedef_id=k.hedef_id,
            gerekce=k.gerekce,
            ip=k.ip,
            detay=k.detay,
        )
        for k, ad, eposta in satirlar
    ], toplam


def personel_secenekleri(db: Session) -> list[tuple[int, str]]:
    """Filtre açılır listesi — yalnızca kaydı olan personel."""
    return [
        (p.id, f"{p.ad} · {p.eposta}")
        for p in db.scalars(
            select(Personel)
            .where(Personel.id.in_(select(DenetimKaydi.personel_id).distinct()))
            .order_by(Personel.ad)
        ).all()
    ]


def eylem_secenekleri(db: Session) -> list[str]:
    """Kayıtlarda geçen eylem adları — sabit liste tutmak yerine veriden."""
    return list(
        db.scalars(select(DenetimKaydi.eylem).distinct().order_by(DenetimKaydi.eylem)).all()
    )
