"""
Denetim kaydı (`docs/panel-plani.md §7`).

**`db.commit()`'ten ÖNCE çağrılıyor.** Denetim satırı ve eylemin etkisi aynı
transaction'da; böylece "gerçekleşmiş ama kaydı olmayan eylem" fiziksel
olarak imkânsız. Dekoratör ya da middleware seçilmedi: ikisi de gerekçe
metnini ve hedef kimliğini genel olarak bilemiyor, üstelik middleware
okumaları da loglardı.

Unutmaya karşı `tests/test_denetim_kapsami.py` panel router'larındaki yazma
handler'larının kaynağını tarayıp `denetle(` çağrısını arıyor.
"""

from __future__ import annotations

import json

from fastapi import Request
from sqlalchemy.orm import Session

from app.modeller import DenetimKaydi, Personel
from app.panel.oturum import istemci_ip

#: `gerekce` alanının asgari uzunluğu. "düzeltme" diyen bir eylemin niçini
#: kaydın kendisinde durmalı; tek kelimelik gerekçe denetim izi değil.
ASGARI_GEREKCE = 20

_DETAY_SINIRI = 2000


def denetle(
    db: Session,
    personel: Personel,
    eylem: str,
    hedef_tur: str,
    hedef_id: int | None = None,
    gerekce: str = "",
    istek: Request | None = None,
    detay: dict | None = None,
) -> DenetimKaydi:
    """
    Denetim satırını oturuma ekler — **commit etmez**.

    Commit'i çağıran uç yapıyor, kendi yazmasıyla birlikte. Burada commit
    edilseydi eylem başarısız olsa bile kayıt kalırdı ve denetim izi
    gerçekleşmemiş eylemler içerirdi.
    """
    kayit = DenetimKaydi(
        personel_id=personel.id,
        eylem=eylem,
        hedef_tur=hedef_tur,
        hedef_id=hedef_id,
        gerekce=gerekce[:500],
        ip=istemci_ip(istek) if istek is not None else "",
        detay=json.dumps(detay, ensure_ascii=False)[:_DETAY_SINIRI] if detay else "",
    )
    db.add(kayit)
    return kayit
