"""
Yönetim paneli (`docs/panel-plani.md §5`).

F1'de yalnızca gösterge kabuğu var; metrikler, hesap yönetimi ve denetim
görüntüleyici F2'de bağlanıyor. Kabuğun şimdi var olmasının sebebi giriş
akışının bir yere düşmesi gerekmesi.

**Her route rolünü açıkça beyan ediyor.** Kapsamı `test_panel_yetki_kapsami`
router listesini tarayarak garanti ediyor: yetkisiz eklenmiş bir uç CI'da
yakalanıyor.
"""

from __future__ import annotations

from fastapi import APIRouter, Depends, Request

from app.modeller import Personel, PersonelRolu
from app.panel.oturum import personel_gerek
from app.panel.sablon import yanit

yonlendirici = APIRouter(prefix="/yonetim", tags=["panel-yonetim"])

#: Toplu/anonim istatistiği görebilenler. `gozlemci` yalnızca burayı
#: görüyor; tekil kullanıcı verisine hiçbir rolün varsayılan erişimi yok
#: (`docs/panel-plani.md` P9).
GOSTERGE_ROLLERI = (
    PersonelRolu.ADMIN,
    PersonelRolu.OPERASYON,
    PersonelRolu.GOZLEMCI,
)


@yonlendirici.get("/gosterge")
def gosterge(
    istek: Request,
    personel: Personel = Depends(personel_gerek(*GOSTERGE_ROLLERI)),
):
    return yanit(istek, "gosterge.html", {"etkin": "gosterge"})
