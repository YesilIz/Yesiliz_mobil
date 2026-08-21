"""
Puan düzeltmelerinin cihaza inmesi (`CLAUDE.md §12 K56`).

**Sunucu toplam puanı hesaplamıyor** (K17 ailesi): cihaz hesaplıyor,
sunucu yalnızca panelden yapılan tekil düzeltmeleri saklıyor ve cihaz
onları kendi toplamına ekliyor.

Cihaz bu listeyi tekrar tekrar çekebiliyor: kendi `puan_duzeltme`
tablosunun birincil anahtarı **sunucu id'si**, yani aynı deltanın ikinci
kez uygulanması fiziksel olarak imkânsız. Bir deltayı iki kez uygulamak
geri dönülemez bir kullanıcı zararı; koruma tek bir bayrağa
bırakılamayacak kadar önemli.
"""

from __future__ import annotations

import logging

from fastapi import APIRouter, Depends
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.guvenlik import mevcut_kullanici
from app.modeller import Kullanici, PuanDuzeltme
from app.semalar import PuanDuzeltmeDto
from app.veritabani import oturum

gunluk = logging.getLogger(__name__)
yonlendirici = APIRouter(prefix="/puan", tags=["puan"])


@yonlendirici.get("/duzeltmeler", response_model=list[PuanDuzeltmeDto])
def duzeltmeler(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> list[PuanDuzeltmeDto]:
    """
    Kullanıcıya uygulanmış düzeltmeler.

    **Rıza kapısına bağlı değil** (K19 istisnası, K28 ile aynı sınıf):
    bu davranışsal veri değil, kullanıcının kendi bakiyesine yapılmış bir
    işlem. Paylaşımı kapalı kullanıcının puanının değişmemesi, düzeltmeyi
    hiç uygulanamaz kılardı.

    `cihaz_uyguladi` bayrağı burada işaretleniyor — **bilgilendirici**:
    panelde "cihaza indi mi" sorusunu cevaplıyor. Tekrar koruması cihazın
    birincil anahtarında.
    """
    kayitlar = db.scalars(
        select(PuanDuzeltme)
        .where(PuanDuzeltme.kullanici_id == kullanici.id)
        .order_by(PuanDuzeltme.zaman.asc(), PuanDuzeltme.id.asc())
    ).all()

    yeniler = [k.id for k in kayitlar if not k.cihaz_uyguladi]
    if yeniler:
        db.execute(
            update(PuanDuzeltme)
            .where(PuanDuzeltme.id.in_(yeniler))
            .values(cihaz_uyguladi=True)
        )
        db.commit()

    return [
        PuanDuzeltmeDto(
            id=k.id, delta=k.delta, gerekce=k.gerekce, zaman=k.zaman
        )
        for k in kayitlar
    ]
