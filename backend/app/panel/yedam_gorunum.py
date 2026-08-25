"""
YEDAM panelinin sorgu katmanı (`docs/panel-plani.md §6`).

**Danışman arama yapamıyor.** Havuz ve detay, `personel`'in rolüne göre
daraltılıyor: `yedam_danisman` yalnızca **kendisine atanmış** başvuruyu
görüyor. Rıza vermemiş bir kullanıcıya ulaşmanın hiçbir yüzeyi yok — rıza
metni bunu açıkça vaat ediyor.

**Rıza her okumada kontrol ediliyor.** Süresi dolmuş ya da geri çekilmiş
rızada detay veri döndürmüyor; zamanlanmış işin koşmuş olmasına
güvenmiyoruz.

Ekranda **olmayanlar** (§6.2):
- uygulama adları ve oturum dökümü — sunucuda hiç yok, mahremiyet
  sözleşmesi değişmiyor
- teşhis etiketi — §4 panelde de geçerli, danışman kendi klinik
  değerlendirmesini yapar
"""

from __future__ import annotations

import datetime as dt
from dataclasses import dataclass, field

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.modeller import (
    AnketYanit,
    BasvuruDurumu,
    DanismanNotu,
    ErisimKaydi,
    GunlukOzet,
    HAVUZDA_GORUNEN,
    Kullanici,
    Personel,
    PersonelRolu,
    RiskSonuc,
    RiskSurukleyici,
    YedamBasvuru,
    YedamRiza,
    YedamTemas,
)
from app.yedam_riza_yonetimi import gecerli_riza

#: Detayda gösterilen risk penceresi. Rıza metni "son 30 günün risk
#: bandın" diyor; daha geniş bir pencere rızanın kapsamadığı veriyi
#: gösterirdi.
RISK_PENCERESI_GUN = 30

#: Sürükleyicide gösterilen en yüksek katkılı faktör sayısı. Rıza metni
#: "skorunu en çok yükselten kategoriler" diyor, tam döküm değil.
SURUKLEYICI_SAYISI = 3


@dataclass(frozen=True)
class HavuzSatiri:
    id: int
    durum: str
    olusturma: dt.datetime
    iletisim_turu: str
    atanan_ad: str
    #: Başvurunun yaşı (gün). En eskisi öne çıkmalı.
    yas_gun: int
    temas_sayisi: int


@dataclass(frozen=True)
class BantNoktasi:
    tarih: dt.date
    bant: str
    trend: str
    veri_durumu: str


@dataclass(frozen=True)
class SurukleyiciSatiri:
    faktor: str
    sira: int


@dataclass(frozen=True)
class AnketSatiri:
    tarih: dt.date
    madde_puanlari: list[int]
    ham_puan: float
    r_s: float


@dataclass(frozen=True)
class TemasSatiri:
    tur: str
    zaman: dt.datetime
    personel_ad: str


@dataclass(frozen=True)
class NotSatiri:
    metin: str
    zaman: dt.datetime
    personel_ad: str


@dataclass(frozen=True)
class BasvuruDetay:
    id: int
    durum: str
    olusturma: dt.datetime
    kullanici_ad: str
    iletisim_turu: str
    iletisim_degeri: str
    kullanici_notu: str
    atanan_personel_id: int | None
    atanan_ad: str

    #: Rıza durumu — başlık şeridinde gösteriliyor.
    riza_gecerli: bool
    riza_bitis: dt.datetime | None
    riza_kapsam: tuple[str, ...] = ()

    #: 18 altı ise ebeveyn onayı durumu (K26).
    beyan_yas: int | None = None
    ebeveyn_onayi_var: bool = False

    bant_cizgisi: list[BantNoktasi] = field(default_factory=list)
    surukleyiciler: list[SurukleyiciSatiri] = field(default_factory=list)
    anketler: list[AnketSatiri] = field(default_factory=list)
    temaslar: list[TemasSatiri] = field(default_factory=list)
    notlar: list[NotSatiri] = field(default_factory=list)

    #: Davranış özeti YALNIZCA "Yeşilay ile paylaşım" açıkken dolu (K19).
    #: İki rıza bağımsız; YEDAM rızası o kapıyı açmıyor.
    davranis_paylasilmiyor: bool = True
    ortalama_ekran_dk: int = 0

    @property
    def guncel_bant(self) -> str:
        return self.bant_cizgisi[-1].bant if self.bant_cizgisi else ""


def _yalnizca_kendi(personel: Personel) -> bool:
    """Danışman yalnızca kendisine atanmış başvuruyu görüyor (§6.2)."""
    return personel.rol == PersonelRolu.YEDAM_DANISMAN.value


def havuz(db: Session, personel: Personel, simdi: dt.datetime | None = None) -> list[HavuzSatiri]:
    simdi = simdi or dt.datetime.now(dt.timezone.utc)

    kosullar = [YedamBasvuru.durum.in_(HAVUZDA_GORUNEN)]
    if _yalnizca_kendi(personel):
        kosullar.append(YedamBasvuru.atanan_personel_id == personel.id)

    temas_sayilari = dict(
        db.execute(
            select(YedamTemas.basvuru_id, func.count()).group_by(YedamTemas.basvuru_id)
        ).all()
    )

    satirlar = db.execute(
        select(YedamBasvuru, Personel.ad)
        .outerjoin(Personel, Personel.id == YedamBasvuru.atanan_personel_id)
        .where(*kosullar)
        # En eski önce: bekleyen başvuru en pahalı olan.
        .order_by(YedamBasvuru.olusturma.asc(), YedamBasvuru.id.asc())
    ).all()

    return [
        HavuzSatiri(
            id=b.id,
            durum=b.durum,
            olusturma=b.olusturma,
            iletisim_turu=b.iletisim_turu,
            atanan_ad=ad or "",
            yas_gun=max(0, (simdi - _utc(b.olusturma)).days),
            temas_sayisi=temas_sayilari.get(b.id, 0),
        )
        for b, ad in satirlar
    ]


def _utc(deger: dt.datetime) -> dt.datetime:
    return deger if deger.tzinfo else deger.replace(tzinfo=dt.timezone.utc)


def basvuruyu_getir(db: Session, personel: Personel, basvuru_id: int) -> YedamBasvuru | None:
    """
    Başvuruyu **yetki daraltmasıyla** okur.

    Danışman kendisine atanmamış bir başvurunun id'sini tahmin edip
    açamıyor: daraltma sorgunun kendisinde, ekranda değil.
    """
    kosullar = [YedamBasvuru.id == basvuru_id]
    if _yalnizca_kendi(personel):
        kosullar.append(YedamBasvuru.atanan_personel_id == personel.id)

    basvuru = db.scalar(select(YedamBasvuru).where(*kosullar))
    if basvuru is None:
        return None
    # Onay bekleyen başvuru hiçbir danışmana görünmüyor (rıza metni
    # "18 yaşından küçüksen" bölümü).
    if basvuru.durum == BasvuruDurumu.EBEVEYN_ONAYI_BEKLIYOR.value:
        return None
    return basvuru


def detay(
    db: Session,
    basvuru: YedamBasvuru,
    simdi: dt.datetime | None = None,
) -> BasvuruDetay:
    """
    Danışmanın gördüğü ekran (§6.2).

    Veri bölümleri **rızanın kapsamına** bağlı: kullanıcı "anket" maddesini
    onaylamadıysa o bölüm boş geliyor. Rıza geçersizse hiçbir veri
    bölümü dolmuyor — kimlik ve temas geçmişi kalıyor, çünkü onlar
    danışmanlık ilişkisinin kaydı (rıza metni §3).
    """
    simdi = simdi or dt.datetime.now(dt.timezone.utc)
    kullanici = db.get(Kullanici, basvuru.kullanici_id)
    riza = db.get(YedamRiza, basvuru.riza_id)
    atanan = (
        db.get(Personel, basvuru.atanan_personel_id)
        if basvuru.atanan_personel_id is not None
        else None
    )

    gecerli = gecerli_riza(db, basvuru.kullanici_id, simdi)
    riza_gecerli = gecerli is not None and riza is not None and gecerli.id == riza.id
    kapsam = tuple(riza.kapsam.split(",")) if riza is not None else ()

    temaslar = [
        TemasSatiri(tur=t.tur, zaman=t.zaman, personel_ad=ad or "")
        for t, ad in db.execute(
            select(YedamTemas, Personel.ad)
            .outerjoin(Personel, Personel.id == YedamTemas.personel_id)
            .where(YedamTemas.basvuru_id == basvuru.id)
            .order_by(YedamTemas.zaman.desc())
        ).all()
    ]

    temel = BasvuruDetay(
        id=basvuru.id,
        durum=basvuru.durum,
        olusturma=basvuru.olusturma,
        kullanici_ad=kullanici.ad if kullanici is not None else "",
        iletisim_turu=basvuru.iletisim_turu,
        iletisim_degeri=basvuru.iletisim_degeri,
        kullanici_notu=basvuru.kullanici_notu,
        atanan_personel_id=basvuru.atanan_personel_id,
        atanan_ad=atanan.ad if atanan is not None else "",
        riza_gecerli=riza_gecerli,
        riza_bitis=riza.son_gecerlilik if riza is not None else None,
        riza_kapsam=kapsam,
        beyan_yas=riza.beyan_yas if riza is not None else None,
        ebeveyn_onayi_var=riza is not None and riza.ebeveyn_onay_tarihi is not None,
        temaslar=temaslar,
        notlar=_notlar(db, basvuru.id),
    )

    if not riza_gecerli:
        return temel

    kid = basvuru.kullanici_id
    bas = (simdi - dt.timedelta(days=RISK_PENCERESI_GUN)).date()

    bant = (
        _bant_cizgisi(db, kid, bas) if "risk" in kapsam else []
    )
    surukleyici = (
        _surukleyiciler(db, kid, bas) if "surukleyici" in kapsam else []
    )
    anket = _anketler(db, kid) if "anket" in kapsam else []
    paylasilmiyor, ortalama = _davranis(db, kid, bas) if "davranis" in kapsam else (True, 0)

    return BasvuruDetay(
        **{
            **temel.__dict__,
            "bant_cizgisi": bant,
            "surukleyiciler": surukleyici,
            "anketler": anket,
            "davranis_paylasilmiyor": paylasilmiyor,
            "ortalama_ekran_dk": ortalama,
        }
    )


def _bant_cizgisi(db: Session, kullanici_id: int, bas: dt.date) -> list[BantNoktasi]:
    return [
        BantNoktasi(
            tarih=r.tarih,
            bant=r.gosterilen_bant,
            trend=r.trend,
            veri_durumu=r.veri_durumu,
        )
        for r in db.scalars(
            select(RiskSonuc)
            .where(RiskSonuc.kullanici_id == kullanici_id, RiskSonuc.tarih >= bas)
            .order_by(RiskSonuc.tarih.asc())
        ).all()
    ]


def _surukleyiciler(db: Session, kullanici_id: int, bas: dt.date) -> list[SurukleyiciSatiri]:
    """
    Penceredeki faktörler, **ortalama sıralarına** göre.

    Ortalama sıra bir katkı değeri değil: "bu faktör pencerede genellikle
    kaçıncıydı" sorusunu cevaplıyor. Danışmana gösterilen de bu — sıra,
    yüzde değil.
    """
    satirlar = db.execute(
        select(RiskSurukleyici.faktor, func.avg(RiskSurukleyici.sira))
        .where(RiskSurukleyici.kullanici_id == kullanici_id, RiskSurukleyici.tarih >= bas)
        .group_by(RiskSurukleyici.faktor)
        .order_by(func.avg(RiskSurukleyici.sira).asc())
        .limit(SURUKLEYICI_SAYISI)
    ).all()
    return [
        SurukleyiciSatiri(faktor=f, sira=sira)
        for sira, (f, _) in enumerate(satirlar, start=1)
    ]


def _anketler(db: Session, kullanici_id: int) -> list[AnketSatiri]:
    return [
        AnketSatiri(
            tarih=a.tarih,
            madde_puanlari=[int(p) for p in a.madde_puanlari.split(",") if p],
            ham_puan=a.ham_puan,
            r_s=a.r_s,
        )
        for a in db.scalars(
            select(AnketYanit)
            .where(AnketYanit.kullanici_id == kullanici_id)
            .order_by(AnketYanit.tarih.desc())
        ).all()
    ]


def _davranis(db: Session, kullanici_id: int, bas: dt.date) -> tuple[bool, int]:
    """
    Davranış özeti — **yalnızca paylaşım rızası varsa** dolu (K19).

    Satır yoksa "kullanıcı paylaşmıyor" diyoruz, sıfır değil: ölçülmeyen
    değeri "hiç kullanmamış" diye göstermek §9'un tuzağı.
    """
    toplam = (
        GunlukOzet.social_dk
        + GunlukOzet.entertainment_dk
        + GunlukOzet.games_dk
        + GunlukOzet.communication_dk
        + GunlukOzet.productivity_dk
        + GunlukOzet.education_dk
        + GunlukOzet.health_dk
        + GunlukOzet.other_dk
    )
    ortalama = db.scalar(
        select(func.avg(toplam)).where(
            GunlukOzet.kullanici_id == kullanici_id,
            GunlukOzet.tarih >= bas,
            toplam > 0,
        )
    )
    if ortalama is None:
        return True, 0
    return False, round(ortalama)


def _notlar(db: Session, basvuru_id: int) -> list[NotSatiri]:
    return [
        NotSatiri(metin=n.metin, zaman=n.zaman, personel_ad=ad or "")
        for n, ad in db.execute(
            select(DanismanNotu, Personel.ad)
            .outerjoin(Personel, Personel.id == DanismanNotu.personel_id)
            .where(DanismanNotu.basvuru_id == basvuru_id)
            .order_by(DanismanNotu.zaman.desc())
        ).all()
    ]


def erisimi_kaydet(
    db: Session, basvuru: YedamBasvuru, personel: Personel, kapsam: tuple[str, ...]
) -> ErisimKaydi:
    """
    "Danışman X, 14 Ağustos 10:32'de baktı." — commit etmez.

    Rıza metninin verdiği sözün karşılığı ve detay ucunun **her**
    çağrısında yazılıyor. Yazmayı atlamak, sözü tutmamak demek.
    """
    kayit = ErisimKaydi(
        kullanici_id=basvuru.kullanici_id,
        personel_id=personel.id,
        basvuru_id=basvuru.id,
        kapsam=",".join(kapsam),
    )
    db.add(kayit)
    return kayit
