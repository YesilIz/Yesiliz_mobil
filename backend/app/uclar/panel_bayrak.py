"""
Özellik bayrağı arayüzü (`docs/panel-plani.md §5.7`).

Bayraklar `app/bayraklar.py`'de tanımlı; buradaki ekran onları **gösteriyor
ve eziyor**. Tanım koda gömülü kalıyor: serbest bir anahtar/değer alanı,
yazım hatasıyla sessizce varsayılana düşen ve kimsenin fark etmediği bir
bayrak üretirdi.

**Risk motoru sabitleri buraya girmiyor** (`CLAUDE.md §12 K17`): skorlama
cihazda ve `core-risk` referans Python ile birebir doğrulanmış; sunucudan
sabit dağıtmak o doğrulamayı bozardı. Panel onları yalnızca gösteriyor —
ekranda ayrı bir bölümde, salt okunur.
"""

from __future__ import annotations

import logging

from fastapi import APIRouter, Depends, Form, HTTPException, Request, status
from sqlalchemy.orm import Session

from app import bayraklar
from app.modeller import Personel, PersonelRolu
from app.panel.denetim import ASGARI_GEREKCE, denetle
from app.panel.oturum import personel_gerek
from app.panel.sablon import yanit, yonlendir
from app.veritabani import oturum

gunluk = logging.getLogger(__name__)
yonlendirici = APIRouter(prefix="/yonetim", tags=["panel-bayrak"])

#: Yalnızca `admin`. Bayrak değiştirmek bir özelliği herkes için açıp
#: kapatmak; operasyonun günlük işi değil.
SADECE_ADMIN = (PersonelRolu.ADMIN,)

#: Panelin **salt okunur** gösterdiği cihaz sabitleri (K17).
#:
#: Değerler `core-risk/Sabitler.kt`'den elle kopyalandı ve **otorite
#: değil**: cihaz kendi derlemesindekini kullanıyor. Burada durmalarının
#: sebebi, bir destek talebinde "hangi eşikle hesaplandı" sorusunun
#: cevabının panelde bulunabilmesi.
RISK_SABITLERI = (
    ("S_REF", "300", "Skor normalleştirme referansı (A2: kalibre edilmedi)"),
    ("GUN_ASGARI_DK", "15", "Altında gün YETERSIZ sayılıyor (K30)"),
    ("VERI_ESKI_ESIK_GUN", "3", "Üstünde bant ESKI işaretleniyor"),
    ("SONUMLEME_TAVANI", "%15", "Aktivitenin skoru düşürebileceği en fazla pay"),
)


@yonlendirici.get("/bayrak")
def bayrak_ekrani(
    istek: Request,
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*SADECE_ADMIN)),
):
    return yanit(
        istek,
        "bayrak.html",
        {
            "etkin": "bayrak",
            "bayraklar": bayraklar.hepsini_oku(db),
            "risk_sabitleri": RISK_SABITLERI,
        },
    )


@yonlendirici.post("/bayrak")
def bayrak_yaz(
    istek: Request,
    anahtar: str = Form(...),
    deger: str = Form(...),
    gerekce: str = Form(""),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*SADECE_ADMIN)),
):
    """
    Bayrağı ezer.

    Gerekçe zorunlu: bir bayrak bir özelliği herkes için açıp kapatıyor ve
    "niçin açıldı" sorusunun cevabı kaydın kendisinde durmalı. Danışman
    notunu açan işlem (S4) tam olarak burası ve o kararın izi olmadan
    açılmamalı.
    """
    if len(gerekce.strip()) < ASGARI_GEREKCE:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT,
            f"Gerekçe en az {ASGARI_GEREKCE} karakter olmalı",
        )

    try:
        bayraklar.yaz(db, anahtar.strip(), deger.strip(), personel.id)
    except ValueError as hata:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT, str(hata)
        ) from None

    denetle(
        db,
        personel,
        eylem="bayrak.yaz",
        hedef_tur="ozellik_bayragi",
        gerekce=gerekce,
        istek=istek,
        detay={"anahtar": anahtar.strip(), "deger": deger.strip()},
    )
    db.commit()
    gunluk.info("Özellik bayrağı değişti", extra={"anahtar": anahtar, "deger": deger})
    return yonlendir(istek, "/yonetim/bayrak")
