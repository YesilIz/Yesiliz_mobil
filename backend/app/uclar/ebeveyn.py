"""Ebeveyn eşleştirme ve özet."""

from __future__ import annotations

import datetime as dt
import secrets

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ayarlar import ayarlar
from app.guvenlik import mevcut_kullanici
from app.modeller import (
    EbeveynBaglanti,
    EslestirmeKodu,
    GunlukOzet,
    Heartbeat,
    Kullanici,
    RiskSonuc,
)
from app.semalar import EbeveynOzetYanit, EslestirIstek, EslestirmeKoduIstek, KodYanit
from app.veritabani import oturum

yonlendirici = APIRouter(prefix="/ebeveyn", tags=["ebeveyn"])

ROLLER = {"ebeveyn", "cocuk"}


@yonlendirici.post("/kod", response_model=KodYanit)
def kod_uret(
    istek: EslestirmeKoduIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> KodYanit:
    """6 haneli kod, **10 dakika** geçerli (tasarım 21'deki metinle birebir)."""
    if istek.rol not in ROLLER:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Rol 'ebeveyn' ya da 'cocuk' olmalı")

    a = ayarlar()

    # Aynı kullanıcının bekleyen eski kodları iptal ediliyor: aynı anda iki
    # geçerli kod, hangisinin kullanıldığını belirsizleştirirdi.
    for eski in db.scalars(
        select(EslestirmeKodu).where(
            EslestirmeKodu.ureten_id == kullanici.id, EslestirmeKodu.kullanildi == False  # noqa: E712
        )
    ):
        eski.kullanildi = True

    kod = _kod_uret(db)
    db.add(
        EslestirmeKodu(
            kod=kod,
            ureten_id=kullanici.id,
            ureten_rol=istek.rol,
            son_gecerlilik=dt.datetime.now(dt.timezone.utc)
            + dt.timedelta(minutes=a.eslestirme_kodu_dakika),
        )
    )
    db.commit()
    return KodYanit(kod=kod)


@yonlendirici.post("/eslestir")
def eslestir(
    istek: EslestirIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> dict[str, str]:
    """
    Kodu **gerçekten doğrular**.

    Cihaz tarafında eski hâli yalnızca "6 hane mi" diye bakıyordu; artık
    kodun var olması, süresinin dolmamış olması, daha önce kullanılmamış
    olması ve **karşı rolde** üretilmiş olması aranıyor.
    """
    if istek.rol not in ROLLER:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Rol 'ebeveyn' ya da 'cocuk' olmalı")

    kayit = db.scalar(select(EslestirmeKodu).where(EslestirmeKodu.kod == istek.kod.strip()))
    simdi = dt.datetime.now(dt.timezone.utc)

    if kayit is None or kayit.kullanildi:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Kod geçersiz")
    if _gecmis(kayit.son_gecerlilik, simdi):
        raise HTTPException(status.HTTP_410_GONE, "Kodun süresi dolmuş")
    if kayit.ureten_id == kullanici.id:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Kendi kodunu giremezsin")
    if kayit.ureten_rol == istek.rol:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            "İki cihaz da aynı rolde — biri ebeveyn, biri çocuk olmalı",
        )

    ebeveyn_id = kullanici.id if istek.rol == "ebeveyn" else kayit.ureten_id
    cocuk_id = kayit.ureten_id if istek.rol == "ebeveyn" else kullanici.id

    var = db.scalar(
        select(EbeveynBaglanti).where(
            EbeveynBaglanti.ebeveyn_id == ebeveyn_id, EbeveynBaglanti.cocuk_id == cocuk_id
        )
    )
    if var is None:
        db.add(EbeveynBaglanti(ebeveyn_id=ebeveyn_id, cocuk_id=cocuk_id))

    kayit.kullanildi = True
    db.commit()
    return {"durum": "eslesti"}


@yonlendirici.get("/ozet", response_model=EbeveynOzetYanit)
def ozet(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> EbeveynOzetYanit:
    """
    Çocuğun **özeti** — ham veri değil.

    `docs/spec-veri.md`: ebeveyne uygulama adı, oturum dökümü ya da saat
    düzeyinde ayrıntı gitmiyor. Yalnızca günlük toplam ekran süresi, bant
    ve trend. Bu uç bilerek dar tutuldu; genişletmek veri politikasını
    değiştirmek demek.
    """
    baglanti = db.scalar(
        select(EbeveynBaglanti).where(EbeveynBaglanti.ebeveyn_id == kullanici.id)
    )
    if baglanti is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Eşleşmiş çocuk cihazı yok")

    cocuk = db.get(Kullanici, baglanti.cocuk_id)
    if cocuk is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Çocuk hesabı bulunamadı")

    bugun = dt.date.today()
    bas = bugun - dt.timedelta(days=6)
    ozetler = db.scalars(
        select(GunlukOzet)
        .where(
            GunlukOzet.kullanici_id == cocuk.id,
            GunlukOzet.tarih >= bas,
            GunlukOzet.tarih <= bugun,
        )
        .order_by(GunlukOzet.tarih)
    ).all()

    dakikalar = [round(o.toplam_ekran_dk) for o in ozetler]
    ortalama = round(sum(dakikalar) / len(dakikalar)) if dakikalar else 0

    son_risk = db.scalar(
        select(RiskSonuc)
        .where(RiskSonuc.kullanici_id == cocuk.id)
        .order_by(RiskSonuc.tarih.desc())
        .limit(1)
    )

    return EbeveynOzetYanit(
        cocukAdi=cocuk.ad,
        gunlukDakikalar=dakikalar,
        ortalamaDk=ortalama,
        bant=son_risk.gosterilen_bant if son_risk else None,
        trend=son_risk.trend if son_risk else None,
        sonSinyalSaatOnce=_sinyal_saat(db, cocuk.id),
    )


def _sinyal_saat(db: Session, kullanici_id: int) -> int | None:
    """
    Son sinyalden bu yana geçen saat.

    Ebeveyn paneli bunu "çocuk uygulamayı silmiş olabilir" uyarısına
    çeviriyor — doğrudan silme tespiti güvenilir olmadığı için
    (`docs/spec-veri.md`).
    """
    kayit = db.get(Heartbeat, kullanici_id)
    if kayit is None:
        return None
    son = kayit.son_sinyal
    if son.tzinfo is None:
        son = son.replace(tzinfo=dt.timezone.utc)
    return int((dt.datetime.now(dt.timezone.utc) - son).total_seconds() // 3600)


def _kod_uret(db: Session) -> str:
    for _ in range(20):
        kod = f"{secrets.randbelow(900000) + 100000}"
        if db.scalar(select(EslestirmeKodu).where(EslestirmeKodu.kod == kod)) is None:
            return kod
    raise HTTPException(status.HTTP_500_INTERNAL_SERVER_ERROR, "Kod üretilemedi")


def _gecmis(son: dt.datetime, simdi: dt.datetime) -> bool:
    if son.tzinfo is None:
        son = son.replace(tzinfo=dt.timezone.utc)
    return son < simdi
