"""
Personel yönetimi (`docs/panel-plani.md §5`, S1 kararı).

S1 (18 Ağu): personel hesaplarını **Yeşilay kendi açacak**, yani panelde
arayüz şart. İlk `admin` hâlâ CLI'dan (`python -m app.personel_ekle`) —
tavuk-yumurta.

**Davet e-postayla değil geçici parolayla** (A15: e-posta sağlayıcısı yok).
Parola bir kez gösteriliyor, admin kurumsal kanaldan iletiyor, personel
ilk girişte hem parolasını değiştiriyor hem TOTP kuruyor. İkisi de
`personel_oturumu` bağımlılığında zorunlu; atlanamıyor.

Personel **silinmiyor, askıya alınıyor**: `denetim_kaydi.personel_id`
yabancı anahtar ve denetim izi silinemez olmalı. Ayrılan personelin
kayıtları, kimin ne yaptığını okunabilir tutuyor.
"""

from __future__ import annotations

import datetime as dt
import secrets
import string
from dataclasses import dataclass

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.guvenlik import sifre_ozetle
from app.modeller import (
    DenetimKaydi,
    Personel,
    PersonelDurumu,
    PersonelOturum,
    PersonelRolu,
)

#: Üretilen geçici parolanın uzunluğu. `personel_ekle.ASGARI_SIFRE`'den
#: uzun: bu parola bir insan kanalından geçiyor ve kısa olması için
#: hiçbir sebep yok — kullanıcı onu ezberlemeyecek, bir kez yapıştıracak.
GECICI_SIFRE_UZUNLUK = 20

#: Karışan karakterler (0/O, 1/l/I) dışarıda: parola elle yazılabilmeli.
_ALFABE = "".join(
    c for c in (string.ascii_letters + string.digits) if c not in "0O1lI"
)


def gecici_sifre() -> str:
    return "".join(secrets.choice(_ALFABE) for _ in range(GECICI_SIFRE_UZUNLUK))


@dataclass(frozen=True)
class PersonelSatiri:
    id: int
    ad: str
    eposta: str
    rol: str
    durum: str
    totp_kurulu: bool
    parola_bekliyor: bool
    son_giris: dt.datetime | None
    acik_oturum: int
    denetim_sayisi: int


@dataclass(frozen=True)
class OturumSatiri:
    id: int
    personel_id: int
    ip: str
    tarayici: str
    olusturma: dt.datetime
    son_erisim: dt.datetime
    bu_oturum: bool


def listele(db: Session) -> list[PersonelSatiri]:
    acik = dict(
        db.execute(
            select(PersonelOturum.personel_id, func.count())
            .where(PersonelOturum.iptal.is_(False))
            .group_by(PersonelOturum.personel_id)
        ).all()
    )
    kayitlar = dict(
        db.execute(
            select(DenetimKaydi.personel_id, func.count()).group_by(
                DenetimKaydi.personel_id
            )
        ).all()
    )

    return [
        PersonelSatiri(
            id=p.id,
            ad=p.ad,
            eposta=p.eposta,
            rol=p.rol,
            durum=p.durum,
            totp_kurulu=bool(p.totp_sir),
            parola_bekliyor=p.parola_degistirmeli,
            son_giris=p.son_giris,
            acik_oturum=acik.get(p.id, 0),
            denetim_sayisi=kayitlar.get(p.id, 0),
        )
        for p in db.scalars(select(Personel).order_by(Personel.ad)).all()
    ]


def oturumlar(db: Session, personel_id: int, bu_oturum_id: int | None) -> list[OturumSatiri]:
    """Açık oturumlar — "kaybolan dizüstü" senaryosu (`§2`)."""
    return [
        OturumSatiri(
            id=o.id,
            personel_id=o.personel_id,
            ip=o.ip,
            tarayici=o.tarayici,
            olusturma=o.olusturma,
            son_erisim=o.son_erisim,
            bu_oturum=o.id == bu_oturum_id,
        )
        for o in db.scalars(
            select(PersonelOturum)
            .where(
                PersonelOturum.personel_id == personel_id,
                PersonelOturum.iptal.is_(False),
            )
            .order_by(PersonelOturum.son_erisim.desc(), PersonelOturum.id.desc())
        ).all()
    ]


def ekle(db: Session, eposta: str, ad: str, rol: PersonelRolu) -> tuple[Personel, str]:
    """
    Yeni personel açar ve **bir kez gösterilecek** geçici parolayı döndürür.

    Parola veritabanına düz yazılmıyor (argon2id özeti), yani bu değeri
    yeniden okumanın yolu yok. Kaybolursa admin parolayı sıfırlıyor.
    """
    ham = gecici_sifre()
    p = Personel(
        eposta=eposta.lower().strip(),
        ad=ad.strip(),
        sifre_ozet=sifre_ozetle(ham),
        rol=rol.value,
        durum=PersonelDurumu.AKTIF.value,
        # TOTP boş: ilk girişte kurulum ekranına düşüyor.
        totp_sir="",
        parola_degistirmeli=True,
    )
    db.add(p)
    return p, ham


def sifre_sifirla(db: Session, personel: Personel) -> str:
    """
    Parolayı yeni bir geçici parolayla değiştirir ve oturumları düşürür.

    Telefonunu ve parolasını birlikte kaybeden personelin yolu. TOTP
    sırrına dokunulmuyor: parola sıfırlamak ikinci faktörü kaldırmaz,
    yoksa parolayı sıfırlayabilen admin 2FA'yı da atlamış olurdu.
    """
    ham = gecici_sifre()
    personel.sifre_ozet = sifre_ozetle(ham)
    personel.parola_degistirmeli = True
    personel.basarisiz_deneme = 0
    personel.kilit_bitis = None
    return ham
