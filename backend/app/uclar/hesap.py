"""Kimlik doğrulama uçları."""

from __future__ import annotations

import datetime as dt
import logging

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ayarlar import ayarlar
from app.guvenlik import (
    erisim_jetonu,
    rastgele_jeton,
    sifre_dogrula,
    sifre_ozetle,
)
from app.modeller import Kullanici, SifreSifirlama, YenilemeJetonu
from app.semalar import (
    GirisIstek,
    Jetonlar,
    KayitIstek,
    SifreSifirlaIstek,
    SifreYenileIstek,
    YenileIstek,
)
from app.veritabani import oturum

gunluk = logging.getLogger(__name__)
yonlendirici = APIRouter(prefix="/auth", tags=["hesap"])


def _jetonlar(db: Session, kullanici: Kullanici) -> Jetonlar:
    a = ayarlar()
    yenileme = rastgele_jeton()
    db.add(
        YenilemeJetonu(
            kullanici_id=kullanici.id,
            jeton=yenileme,
            son_gecerlilik=dt.datetime.now(dt.timezone.utc) + dt.timedelta(days=a.yenileme_gun),
        )
    )
    db.commit()
    return Jetonlar(
        erisim=erisim_jetonu(kullanici.id),
        yenileme=yenileme,
        ad=kullanici.ad,
        eposta=kullanici.eposta,
    )


@yonlendirici.post("/kayit", response_model=Jetonlar, status_code=status.HTTP_201_CREATED)
def kayit(istek: KayitIstek, db: Session = Depends(oturum)) -> Jetonlar:
    eposta = istek.eposta.lower().strip()
    if db.scalar(select(Kullanici).where(Kullanici.eposta == eposta)):
        raise HTTPException(status.HTTP_409_CONFLICT, "Bu e-posta zaten kayıtlı")

    kullanici = Kullanici(
        eposta=eposta,
        ad=istek.ad.strip(),
        sifre_ozet=sifre_ozetle(istek.sifre),
    )
    db.add(kullanici)
    db.commit()
    db.refresh(kullanici)
    return _jetonlar(db, kullanici)


@yonlendirici.post("/giris", response_model=Jetonlar)
def giris(istek: GirisIstek, db: Session = Depends(oturum)) -> Jetonlar:
    kullanici = db.scalar(select(Kullanici).where(Kullanici.eposta == istek.eposta.lower().strip()))

    # Kullanıcı yoksa da şifre yanlışsa da AYNI mesaj: hangi e-postaların
    # kayıtlı olduğunu sızdırmamak için.
    if kullanici is None or not sifre_dogrula(kullanici.sifre_ozet, istek.sifre):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "E-posta ya da şifre hatalı")

    return _jetonlar(db, kullanici)


@yonlendirici.post("/yenile", response_model=Jetonlar)
def yenile(istek: YenileIstek, db: Session = Depends(oturum)) -> Jetonlar:
    kayit_ = db.scalar(select(YenilemeJetonu).where(YenilemeJetonu.jeton == istek.yenileme))
    simdi = dt.datetime.now(dt.timezone.utc)

    if kayit_ is None or kayit_.iptal or _gecmis(kayit_.son_gecerlilik, simdi):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Yenileme jetonu geçersiz")

    # Dönüşümlü jeton: eski jeton iptal edilip yenisi veriliyor. Çalınan bir
    # yenileme jetonu ikinci kez kullanılamıyor.
    kayit_.iptal = True
    kullanici = db.get(Kullanici, kayit_.kullanici_id)
    if kullanici is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Kullanıcı bulunamadı")
    return _jetonlar(db, kullanici)


@yonlendirici.post("/sifre-sifirla", status_code=status.HTTP_202_ACCEPTED)
def sifre_sifirla(istek: SifreSifirlaIstek, db: Session = Depends(oturum)) -> dict[str, str]:
    """
    Sıfırlama jetonu üretir.

    **E-posta gönderimi yok** — SMTP yapılandırması bu sürümün kapsamında
    değil. Jeton sunucu günlüğüne yazılıyor; e-posta sağlayıcısı
    bağlandığında yalnızca bu fonksiyonun son satırı değişecek.

    Kayıtlı olmayan e-postada da **202** dönüyor: hangi adreslerin kayıtlı
    olduğunu sızdırmamak için.
    """
    kullanici = db.scalar(select(Kullanici).where(Kullanici.eposta == istek.eposta.lower().strip()))
    if kullanici is None:
        return {"durum": "kabul"}

    jeton = rastgele_jeton()
    db.add(
        SifreSifirlama(
            kullanici_id=kullanici.id,
            jeton=jeton,
            son_gecerlilik=dt.datetime.now(dt.timezone.utc)
            + dt.timedelta(hours=ayarlar().sifirlama_saat),
        )
    )
    db.commit()
    gunluk.info("Şifre sıfırlama jetonu üretildi: kullanici=%s jeton=%s", kullanici.id, jeton)
    return {"durum": "kabul"}


@yonlendirici.post("/sifre-yenile")
def sifre_yenile(istek: SifreYenileIstek, db: Session = Depends(oturum)) -> dict[str, str]:
    kayit_ = db.scalar(select(SifreSifirlama).where(SifreSifirlama.jeton == istek.jeton))
    simdi = dt.datetime.now(dt.timezone.utc)

    if kayit_ is None or kayit_.kullanildi or _gecmis(kayit_.son_gecerlilik, simdi):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Sıfırlama bağlantısı geçersiz")

    kullanici = db.get(Kullanici, kayit_.kullanici_id)
    if kullanici is None:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Kullanıcı bulunamadı")

    kullanici.sifre_ozet = sifre_ozetle(istek.yeniSifre)
    kayit_.kullanildi = True

    # Şifre değişince tüm oturumlar kapanıyor — hesabı ele geçiren varsa
    # erişimi de bitsin.
    for jeton in db.scalars(
        select(YenilemeJetonu).where(YenilemeJetonu.kullanici_id == kullanici.id)
    ):
        jeton.iptal = True

    db.commit()
    return {"durum": "tamam"}


def _gecmis(son: dt.datetime, simdi: dt.datetime) -> bool:
    """SQLite saat dilimi bilgisini düşürüyor; karşılaştırmadan önce eşitle."""
    if son.tzinfo is None:
        son = son.replace(tzinfo=dt.timezone.utc)
    return son < simdi
