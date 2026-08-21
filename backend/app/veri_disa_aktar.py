"""
KVKK erişim hakkı — kullanıcının sunucudaki **tüm** satırları.

`docs/panel-plani.md §5.2`. İki yoldan servis ediliyor ve gövde ikisinde
de aynı:

- **varsayılan** `GET /auth/verilerim` — kullanıcı kendi cihazından
  indiriyor, dosya panele hiç düşmüyor
- **istisna** `POST /yonetim/kullanici/{id}/veri-disa-aktar` — kullanıcı
  uygulamaya erişemiyorsa (telefonu yok, hesabı askıda); gerekçeli ve
  denetim kayıtlı

Tek üretici olmasının sebebi silme kaskadıyla aynı: iki kopya, yeni bir
tablo eklendiğinde birinin güncellenip diğerinin unutulmasıyla **eksik**
bir erişim yanıtı üretir. Eksik yanıt, silme kaskadındaki eksik satır
kadar açık bir yükümlülük ihlali.

Cihazdaki yerel veri bunun **dışında**: `DisaAktarDeposu` onu zaten
Room'dan üretiyor ve ham olaylar sunucuya hiç çıkmıyor (CLAUDE.md §4).
Bu uç yalnızca sunucudaki kopyayı veriyor.
"""

from __future__ import annotations

import datetime as dt
from typing import Any

from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.modeller import (
    Arkadaslik,
    EbeveynBaglanti,
    GunlukOzet,
    Heartbeat,
    Kullanici,
    LigUyelik,
    OdulSecimi,
    RiskSonuc,
)

#: Gövde sürümü. Alan eklenirse artırılır; indirdiği dosyanın hangi
#: şemayla üretildiğini bilmek, veriyi başka bir yere taşımak isteyen
#: kullanıcının işine yarıyor (taşınabilirlik hakkı).
SURUM = 1


def _an(deger: dt.datetime | dt.date | None) -> str | None:
    return deger.isoformat() if deger is not None else None


def kullanici_verisi(db: Session, kullanici: Kullanici) -> dict[str, Any]:
    """Kullanıcının sunucudaki tüm satırlarını JSON'a çevrilebilir hâlde döndürür."""
    kid = kullanici.id

    ozetler = db.scalars(
        select(GunlukOzet).where(GunlukOzet.kullanici_id == kid).order_by(GunlukOzet.tarih)
    ).all()
    riskler = db.scalars(
        select(RiskSonuc).where(RiskSonuc.kullanici_id == kid).order_by(RiskSonuc.tarih)
    ).all()
    uyelikler = db.scalars(
        select(LigUyelik)
        .where(LigUyelik.kullanici_id == kid)
        .order_by(LigUyelik.hafta_baslangic)
    ).all()
    oduller = db.scalars(
        select(OdulSecimi).where(OdulSecimi.kullanici_id == kid).order_by(OdulSecimi.tarih)
    ).all()
    sinyal = db.scalar(select(Heartbeat).where(Heartbeat.kullanici_id == kid))
    bag = db.scalar(
        select(EbeveynBaglanti).where(
            or_(EbeveynBaglanti.ebeveyn_id == kid, EbeveynBaglanti.cocuk_id == kid)
        )
    )
    arkadaslar = db.scalars(
        select(Arkadaslik).where(
            or_(Arkadaslik.kullanici_id == kid, Arkadaslik.arkadas_id == kid)
        )
    ).all()

    return {
        "surum": SURUM,
        "uretim_zamani": _an(dt.datetime.now(dt.timezone.utc)),
        "aciklama": (
            "Yeşilİz sunucusunda bu hesaba ait tüm kayıtlar. Uygulama "
            "kullanım olaylarını (hangi uygulama, ne zaman) sunucuya hiç "
            "göndermiyor; onlar yalnızca telefonunda duruyor ve "
            "Profil › Gizlilik'ten ayrıca dışa aktarılabiliyor."
        ),
        "hesap": {
            "id": kullanici.id,
            "eposta": kullanici.eposta,
            "ad": kullanici.ad,
            "avatar": kullanici.avatar or "",
            "durum": kullanici.durum,
            "kayit_tarihi": _an(kullanici.olusturma),
        },
        "gunluk_ozetler": [
            {
                "tarih": _an(o.tarih),
                "kategori_dk": {
                    "social": o.social_dk,
                    "entertainment": o.entertainment_dk,
                    "games": o.games_dk,
                    "communication": o.communication_dk,
                    "productivity": o.productivity_dk,
                    "education": o.education_dk,
                    "health": o.health_dk,
                    "other": o.other_dk,
                },
                "oruntuler": {
                    "gece": o.gece,
                    "oturum": o.oturum,
                    "acilis": o.acilis,
                    "sabah": o.sabah,
                    "uyku_oncesi": o.uyku_oncesi,
                    "yer_degis": o.yer_degis,
                },
                "adim": o.adim,
            }
            for o in ozetler
        ],
        "risk_sonuclari": [
            {
                "tarih": _an(r.tarih),
                "s": r.s,
                "r": r.r,
                "gosterilen_bant": r.gosterilen_bant,
                "trend": r.trend,
                "veri_durumu": r.veri_durumu,
            }
            for r in riskler
        ],
        "lig_uyelikleri": [
            {
                "hafta_baslangic": _an(u.hafta_baslangic),
                "haftalik_puan": u.haftalik_puan,
                "gorunur": u.gorunur,
            }
            for u in uyelikler
        ],
        "odul_secimleri": [
            {"kademe_id": o.kademe_id, "secenek_id": o.secenek_id, "tarih": _an(o.tarih)}
            for o in oduller
        ],
        "son_sinyal": _an(sinyal.son_sinyal) if sinyal is not None else None,
        # Karşı tarafın kimliği YAZILMIYOR: bu dosya bir kişinin erişim
        # hakkı, ikinci bir kullanıcının verisini açmanın gerekçesi değil.
        "ebeveyn_bagi": (
            None
            if bag is None
            else {
                "rol": "ebeveyn" if bag.ebeveyn_id == kid else "cocuk",
                "onay_tarihi": _an(bag.onay_tarihi),
            }
        ),
        "arkadas_sayisi": len(arkadaslar),
    }
