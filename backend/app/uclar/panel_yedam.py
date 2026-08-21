"""
YEDAM danışman paneli (`docs/panel-plani.md §6`).

**Danışman kullanıcı arayamıyor.** Havuz ve detay `personel`'in rolüne
göre daraltılıyor ve daraltma **sorgunun kendisinde** (ekranda değil):
`yedam_danisman` yalnızca kendisine atanmış başvuruyu açabiliyor. Rıza
vermemiş bir kullanıcıya ulaşmanın hiçbir yüzeyi yok.

**Detayı açmak bir olaydır.** `GET /yedam/basvuru/{id}` her çağrısında
`erisim_kaydi` yazıyor ve kullanıcı bunu uygulamada görüyor. Rıza metni
bunu vaat ediyor; yazmayı atlamak sözü tutmamak olurdu.
"""

from __future__ import annotations

import datetime as dt
import logging

from fastapi import APIRouter, Depends, Form, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app import bayraklar
from app.modeller import (
    BasvuruDurumu,
    DanismanNotu,
    KAPANMIS,
    Personel,
    PersonelRolu,
    TemasTuru,
    YedamBasvuru,
    YedamTemas,
)
from app.panel.denetim import ASGARI_GEREKCE, denetle
from app.panel.oturum import personel_gerek
from app.panel.sablon import yanit, yonlendir
from app.panel.yedam_gorunum import basvuruyu_getir, detay, erisimi_kaydet, havuz
from app.veritabani import oturum

gunluk = logging.getLogger(__name__)
yonlendirici = APIRouter(prefix="/yedam", tags=["panel-yedam"])

HAVUZ_ROLLERI = (
    PersonelRolu.ADMIN,
    PersonelRolu.YEDAM_YONETICI,
    PersonelRolu.YEDAM_DANISMAN,
)

#: Atama yapabilenler. Danışman **kendine bile** atayamıyor: havuzdan
#: iş seçmek, iş yükünün yöneticide olması gereken bir karar (§2 "Roller").
ATAMA_ROLLERI = (PersonelRolu.ADMIN, PersonelRolu.YEDAM_YONETICI)

#: Üç denemeden sonra başvuru `ulasilamadi` durumuna geçiyor (§6.3).
ULASILAMADI_ESIGI = 3


def _basvuru_gerek(db: Session, personel: Personel, basvuru_id: int) -> YedamBasvuru:
    basvuru = basvuruyu_getir(db, personel, basvuru_id)
    if basvuru is None:
        # 404: yetkisi olmayan başvurunun VAR OLDUĞUNU da söylemiyoruz.
        # 403 dönmek, danışmana "böyle bir başvuru var ama senin değil"
        # bilgisini verirdi.
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Başvuru bulunamadı")
    return basvuru


def _acik_gerek(basvuru: YedamBasvuru) -> None:
    if basvuru.durum in KAPANMIS:
        raise HTTPException(status.HTTP_409_CONFLICT, "Başvuru kapanmış")


@yonlendirici.get("/havuz")
def havuz_ekrani(
    istek: Request,
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*HAVUZ_ROLLERI)),
):
    satirlar = havuz(db, personel)
    return yanit(
        istek,
        "yedam/havuz.html",
        {
            "etkin": "yedam",
            "satirlar": satirlar,
            "atayabilir": personel.rol in {r.value for r in ATAMA_ROLLERI},
            "yalnizca_kendi": personel.rol == PersonelRolu.YEDAM_DANISMAN.value,
            "danismanlar": _danismanlar(db),
        },
    )


def _danismanlar(db: Session) -> list[tuple[int, str]]:
    return [
        (p.id, p.ad)
        for p in db.scalars(
            select(Personel)
            .where(
                Personel.rol == PersonelRolu.YEDAM_DANISMAN.value,
                Personel.durum == "aktif",
            )
            .order_by(Personel.ad)
        ).all()
    ]


@yonlendirici.get("/basvuru/{basvuru_id}")
def basvuru_detay(
    istek: Request,
    basvuru_id: int,
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*HAVUZ_ROLLERI)),
):
    """
    Başvuru detayı — **`erisim_kaydi` YAZAR**.

    Kayıt, gösterilen veri **boş olsa bile** yazılıyor: rıza süresi dolmuş
    bir başvuruyu açmak da bir erişim denemesidir ve kullanıcının bunu
    görme hakkı var.
    """
    basvuru = _basvuru_gerek(db, personel, basvuru_id)
    d = detay(db, basvuru)

    erisimi_kaydet(db, basvuru, personel, d.riza_kapsam if d.riza_gecerli else ())
    db.commit()

    return yanit(
        istek,
        "yedam/detay.html",
        {
            "etkin": "yedam",
            "d": d,
            "atayabilir": personel.rol in {r.value for r in ATAMA_ROLLERI},
            "danismanlar": _danismanlar(db),
            "not_acik": bayraklar.acik_mi(db, bayraklar.DANISMAN_NOTU),
            "temas_turleri": [t.value for t in TemasTuru],
        },
    )


@yonlendirici.post("/basvuru/{basvuru_id}/ata")
def basvuru_ata(
    istek: Request,
    basvuru_id: int,
    danisman_id: int = Form(...),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*ATAMA_ROLLERI)),
):
    """Başvuruyu bir danışmana atar. Yeniden atama serbest ve kayıtlı."""
    basvuru = _basvuru_gerek(db, personel, basvuru_id)
    _acik_gerek(basvuru)

    danisman = db.get(Personel, danisman_id)
    if (
        danisman is None
        or danisman.rol != PersonelRolu.YEDAM_DANISMAN.value
        or danisman.durum != "aktif"
    ):
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT, "Geçerli bir danışman seçilmeli"
        )

    onceki = basvuru.atanan_personel_id
    basvuru.atanan_personel_id = danisman.id
    if basvuru.durum == BasvuruDurumu.YENI.value:
        basvuru.durum = BasvuruDurumu.ATANDI.value

    denetle(
        db,
        personel,
        eylem="yedam.ata",
        hedef_tur="yedam_basvuru",
        hedef_id=basvuru.id,
        gerekce=f"Atama {onceki} → {danisman.id}",
        istek=istek,
    )
    db.commit()
    return yonlendir(istek, f"/yedam/basvuru/{basvuru.id}")


@yonlendirici.post("/basvuru/{basvuru_id}/temas")
def basvuru_temas(
    istek: Request,
    basvuru_id: int,
    tur: str = Form(...),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*HAVUZ_ROLLERI)),
):
    """
    Görüşme kaydı. Kullanıcı bunu **görüyor** (`GET /yedam/basvurum`).

    Üç `ulasilamadi`'dan sonra durum otomatik `ulasilamadi`'ya geçiyor
    (§6.3): sayaç yöneticiye "bu başvuru tıkandı" sinyali veriyor.
    Başvuru **kapanmıyor** — ulaşılamamak vazgeçmek değil.
    """
    basvuru = _basvuru_gerek(db, personel, basvuru_id)
    _acik_gerek(basvuru)

    try:
        temas_turu = TemasTuru(tur.strip())
    except ValueError:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT, "Geçersiz temas türü"
        ) from None

    db.add(
        YedamTemas(basvuru_id=basvuru.id, personel_id=personel.id, tur=temas_turu.value)
    )

    if temas_turu is TemasTuru.ULASILAMADI:
        basvuru.ulasilamadi_sayisi += 1
        if basvuru.ulasilamadi_sayisi >= ULASILAMADI_ESIGI:
            basvuru.durum = BasvuruDurumu.ULASILAMADI.value
    else:
        # Görüşme ya da arama gerçekleşti: sayaç sıfırlanıyor ve başvuru
        # tıkanmış sayılmaktan çıkıyor.
        basvuru.ulasilamadi_sayisi = 0
        basvuru.durum = BasvuruDurumu.TEMAS_KURULDU.value

    denetle(
        db,
        personel,
        eylem="yedam.temas",
        hedef_tur="yedam_basvuru",
        hedef_id=basvuru.id,
        gerekce=f"Temas kaydı: {temas_turu.value}",
        istek=istek,
    )
    db.commit()
    return yonlendir(istek, f"/yedam/basvuru/{basvuru.id}")


@yonlendirici.post("/basvuru/{basvuru_id}/not")
def basvuru_not(
    istek: Request,
    basvuru_id: int,
    metin: str = Form(...),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*HAVUZ_ROLLERI)),
):
    """
    Danışman notu — **özellik bayrağıyla kapalı** (S4).

    Saklama süresi ve erişim rejimi hukuktan gelene kadar klinik kayıt
    üretmiyoruz. Bayrak kapalıyken uç `503` dönüyor ve **hiçbir satır
    yazmıyor**; ekranda da form çizilmiyor. K28'in maddi ödül kapısıyla
    aynı desen: özellik görünür, sebebi yazılı, ama işlemiyor.
    """
    if not bayraklar.acik_mi(db, bayraklar.DANISMAN_NOTU):
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "Danışman notu şu anda kapalı: saklama rejimi henüz yazılmadı.",
        )

    basvuru = _basvuru_gerek(db, personel, basvuru_id)
    temiz = metin.strip()
    if not temiz:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_CONTENT, "Not boş olamaz")

    db.add(DanismanNotu(basvuru_id=basvuru.id, personel_id=personel.id, metin=temiz))
    denetle(
        db,
        personel,
        eylem="yedam.not",
        hedef_tur="yedam_basvuru",
        hedef_id=basvuru.id,
        gerekce="Danışman notu eklendi",
        istek=istek,
    )
    db.commit()
    return yonlendir(istek, f"/yedam/basvuru/{basvuru.id}")


@yonlendirici.post("/basvuru/{basvuru_id}/kapat")
def basvuru_kapat(
    istek: Request,
    basvuru_id: int,
    gerekce: str = Form(""),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*HAVUZ_ROLLERI)),
):
    """
    Başvuruyu kapatır. Gerekçe zorunlu — kapatma bir karar.

    **Rıza otomatik bitmiyor.** Kapanan başvuru danışmanlık ilişkisinin
    sonu; rızanın süresi kendi takvimine göre işliyor ve kullanıcı onu
    kendi geri çekiyor. Kapatmanın rızayı da bitirmesi, kullanıcının
    vermediği bir kararı danışmanın vermesi olurdu.
    """
    basvuru = _basvuru_gerek(db, personel, basvuru_id)
    _acik_gerek(basvuru)

    if len(gerekce.strip()) < ASGARI_GEREKCE:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT,
            f"Kapanış gerekçesi en az {ASGARI_GEREKCE} karakter olmalı",
        )

    basvuru.durum = BasvuruDurumu.KAPANDI.value
    basvuru.kapanis = dt.datetime.now(dt.timezone.utc)
    basvuru.kapanis_nedeni = gerekce.strip()[:300]

    denetle(
        db,
        personel,
        eylem="yedam.kapat",
        hedef_tur="yedam_basvuru",
        hedef_id=basvuru.id,
        gerekce=gerekce,
        istek=istek,
    )
    db.commit()
    return yonlendir(istek, "/yedam/havuz")
