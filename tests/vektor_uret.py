# -*- coding: utf-8 -*-
"""
YEŞİLİZ — DOĞRULAMA VEKTÖRÜ ÜRETİCİ

docs/referans/yesiliz_hesap.py referans implementasyonunu çalıştırır ve
tests/test_vectors.json dosyasını üretir. Kotlin portu (core-risk) bu dosyayı
okuyup 1e-4 toleransla aynı sonucu vermek zorunda (CLAUDE.md §7).

Çalıştırma:
    python3 tests/vektor_uret.py

DİKKAT — yuvarlama:
Referans `risk_skoru()` çıktısını `round(..., 1)` ile yuvarlıyor. 1e-4 tolerans
yuvarlanmış değerle anlamsız olurdu, bu yüzden referans modülün global
ad alanına birim (identity) bir `round` enjekte ediyoruz. Python globalleri
builtins'ten önce çözdüğü için bu SADECE o modülün içindeki round çağrılarını
etkiler; referans dosyaya dokunmayız.
"""

import json
import os
import sys

BURASI = os.path.dirname(os.path.abspath(__file__))
KOK = os.path.dirname(BURASI)
sys.path.insert(0, os.path.join(KOK, "docs", "referans"))

import yesiliz_hesap as ref  # noqa: E402

# Yuvarlamayı devre dışı bırak — ham float saklanacak.
ref.round = lambda deger, basamak=None: deger


# =====================================================================
# §4.7 DOĞRULAMA PROFİLLERİ
# =====================================================================
# spec-risk.md §4.7'deki 5 profil. Girdiler burada KAYITLI — tablodaki
# sayılar bu girdilerden üretilir, tersi değil.
# Beşinde de anket YOK (anket_puani=None) → R = R_b, alpha = 0.

PROFILLER = [
    {
        "ad": "profil_hafif",
        "aciklama": "Hafif (2,5 sa, üretim ağırlıklı) — beklenen bant AZ",
        "kategori_dakika": {
            "social": 40.0, "entertainment": 20.0,
            "productivity": 50.0, "communication": 30.0, "education": 10.0,
        },
        "oruntu": {
            "gece": 0.10, "oturum": 0.30, "acilis": 0.40,
            "sabah": 0.30, "uyku_oncesi": 0.30, "yer_degis": 0.10,
        },
        "adim": 6000.0, "spor_dk": 20.0,
    },
    {
        "ad": "profil_uretim_iyi_oruntu",
        "aciklama": "Üretim ağırlıklı (5,5 sa), iyi örüntü — beklenen bant AZ",
        "kategori_dakika": {
            "social": 60.0, "entertainment": 30.0,
            "productivity": 150.0, "communication": 50.0, "education": 40.0,
        },
        "oruntu": {
            "gece": 0.05, "oturum": 0.15, "acilis": 0.25,
            "sabah": 0.10, "uyku_oncesi": 0.10, "yer_degis": 0.05,
        },
        "adim": 9000.0, "spor_dk": 45.0,
    },
    {
        "ad": "profil_ortalama_dengeli",
        "aciklama": "Ortalama (4 sa, dengeli) — beklenen bant ORTA",
        "kategori_dakika": {
            "social": 70.0, "entertainment": 50.0, "games": 30.0,
            "communication": 50.0, "productivity": 30.0, "education": 10.0,
        },
        "oruntu": {
            "gece": 0.15, "oturum": 0.30, "acilis": 0.40,
            "sabah": 0.30, "uyku_oncesi": 0.30, "yer_degis": 0.20,
        },
        "adim": 5000.0, "spor_dk": 15.0,
    },
    {
        "ad": "profil_uretim_kotu_oruntu",
        "aciklama": "Üretim ağırlıklı (5,5 sa), kötü örüntü — beklenen bant ORTA",
        "kategori_dakika": {
            "social": 80.0, "entertainment": 30.0,
            "productivity": 140.0, "communication": 60.0, "education": 20.0,
        },
        "oruntu": {
            "gece": 0.50, "oturum": 0.70, "acilis": 0.80,
            "sabah": 0.80, "uyku_oncesi": 0.70, "yer_degis": 0.60,
        },
        "adim": 3000.0, "spor_dk": 0.0,
    },
    {
        "ad": "profil_agir_tuketici",
        "aciklama": "Ağır tüketici (6,5 sa, gece ağırlıklı) — beklenen bant ÇOK",
        "kategori_dakika": {
            "social": 180.0, "entertainment": 150.0, "games": 40.0,
            "communication": 20.0,
        },
        "oruntu": {
            "gece": 0.70, "oturum": 0.60, "acilis": 0.50,
            "sabah": 0.50, "uyku_oncesi": 0.50, "yer_degis": 0.15,
        },
        "adim": 1500.0, "spor_dk": 0.0,
    },
]


# =====================================================================
# KENAR DURUMLAR — alpha eğrisi ve sıfır girdi
# =====================================================================
# Temel kullanım sabit tutulur, sadece anket/gün parametreleri değişir ki
# alpha eğrisi (§4.5) izole şekilde test edilsin.

TEMEL_KULLANIM = {
    "social": 90.0, "entertainment": 60.0,
    "communication": 40.0, "productivity": 50.0,
}
TEMEL_ORUNTU = {
    "gece": 0.20, "oturum": 0.35, "acilis": 0.45,
    "sabah": 0.30, "uyku_oncesi": 0.35, "yer_degis": 0.25,
}

KENAR_DURUMLAR = [
    {
        "ad": "anket_yok_alpha_sifir",
        "aciklama": "Anket girilmemiş → alpha = 0, R = R_b",
        "kategori_dakika": TEMEL_KULLANIM, "oruntu": TEMEL_ORUNTU,
        "anket_puani": None, "kacinci_gun": 14, "anket_kac_gun_once": 0,
        "adim": 4000.0, "spor_dk": 10.0,
    },
    {
        "ad": "gun_sifir_alpha_bir",
        "aciklama": "d=0, anket taze → alpha = 1.0, R = R_s",
        "kategori_dakika": TEMEL_KULLANIM, "oruntu": TEMEL_ORUNTU,
        "anket_puani": 35.0, "kacinci_gun": 0, "anket_kac_gun_once": 0,
        "adim": 4000.0, "spor_dk": 10.0,
    },
    {
        "ad": "gun_uc_alpha_dusuyor",
        "aciklama": "d=3 → alpha0 = 1.00 - 0.5*(3/7)",
        "kategori_dakika": TEMEL_KULLANIM, "oruntu": TEMEL_ORUNTU,
        "anket_puani": 35.0, "kacinci_gun": 3, "anket_kac_gun_once": 0,
        "adim": 4000.0, "spor_dk": 10.0,
    },
    {
        "ad": "gun_yedi_alpha_yarim",
        "aciklama": "d=7 sınırı → ikinci kola geçiş, alpha0 = 0.50",
        "kategori_dakika": TEMEL_KULLANIM, "oruntu": TEMEL_ORUNTU,
        "anket_puani": 35.0, "kacinci_gun": 7, "anket_kac_gun_once": 0,
        "adim": 4000.0, "spor_dk": 10.0,
    },
    {
        "ad": "gun_yirmisekiz_alpha_taban",
        "aciklama": "d=28 → alpha0 tabanı 0.30",
        "kategori_dakika": TEMEL_KULLANIM, "oruntu": TEMEL_ORUNTU,
        "anket_puani": 35.0, "kacinci_gun": 28, "anket_kac_gun_once": 0,
        "adim": 4000.0, "spor_dk": 10.0,
    },
    {
        "ad": "anket_altmis_gun_once_alpha_sifir",
        "aciklama": "g=60 → anket ağırlığı tamamen sönüyor, alpha = 0",
        "kategori_dakika": TEMEL_KULLANIM, "oruntu": TEMEL_ORUNTU,
        "anket_puani": 35.0, "kacinci_gun": 14, "anket_kac_gun_once": 60,
        "adim": 4000.0, "spor_dk": 10.0,
    },
    {
        "ad": "anket_kesme_noktasi_elli",
        "aciklama": "SAS-SV = 29.5 (kesme) → R_s tam 50.0",
        "kategori_dakika": TEMEL_KULLANIM, "oruntu": TEMEL_ORUNTU,
        "anket_puani": 29.5, "kacinci_gun": 0, "anket_kac_gun_once": 0,
        "adim": 4000.0, "spor_dk": 10.0,
    },
    {
        "ad": "anket_taban_on",
        "aciklama": "SAS-SV = 10 (mümkün en düşük) → R_s = 0.0",
        "kategori_dakika": TEMEL_KULLANIM, "oruntu": TEMEL_ORUNTU,
        "anket_puani": 10.0, "kacinci_gun": 0, "anket_kac_gun_once": 0,
        "adim": 4000.0, "spor_dk": 10.0,
    },
    {
        "ad": "anket_tavan_altmis",
        "aciklama": "SAS-SV = 60 (mümkün en yüksek) → R_s = 100.0",
        "kategori_dakika": TEMEL_KULLANIM, "oruntu": TEMEL_ORUNTU,
        "anket_puani": 60.0, "kacinci_gun": 0, "anket_kac_gun_once": 0,
        "adim": 4000.0, "spor_dk": 10.0,
    },
    {
        "ad": "sifir_kullanim",
        "aciklama": "Hiç kullanım yok → S = 0, R_b = 0",
        "kategori_dakika": {}, "oruntu": {},
        "anket_puani": None, "kacinci_gun": 14, "anket_kac_gun_once": 0,
        "adim": 0.0, "spor_dk": 0.0,
    },
    {
        "ad": "sonumleme_tavani",
        "aciklama": "adım ve spor tavanda → A = 1.0, D = 0.85 (tavan %15)",
        "kategori_dakika": {"social": 200.0}, "oruntu": {},
        "anket_puani": None, "kacinci_gun": 14, "anket_kac_gun_once": 0,
        "adim": 20000.0, "spor_dk": 120.0,
    },
    {
        "ad": "sonumleme_yok",
        "aciklama": "hiç hareket yok → A = 0, D = 1.0",
        "kategori_dakika": {"social": 200.0}, "oruntu": {},
        "anket_puani": None, "kacinci_gun": 14, "anket_kac_gun_once": 0,
        "adim": 0.0, "spor_dk": 0.0,
    },
    {
        "ad": "bilinmeyen_kategori_other",
        "aciklama": "tabloda olmayan kategori → 'other' ağırlığı 0.5, işlevsel tarafta",
        "kategori_dakika": {"other": 200.0}, "oruntu": {"gece": 1.0},
        "anket_puani": None, "kacinci_gun": 14, "anket_kac_gun_once": 0,
        "adim": 0.0, "spor_dk": 0.0,
    },
]


# =====================================================================
# KATEGORİLENDİRME
# =====================================================================

KATEGORI_VEKTORLERI = [
    {
        "ad": "tablodan_eslesme",
        "kullanim_listesi": [
            ["com.instagram.android", 120],
            ["com.zhiliaoapp.musically", 90],
            ["com.supercell.clashofclans", 45],
            ["com.whatsapp", 30],
            ["com.lemon.lvoverseas", 25],
            ["com.duolingo", 15],
            ["com.strava", 10],
        ],
        "kullanici_secimleri": None,
    },
    {
        "ad": "tabloda_yok_other",
        "kullanim_listesi": [["com.bilinmeyen.uygulama", 60]],
        "kullanici_secimleri": None,
    },
    {
        "ad": "kullanici_secimi_tabloyu_ezer",
        "kullanim_listesi": [["com.instagram.android", 100]],
        "kullanici_secimleri": {"com.instagram.android": "productivity"},
    },
    {
        "ad": "ayni_kategoride_toplama",
        "kullanim_listesi": [
            ["com.instagram.android", 40],
            ["com.twitter.android", 35],
            ["com.snapchat.android", 25],
        ],
        "kullanici_secimleri": None,
    },
]


# =====================================================================
# ETKİLEYEN FAKTÖRLER — risk detayı ekranı (docs/tasarim-referans.md §2/30)
# =====================================================================
# core-risk `EtkileyenFaktorler.kt`'nin KENDİ formülü — referans Python'da
# (yesiliz_hesap.py) karşılığı YOK, o yüzden formül burada BİREBİR
# kopyalanıyor (A = aktivite payı, risk_skoru() §3'teki ile aynı sabitler).
# GEÇİCİ eşikler — CLAUDE.md §12 A3.

FAKTOR_ESIK_DUSUK = 0.33
FAKTOR_ESIK_ORTA = 0.66
FAKTOR_ASGARI_DK = 15.0


def _faktor_seviyesi(oran):
    if oran < FAKTOR_ESIK_DUSUK:
        return "DUSUK"
    if oran < FAKTOR_ESIK_ORTA:
        return "ORTA"
    return "YUKSEK"


def _etkileyen_faktorler(kategori_dakika, oruntu, adim, spor_dk):
    toplam_dk = sum(kategori_dakika.values())
    gece = oruntu.get("gece", 0.0)
    oturum = oruntu.get("oturum", 0.0)
    sosyal_oran = (kategori_dakika.get("social", 0.0) / toplam_dk) if toplam_dk > 0 else 0.0
    A = 0.4 * min(1.0, adim / 8000.0) + 0.6 * min(1.0, spor_dk / 30.0)
    hareket_oran = 1.0 - A
    # Aktivite hiç ölçülmemişse hareket faktörü BİLİNMİYOR. `A = 0` ile
    # "hiç hareket etmedi" ayırt edilemiyor ve veri yokluğu maksimum risk
    # olarak gösteriliyordu (debug.md K-7).
    aktivite_olculdu = adim > 0.0 or spor_dk > 0.0
    # Örüntüler günün toplamına bölünüyor; toplam birkaç dakikayken bölme
    # uç değerler üretiyor (34 saniyenin tamamı geceye düşerse gece = 1,0).
    # K-7 ile aynı ilke: az veriden maksimum iddia edilmiyor.
    yeterli = toplam_dk >= FAKTOR_ASGARI_DK

    def _kullanim(ad, oran):
        return {
            "ad": ad,
            "seviye": _faktor_seviyesi(oran) if yeterli else "BILINMIYOR",
            "oran": oran if yeterli else 0.0,
        }

    faktorler = [
        _kullanim("Gece kullanımı", gece),
        _kullanim("Kesintisiz kullanım süresi", oturum),
        _kullanim("Sosyal medya oranı", sosyal_oran),
        {
            "ad": "Fiziksel hareket",
            "seviye": _faktor_seviyesi(hareket_oran) if aktivite_olculdu else "BILINMIYOR",
            "oran": hareket_oran if aktivite_olculdu else 0.0,
        },
    ]
    return faktorler


FAKTOR_VEKTORLERI = [
    {
        "ad": "hafif_kullanim_iyi_hareket",
        "aciklama": "Düşük gece/oturum/sosyal, tavan hareket → 4 faktör de DÜŞÜK",
        "kategori_dakika": {"social": 20.0, "productivity": 80.0},
        "oruntu": {"gece": 0.05, "oturum": 0.10},
        "adim": 9000.0, "spor_dk": 40.0,
    },
    {
        "ad": "agir_kullanim_hareket_olculmemis",
        "aciklama": (
            "Yüksek gece/oturum/sosyal ama adım/spor HİÇ yok → ilk üçü YÜKSEK, "
            "hareket BILINMIYOR. Veri yokluğu maksimum risk olarak sunulamaz "
            "(debug.md K-7)."
        ),
        "kategori_dakika": {"social": 180.0, "entertainment": 20.0},
        "oruntu": {"gece": 0.80, "oturum": 0.90},
        "adim": 0.0, "spor_dk": 0.0,
    },
    {
        "ad": "agir_kullanim_gercekten_hareketsiz",
        "aciklama": (
            "Aynı kullanım, ama az da olsa ÖLÇÜLMÜŞ hareket var → hareket "
            "faktörü artık YÜKSEK diyebilir."
        ),
        "kategori_dakika": {"social": 180.0, "entertainment": 20.0},
        "oruntu": {"gece": 0.80, "oturum": 0.90},
        "adim": 300.0, "spor_dk": 0.0,
    },
    {
        "ad": "orta_karisik",
        "aciklama": "Sınır civarı değerler → ORTA bandı ağırlıklı",
        "kategori_dakika": {"social": 60.0, "productivity": 60.0},
        "oruntu": {"gece": 0.40, "oturum": 0.50},
        "adim": 4000.0, "spor_dk": 15.0,
    },
    {
        "ad": "cok_az_kullanim",
        "aciklama": (
            "Toplam yarım dakika ve tamamı gece penceresinde. Ham oran 1,0 "
            "ama ölçüm yetersiz: seviye iddia edilmiyor. Uygulamayı yeni "
            "kuran kullanıcının ilk gecesi."
        ),
        "kategori_dakika": {"social": 0.6},
        "oruntu": {"gece": 1.0, "oturum": 0.0},
        "adim": 0.0, "spor_dk": 0.0,
    },
    {
        "ad": "sifir_ekran",
        "aciklama": (
            "Kullanım yok → sosyal oran 0/0 güvenli şekilde 0.0, hareket "
            "BILINMIYOR. Uygulamayı yeni kuran kullanıcının gördüğü durum."
        ),
        "kategori_dakika": {},
        "oruntu": {},
        "adim": 0.0, "spor_dk": 0.0,
    },
    {
        "ad": "esik_sinirlari_tam",
        "aciklama": "oran tam 0.33/0.66 sınırında — '< eşik' katı, sınır bir üst banda düşer",
        "kategori_dakika": {"social": 33.0, "productivity": 67.0},
        "oruntu": {"gece": 0.33, "oturum": 0.66},
        "adim": 8000.0, "spor_dk": 30.0,
    },
]


# =====================================================================
# BANTLAMA — eşik sınırları
# =====================================================================

ESIK_SKORLARI = [0.0, 39.9, 39.99999, 40.0, 40.1, 64.9, 64.99999, 65.0, 65.1, 100.0]


# =====================================================================
# GÜVENLİK TABANI (§5.3)
# =====================================================================

def _gun(tuketim, gece):
    return {"tuketim_dk": tuketim, "gece_dk": gece}


GUVENLIK_VEKTORLERI = [
    {
        "ad": "yetersiz_gun_sayisi",
        "aciklama": "4 gün < GUVENLIK_GUN(5) → taban yok",
        "gunluk_veri": [_gun(500, 120) for _ in range(4)],
    },
    {
        "ad": "cok_taban_480_ustu",
        "aciklama": "5 gün üst üste tüketim > 480 → ÇOK",
        "gunluk_veri": [_gun(500, 10) for _ in range(5)],
    },
    {
        "ad": "orta_taban_360_ustu",
        "aciklama": "5 gün üst üste tüketim > 360 (ama 480 altı) → ORTA",
        "gunluk_veri": [_gun(400, 10) for _ in range(5)],
    },
    {
        "ad": "orta_taban_gece_90_ustu",
        "aciklama": "5 gün üst üste gece > 90 dk → ORTA",
        "gunluk_veri": [_gun(100, 120) for _ in range(5)],
    },
    {
        "ad": "sinir_tam_480_tetiklemez",
        "aciklama": "tüketim tam 480 → '> 480' sağlanmaz, ORTA'ya düşer",
        "gunluk_veri": [_gun(480, 10) for _ in range(5)],
    },
    {
        "ad": "bir_gun_bozunca_tetiklemez",
        "aciklama": "son 5 günün biri eşiğin altında → taban yok",
        "gunluk_veri": [_gun(500, 10), _gun(500, 10), _gun(100, 10),
                        _gun(500, 10), _gun(500, 10)],
    },
    {
        "ad": "sadece_son_bes_gun_sayilir",
        "aciklama": "eski günler kötü ama son 5 gün temiz → taban yok",
        "gunluk_veri": [_gun(600, 200) for _ in range(3)] +
                       [_gun(60, 0) for _ in range(5)],
    },
]


# =====================================================================
# HİSTEREZİS (§5.4)
# =====================================================================
# BantTakipci'ye aday bant dizisi verilir, gösterilen bant dizisi kaydedilir.

AZ, ORTA, COK = ref.AZ, ref.ORTA, ref.COK

HISTEREZIS_VEKTORLERI = [
    {
        "ad": "ilk_gun_dogrudan_yerlesir",
        "aciklama": "mevcut bant yokken ilk aday sorgusuz kabul edilir",
        "aday_dizisi": [ORTA],
    },
    {
        "ad": "yukari_uc_gun",
        "aciklama": "GUN_YUKARI=3 → üçüncü günde ÇOK'a çıkar",
        "aday_dizisi": [ORTA, COK, COK, COK, COK],
    },
    {
        "ad": "yukari_iki_gun_yetmez",
        "aciklama": "2 gün aday sonra geri dönüş → bant değişmez",
        "aday_dizisi": [ORTA, COK, COK, ORTA],
    },
    {
        "ad": "asagi_yedi_gun",
        "aciklama": "GUN_ASAGI=7 → yedinci günde AZ'a iner, altıncıda inmez",
        "aday_dizisi": [ORTA, AZ, AZ, AZ, AZ, AZ, AZ, AZ],
    },
    {
        "ad": "asagi_alti_gun_yetmez",
        "aciklama": "6 gün aşağı adayı yeterli değil",
        "aday_dizisi": [ORTA, AZ, AZ, AZ, AZ, AZ, AZ],
    },
    {
        "ad": "TUZAK_ayni_yon_sayac_sifirlanmaz",
        "aciklama": (
            "spec-risk.md §5.4 tuzağı: ÇOK'tan inerken aday ORTA iken AZ'a "
            "derinleşiyor. Yön aynı (aşağı) olduğu için sayaç SIFIRLANMAMALI; "
            "7. günde AZ'a inmeli. Bu hata bir kez yapıldı, düzeltildi."
        ),
        "aday_dizisi": [COK, ORTA, ORTA, ORTA, AZ, AZ, AZ, AZ],
    },
    {
        "ad": "yon_degisince_sayac_sifirlanir",
        "aciklama": "aşağı adayı sürerken yukarı adayı gelirse sayaç 1'e döner",
        "aday_dizisi": [ORTA, AZ, AZ, AZ, COK, COK, COK],
    },
    {
        "ad": "ayni_banda_donunce_bekleyen_temizlenir",
        "aciklama": "aday mevcut banda dönerse bekleyen sıfırlanır",
        "aday_dizisi": [ORTA, AZ, AZ, ORTA, AZ, AZ],
    },
]


# =====================================================================
# TREND (§5.5)
# =====================================================================

TREND_VEKTORLERI = [
    {
        "ad": "veri_yetersiz",
        "aciklama": "14 günden az geçmiş → belirsiz",
        "skor_gecmisi": [50.0] * 13,
    },
    {
        "ad": "sabit",
        "aciklama": "iki hafta ortalaması aynı → sabit",
        "skor_gecmisi": [50.0] * 14,
    },
    {
        "ad": "iyilesiyor",
        "aciklama": "son hafta 10 puan düşük (< -5) → iyileşiyor",
        "skor_gecmisi": [60.0] * 7 + [50.0] * 7,
    },
    {
        "ad": "kotulesiyor",
        "aciklama": "son hafta 10 puan yüksek (> +5) → kötüleşiyor",
        "skor_gecmisi": [50.0] * 7 + [60.0] * 7,
    },
    {
        "ad": "sinir_bes_puan_sabit",
        "aciklama": "fark tam +5 → '> 5' sağlanmaz, sabit",
        "skor_gecmisi": [50.0] * 7 + [55.0] * 7,
    },
    {
        "ad": "sinir_eksi_bes_puan_sabit",
        "aciklama": "fark tam -5 → '< -5' sağlanmaz, sabit",
        "skor_gecmisi": [55.0] * 7 + [50.0] * 7,
    },
    {
        "ad": "sadece_son_ondort_gun",
        "aciklama": "14 günden eski skorlar hesaba katılmaz",
        "skor_gecmisi": [90.0] * 10 + [50.0] * 7 + [50.0] * 7,
    },
]


# =====================================================================
# ÜRETİM
# =====================================================================

def _risk_beklenen(sonuc):
    """risk_skoru() çıktısından test edilecek alanları seçer."""
    return {
        "S": sonuc["S"],
        "P": sonuc["P"],
        "D": sonuc["D"],
        "B_tuketim": sonuc["B_tuketim"],
        "B_islevsel": sonuc["B_islevsel"],
        "R_b": sonuc["R_b"],
        "R_s": sonuc["R_s"],
        "alpha": sonuc["alpha"],
        "R": sonuc["R"],
        "surukleyiciler": sonuc["surukleyiciler"],
    }


def uret():
    vektorler = {}

    # --- §4.7 profilleri ---
    profil_vektorleri = []
    for p in PROFILLER:
        sonuc = ref.risk_skoru(
            p["kategori_dakika"], p["oruntu"],
            anket_puani=None, kacinci_gun=14, anket_kac_gun_once=0,
            adim=p["adim"], spor_dk=p["spor_dk"],
        )
        beklenen = _risk_beklenen(sonuc)
        beklenen["bant"] = ref.esikten_bant(sonuc["R"])
        beklenen["toplam_dk"] = sonuc["toplam_dk"]
        profil_vektorleri.append({
            "ad": p["ad"],
            "aciklama": p["aciklama"],
            "girdi": {
                "kategori_dakika": p["kategori_dakika"],
                "oruntu": p["oruntu"],
                "anket_puani": None,
                "kacinci_gun": 14,
                "anket_kac_gun_once": 0,
                "adim": p["adim"],
                "spor_dk": p["spor_dk"],
            },
            "beklenen": beklenen,
        })
    vektorler["dogrulama_profilleri"] = profil_vektorleri

    # --- kenar durumlar ---
    kenar = []
    for k in KENAR_DURUMLAR:
        sonuc = ref.risk_skoru(
            k["kategori_dakika"], k["oruntu"],
            anket_puani=k["anket_puani"],
            kacinci_gun=k["kacinci_gun"],
            anket_kac_gun_once=k["anket_kac_gun_once"],
            adim=k["adim"], spor_dk=k["spor_dk"],
        )
        beklenen = _risk_beklenen(sonuc)
        beklenen["bant"] = ref.esikten_bant(sonuc["R"])
        kenar.append({
            "ad": k["ad"],
            "aciklama": k["aciklama"],
            "girdi": {
                "kategori_dakika": k["kategori_dakika"],
                "oruntu": k["oruntu"],
                "anket_puani": k["anket_puani"],
                "kacinci_gun": k["kacinci_gun"],
                "anket_kac_gun_once": k["anket_kac_gun_once"],
                "adim": k["adim"],
                "spor_dk": k["spor_dk"],
            },
            "beklenen": beklenen,
        })
    vektorler["risk_skoru_kenar"] = kenar

    # --- kategorilendirme ---
    kategori = []
    for k in KATEGORI_VEKTORLERI:
        sonuc = ref.kategorilere_dagit(
            [tuple(x) for x in k["kullanim_listesi"]],
            k["kullanici_secimleri"],
        )
        kategori.append({
            "ad": k["ad"],
            "girdi": {
                "kullanim_listesi": k["kullanim_listesi"],
                "kullanici_secimleri": k["kullanici_secimleri"],
            },
            "beklenen": {"kategori_dakika": sonuc},
        })
    vektorler["kategorilendirme"] = kategori

    # --- eşik ---
    vektorler["esikten_bant"] = [
        {"girdi": {"skor": s}, "beklenen": {"bant": ref.esikten_bant(s)}}
        for s in ESIK_SKORLARI
    ]

    # --- güvenlik tabanı ---
    guvenlik = []
    for g in GUVENLIK_VEKTORLERI:
        taban, kural = ref.guvenlik_tabani(g["gunluk_veri"])
        guvenlik.append({
            "ad": g["ad"],
            "aciklama": g["aciklama"],
            "girdi": {"gunluk_veri": g["gunluk_veri"]},
            "beklenen": {"taban": taban, "kural": kural},
        })
    vektorler["guvenlik_tabani"] = guvenlik

    # --- histerezis ---
    histerezis = []
    for h in HISTEREZIS_VEKTORLERI:
        takipci = ref.BantTakipci()
        gosterilen = [takipci.guncelle(aday) for aday in h["aday_dizisi"]]
        histerezis.append({
            "ad": h["ad"],
            "aciklama": h["aciklama"],
            "girdi": {"aday_dizisi": h["aday_dizisi"]},
            "beklenen": {
                "gosterilen_dizisi": gosterilen,
                "son_bant": takipci.mevcut_bant,
                "son_durum": takipci.durum(),
            },
        })
    vektorler["histerezis"] = histerezis

    # --- trend ---
    vektorler["trend"] = [
        {
            "ad": t["ad"],
            "aciklama": t["aciklama"],
            "girdi": {"skor_gecmisi": t["skor_gecmisi"]},
            "beklenen": {"trend": ref.trend_bul(t["skor_gecmisi"])},
        }
        for t in TREND_VEKTORLERI
    ]

    # --- etkileyen faktörler ---
    faktor = []
    for f in FAKTOR_VEKTORLERI:
        sonuc = _etkileyen_faktorler(
            f["kategori_dakika"], f["oruntu"], f["adim"], f["spor_dk"],
        )
        faktor.append({
            "ad": f["ad"],
            "aciklama": f["aciklama"],
            "girdi": {
                "kategori_dakika": f["kategori_dakika"],
                "oruntu": f["oruntu"],
                "adim": f["adim"],
                "spor_dk": f["spor_dk"],
            },
            "beklenen": {"faktorler": sonuc},
        })
    vektorler["etkileyen_faktorler"] = faktor

    return {
        "surum": 1,
        "kaynak": "docs/referans/yesiliz_hesap.py",
        "uretici": "tests/vektor_uret.py",
        "tolerans": 1e-4,
        "not": (
            "ELLE DÜZENLEME. Referans Python değişince 'python3 tests/vektor_uret.py' "
            "ile yeniden üret. Kotlin portu (core-risk) bu dosyayı okuyup aynı "
            "sonucu vermeli — CLAUDE.md §7."
        ),
        "sabitler": {
            "S_REF": ref.S_REF,
            "SASSV_KESME": ref.SASSV_KESME,
            "MAKS_SONUMLEME": ref.MAKS_SONUMLEME,
            "BANT_AZ_UST": ref.BANT_AZ_UST,
            "BANT_ORTA_UST": ref.BANT_ORTA_UST,
            "GUVENLIK_GUN": ref.GUVENLIK_GUN,
            "GUN_YUKARI": ref.GUN_YUKARI,
            "GUN_ASAGI": ref.GUN_ASAGI,
            "KATEGORI_AGIRLIK": ref.KATEGORI_AGIRLIK,
            "TUKETIM": sorted(ref.TUKETIM),
            "ORUNTU_KATSAYI": ref.ORUNTU_KATSAYI,
            "FAKTOR_ESIK_DUSUK": FAKTOR_ESIK_DUSUK,
            "FAKTOR_ESIK_ORTA": FAKTOR_ESIK_ORTA,
        "FAKTOR_ASGARI_DK": FAKTOR_ASGARI_DK,
        },
        "vektorler": vektorler,
    }


def main():
    veri = uret()
    hedef = os.path.join(BURASI, "test_vectors.json")
    with open(hedef, "w", encoding="utf-8") as f:
        json.dump(veri, f, ensure_ascii=False, indent=2)
        f.write("\n")

    toplam = sum(len(v) for v in veri["vektorler"].values())
    print("Yazıldı: %s" % hedef)
    print("Toplam vektör: %d" % toplam)
    for ad, liste in veri["vektorler"].items():
        print("  %-24s %d" % (ad, len(liste)))

    print("\n§4.7 doğrulama profilleri (spec-risk.md tablosu bu değerlerden gelir):")
    print("  %-28s %8s %8s %8s  %s" % ("profil", "dk", "S", "R_b", "bant"))
    for v in veri["vektorler"]["dogrulama_profilleri"]:
        b = v["beklenen"]
        print("  %-28s %8.0f %8.1f %8.1f  %s" % (
            v["ad"], b["toplam_dk"], b["S"], b["R_b"], b["bant"]))


if __name__ == "__main__":
    main()
