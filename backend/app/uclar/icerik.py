"""
İçerik uçları — bilgi bankası ve duyuru (`docs/panel-plani.md §5.5, §5.6`).

**Kimlik istemiyor.** Bilgi bankası herkese açık bir içerik ve misafir
kullanıcı da okuyor; duyuru da kişiye özel değil (`hedef = hepsi`).
Kimlik şartı koymak, misafiri okuyamaz hâle getirirdi.

**Rıza kapısı da yok** ve olmamalı: bu uçlar veri **indiriyor**, kullanıcı
verisi göndermiyor. K19 gönderim kapısı.
"""

from __future__ import annotations

import datetime as dt
import logging

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.modeller import BilgiYazisi, Duyuru
from app.semalar import BilgiBankasiYanit, BilgiYazisiDto, DuyuruDto
from app.veritabani import oturum

gunluk = logging.getLogger(__name__)
yonlendirici = APIRouter(prefix="/icerik", tags=["icerik"])

#: Tek istekte dönen en fazla duyuru. Cihaz `sonrasi` ile ilerliyor.
DUYURU_SINIRI = 50


def _dto(y: BilgiYazisi) -> BilgiYazisiDto:
    return BilgiYazisiDto(
        id=y.id,
        baslik=y.baslik,
        ozet=y.ozet,
        kategori=y.kategori,
        okumaDk=y.okuma_dk,
        # Gövde satır sonuyla saklanıyor; cihaz paragraf listesi bekliyor.
        govde=[p for p in y.govde.split("\n") if p.strip()],
    )


@yonlendirici.get("/bilgi-bankasi", response_model=BilgiBankasiYanit)
def bilgi_bankasi(db: Session = Depends(oturum)) -> BilgiBankasiYanit:
    """
    Yayındaki yazılar.

    **Boş liste dönerse cihaz gömülü sete düşüyor** (S5): sunucuda hiç
    içerik yokken boş bir bilgi bankası göstermek, var olan bir özelliği
    yok gibi sunmak olurdu. Sunucu bunu bilemez; kararı cihaz veriyor ve
    bu uç yalnızca dürüst davranıyor — uydurma yazı üretmiyor.

    `surum` yazıların en yüksek sürümü ile sayısının birleşimi: bir yazı
    düzeltilince de, bir yazı eklenince/kaldırılınca da değişiyor.
    """
    yazilar = db.scalars(
        select(BilgiYazisi)
        .where(BilgiYazisi.yayinda.is_(True))
        .order_by(BilgiYazisi.sira, BilgiYazisi.id)
    ).all()

    surum = sum(y.surum for y in yazilar) + len(yazilar)
    return BilgiBankasiYanit(surum=surum, yazilar=[_dto(y) for y in yazilar])


@yonlendirici.get("/bilgi-bankasi/{yazi_id}", response_model=BilgiYazisiDto)
def bilgi_yazisi(yazi_id: str, db: Session = Depends(oturum)) -> BilgiYazisiDto:
    """
    Tek yazı — **yayından kaldırılmış olsa da dönüyor**.

    Cihazın önbelleğinde kalmış bir yazının bağlantısını kırmak, onu
    listeden düşürmekten farklı bir şey: kullanıcı okumaya başlamış
    olabilir. Liste ucundan düşüyor, tekil okuma çalışmaya devam ediyor.
    """
    yazi = db.get(BilgiYazisi, yazi_id)
    if yazi is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Yazı bulunamadı")
    return _dto(yazi)


@yonlendirici.get("/duyuru", response_model=list[DuyuruDto])
def duyurular(
    sonrasi: int = Query(0, ge=0),
    db: Session = Depends(oturum),
) -> list[DuyuruDto]:
    """
    Yayımlanmış duyurular; `sonrasi`'ndan büyük id'ler.

    Cihaz en son gördüğü id'yi tutuyor ve yalnızca yenilerini çekiyor:
    aynı duyuruyu ikinci kez bildirim defterine yazmak, kullanıcıya aynı
    şeyi iki kez söylemek olurdu.

    Yayımlanmamış (taslak) duyuru **hiç dönmüyor** — `yayin_zamani` boş
    olan kayıt henüz bir karar değil.
    """
    simdi = dt.datetime.now(dt.timezone.utc)
    kayitlar = db.scalars(
        select(Duyuru)
        .where(
            Duyuru.id > sonrasi,
            Duyuru.yayin_zamani.is_not(None),
            Duyuru.yayin_zamani <= simdi,
        )
        .order_by(Duyuru.id)
        .limit(DUYURU_SINIRI)
    ).all()
    return [
        DuyuruDto(
            id=d.id, baslik=d.baslik, metin=d.metin, yayinZamani=d.yayin_zamani
        )
        for d in kayitlar
    ]
