"""
YEDAM danışman paneli (`docs/panel-plani.md §6`).

F1'de yalnızca yetkilendirilmiş kabuk var: başvuru havuzu, atama ve
başvuru detayı F3'te geliyor. Kabuk şimdi var çünkü YEDAM rolüyle giren bir
personelin düşeceği bir yer olmalı ve o yerin **yetki kapısı baştan doğru**
kurulmalı.

Ekran uydurma veri göstermiyor (CLAUDE.md §4, K37): başvuru akışının arka
ucu yokken boş bir havuz tablosu çizmek, "kimse destek istememiş" demek
olurdu.
"""

from __future__ import annotations

from fastapi import APIRouter, Depends, Request

from app.modeller import Personel, PersonelRolu
from app.panel.oturum import personel_gerek
from app.panel.sablon import yanit

yonlendirici = APIRouter(prefix="/yedam", tags=["panel-yedam"])

HAVUZ_ROLLERI = (
    PersonelRolu.ADMIN,
    PersonelRolu.YEDAM_YONETICI,
    PersonelRolu.YEDAM_DANISMAN,
)


@yonlendirici.get("/havuz")
def havuz(
    istek: Request,
    personel: Personel = Depends(personel_gerek(*HAVUZ_ROLLERI)),
):
    return yanit(istek, "yedam/havuz.html", {"etkin": "yedam"})
