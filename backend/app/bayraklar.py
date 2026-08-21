"""
Özellik bayrakları — sunucu davranışını panelden değiştirilebilir kılan
anahtarlar (`docs/panel-plani.md §5.7`).

**Risk motoru sabitleri buraya girmiyor** (CLAUDE.md §12 K17): skorlama
cihazda ve referans Python ile birebir doğrulanmış; sunucudan sabit
dağıtmak o doğrulamayı bozardı.

**Varsayılan koda gömülü.** Tabloda satır yoksa buradaki değer geçerli:
boş bir veritabanıyla açılan sunucu çalışmak zorunda ve "bayrak yok" bir
hata durumu değil. Panel yalnızca varsayılanı **eziyor**.

Okuma bir istek boyunca önbellekleniyor değil — bayrak sayısı bir avuç ve
sorgu birincil anahtar üzerinden. Süreç ömrü boyunca önbelleklemek ise
panelden yapılan değişikliğin ne zaman etkili olacağını belirsiz kılardı;
bir yetki/rıza kapısında bu kabul edilemez.
"""

from __future__ import annotations

from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.modeller import OzellikBayragi


@dataclass(frozen=True)
class Bayrak:
    anahtar: str
    varsayilan: str
    aciklama: str
    secenekler: tuple[str, ...] = ()

    def gecerli_mi(self, deger: str) -> bool:
        return not self.secenekler or deger in self.secenekler


#: 18 yaş altı kullanıcıda YEDAM rızasının ebeveyn onayına bağlanma kuralı.
#: S2 kararı (18 Ağu): varsayılan "eşleşmiş ebeveyn varsa onayı da gerekli".
YEDAM_EBEVEYN_ONAYI = Bayrak(
    anahtar="yedam.ebeveyn_onayi",
    varsayilan="eslesmisse",
    aciklama=(
        "18 yaş altında ebeveyn onayı ne zaman gerekli? "
        "eslesmisse: bağlı ebeveyn hesabı varsa · her_zaman: her durumda "
        "(eşleşme yoksa başvuru açılamaz) · hicbir_zaman: hiç sorulmaz."
    ),
    secenekler=("eslesmisse", "her_zaman", "hicbir_zaman"),
)

#: YEDAM köprüsü tamamen kapatılabilsin: Yeşilay tarafında danışman
#: hazır değilken başvuru toplamak, karşılanmayacak bir vaat üretir (§4).
YEDAM_ACIK = Bayrak(
    anahtar="yedam.acik",
    varsayilan="evet",
    aciklama="YEDAM köprüsü açık mı? Kapalıyken cihaz başvuru açamıyor.",
    secenekler=("evet", "hayir"),
)

#: Danışman notu (`danisman_notu`). S4 kararı (21 Ağu): saklama rejimi
#: hukuktan gelene kadar **kapalı** — klinik kayıt üretmiyoruz.
DANISMAN_NOTU = Bayrak(
    anahtar="yedam.danisman_notu",
    varsayilan="hayir",
    aciklama=(
        "Danışman serbest metin not yazabilsin mi? Klinik kayıt sayılabilir; "
        "saklama süresi ve erişim rejimi yazılana kadar kapalı (S4)."
    ),
    secenekler=("evet", "hayir"),
)

#: Maddi ödül teslimatı (K28). Stoğa bağlanması F4'ün işi; bayrak şimdiden
#: burada ki F4 yeni bir mekanizma icat etmesin.
MADDI_ODUL = Bayrak(
    anahtar="odul.maddi_teslimat",
    varsayilan="hayir",
    aciklama="Maddi ödül kademeleri seçilebilir mi? Tedarik hazır değilse hayır (A4).",
    secenekler=("evet", "hayir"),
)

TUMU: tuple[Bayrak, ...] = (
    YEDAM_ACIK,
    YEDAM_EBEVEYN_ONAYI,
    DANISMAN_NOTU,
    MADDI_ODUL,
)

_ANAHTARA_GORE = {b.anahtar: b for b in TUMU}


def oku(db: Session, bayrak: Bayrak) -> str:
    """
    Bayrağın etkin değeri; tabloda yoksa ya da geçersizse **varsayılan**.

    Geçersiz değeri varsayılana düşürmek bilinçli: elle düzenlenmiş ya da
    eski bir sürümden kalmış bir satır, kapıyı tanımsız bir duruma
    sokmamalı. Kapı her zaman bilinen bir davranışa oturuyor.
    """
    satir = db.get(OzellikBayragi, bayrak.anahtar)
    if satir is None or not bayrak.gecerli_mi(satir.deger):
        return bayrak.varsayilan
    return satir.deger


def acik_mi(db: Session, bayrak: Bayrak) -> bool:
    """`evet`/`hayir` tipli bayraklar için kısayol."""
    return oku(db, bayrak) == "evet"


def yaz(db: Session, anahtar: str, deger: str, personel_id: int) -> OzellikBayragi:
    """
    Bayrağı ezer; commit etmez.

    Bilinmeyen anahtar ve geçersiz değer reddediliyor: serbest bir
    anahtar/değer alanı, yazım hatasıyla sessizce varsayılana düşen ve
    kimsenin fark etmediği bir bayrak üretir.
    """
    bayrak = _ANAHTARA_GORE.get(anahtar)
    if bayrak is None:
        raise ValueError(f"Bilinmeyen bayrak: {anahtar}")
    if not bayrak.gecerli_mi(deger):
        raise ValueError(f"{anahtar} için geçersiz değer: {deger}")

    satir = db.get(OzellikBayragi, anahtar)
    if satir is None:
        satir = OzellikBayragi(anahtar=anahtar, deger=deger, aciklama=bayrak.aciklama)
        db.add(satir)
    else:
        satir.deger = deger
    satir.guncelleyen_id = personel_id
    return satir


def hepsini_oku(db: Session) -> list[tuple[Bayrak, str]]:
    """Panelin bayrak ekranı için: tanım + etkin değer."""
    return [(b, oku(db, b)) for b in TUMU]
