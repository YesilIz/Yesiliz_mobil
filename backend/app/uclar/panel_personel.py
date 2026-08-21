"""
Personel yönetimi uçları (`docs/panel-plani.md §5`, S1).

Hepsi **yalnızca `ADMIN`**. `OPERASYON` bu bölümü hiç görmüyor: personel
açmak ve rol atmak yetkinin kendisini dağıtmak demek.
"""

from __future__ import annotations

from fastapi import APIRouter, Depends, Form, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.modeller import Personel, PersonelDurumu, PersonelRolu
from app.panel import oturum as panel_oturum
from app.panel.denetim import denetle
from app.panel.personel_yonetim import ekle, listele, oturumlar, sifre_sifirla
from app.panel.sablon import yanit, yonlendir
from app.veritabani import oturum

yonlendirici = APIRouter(prefix="/yonetim", tags=["panel-personel"])

SADECE_ADMIN = (PersonelRolu.ADMIN,)


def _hedef(db: Session, personel_id: int) -> Personel:
    p = db.scalar(
        select(Personel).where(Personel.id == personel_id).with_for_update()
    )
    if p is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Personel bulunamadı")
    return p


def _rol_coz(ham: str) -> PersonelRolu:
    try:
        return PersonelRolu(ham.strip())
    except ValueError:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_CONTENT, "Geçersiz rol") from None


def _sayfa(istek: Request, db: Session, personel: Personel, **ek):
    bu_oturum = getattr(istek.state, "oturum", None)
    return yanit(
        istek,
        "personel.html",
        {
            "etkin": "personel",
            "satirlar": listele(db),
            "roller": [(r.value, r.value) for r in PersonelRolu],
            "ben": personel,
            "oturumlar": oturumlar(
                db, personel.id, bu_oturum.id if bu_oturum is not None else None
            ),
            **ek,
        },
    )


@yonlendirici.get("/personel")
def personel_listesi(
    istek: Request,
    db: Session = Depends(oturum),
    personel: Personel = Depends(panel_oturum.personel_gerek(*SADECE_ADMIN)),
):
    return _sayfa(istek, db, personel)


@yonlendirici.post("/personel")
def personel_ekle_uc(
    istek: Request,
    eposta: str = Form(...),
    ad: str = Form(...),
    rol: str = Form(...),
    db: Session = Depends(oturum),
    personel: Personel = Depends(panel_oturum.personel_gerek(*SADECE_ADMIN)),
):
    """
    Yeni personel açar; geçici parola **bir kez** gösterilir.

    Parola veritabanına düz yazılmıyor, yani bu ekrandan sonra okunamıyor.
    Kaybolursa sıfırlanıyor — kurtarılamayan bir sır, kurtarılabilir bir
    sırdan iyi.
    """
    temiz = eposta.lower().strip()
    if db.scalar(select(Personel).where(Personel.eposta == temiz)) is not None:
        raise HTTPException(status.HTTP_409_CONFLICT, "Bu e-posta zaten kayıtlı")

    yeni, ham_sifre = ekle(db, temiz, ad, _rol_coz(rol))
    db.flush()
    denetle(
        db,
        personel,
        eylem="personel.ekle",
        hedef_tur="personel",
        hedef_id=yeni.id,
        gerekce=f"Yeni personel: {temiz} ({rol})",
        istek=istek,
    )
    db.commit()

    return _sayfa(
        istek,
        db,
        personel,
        yeni_personel=yeni,
        gecici_sifre=ham_sifre,
    )


@yonlendirici.post("/personel/{personel_id}/rol")
def personel_rol(
    istek: Request,
    personel_id: int,
    rol: str = Form(...),
    db: Session = Depends(oturum),
    personel: Personel = Depends(panel_oturum.personel_gerek(*SADECE_ADMIN)),
):
    """
    Rol atar.

    Admin **kendi rolünü düşüremiyor**: tek adminin kendini `gozlemci`
    yapması, panelde hiç admin bırakmaz ve kilidi açmanın tek yolu
    veritabanına elle girmek olurdu.
    """
    hedef = _hedef(db, personel_id)
    if hedef.id == personel.id:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT, "Kendi rolünü değiştiremezsin"
        )

    yeni_rol = _rol_coz(rol)
    eski = hedef.rol
    hedef.rol = yeni_rol.value
    # Rol değişince açık oturumlar düşüyor: yetkisi daralan personelin
    # elindeki sayfa, yenilenene kadar eski yetkiyle çalışmaya devam
    # ederdi.
    panel_oturum.oturumlari_dusur(db, hedef.id)

    denetle(
        db,
        personel,
        eylem="personel.rol",
        hedef_tur="personel",
        hedef_id=hedef.id,
        gerekce=f"Rol {eski} → {yeni_rol.value}",
        istek=istek,
    )
    db.commit()
    return yonlendir(istek, "/yonetim/personel")


@yonlendirici.post("/personel/{personel_id}/durum")
def personel_durum(
    istek: Request,
    personel_id: int,
    durum: str = Form(...),
    db: Session = Depends(oturum),
    personel: Personel = Depends(panel_oturum.personel_gerek(*SADECE_ADMIN)),
):
    """
    Personeli askıya alır ya da geri açar. **Silme yok.**

    `denetim_kaydi.personel_id` yabancı anahtar ve denetim izi silinemez
    olmalı; ayrılan personelin satırları kimin ne yaptığını okunabilir
    tutuyor.
    """
    hedef = _hedef(db, personel_id)
    if hedef.id == personel.id:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT, "Kendi hesabını askıya alamazsın"
        )

    try:
        yeni = PersonelDurumu(durum.strip())
    except ValueError:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_CONTENT, "Geçersiz durum") from None

    hedef.durum = yeni.value
    if yeni is PersonelDurumu.ASKIDA:
        panel_oturum.oturumlari_dusur(db, hedef.id)

    denetle(
        db,
        personel,
        eylem="personel.durum",
        hedef_tur="personel",
        hedef_id=hedef.id,
        gerekce=f"Durum → {yeni.value}",
        istek=istek,
    )
    db.commit()
    return yonlendir(istek, "/yonetim/personel")


@yonlendirici.post("/personel/{personel_id}/sifre-sifirla")
def personel_sifre_sifirla(
    istek: Request,
    personel_id: int,
    db: Session = Depends(oturum),
    personel: Personel = Depends(panel_oturum.personel_gerek(*SADECE_ADMIN)),
):
    """
    Yeni geçici parola üretir. **TOTP sırrına dokunmuyor** — parolayı
    sıfırlayabilen admin ikinci faktörü de atlayabilseydi 2FA'nın anlamı
    kalmazdı.
    """
    hedef = _hedef(db, personel_id)
    ham = sifre_sifirla(db, hedef)
    panel_oturum.oturumlari_dusur(db, hedef.id)

    denetle(
        db,
        personel,
        eylem="personel.sifre_sifirla",
        hedef_tur="personel",
        hedef_id=hedef.id,
        gerekce="Parola sıfırlandı, geçici parola üretildi",
        istek=istek,
    )
    db.commit()
    return _sayfa(istek, db, personel, yeni_personel=hedef, gecici_sifre=ham)


@yonlendirici.post("/oturum/{oturum_id}/dusur")
def oturum_dusur(
    istek: Request,
    oturum_id: int,
    db: Session = Depends(oturum),
    personel: Personel = Depends(panel_oturum.personel_gerek(*SADECE_ADMIN)),
):
    """
    Tek bir açık oturumu kapatır — "kaybolan dizüstü" (`§2`).

    Yalnızca **kendi** oturumları düşürülebiliyor. Başkasının oturumunu
    tek tek kapatmak için doğru araç `personel/{id}/durum` (askıya alma):
    orada karar gerekçesiyle kaydediliyor, burada tek tıkla sessizce
    yapılırdı.
    """
    kayit = db.get(panel_oturum.PersonelOturum, oturum_id)
    if kayit is None or kayit.personel_id != personel.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Oturum bulunamadı")

    kayit.iptal = True
    denetle(
        db,
        personel,
        eylem="personel.oturum_dusur",
        hedef_tur="personel_oturum",
        hedef_id=oturum_id,
        gerekce="Kendi oturumu kapatıldı",
        istek=istek,
    )
    db.commit()
    return yonlendir(istek, "/yonetim/personel")
