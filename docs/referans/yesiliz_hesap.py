# -*- coding: utf-8 -*-
"""
YEŞİLİZ — TEK DOSYA: KATEGORİ -> RİSK SKORU -> BANT

Bu dosya risk.py + bantlama.py'nin birleşimi. Uçtan uca akış:

    ham kullanım (paket, dk)
        -> kategori süreleri
        -> risk skoru R (0-100)
        -> bant (AZ / ORTA / ÇOK RİSKLİ)   [eşik + güvenlik + histerezis + trend]

Tek gün için puan + bant istiyorsan: hesapla_tek_gun()
Zaman içinde takip (histerezis çalışsın) istiyorsan: BantTakipci + gunu_isle()

Risk motoru SAF FONKSİYON. Kotlin portu birebir aynı sonucu vermeli.
"""

import math


# =====================================================================
# BÖLÜM A — KATEGORİLENDİRME
# =====================================================================

# Ağırlık = "bu kategoride geçen 1 dakika ne kadar riskli?"
KATEGORI_AGIRLIK = {
    "social":        1.0,
    "entertainment": 1.0,
    "games":         0.8,
    "communication": 0.5,
    "productivity":  0.3,
    "education":     0.2,
    "health":        0.1,
    "other":         0.5,   # tabloda bulunamayan uygulamalar
}

# Tüketim kategorileri — çarpan ve sönümleme SADECE bunlara uygulanır
TUKETIM = {"social", "entertainment", "games"}

# Paket adı -> kategori (VERİ TABLOSU, algoritma değil)
PAKET_KATEGORI = {
    # social
    "com.instagram.android":        "social",
    "com.twitter.android":          "social",
    "com.snapchat.android":         "social",
    "com.facebook.katana":          "social",
    "com.reddit.frontpage":         "social",
    # entertainment
    "com.zhiliaoapp.musically":     "entertainment",   # TikTok
    "com.google.android.youtube":   "entertainment",
    "com.netflix.mediaclient":      "entertainment",
    "com.spotify.music":            "entertainment",
    "tv.twitch.android.app":        "entertainment",
    # games
    "com.supercell.clashofclans":   "games",
    "com.dts.freefireth":           "games",
    "com.mobile.legends":           "games",
    "com.roblox.client":            "games",
    # communication
    "com.whatsapp":                 "communication",
    "org.telegram.messenger":       "communication",
    "com.discord":                  "communication",
    "com.google.android.gm":        "communication",
    # productivity
    "com.lemon.lvoverseas":         "productivity",    # CapCut
    "com.adobe.lrmobile":           "productivity",    # Lightroom
    "notion.id":                    "productivity",
    "com.microsoft.office.word":    "productivity",
    "com.google.android.apps.docs": "productivity",
    # education
    "com.duolingo":                 "education",
    "com.udemy.android":            "education",
    "com.khanacademy.android":      "education",
    # health
    "com.strava":                   "health",
    "com.fitbit.FitbitMobile":      "health",
    "com.nike.plusgps":             "health",
}


def kategori_bul(paket_adi, kullanici_secimi=None):
    """Paket adını kategoriye çevirir: kullanıcı seçimi > tablo > 'other'."""
    if kullanici_secimi:
        return kullanici_secimi
    return PAKET_KATEGORI.get(paket_adi, "other")


def kategorilere_dagit(kullanim_listesi, kullanici_secimleri=None):
    """[("com.instagram.android", 120), ...] -> {"social": 120.0, ...}"""
    kullanici_secimleri = kullanici_secimleri or {}
    sonuc = {}
    for paket, dakika in kullanim_listesi:
        kat = kategori_bul(paket, kullanici_secimleri.get(paket))
        sonuc[kat] = sonuc.get(kat, 0.0) + dakika
    return sonuc


# =====================================================================
# BÖLÜM B — RİSK SKORU (0-100)
# =====================================================================

ORUNTU_KATSAYI = {
    "gece":        0.6,   # 00:00-06:00 kullanım oranı
    "oturum":      0.4,   # 30dk+ kesintisiz tüketim blokları
    "acilis":      0.3,   # saat başına telefon açma
    "sabah":       0.2,   # uyanınca hemen telefona sarılma
    "uyku_oncesi": 0.3,   # yatmadan önceki 30 dk
    "yer_degis":   0.4,   # ekran artarken aktivite düşüyor mu
}

SASSV_KESME    = 29.5    # anket kesme puanı -> 50
S_REF          = 300.0   # davranış skoru referansı -> 50
                         # DİKKAT: TBM'in 120 dk'sı HEDEFTİR, eşik değil.
                         # 120 alınırsa ortalama kullanıcı bile "çok riskli"
                         # çıkar ve ölçüm ayırt ediciliğini kaybeder.
MAKS_SONUMLEME = 0.15    # spor riski en fazla %15 azaltır


def risk_skoru(kategori_dakika, oruntu, anket_puani=None,
               kacinci_gun=0, anket_kac_gun_once=0, adim=0.0, spor_dk=0.0):
    """
    Kategori süreleri + örüntü + anket -> birleşik risk skoru R (0-100).
    Bant BURADA belirlenmez; ham skoru döndürür. Bant için BÖLÜM C.
    """
    # 1) yükü ikiye ayır
    B_tuketim = B_islevsel = 0.0
    for kat, dk in kategori_dakika.items():
        agirlikli = dk * KATEGORI_AGIRLIK.get(kat, 0.5)
        if kat in TUKETIM:
            B_tuketim += agirlikli
        else:
            B_islevsel += agirlikli

    # 2) örüntü çarpanı
    P = 1.0
    for ad, katsayi in ORUNTU_KATSAYI.items():
        P += katsayi * oruntu.get(ad, 0.0)

    # 3) aktivite sönümlemesi (tavan %15)
    A = 0.4 * min(1.0, adim / 8000.0) + 0.6 * min(1.0, spor_dk / 30.0)
    D = 1.0 - min(MAKS_SONUMLEME, MAKS_SONUMLEME * A)

    # 4) ham skor — çarpan ve sönümleme SADECE tüketime
    S = B_islevsel + B_tuketim * P * D

    # 5) davranış skoru 0-100 (S = S_REF -> 50)
    R_b =  100.0* (1.0 - math.exp(-0.693 * S / S_REF))

    # 6) anket skoru 0-100 (kesme puanı -> 50)
    R_s = None
    if anket_puani is not None:
        if anket_puani <= SASSV_KESME:
            R_s = 50.0 * (anket_puani - 10.0) / (SASSV_KESME - 10.0)
        else:
            R_s = 50.0 + 50.0 * (anket_puani - SASSV_KESME) / (60.0 - SASSV_KESME)

    # 7) alpha = ankete verilen ağırlık (davranış birikince azalır)
    if R_s is None:
        alpha = 0.0
    elif kacinci_gun <= 0:
        alpha = 1.00
    elif kacinci_gun < 7:
        alpha = 1.00 - 0.5 * (kacinci_gun / 7.0)
    elif kacinci_gun < 28:
        alpha = 0.50 - 0.2 * ((kacinci_gun - 7) / 21.0)
    else:
        alpha = 0.30
    if R_s is not None:
        alpha *= max(0.0, 1.0 - anket_kac_gun_once / 60.0)   # 60 günde sıfır

    R = alpha * R_s + (1.0 - alpha) * R_b if R_s is not None else R_b

    # açıklanabilirlik: en çok katkı yapan 3 kategori
    katkilar = {k: dk * KATEGORI_AGIRLIK.get(k, 0.5)
                for k, dk in kategori_dakika.items()}
    surukleyiciler = [k for k, _ in
                      sorted(katkilar.items(), key=lambda x: x[1], reverse=True)[:3]]

    return {
        "R": round(R, 1), "R_b": round(R_b, 1),
        "R_s": round(R_s, 1) if R_s is not None else None,
        "alpha": round(alpha, 2), "S": round(S, 1), "P": round(P, 2),
        "D": round(D, 3), "B_tuketim": round(B_tuketim, 1),
        "B_islevsel": round(B_islevsel, 1),
        "tuketim_dk": sum(dk for k, dk in kategori_dakika.items() if k in TUKETIM),
        "gece_dk": round(oruntu.get("gece", 0.0) * sum(kategori_dakika.values()), 1),
        "toplam_dk": sum(kategori_dakika.values()),
        "surukleyiciler": surukleyiciler,
    }


# =====================================================================
# BÖLÜM C — BANTLAMA (eşik + güvenlik + histerezis + trend)
# =====================================================================

BANT_AZ_UST   = 40.0    # altı: AZ
BANT_ORTA_UST = 65.0    # altı: ORTA, üstü: ÇOK

AZ, ORTA, COK = "AZ RİSKLİ", "ORTA RİSKLİ", "ÇOK RİSKLİ"
SIRA = {AZ: 0, ORTA: 1, COK: 2}


def esikten_bant(skor):
    if skor < BANT_AZ_UST:
        return AZ
    if skor < BANT_ORTA_UST:
        return ORTA
    return COK


# --- Güvenlik: skor ne derse desin devreye giren mutlak tabanlar ---
GUVENLIK_GUN = 5

def guvenlik_tabani(gunluk_veri):
    """gunluk_veri: [{"tuketim_dk":360,"gece_dk":90}, ...] -> (taban, kural)."""
    if len(gunluk_veri) < GUVENLIK_GUN:
        return None, None
    son = gunluk_veri[-GUVENLIK_GUN:]
    if all(g["tuketim_dk"] > 480 for g in son):
        return COK, f"{GUVENLIK_GUN} gün üst üste 8 saat+ tüketim"
    if all(g["tuketim_dk"] > 360 for g in son):
        return ORTA, f"{GUVENLIK_GUN} gün üst üste 6 saat+ tüketim"
    if all(g["gece_dk"] > 90 for g in son):
        return ORTA, f"{GUVENLIK_GUN} gün üst üste 90 dk+ gece kullanımı"
    return None, None


# --- Histerezis: bant değişimi için ardışık gün şartı (asimetrik) ---
GUN_YUKARI = 3
GUN_ASAGI  = 7

class BantTakipci:
    def __init__(self):
        self.mevcut_bant = None
        self.bekleyen = None
        self.bekleyen_gun = 0

    def guncelle(self, aday_bant):
        if self.mevcut_bant is None:
            self.mevcut_bant = aday_bant
            return self.mevcut_bant
        if aday_bant == self.mevcut_bant:
            self.bekleyen = None
            self.bekleyen_gun = 0
            return self.mevcut_bant

        aday_yon = SIRA[aday_bant] > SIRA[self.mevcut_bant]   # True = yukarı
        if self.bekleyen is None:
            self.bekleyen = aday_bant
            self.bekleyen_gun = 1
        else:
            bekleyen_yon = SIRA[self.bekleyen] > SIRA[self.mevcut_bant]
            if aday_yon == bekleyen_yon:
                # Aynı yön: sayaç devam (ORTA->AZ iyileşmesi cezalanmaz)
                self.bekleyen = aday_bant
                self.bekleyen_gun += 1
            else:
                self.bekleyen = aday_bant
                self.bekleyen_gun = 1

        gereken = GUN_YUKARI if aday_yon else GUN_ASAGI
        if self.bekleyen_gun >= gereken:
            self.mevcut_bant = aday_bant
            self.bekleyen = None
            self.bekleyen_gun = 0
        return self.mevcut_bant

    def durum(self):
        if not self.bekleyen:
            return None
        yukari = SIRA[self.bekleyen] > SIRA[self.mevcut_bant]
        gereken = GUN_YUKARI if yukari else GUN_ASAGI
        return f"{self.bekleyen} adayı: {self.bekleyen_gun}/{gereken} gün"


# --- Trend: bandı değiştirmez, MESAJI değiştirir ---
def trend_bul(skor_gecmisi):
    if len(skor_gecmisi) < 14:
        return "belirsiz"
    fark = sum(skor_gecmisi[-7:]) / 7 - sum(skor_gecmisi[-14:-7]) / 7
    if fark > 5:
        return "kötüleşiyor"
    if fark < -5:
        return "iyileşiyor"
    return "sabit"


# =====================================================================
# BÖLÜM D — UÇTAN UCA
# =====================================================================

def hesapla_tek_gun(ham_kullanim, oruntu, **kw):
    """
    Tek gün için hızlı yol: ham kullanım -> skor + eşik bandı.
    Histerezis/güvenlik YOK (onlar çok günlük). Demo/anlık gösterim için.
    """
    kategori_dakika = kategorilere_dagit(ham_kullanim, kw.pop("kullanici_secimleri", None))
    s = risk_skoru(kategori_dakika, oruntu, **kw)
    s["kategori_dakika"] = kategori_dakika
    s["bant"] = esikten_bant(s["R"])
    return s


def gunu_isle(skor, gunluk_veri, skor_gecmisi, takipci):
    """
    Zaman içinde takip: skoru banda çevirir.
    Sıra: 1) eşik  2) güvenlik tabanı  3) histerezis  4) trend
    """
    aday = esikten_bant(skor)
    taban, kural = guvenlik_tabani(gunluk_veri)
    if taban and SIRA[taban] > SIRA[aday]:
        aday = taban
    else:
        kural = None
    gosterilen = takipci.guncelle(aday)
    return {
        "skor": round(skor, 1),
        "aday_bant": aday,
        "gosterilen_bant": gosterilen,
        "guvenlik_kurali": kural,
        "bekleyen": takipci.durum(),
        "trend": trend_bul(skor_gecmisi),
    }


# =====================================================================
# DEMO
# =====================================================================

if __name__ == "__main__":

    ham_kullanim = [
        ("com.instagram.android",      150),
        ("com.zhiliaoapp.musically",    90),   # TikTok
        ("com.supercell.clashofclans",  60),
        ("com.whatsapp",                40),
        ("com.lemon.lvoverseas",        20),   # CapCut
        ("com.bilinmeyen.uygulama",     15),   # tabloda yok -> other
    ]
    oruntu = {
        "gece": 0.25, "oturum": 0.55, "acilis": 0.60,
        "sabah": 0.80, "uyku_oncesi": 0.90, "yer_degis": 0.55,
    }

    print("=" * 60)
    print("1) TEK GÜN — PUAN + BANT")
    print("=" * 60)
    s = hesapla_tek_gun(ham_kullanim, oruntu, anket_puani=41.0,
                        kacinci_gun=28, adim=3200, spor_dk=0)
    print(f"  Kategori dakika : {s['kategori_dakika']}")
    print(f"  Ham ekran       : {s['toplam_dk']:.0f} dk "
          f"(tüketim {s['tuketim_dk']:.0f}, gece {s['gece_dk']:.0f})")
    print(f"  Ham skor S      : {s['S']}")
    print(f"  Davranış R_b    : {s['R_b']} / 100")
    print(f"  Anket    R_s    : {s['R_s']} / 100   (alpha {s['alpha']})")
    print(f"  ------------------------------------")
    print(f"  >> PUAN R       : {s['R']} / 100")
    print(f"  >> BANT         : {s['bant']}")
    print(f"  Sürükleyiciler  : {s['surukleyiciler']}")

    print()
    print("=" * 74)
    print("2) 20 GÜNLÜK TAKİP — histerezis + güvenlik + trend")
    print("=" * 74)
    skorlar = [
        45, 48, 52, 55,          # gün 1-4   orta, stabil
        68, 71, 63,              # gün 5-7   yüksek zıplama (3. gün geri düştü)
        70, 72, 74, 73,          # gün 8-11  yüksek, ısrarlı
        66, 60, 55, 50,          # gün 12-15 düşmeye başladı
        45, 42, 38, 36, 34,      # gün 16-20 iyileşme sürüyor
    ]
    takipci = BantTakipci()
    gunluk_veri, skor_gecmisi = [], []
    onceki_bant = None
    print(f"{'Gün':>3} {'Skor':>5}  {'Aday':<12} {'GÖSTERİLEN':<12} "
          f"{'Bekleyen':<22} {'Trend'}")
    print("-" * 74)
    for i, skor in enumerate(skorlar, 1):
        gunluk_veri.append({"tuketim_dk": skor * 4, "gece_dk": skor * 0.8})
        skor_gecmisi.append(skor)
        r = gunu_isle(skor, gunluk_veri, skor_gecmisi, takipci)
        isaret = "  <-- BANT DEĞİŞTİ" if (onceki_bant and
                 r["gosterilen_bant"] != onceki_bant) else ""
        onceki_bant = r["gosterilen_bant"]
        print(f"{i:>3} {r['skor']:>5}  {r['aday_bant']:<12} "
              f"{r['gosterilen_bant']:<12} {r['bekleyen'] or '':<22} "
              f"{r['trend']}{isaret}")