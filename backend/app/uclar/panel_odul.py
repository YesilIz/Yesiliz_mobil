"""
Ödül operasyonu — stok ve teslimat (`docs/panel-plani.md §5.4`).

**A4'ün teknik ayağı.** K28 maddi ödülleri `PLANLANAN` tutuyordu; kapı
artık stoğun kendisi: buradan parti yüklenince kademe cihazda açılıyor,
stok bitince kapanıyor. Finansman ayağı Yeşilay'da.

`operasyon` ve `admin` erişiyor; `gozlemci` yok (toplu istatistikle
sınırlı) ve YEDAM rolleri de yok — ödül operasyonu danışmanlık işi değil.
"""

from __future__ import annotations

import datetime as dt
import logging

from fastapi import APIRouter, Depends, Form, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app import odul_stogu
from app.modeller import (
    Kullanici,
    OdulSecimi,
    OdulStok,
    OdulTeslimat,
    Personel,
    PersonelRolu,
    TeslimatDurumu,
)
from app.panel.denetim import ASGARI_GEREKCE, denetle
from app.panel.oturum import personel_gerek
from app.panel.sablon import yanit, yonlendir
from app.uclar.odul import SECILEBILIR_ODULLER
from app.veritabani import oturum

gunluk = logging.getLogger(__name__)
yonlendirici = APIRouter(prefix="/yonetim", tags=["panel-odul"])

ODUL_ROLLERI = (PersonelRolu.ADMIN, PersonelRolu.OPERASYON)

#: Teslimat listesinde tek sayfada gösterilen kayıt sayısı.
SAYFA_BOYU = 50


@yonlendirici.get("/odul")
def odul_ekrani(
    istek: Request,
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*ODUL_ROLLERI)),
):
    return yanit(
        istek,
        "odul.html",
        {
            "etkin": "odul",
            "stoklar": _stok_gorunumu(db),
            "teslimatlar": _teslimatlar(db),
            "durumlar": [d.value for d in TeslimatDurumu],
        },
    )


def _stok_gorunumu(db: Session) -> list[dict]:
    """
    Tanımlı **her** kademe/seçenek çifti — satırı olmayanlar da.

    Yalnızca var olan satırları listelemek, hiç parti yüklenmemiş bir
    seçeneği ekrandan gizlerdi ve personel onu "yok" sanardı; oysa
    yüklenmesi gereken tam da o.
    """
    mevcut = {
        (s.kademe_id, s.secenek_id): s for s in db.scalars(select(OdulStok)).all()
    }
    satirlar = []
    for kademe_id, secenekler in SECILEBILIR_ODULLER.items():
        for secenek_id in sorted(secenekler):
            s = mevcut.get((kademe_id, secenek_id))
            satirlar.append(
                {
                    "kademe_id": kademe_id,
                    "secenek_id": secenek_id,
                    "toplam": s.toplam if s else 0,
                    "kalan": s.kalan if s else 0,
                    "son_kullanma": s.son_kullanma if s else None,
                    "acik": bool(s and s.kalan > 0),
                }
            )
    return satirlar


def _teslimatlar(db: Session) -> list[dict]:
    return [
        {
            "id": t.id,
            "durum": t.durum,
            "kod": t.kod,
            "kademe_id": secim.kademe_id,
            "secenek_id": secim.secenek_id,
            "kullanici_id": secim.kullanici_id,
            "eposta": eposta,
            "guncelleme": t.guncelleme,
        }
        for t, secim, eposta in db.execute(
            select(OdulTeslimat, OdulSecimi, Kullanici.eposta)
            .join(OdulSecimi, OdulSecimi.id == OdulTeslimat.odul_secimi_id)
            .join(Kullanici, Kullanici.id == OdulSecimi.kullanici_id)
            .order_by(OdulTeslimat.guncelleme.desc(), OdulTeslimat.id.desc())
            .limit(SAYFA_BOYU)
        ).all()
    ]


@yonlendirici.post("/odul/stok")
def stok_yukle(
    istek: Request,
    kademe_id: str = Form(...),
    secenek_id: str = Form(...),
    adet: int = Form(...),
    son_kullanma: str = Form(""),
    gerekce: str = Form(""),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*ODUL_ROLLERI)),
):
    """
    Parti yükler — **kademeyi cihazda açan işlem bu**.

    Gerekçe zorunlu: stok yüklemek bir tedarik taahhüdü ve kullanıcıya
    "kazandın" demeye başlayan şey. Hangi partiden geldiği denetim
    kaydında durmalı.

    Adet **ekleniyor, ezilmiyor**: ikinci bir parti gelince kalan sıfıra
    dönmemeli.
    """
    if secenek_id not in SECILEBILIR_ODULLER.get(kademe_id, set()):
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT, "Tanımsız kademe/seçenek"
        )
    if adet <= 0:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_CONTENT, "Adet pozitif olmalı")
    if len(gerekce.strip()) < ASGARI_GEREKCE:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT,
            f"Gerekçe en az {ASGARI_GEREKCE} karakter olmalı",
        )

    tarih = None
    if son_kullanma.strip():
        try:
            tarih = dt.date.fromisoformat(son_kullanma.strip())
        except ValueError:
            raise HTTPException(
                status.HTTP_422_UNPROCESSABLE_CONTENT, "Son kullanma tarihi geçersiz"
            ) from None

    satir = db.scalar(
        select(OdulStok)
        .where(OdulStok.kademe_id == kademe_id, OdulStok.secenek_id == secenek_id)
        .with_for_update()
    )
    if satir is None:
        satir = OdulStok(
            kademe_id=kademe_id, secenek_id=secenek_id, toplam=0, kalan=0
        )
        db.add(satir)
    satir.toplam += adet
    satir.kalan += adet
    if tarih is not None:
        satir.son_kullanma = tarih

    denetle(
        db,
        personel,
        eylem="odul.stok_yukle",
        hedef_tur="odul_stok",
        gerekce=gerekce,
        istek=istek,
        detay={"kademe": kademe_id, "secenek": secenek_id, "adet": adet},
    )
    db.commit()
    gunluk.info(
        "Ödül stoğu yüklendi",
        extra={"kademe": kademe_id, "secenek": secenek_id, "adet": adet},
    )
    return yonlendir(istek, "/yonetim/odul")


@yonlendirici.post("/odul/teslimat/{teslimat_id}")
def teslimat_guncelle(
    istek: Request,
    teslimat_id: int,
    durum: str = Form(...),
    kod: str = Form(""),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*ODUL_ROLLERI)),
):
    """
    Teslimat durumunu ilerletir: `bekliyor` → `gonderildi` → `teslim_edildi`.

    **İptal stoğu geri veriyor** — teslim edilmemiş bir kupon havuza
    dönmeli. Ama kullanıcının **seçimi kalıyor** (K28: geri alma yok):
    iptal bir teslimat sorunu, kullanıcının hakkının kaybı değil.
    """
    try:
        yeni = TeslimatDurumu(durum.strip())
    except ValueError:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT, "Geçersiz teslimat durumu"
        ) from None

    teslimat = db.scalar(
        select(OdulTeslimat).where(OdulTeslimat.id == teslimat_id).with_for_update()
    )
    if teslimat is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Teslimat bulunamadı")

    secim = db.get(OdulSecimi, teslimat.odul_secimi_id)
    onceki = teslimat.durum

    if yeni is TeslimatDurumu.IPTAL and onceki != TeslimatDurumu.IPTAL.value:
        if secim is not None:
            odul_stogu.geri_ver(db, secim.kademe_id, secim.secenek_id)

    teslimat.durum = yeni.value
    if kod.strip():
        teslimat.kod = kod.strip()[:120]
    if yeni is TeslimatDurumu.GONDERILDI and teslimat.gonderim_zamani is None:
        teslimat.gonderim_zamani = dt.datetime.now(dt.timezone.utc)
    teslimat.personel_id = personel.id

    denetle(
        db,
        personel,
        eylem="odul.teslimat",
        hedef_tur="odul_teslimat",
        hedef_id=teslimat.id,
        gerekce=f"Durum {onceki} → {yeni.value}",
        istek=istek,
    )
    db.commit()
    return yonlendir(istek, "/yonetim/odul")
