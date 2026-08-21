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

from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from sqlalchemy.orm import Session

from app.modeller import Personel, PersonelRolu
from app.panel.gosterge import BANT_ETIKETI, BANTLAR, gostergeyi_hesapla
from app.panel.grafik import bant_dagilimi_svg
from app.panel.hesaplar import SAYFA_BOYU, ara, detay
from app.panel.oturum import personel_gerek
from app.panel.sablon import yanit
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
