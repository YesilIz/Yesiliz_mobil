"""Günlük özet ve risk sonucu senkronu."""

from __future__ import annotations

import datetime as dt

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.guvenlik import kullaniciyi_kilitle, mevcut_kullanici
from app.modeller import GunlukOzet, Heartbeat, Kullanici, LigUyelik, RiskSonuc
from app.semalar import GunlukOzetDto, RiskSonucDto, SenkronIstek, SenkronYanit
from app.uclar.lig import hafta_baslangici
from app.veritabani import oturum

yonlendirici = APIRouter(tags=["ozet"])


@yonlendirici.post("/ozet", response_model=SenkronYanit)
def senkron(
    istek: SenkronIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> SenkronYanit:
    """
    Cihazdan gelen özetleri yazar.

    **Idempotent**: aynı gün tekrar gönderilirse satır güncelleniyor, yeni
    satır açılmıyor. Cihaz gecelik worker'da geriye dönük 7 günü de
    gönderiyor (`GunlukOzetWorker.TELAFI_GUN`), o yüzden tekrar kaçınılmaz.
    """
    kullaniciyi_kilitle(db, kullanici.id)
    _senkron_tarihlerini_dogrula(istek)

    for dto in istek.ozetler:
        _ozet_yaz(db, kullanici.id, dto)
    for dto in istek.riskler:
        _risk_yaz(db, kullanici.id, dto)

    _haftalik_puan(db, kullanici.id, istek.haftalikPuan, istek.ligdeGorunur)
    _heartbeat(db, kullanici.id)

    db.commit()
    return SenkronYanit(ozetSayisi=len(istek.ozetler), riskSayisi=len(istek.riskler))


@yonlendirici.get("/ozet", response_model=list[GunlukOzetDto])
def ozetleri_getir(
    bas: dt.date,
    bit: dt.date,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> list[GunlukOzetDto]:
    """Cihaz değişince geçmişi geri yükleme."""
    if bas > bit:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_CONTENT, "Başlangıç bitişten sonra olamaz")
    if (bit - bas).days > 366:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_CONTENT, "Tarih aralığı en fazla 366 gün")

    satirlar = db.scalars(
        select(GunlukOzet)
        .where(
            GunlukOzet.kullanici_id == kullanici.id,
            GunlukOzet.tarih >= bas,
            GunlukOzet.tarih <= bit,
        )
        .order_by(GunlukOzet.tarih)
    )
    return [
        GunlukOzetDto(
            tarih=s.tarih,
            socialDk=s.social_dk,
            entertainmentDk=s.entertainment_dk,
            gamesDk=s.games_dk,
            communicationDk=s.communication_dk,
            productivityDk=s.productivity_dk,
            educationDk=s.education_dk,
            healthDk=s.health_dk,
            otherDk=s.other_dk,
            gece=s.gece,
            oturum=s.oturum,
            acilis=s.acilis,
            sabah=s.sabah,
            uykuOncesi=s.uyku_oncesi,
            yerDegis=s.yer_degis,
            adim=s.adim,
            sporDk=s.spor_dk,
            S=s.s,
        )
        for s in satirlar
    ]


@yonlendirici.post("/heartbeat")
def heartbeat(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> dict[str, str]:
    """
    Cihaz canlı sinyali.

    `docs/spec-veri.md`: çocuk uygulamayı silerse ebeveyne bildirim, doğrudan
    silme tespitiyle değil **sinyal kesilmesiyle** anlaşılıyor.
    """
    kullaniciyi_kilitle(db, kullanici.id)
    _heartbeat(db, kullanici.id)
    db.commit()
    return {"durum": "tamam"}


def _ozet_yaz(db: Session, kullanici_id: int, dto: GunlukOzetDto) -> None:
    satir = db.scalar(
        select(GunlukOzet).where(
            GunlukOzet.kullanici_id == kullanici_id, GunlukOzet.tarih == dto.tarih
        )
    )
    if satir is None:
        satir = GunlukOzet(kullanici_id=kullanici_id, tarih=dto.tarih)
        db.add(satir)

    satir.social_dk = dto.socialDk
    satir.entertainment_dk = dto.entertainmentDk
    satir.games_dk = dto.gamesDk
    satir.communication_dk = dto.communicationDk
    satir.productivity_dk = dto.productivityDk
    satir.education_dk = dto.educationDk
    satir.health_dk = dto.healthDk
    satir.other_dk = dto.otherDk
    satir.gece = dto.gece
    satir.oturum = dto.oturum
    satir.acilis = dto.acilis
    satir.sabah = dto.sabah
    satir.uyku_oncesi = dto.uykuOncesi
    satir.yer_degis = dto.yerDegis
    satir.adim = dto.adim
    satir.spor_dk = dto.sporDk
    satir.s = dto.S


def _risk_yaz(db: Session, kullanici_id: int, dto: RiskSonucDto) -> None:
    satir = db.scalar(
        select(RiskSonuc).where(
            RiskSonuc.kullanici_id == kullanici_id, RiskSonuc.tarih == dto.tarih
        )
    )
    if satir is None:
        satir = RiskSonuc(kullanici_id=kullanici_id, tarih=dto.tarih)
        db.add(satir)

    satir.s = dto.S
    satir.r = dto.R
    satir.gosterilen_bant = dto.gosterilenBant
    satir.trend = dto.trend
    satir.veri_durumu = dto.veriDurumu


def _haftalik_puan(
    db: Session, kullanici_id: int, puan: int, gorunur: bool | None = None
) -> None:
    """
    Lig üyeliği varsa haftalık puanı günceller; yoksa sessizce geçer.

    "Ligde görün" tercihi geriye uyumluluk için burada da taşınır. Güncel
    istemci değişikliği ayrı `/lig/gorunurluk` ucuna anında gönderir.
    """
    hafta = hafta_baslangici()
    uyelik = db.scalar(
        select(LigUyelik).where(
            LigUyelik.kullanici_id == kullanici_id, LigUyelik.hafta_baslangic == hafta
        )
    )
    if uyelik is not None:
        # Puan yalnızca ARTAR. Cihaz düşük bir değer gönderse bile (veri
        # silinmiş, yeniden kurulmuş) ligdeki puan geri alınmıyor —
        # CLAUDE.md §4 "ceza yok".
        uyelik.haftalik_puan = max(uyelik.haftalik_puan, puan)
        if gorunur is not None:
            uyelik.gorunur = gorunur


def _heartbeat(db: Session, kullanici_id: int) -> None:
    kayit = db.get(Heartbeat, kullanici_id)
    simdi = dt.datetime.now(dt.timezone.utc)
    if kayit is None:
        db.add(Heartbeat(kullanici_id=kullanici_id, son_sinyal=simdi))
    else:
        kayit.son_sinyal = simdi




def _senkron_tarihlerini_dogrula(istek: SenkronIstek) -> None:
    """Bozuk cihaz saatinin ebeveyn özetini gelecekteki veriyle zehirlemesini önler."""
    from app.zaman import yerel_bugun

    bugun = yerel_bugun()
    en_eski = bugun - dt.timedelta(days=31)
    en_yeni = bugun + dt.timedelta(days=1)
    tarihler = [dto.tarih for dto in istek.ozetler] + [dto.tarih for dto in istek.riskler]
    if any(tarih < en_eski or tarih > en_yeni for tarih in tarihler):
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT,
            "Senkron tarihi izin verilen pencerenin dışında",
        )
