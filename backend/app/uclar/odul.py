"""Ödül seçimi — bazı kademelerde market kuponu yerine fidan bağışı gibi alternatifler."""

from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.guvenlik import kullaniciyi_kilitle, mevcut_kullanici
from app.modeller import Kullanici, OdulSecimi
from app.semalar import OdulSecimIstek, OdulSecimiDto
from app.veritabani import oturum

yonlendirici = APIRouter(prefix="/odul", tags=["odul"])

# Sunucu puan hak edişini henüz bağımsız hesaplamıyor; yine de istemci
# keyfî bir kimlikle kupon/bağış kaydı oluşturamamalı. Bu sözleşme
# `OdulViewModel.KADEMELER` ve `docs/spec-puan.md §9.1` ile aynıdır.
SECILEBILIR_ODULLER = {
    "kupon_market": {"market_kuponu", "fidan_bagisi"},
    "kupon_spor": {"kupon_spor_kitap", "fidan_bagisi"},
}


@yonlendirici.post("/sec", response_model=OdulSecimiDto, status_code=status.HTTP_201_CREATED)
def sec(
    istek: OdulSecimIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> OdulSecimiDto:
    """
    Bir kademe için seçim **bir kez** yapılır.

    Tekil kısıt (`kullanici_id`, `kademe_id`) bunu veritabanı seviyesinde
    garanti ediyor; burada önce bakılıyor ki ikinci istek 500 yerine
    anlamlı bir 409 dönsün.
    """
    if istek.secenekId not in SECILEBILIR_ODULLER.get(istek.kademeId, set()):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Geçersiz ödül seçimi")

    kullaniciyi_kilitle(db, kullanici.id)
    var = db.scalar(
        select(OdulSecimi).where(
            OdulSecimi.kullanici_id == kullanici.id,
            OdulSecimi.kademe_id == istek.kademeId,
        )
    )
    if var is not None:
        raise HTTPException(status.HTTP_409_CONFLICT, "Bu kademe için seçim zaten yapılmış")

    kayit = OdulSecimi(
        kullanici_id=kullanici.id,
        kademe_id=istek.kademeId,
        secenek_id=istek.secenekId,
    )
    db.add(kayit)
    db.commit()
    db.refresh(kayit)
    return OdulSecimiDto(kademeId=kayit.kademe_id, secenekId=kayit.secenek_id)


@yonlendirici.get("/secimlerim", response_model=list[OdulSecimiDto])
def secimlerim(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> list[OdulSecimiDto]:
    """Telefon değişince/yeniden kurulumda geçmiş seçimleri geri yükler."""
    kayitlar = db.scalars(
        select(OdulSecimi).where(OdulSecimi.kullanici_id == kullanici.id)
    ).all()
    return [OdulSecimiDto(kademeId=k.kademe_id, secenekId=k.secenek_id) for k in kayitlar]
