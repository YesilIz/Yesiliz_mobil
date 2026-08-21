"""
İçerik ve duyuru yönetimi (`docs/panel-plani.md §5.5, §5.6`).

**Bitiş ölçütü burası:** bir yazı düzeltmesi APK yayını olmadan telefona
iniyor. Gömülü set (`assets/bilgi_bankasi.json`) çevrimdışı yedek olarak
kalıyor; sunucuda içerik varsa cihaz onu kullanıyor (S5).

Duyuruda **FCM yok** (§5.6): yayımlanan duyuru senkronla iniyor ve
kullanıcı uygulamayı açtığında bildirim defterinde görüyor.
"""

from __future__ import annotations

import datetime as dt
import logging

from fastapi import APIRouter, Depends, Form, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.modeller import BilgiYazisi, Duyuru, Personel, PersonelRolu
from app.panel.denetim import ASGARI_GEREKCE, denetle
from app.panel.oturum import personel_gerek
from app.panel.sablon import yanit, yonlendir
from app.veritabani import oturum

gunluk = logging.getLogger(__name__)
yonlendirici = APIRouter(prefix="/yonetim", tags=["panel-icerik"])

ICERIK_ROLLERI = (PersonelRolu.ADMIN, PersonelRolu.OPERASYON)

#: Duyuru metninde aranan **emir kipi / korkutma** işaretleri.
#:
#: `CLAUDE.md §4` bunları yasaklıyor ve kural serbest metinde kodla
#: zorlanamıyor — ama uyarmak zorlamaktan farklı: form kaydı engellemiyor,
#: personele "bu ifade marka kılavuzuna aykırı" diyor. Karar insanda
#: kalıyor, hatırlatma otomatik.
RISKLI_IFADELER = (
    "yapmalısın",
    "etmelisin",
    "zorundasın",
    "hemen",
    "derhal",
    "tehlike",
    "bağımlısın",
    "geç kalma",
    "kaybediyorsun",
)


def _uyarilar(metin: str) -> list[str]:
    kucuk = metin.lower()
    return [ifade for ifade in RISKLI_IFADELER if ifade in kucuk]


@yonlendirici.get("/icerik")
def icerik_ekrani(
    istek: Request,
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*ICERIK_ROLLERI)),
):
    yazilar = db.scalars(
        select(BilgiYazisi).order_by(BilgiYazisi.sira, BilgiYazisi.id)
    ).all()
    duyurular = db.scalars(
        select(Duyuru).order_by(Duyuru.id.desc()).limit(20)
    ).all()
    return yanit(
        istek,
        "icerik.html",
        {
            "etkin": "icerik",
            "yazilar": yazilar,
            "duyurular": duyurular,
            "riskli_ifadeler": RISKLI_IFADELER,
        },
    )


@yonlendirici.post("/icerik")
def yazi_kaydet(
    istek: Request,
    yazi_id: str = Form(...),
    baslik: str = Form(...),
    ozet: str = Form(...),
    kategori: str = Form(...),
    okuma_dk: int = Form(1),
    govde: str = Form(...),
    yayinda: str = Form(""),
    gerekce: str = Form(""),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*ICERIK_ROLLERI)),
):
    """
    Yazıyı ekler ya da günceller.

    Var olan bir yazıda `surum` **artıyor**: cihazın önbelleği bununla
    geçersizleşiyor ve "hangi sürümü gördü" sorusunun cevabı kalıyor.

    Yayından kaldırma `yayinda = false` — **silme yok**. Cihazın
    önbelleğinde kalmış bir yazının bağlantısını kırmak, onu listeden
    düşürmekten farklı bir şey.
    """
    if len(gerekce.strip()) < ASGARI_GEREKCE:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT,
            f"Gerekçe en az {ASGARI_GEREKCE} karakter olmalı",
        )

    temiz_id = yazi_id.strip()
    if not temiz_id:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_CONTENT, "Yazı kimliği boş olamaz")

    yazi = db.get(BilgiYazisi, temiz_id)
    yeni_mi = yazi is None
    if yazi is None:
        yazi = BilgiYazisi(id=temiz_id, surum=1)
        db.add(yazi)
    else:
        yazi.surum += 1

    yazi.baslik = baslik.strip()[:200]
    yazi.ozet = ozet.strip()[:500]
    yazi.kategori = kategori.strip()[:60]
    yazi.okuma_dk = max(1, okuma_dk)
    yazi.govde = govde.strip()
    yazi.yayinda = yayinda.strip().lower() in ("evet", "on", "true", "1")
    yazi.guncelleyen_id = personel.id

    denetle(
        db,
        personel,
        eylem="icerik.yazi" if not yeni_mi else "icerik.yazi_ekle",
        hedef_tur="bilgi_yazisi",
        gerekce=gerekce,
        istek=istek,
        detay={"id": temiz_id, "surum": yazi.surum, "yayinda": yazi.yayinda},
    )
    db.commit()
    return yonlendir(istek, "/yonetim/icerik")


@yonlendirici.post("/duyuru")
def duyuru_olustur(
    istek: Request,
    baslik: str = Form(...),
    metin: str = Form(...),
    yayimla: str = Form(""),
    gerekce: str = Form(""),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*ICERIK_ROLLERI)),
):
    """
    Duyuru oluşturur; `yayimla` işaretliyse hemen yayımlar.

    **Yayımlamak geri alınamaz sayılıyor:** duyuru bir kez indiğinde
    kullanıcının bildirim defterine yazılıyor ve oradan geri çekmenin yolu
    yok. Bu yüzden taslak olarak kaydetmek varsayılan.

    Metin `§4`'e karşı **taranıyor ama engellenmiyor**: emir kipi ve
    korkutma ifadeleri uyarı olarak dönüyor, karar personelde kalıyor.
    Otomatik reddetmek, meşru bir cümleyi ("hemen şimdi değil, ne zaman
    istersen") de keserdi.
    """
    if len(gerekce.strip()) < ASGARI_GEREKCE:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT,
            f"Gerekçe en az {ASGARI_GEREKCE} karakter olmalı",
        )

    temiz = metin.strip()
    if not temiz or not baslik.strip():
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_CONTENT, "Başlık ve metin zorunlu")

    yayimlansin = yayimla.strip().lower() in ("evet", "on", "true", "1")
    kayit = Duyuru(
        baslik=baslik.strip()[:120],
        metin=temiz[:1000],
        hedef="hepsi",
        yayin_zamani=dt.datetime.now(dt.timezone.utc) if yayimlansin else None,
        personel_id=personel.id,
    )
    db.add(kayit)
    db.flush()

    uyarilar = _uyarilar(temiz)
    denetle(
        db,
        personel,
        eylem="duyuru.yayimla" if yayimlansin else "duyuru.taslak",
        hedef_tur="duyuru",
        hedef_id=kayit.id,
        gerekce=gerekce,
        istek=istek,
        detay={"uyarilar": uyarilar} if uyarilar else None,
    )
    db.commit()

    if uyarilar:
        gunluk.warning(
            "Duyuru metninde §4 uyarısı",
            extra={"duyuru": kayit.id, "ifadeler": uyarilar},
        )
    return yonlendir(istek, "/yonetim/icerik")


@yonlendirici.post("/duyuru/{duyuru_id}/yayimla")
def duyuru_yayimla(
    istek: Request,
    duyuru_id: int,
    gerekce: str = Form(""),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*ICERIK_ROLLERI)),
):
    """Taslağı yayımlar. Zaten yayımlanmışsa 409 — ikinci kez inmemeli."""
    if len(gerekce.strip()) < ASGARI_GEREKCE:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT,
            f"Gerekçe en az {ASGARI_GEREKCE} karakter olmalı",
        )

    kayit = db.get(Duyuru, duyuru_id)
    if kayit is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Duyuru bulunamadı")
    if kayit.yayin_zamani is not None:
        raise HTTPException(status.HTTP_409_CONFLICT, "Duyuru zaten yayımlanmış")

    kayit.yayin_zamani = dt.datetime.now(dt.timezone.utc)
    denetle(
        db,
        personel,
        eylem="duyuru.yayimla",
        hedef_tur="duyuru",
        hedef_id=kayit.id,
        gerekce=gerekce,
        istek=istek,
    )
    db.commit()
    return yonlendir(istek, "/yonetim/icerik")
