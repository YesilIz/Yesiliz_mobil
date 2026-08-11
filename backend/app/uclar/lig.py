"""Lig sıralaması ve arkadaşlık."""

from __future__ import annotations

import datetime as dt
import secrets

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.ayarlar import ayarlar
from app.guvenlik import kullaniciyi_kilitle, mevcut_kullanici
from app.modeller import Arkadaslik, DavetKodu, Kullanici, LigGrup, LigUyelik
from app.semalar import (
    ArkadasDto,
    DavetIstek,
    KodYanit,
    LigGorunurlukIstek,
    LigKatilIstek,
    LigSatiriDto,
    LigYanit,
)
from app.veritabani import oturum
from app.zaman import yerel_bugun

yonlendirici = APIRouter(tags=["lig"])

# CLAUDE.md §12 K1 — ağaç metaforu.
SEVIYELER = ["Fidan Ligi", "Filiz Ligi", "Ağaç Ligi", "Koru Ligi", "Orman Ligi"]

# Davet kodu alfabesi — cihazdaki `DavetKodu.kt` ile BİREBİR aynı.
# I, O, 0, 1 yok: kod sesli paylaşılıyor ve bu karakterler ayırt edilemiyor.
_ALFABE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
_ONEK = "YSZ"


def hafta_baslangici(gun: dt.date | None = None) -> dt.date:
    """
    İçinde bulunulan takvim haftasının pazartesisi.

    `Aralik.HAFTA` (İstatistik) ve `BildirimGrupla` ile aynı tanım —
    ISO haftası, pazartesi başlangıçlı.
    """
    gun = gun or yerel_bugun()
    return gun - dt.timedelta(days=gun.weekday())


@yonlendirici.get("/lig", response_model=LigYanit)
def lig(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> LigYanit:
    a = ayarlar()
    hafta = hafta_baslangici()
    uyelik = db.scalar(
        select(LigUyelik).where(
            LigUyelik.kullanici_id == kullanici.id, LigUyelik.hafta_baslangic == hafta
        )
    )

    if uyelik is None:
        # Katılmamış kullanıcı: boş durum (tasarım 37). Sahte bir sıralama
        # göstermek yerine cihaz boş durumu çiziyor.
        return LigYanit(
            ligAdi=SEVIYELER[0],
            kalanGun=_kalan_gun(hafta),
            yukselen=a.lig_yukselen,
            dusen=a.lig_dusen,
            siralama=[],
            katildiMi=False,
        )

    grup = db.get(LigGrup, uyelik.grup_id)
    arkadas_idler = _arkadas_idleri(db, kullanici.id)

    uyelikler = db.scalars(
        select(LigUyelik)
        .where(LigUyelik.grup_id == uyelik.grup_id)
        .order_by(LigUyelik.haftalik_puan.desc(), LigUyelik.kullanici_id)
    ).all()

    # "Ligde görün" kapalı olanlar listeden çıkıyor; kullanıcının KENDİSİ
    # her hâlükârda kendi satırını görüyor, yoksa sıralamada nerede
    # olduğunu hiç öğrenemezdi (debug.md Y-5).
    gorunenler = [u for u in uyelikler if u.gorunur or u.kullanici_id == kullanici.id]

    siralama = []
    for indeks, u in enumerate(gorunenler, start=1):
        k = db.get(Kullanici, u.kullanici_id)
        if k is None:
            continue
        siralama.append(
            LigSatiriDto(
                sira=indeks,
                isim=k.ad,
                puan=u.haftalik_puan,
                benMiyim=k.id == kullanici.id,
                arkadasMi=k.id in arkadas_idler,
            )
        )

    return LigYanit(
        ligAdi=SEVIYELER[min(grup.seviye if grup else 0, len(SEVIYELER) - 1)],
        kalanGun=_kalan_gun(hafta),
        yukselen=a.lig_yukselen,
        dusen=a.lig_dusen,
        siralama=siralama,
        katildiMi=True,
        gorunuyorMuyum=uyelik.gorunur,
    )


@yonlendirici.post("/lig/katil", response_model=LigYanit)
def katil(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
    istek: LigKatilIstek | None = None,
) -> LigYanit:
    """
    Uygun gruba yerleştirir; yer yoksa yeni grup açar.

    Eşleştirme **seviyeye göre**: yeni kullanıcı Fidan'dan başlıyor.
    Hafta sonunda ilk N yükseliyor, son N düşüyor — ama **puan silinmiyor**,
    yalnızca grup değişiyor (CLAUDE.md §4 ceza yasağı).
    """
    a = ayarlar()
    hafta = hafta_baslangici()

    # Farklı kullanıcılar aynı anda ilk grubu oluşturursa her biri tek
    # kişilik ayrı grup açabilirdi. En eski kullanıcı satırı bu kısa
    # yerleştirme işlemi için kararlı, ortak kilittir.
    db.scalar(select(Kullanici.id).order_by(Kullanici.id).limit(1).with_for_update())
    # Aynı hesabın paralel katılma istekleri tekillik kontrolünden aynı
    # anda geçmesin. Kilit işlem bitene kadar tutulur.
    kullaniciyi_kilitle(db, kullanici.id)

    mevcut = db.scalar(
        select(LigUyelik).where(
            LigUyelik.kullanici_id == kullanici.id, LigUyelik.hafta_baslangic == hafta
        )
    )
    if mevcut is None:
        seviye = _onceki_seviye(db, kullanici.id, hafta)
        grup = _uygun_grup(db, hafta, seviye, a.lig_grup_boyu)
        db.add(
            LigUyelik(
                kullanici_id=kullanici.id,
                grup_id=grup.id,
                hafta_baslangic=hafta,
                haftalik_puan=0,
                # Yeni istemci yerel gizlilik tercihini katılım anında
                # gönderiyor. Gövdesiz eski istemci için geriye uyumlu
                # varsayılan görünürdür.
                gorunur=istek.gorunur if istek is not None else True,
            )
        )
        db.commit()
    elif istek is not None and mevcut.gorunur != istek.gorunur:
        # Aynı ucu yeniden çağırmak üyelik açmaz ama cihazdaki güncel
        # tercihi geri yükleyebilir.
        mevcut.gorunur = istek.gorunur
        db.commit()

    return lig(db=db, kullanici=kullanici)


@yonlendirici.post("/lig/gorunurluk")
def gorunurluk(
    istek: LigGorunurlukIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> dict[str, str]:
    """Lig gizliliğini davranış verisi paylaşımından bağımsız kaydeder.

    Üyelik yoksa işlem idempotenttir. Uygulama gelecekteki katılımda aynı
    tercihi `/lig/katil` gövdesiyle yeniden yollar.
    """
    kullaniciyi_kilitle(db, kullanici.id)
    uyelik = db.scalar(
        select(LigUyelik).where(
            LigUyelik.kullanici_id == kullanici.id,
            LigUyelik.hafta_baslangic == hafta_baslangici(),
        )
    )
    if uyelik is not None:
        uyelik.gorunur = istek.gorunur
    db.commit()
    return {"durum": "guncellendi"}


@yonlendirici.get("/arkadas", response_model=list[ArkadasDto])
def arkadaslar(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> list[ArkadasDto]:
    satirlar = db.scalars(
        select(Arkadaslik)
        .where(Arkadaslik.kullanici_id == kullanici.id)
        .order_by(Arkadaslik.olusturma.desc(), Arkadaslik.id.desc())
    ).all()

    sonuc = []
    for s in satirlar:
        k = db.get(Kullanici, s.arkadas_id)
        if k is not None:
            sonuc.append(ArkadasDto(isim=k.ad, davetBekliyorMu=s.durum == "bekliyor"))
    return sonuc


@yonlendirici.get("/arkadas/kod", response_model=KodYanit)
def davet_kodu(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> KodYanit:
    """Kullanıcı başına tek, kalıcı kod. Var olan kod **değiştirilmiyor**."""
    kullaniciyi_kilitle(db, kullanici.id)
    kayit = db.scalar(select(DavetKodu).where(DavetKodu.kullanici_id == kullanici.id))
    if kayit is None:
        kayit = DavetKodu(kullanici_id=kullanici.id, kod=_kod_uret(db))
        db.add(kayit)
        db.commit()
    return KodYanit(kod=kayit.kod)


@yonlendirici.post("/arkadas/davet", response_model=list[ArkadasDto])
def davet(
    istek: DavetIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> list[ArkadasDto]:
    kod = istek.kod.strip().upper()
    kayit = db.scalar(select(DavetKodu).where(DavetKodu.kod == kod))
    if kayit is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Davet kodu bulunamadı")
    if kayit.kullanici_id == kullanici.id:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Kendi kodunu kullanamazsın")

    # İki kullanıcı birbirinin kodunu aynı anda girse bile karşılıklı
    # tekil satırları bir kez kur. Sabit id sırası deadlock'u önler.
    kilitlenenler = db.scalars(
        select(Kullanici.id)
        .where(Kullanici.id.in_(sorted((kullanici.id, kayit.kullanici_id))))
        .order_by(Kullanici.id)
        .with_for_update()
    ).all()
    if len(kilitlenenler) != 2:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Davet kodu bulunamadı")
    _arkadaslik_kur(db, kullanici.id, kayit.kullanici_id)
    db.commit()
    return arkadaslar(db=db, kullanici=kullanici)


def _arkadaslik_kur(db: Session, a_id: int, b_id: int) -> None:
    """
    Karşılıklı iki satır.

    Kod paylaşmak zaten bir rıza göstergesi olduğu için ayrı bir onay adımı
    yok — kodu veren kişi daveti kabul etmiş sayılıyor.
    """
    for kaynak, hedef in ((a_id, b_id), (b_id, a_id)):
        var = db.scalar(
            select(Arkadaslik).where(
                Arkadaslik.kullanici_id == kaynak, Arkadaslik.arkadas_id == hedef
            )
        )
        if var is None:
            db.add(Arkadaslik(kullanici_id=kaynak, arkadas_id=hedef, durum="kabul"))
        else:
            var.durum = "kabul"


def _arkadas_idleri(db: Session, kullanici_id: int) -> set[int]:
    return {
        s.arkadas_id
        for s in db.scalars(
            select(Arkadaslik).where(
                Arkadaslik.kullanici_id == kullanici_id, Arkadaslik.durum == "kabul"
            )
        )
    }


def _uygun_grup(db: Session, hafta: dt.date, seviye: int, boy: int) -> LigGrup:
    """Aynı seviyede yeri olan ilk grup; yoksa yeni grup."""
    gruplar = db.scalars(
        select(LigGrup)
        .where(LigGrup.hafta_baslangic == hafta, LigGrup.seviye == seviye)
        .order_by(LigGrup.id)
        .with_for_update()
    ).all()

    for grup in gruplar:
        sayi = db.scalar(select(func.count()).select_from(LigUyelik).where(LigUyelik.grup_id == grup.id))
        if (sayi or 0) < boy:
            return grup

    yeni = LigGrup(hafta_baslangic=hafta, seviye=seviye)
    db.add(yeni)
    db.flush()
    db.refresh(yeni)
    return yeni


def _onceki_seviye(db: Session, kullanici_id: int, hafta: dt.date) -> int:
    """
    Geçen haftanın sonucuna göre seviye.

    Yükselme/düşme **grup değiştiriyor, puan silmiyor**. Düşen kullanıcı da
    kazandığı puanı koruyor; yalnızca bir alt seviyeye geçiyor.
    """
    a = ayarlar()
    onceki_hafta = hafta - dt.timedelta(days=7)
    onceki = db.scalar(
        select(LigUyelik).where(
            LigUyelik.kullanici_id == kullanici_id, LigUyelik.hafta_baslangic == onceki_hafta
        )
    )
    if onceki is None:
        return 0

    grup = db.get(LigGrup, onceki.grup_id)
    seviye = grup.seviye if grup else 0

    siralama = db.scalars(
        select(LigUyelik)
        .where(LigUyelik.grup_id == onceki.grup_id)
        .order_by(LigUyelik.haftalik_puan.desc(), LigUyelik.kullanici_id)
    ).all()
    idler = [u.kullanici_id for u in siralama]
    if kullanici_id not in idler:
        return seviye

    sira = idler.index(kullanici_id) + 1
    if sira <= a.lig_yukselen:
        return min(seviye + 1, len(SEVIYELER) - 1)
    if sira > len(idler) - a.lig_dusen:
        return max(seviye - 1, 0)
    return seviye


def _kalan_gun(hafta: dt.date, bugun: dt.date | None = None) -> int:
    bitis = hafta + dt.timedelta(days=7)
    return max((bitis - (bugun or yerel_bugun())).days, 0)


def _kod_uret(db: Session) -> str:
    """Çakışma olmayana kadar dene — alfabe 32^4 ≈ 1M olasılık."""
    for _ in range(20):
        govde = "".join(secrets.choice(_ALFABE) for _ in range(4))
        kod = f"{_ONEK}-{govde}"
        if db.scalar(select(DavetKodu).where(DavetKodu.kod == kod)) is None:
            return kod
    raise HTTPException(status.HTTP_500_INTERNAL_SERVER_ERROR, "Kod üretilemedi")
