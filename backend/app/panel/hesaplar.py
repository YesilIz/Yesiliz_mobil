"""
Hesap arama ve detayı (`docs/panel-plani.md §5.2`).

**P9 sınırı bu dosyada yaşıyor.** Admin ve operasyon, tekil kullanıcının
davranış verisini varsayılan olarak göremiyor: kategori dakikası, örüntü
değeri, risk skoru ve bandı burada **hiç sorgulanmıyor**. `gunluk_ozet` ve
`risk_sonuc` tablolarına dokunan tek satır yok ve `test_panel_hesaplar.py`
çalışan SQL'i dinleyip bunu doğruluyor — "faydalı olur" diye eklenmesi
kolay, sessizce eklenmesi ise planın en açık sözünü bozar.

Toplu ve anonim istatistik ayrı bir şey ve serbest: `panel/gosterge.py`
aynı tabloları okuyor ama kullanıcı ayırt etmeden.

Arama **ad ile yapılmıyor**: ad benzersiz değil ve yanlış hesabı askıya
almak ya da silmek geri alınamaz.
"""

from __future__ import annotations

import datetime as dt
from dataclasses import dataclass, field

from sqlalchemy import Select, func, or_, select, update
from sqlalchemy.orm import Session

from app.modeller import (
    EbeveynBaglanti,
    Heartbeat,
    Kullanici,
    KullaniciDurumu,
    LigUyelik,
    OdulSecimi,
    YenilemeJetonu,
)

#: Tek sayfada gösterilen hesap sayısı.
SAYFA_BOYU = 25

#: Arama kutusunun kabul ettiği en uzun metin. Uzun girdi indeks
#: kullanmayan bir `LIKE` taramasına dönüşüyor.
_SORGU_SINIRI = 320


@dataclass(frozen=True)
class KullaniciSatiri:
    """Arama sonucu satırı — listede görünen asgari alan."""

    id: int
    eposta: str
    ad: str
    olusturma: dt.datetime
    son_sinyal: dt.datetime | None
    durum: str


@dataclass(frozen=True)
class LigSatiri:
    hafta_baslangic: dt.date
    haftalik_puan: int
    gorunur: bool


@dataclass(frozen=True)
class KullaniciDetay:
    """
    Hesap detayı — **davranış verisi yok** (P9).

    Buraya `risk_bandi`, `ekran_dk` ya da kategori alanı eklemek plan
    kararını değiştirmek demek; ayrı bir karar olarak alınmalı ve KVKK
    talebi işlemi olarak gerekçeyle kaydedilmeli.
    """

    id: int
    eposta: str
    ad: str
    avatar: str
    olusturma: dt.datetime
    son_sinyal: dt.datetime | None
    durum: str
    askiya_alma_sebebi: str
    askiya_alma_zamani: dt.datetime | None
    ebeveyn_bagi_var: bool
    ebeveyn_rolu: str
    lig_gecmisi: list[LigSatiri] = field(default_factory=list)
    odul_secimleri: list[OdulSecimi] = field(default_factory=list)


def _arama_kosulu(sorgu: str) -> Select:
    """
    E-posta parçası ya da kullanıcı numarası.

    Sayı girildiğinde kimlik araması **de** yapılıyor ama e-posta araması
    kapanmıyor: `115` hem bir kullanıcı numarası hem de bir e-posta parçası
    olabilir.
    """
    temiz = sorgu.strip()[:_SORGU_SINIRI]
    kosullar = [Kullanici.eposta.ilike(f"%{temiz}%")]
    if temiz.isdigit():
        kosullar.append(Kullanici.id == int(temiz))
    return or_(*kosullar)


def ara(db: Session, sorgu: str, sayfa: int = 0) -> tuple[list[KullaniciSatiri], int]:
    """Eşleşen hesapları ve toplam sayıyı döndürür."""
    temiz = sorgu.strip()
    if not temiz:
        return [], 0

    kosul = _arama_kosulu(temiz)
    toplam = db.scalar(select(func.count()).select_from(Kullanici).where(kosul)) or 0

    satirlar = db.execute(
        select(Kullanici, Heartbeat.son_sinyal)
        .outerjoin(Heartbeat, Heartbeat.kullanici_id == Kullanici.id)
        .where(kosul)
        .order_by(Kullanici.id)
        .offset(sayfa * SAYFA_BOYU)
        .limit(SAYFA_BOYU)
    ).all()

    return [
        KullaniciSatiri(
            id=k.id,
            eposta=k.eposta,
            ad=k.ad,
            olusturma=k.olusturma,
            son_sinyal=sinyal,
            durum=k.durum,
        )
        for k, sinyal in satirlar
    ], toplam


def detay(db: Session, kullanici_id: int) -> KullaniciDetay | None:
    kullanici = db.get(Kullanici, kullanici_id)
    if kullanici is None:
        return None

    sinyal = db.scalar(
        select(Heartbeat.son_sinyal).where(Heartbeat.kullanici_id == kullanici_id)
    )

    bag = db.scalar(
        select(EbeveynBaglanti).where(
            or_(
                EbeveynBaglanti.ebeveyn_id == kullanici_id,
                EbeveynBaglanti.cocuk_id == kullanici_id,
            )
        )
    )
    if bag is None:
        rol = ""
    elif bag.ebeveyn_id == kullanici_id:
        rol = "ebeveyn"
    else:
        rol = "çocuk"

    # Karşı tarafın kimliği BİLİNÇLİ olarak taşınmıyor: bağın varlığı
    # destek talebini çözmeye yetiyor, karşı hesabın e-postasını göstermek
    # ikinci bir kullanıcının verisini açardı.
    lig = db.scalars(
        select(LigUyelik)
        .where(LigUyelik.kullanici_id == kullanici_id)
        .order_by(LigUyelik.hafta_baslangic.desc())
        .limit(8)
    ).all()

    oduller = db.scalars(
        select(OdulSecimi)
        .where(OdulSecimi.kullanici_id == kullanici_id)
        .order_by(OdulSecimi.tarih.desc())
    ).all()

    return KullaniciDetay(
        id=kullanici.id,
        eposta=kullanici.eposta,
        ad=kullanici.ad,
        avatar=kullanici.avatar or "",
        olusturma=kullanici.olusturma,
        son_sinyal=sinyal,
        durum=kullanici.durum,
        askiya_alma_sebebi=kullanici.askiya_alma_sebebi or "",
        askiya_alma_zamani=kullanici.askiya_alma_zamani,
        ebeveyn_bagi_var=bag is not None,
        ebeveyn_rolu=rol,
        lig_gecmisi=[
            LigSatiri(
                hafta_baslangic=u.hafta_baslangic,
                haftalik_puan=u.haftalik_puan,
                gorunur=u.gorunur,
            )
            for u in lig
        ],
        odul_secimleri=list(oduller),
    )


def askiya_al(
    db: Session,
    kullanici: Kullanici,
    personel_id: int,
    sebep: str,
) -> None:
    """
    Hesabı askıya alır ve **açık oturumlarını düşürür**; commit etmez.

    Jetonları iptal etmek şart: erişim jetonu 30 dakika daha geçerli
    olurdu ve `mevcut_kullanici` kapısı olmasa askı o pencerede fiilen
    uygulanmazdı. İki koruma birlikte duruyor — kapı kalıcı, iptal ise
    cihazı hemen dışarı atıyor.

    **Veri silinmiyor.** Silme ayrı ve sert bir işlem
    (`app/kullanici_silme.py`); askı geri alınabilir.
    """
    kullanici.durum = KullaniciDurumu.ASKIDA.value
    kullanici.askiya_alma_sebebi = sebep.strip()[:500]
    kullanici.askiya_alan_id = personel_id
    kullanici.askiya_alma_zamani = dt.datetime.now(dt.timezone.utc)

    db.execute(
        update(YenilemeJetonu)
        .where(YenilemeJetonu.kullanici_id == kullanici.id)
        .values(iptal=True)
    )


def askiyi_kaldir(db: Session, kullanici: Kullanici) -> None:
    """
    Askıyı kaldırır; commit etmez.

    Sebep alanı **temizleniyor**: hesap yeniden aktifken orada duran eski
    bir gerekçe, sonraki bir askıda yanlışlıkla kullanıcıya gösterilirdi.
    Kalıcı kayıt `denetim_kaydi`'nda.

    Jetonlar geri gelmiyor — kullanıcı yeniden giriş yapıyor.
    """
    kullanici.durum = KullaniciDurumu.AKTIF.value
    kullanici.askiya_alma_sebebi = ""
    kullanici.askiya_alan_id = None
    kullanici.askiya_alma_zamani = None
