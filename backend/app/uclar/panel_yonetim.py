"""
Yönetim paneli (`docs/panel-plani.md §5`).

Gösterge toplu/anonim (`GOSTERGE_ROLLERI`), hesap yönetimi tekil
(`HESAP_ROLLERI`). İkisi ayrı rol kümesi: `gozlemci` yalnızca göstergeyi
görüyor, tekil kullanıcıya hiç erişemiyor.

Hesap detayının **davranış verisi taşımadığına** dikkat (P9) — sorguların
kendisi `panel/hesaplar.py`'de ve o dosya `gunluk_ozet`/`risk_sonuc`
tablolarına hiç dokunmuyor.

**Her route rolünü açıkça beyan ediyor.** Kapsamı `test_panel_yetki_kapsami`
router listesini tarayarak garanti ediyor: yetkisiz eklenmiş bir uç CI'da
yakalanıyor.
"""

from __future__ import annotations

from fastapi import APIRouter, Depends, Form, HTTPException, Query, Request, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.modeller import Kullanici, Personel, PersonelRolu
from app.panel.gosterge import BANT_ETIKETI, BANTLAR, gostergeyi_hesapla
from app.panel.grafik import bant_dagilimi_svg
from app.panel.denetim import ASGARI_GEREKCE, denetle
from app.panel.hesaplar import SAYFA_BOYU, ara, askiya_al, askiyi_kaldir, detay
from app.panel.oturum import personel_gerek
from app.panel.sablon import yanit, yonlendir
from app.surum_kapisi import beklenen_revizyon
from app.veritabani import oturum

yonlendirici = APIRouter(prefix="/yonetim", tags=["panel-yonetim"])

#: Toplu/anonim istatistiği görebilenler. `gozlemci` yalnızca burayı
#: görüyor; tekil kullanıcı verisine hiçbir rolün varsayılan erişimi yok
#: (`docs/panel-plani.md` P9).
GOSTERGE_ROLLERI = (
    PersonelRolu.ADMIN,
    PersonelRolu.OPERASYON,
    PersonelRolu.GOZLEMCI,
)


#: Tekil hesabı görebilenler. `GOZLEMCI` **yok**: rolün tanımı toplu ve
#: anonim istatistik (§2 "Roller"), tekil hesap detayı o tanımın dışında.
HESAP_ROLLERI = (
    PersonelRolu.ADMIN,
    PersonelRolu.OPERASYON,
)


#: Bant dağılımının bakacağı pencere. 7 gün: cihaz da son 7 günü
#: gönderiyor (`SenkronDeposu.GUN`), daha geniş bir pencere sunucuda
#: olmayan veriyi sorardı.
PENCERE_GUN = 7


def _kilitli_hesap(db: Session, kullanici_id: int) -> Kullanici:
    """
    Yazma boyunca hesabı kilitler; panelde eksik hesap **404**.

    `guvenlik.kullaniciyi_kilitle` 401 atıyor — cihaz tarafında doğru
    ("senin oturumun geçersiz") ama panelde yanıltıcı: personelin oturumu
    geçerli, aranan hesap yok.
    """
    kullanici = db.scalar(
        select(Kullanici).where(Kullanici.id == kullanici_id).with_for_update()
    )
    if kullanici is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Hesap bulunamadı")
    return kullanici


@yonlendirici.get("/gosterge")
def gosterge(
    istek: Request,
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*GOSTERGE_ROLLERI)),
):
    g = gostergeyi_hesapla(db, pencere_gun=PENCERE_GUN)
    return yanit(
        istek,
        "gosterge.html",
        {
            "etkin": "gosterge",
            "g": g,
            "bantlar": [(kod, BANT_ETIKETI[kod]) for kod in BANTLAR],
            "bant_grafigi": bant_dagilimi_svg(g.bant_dagilimi),
            "pencere_gun": PENCERE_GUN,
            "revizyon": beklenen_revizyon(),
        },
    )


@yonlendirici.get("/kullanici")
def kullanici_ara(
    istek: Request,
    ara_metni: str = Query("", alias="ara"),
    sayfa: int = Query(0, ge=0),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*HESAP_ROLLERI)),
):
    """
    Hesap arama — e-posta ya da kullanıcı numarası.

    Boş sorgu **tüm kullanıcıları listelemiyor**: hesap listesini gezinmek
    bir destek talebini çözmüyor, yalnızca kişisel veriyi göz önüne
    seriyor. Arama bir hedef gerektiriyor.
    """
    satirlar, toplam = ara(db, ara_metni, sayfa)
    return yanit(
        istek,
        "hesap_ara.html",
        {
            "etkin": "kullanici",
            "ara": ara_metni,
            "satirlar": satirlar,
            "toplam": toplam,
            "sayfa": sayfa,
            "sayfa_boyu": SAYFA_BOYU,
        },
        parca="parca/hesap_sonuc.html",
    )


@yonlendirici.get("/kullanici/{kullanici_id}")
def kullanici_detay(
    istek: Request,
    kullanici_id: int,
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*HESAP_ROLLERI)),
):
    d = detay(db, kullanici_id)
    if d is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Hesap bulunamadı")
    return yanit(istek, "hesap_detay.html", {"etkin": "kullanici", "d": d})


@yonlendirici.post("/kullanici/{kullanici_id}/askiya-al")
def kullanici_askiya_al(
    istek: Request,
    kullanici_id: int,
    gerekce: str = Form(""),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*HESAP_ROLLERI)),
):
    """
    Hesabı askıya alır — gerekçe zorunlu, denetim kaydı zorunlu.

    Gerekçe iki yere birden gidiyor: `denetim_kaydi`'na (kalıcı iz) ve
    kullanıcının cihazına (403 gövdesi). İkincisi §4 gereği — sebebini
    öğrenemeyen kullanıcı uygulamayı bozuk sanar.
    """
    if len(gerekce.strip()) < ASGARI_GEREKCE:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT,
            f"Gerekçe en az {ASGARI_GEREKCE} karakter olmalı",
        )

    kullanici = _kilitli_hesap(db, kullanici_id)
    if kullanici.askida_mi:
        # İdempotent değil, gerçekten bir hata: ikinci kez askıya almak
        # sebebi sessizce EZERDİ ve kullanıcı başka bir gerekçe görürdü.
        raise HTTPException(status.HTTP_409_CONFLICT, "Hesap zaten askıda")

    askiya_al(db, kullanici, personel.id, gerekce)
    denetle(
        db,
        personel,
        eylem="kullanici_askiya_al",
        hedef_tur="kullanici",
        hedef_id=kullanici_id,
        gerekce=gerekce,
        istek=istek,
    )
    db.commit()
    return yonlendir(istek, f"/yonetim/kullanici/{kullanici_id}")


@yonlendirici.post("/kullanici/{kullanici_id}/askiyi-kaldir")
def kullanici_askiyi_kaldir(
    istek: Request,
    kullanici_id: int,
    gerekce: str = Form(""),
    db: Session = Depends(oturum),
    personel: Personel = Depends(personel_gerek(*HESAP_ROLLERI)),
):
    """Askıyı kaldırır. Gerekçe burada da zorunlu: geri alma da bir karar."""
    if len(gerekce.strip()) < ASGARI_GEREKCE:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT,
            f"Gerekçe en az {ASGARI_GEREKCE} karakter olmalı",
        )

    kullanici = _kilitli_hesap(db, kullanici_id)
    if not kullanici.askida_mi:
        raise HTTPException(status.HTTP_409_CONFLICT, "Hesap zaten aktif")

    askiyi_kaldir(db, kullanici)
    denetle(
        db,
        personel,
        eylem="kullanici_askiyi_kaldir",
        hedef_tur="kullanici",
        hedef_id=kullanici_id,
        gerekce=gerekce,
        istek=istek,
    )
    db.commit()
    return yonlendir(istek, f"/yonetim/kullanici/{kullanici_id}")
