"""Ödül seçimi — bazı kademelerde market kuponu yerine fidan bağışı gibi alternatifler."""

from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app import odul_stogu
from app.guvenlik import kullaniciyi_kilitle, mevcut_kullanici
from app.modeller import Kullanici, OdulSecimi, OdulTeslimat
from app.semalar import OdulKademeDurumuDto, OdulSecimIstek, OdulSecimiDto
from app.veritabani import oturum

yonlendirici = APIRouter(prefix="/odul", tags=["odul"])

# Sunucu puan hak edişini henüz bağımsız hesaplamıyor; yine de istemci
# keyfî bir kimlikle kupon/bağış kaydı oluşturamamalı. Bu sözleşme
# `OdulViewModel.KADEMELER` ve `docs/spec-puan.md §9.1` ile aynıdır.
# `rozet` kademesi BİLEREK yok: dijital bir başarı kartı, tedarik zinciri
# gerektirmiyor (K28) ve stok kapısına takılmamalı. Cihaz da o kademeye
# kapıyı hiç uygulamıyor (`OdulViewModel.Kademe.stogaBagli`).
SECILEBILIR_ODULLER = {
    "fidan": {"fidan_bagisi"},
    "kupon_market": {"market_kuponu", "fidan_bagisi"},
    "kupon_spor": {"kupon_spor_kitap", "fidan_bagisi"},
}

# Kapı artık sabit değil **stoğun kendisi** (`app/odul_stogu.py`,
# `docs/panel-plani.md §5.4`): panelden parti yüklenince kademe açılıyor,
# stok bitince kapanıyor. A4'ün teknik ayağı böyle çözüldü; finansman
# ayağı hâlâ Yeşilay'da.
#
# Sabit `MADDI_ODUL_TESLIMATI_ETKIN = False` kaldırıldı. Stok satırı
# yoksa kapı yine kapalı — "satır yok" "sınırsız" demek değil (fail-closed,
# §4/K37), yani hiç parti yüklenmemiş bir kurulumda davranış aynı.
#
# `fidan` kademesi 27 Ağu 2026'da eklendi. K62 "fidan bağışı da stok
# istiyor" diyor ama bu sözlük onu tanımıyordu: panel ona parti
# yükleyemiyor, `/odul/durum` satır döndürmüyordu. Yani kademe finansman
# gelse bile açılamıyordu — kararın vaat ettiğinin tersi. Tek seçenekli
# olduğu için `POST /odul/sec` yolundan geçmiyor, ama doğrulama listesi ve
# stok ekranı da aynı sözlükten türediği için burada durması gerekiyor.


@yonlendirici.post("/sec", response_model=OdulSecimiDto, status_code=status.HTTP_201_CREATED)
def sec(
    istek: OdulSecimIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> OdulSecimiDto:
    """
    Bir kademe için seçim **bir kez** yapılır.

    Seçim **stoktan düşüyor**: yoksa stok biterken aynı anda gelen iki
    seçim ikisi de kabul edilir ve karşılanamayan bir vaat üretilirdi.
    K28 seçimi geri alınamaz kıldığı için bu düzeltilemezdi.

    Stok yoksa `503` ve **hiçbir satır yazılmıyor** — fail-closed.
    """
    if istek.secenekId not in SECILEBILIR_ODULLER.get(istek.kademeId, set()):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Geçersiz ödül seçimi")

    kullaniciyi_kilitle(db, kullanici.id)

    var = db.scalar(
        select(OdulSecimi).where(
            OdulSecimi.kullanici_id == kullanici.id,
            OdulSecimi.kademe_id == istek.kademeId,
        )
    )
    if var is not None:
        raise HTTPException(status.HTTP_409_CONFLICT, "Bu kademe için seçim zaten yapılmış")

    if not odul_stogu.dus(db, istek.kademeId, istek.secenekId):
        db.rollback()
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "Bu ödül şu anda karşılanamıyor. Stok geldiğinde yeniden seçebilirsin.",
        )

    kayit = OdulSecimi(
        kullanici_id=kullanici.id,
        kademe_id=istek.kademeId,
        secenek_id=istek.secenekId,
    )
    db.add(kayit)
    db.flush()
    # Teslimat kaydı seçimle **birlikte** açılıyor: panelin "kimin
    # kuponu bekliyor" listesi bunun üzerinden ve sonradan açmak, arada
    # kalan seçimleri listede görünmez bırakırdı.
    db.add(OdulTeslimat(odul_secimi_id=kayit.id))
    db.commit()
    db.refresh(kayit)
    return OdulSecimiDto(kademeId=kayit.kademe_id, secenekId=kayit.secenek_id)


@yonlendirici.get("/secimlerim", response_model=list[OdulSecimiDto])
def secimlerim(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> list[OdulSecimiDto]:
    """Telefon değişince/yeniden kurulumda geçmiş seçimleri geri yükler."""
    kayitlar = db.scalars(
        select(OdulSecimi).where(OdulSecimi.kullanici_id == kullanici.id)
    ).all()
    return [OdulSecimiDto(kademeId=k.kademe_id, secenekId=k.secenek_id) for k in kayitlar]


@yonlendirici.get("/durum", response_model=list[OdulKademeDurumuDto])
def kademe_durumlari(db: Session = Depends(oturum)) -> list[OdulKademeDurumuDto]:
    """
    Hangi kademe/seçenek şu an **seçilebilir**?

    Cihaz bunu seçim ekranını çizmeden önce soruyor: kapalı bir seçeneği
    sunup 503 ile reddetmek, olmayan bir eylemi sunmak olurdu (§4, K37).
    Kullanıcı kademeyi görüyor ama "şu anda karşılanamıyor" yazısıyla.

    Kimlik **istemiyor**: hangi ödüllerin stokta olduğu kişisel bir bilgi
    değil ve misafir kullanıcı da kademeleri görüyor.
    """
    return [
        OdulKademeDurumuDto(
            kademeId=kademe_id,
            secenekId=secenek_id,
            acik=odul_stogu.acik_mi(db, kademe_id, secenek_id),
        )
        for kademe_id, secenekler in SECILEBILIR_ODULLER.items()
        for secenek_id in sorted(secenekler)
    ]
