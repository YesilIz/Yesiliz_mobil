# -*- coding: utf-8 -*-
"""
YEŞİLİZ — KATEGORİ TABLOSU ÜRETİCİ  (CLAUDE.md §11)

`app/src/main/assets/kategori_tablosu.json` dosyasını üretir.
Paket adı → kategori eşlemesi; risk motorunun girdisi bu tablodan çıkar.

Üç katman, sonraki öncekini EZER:

    1. LMU açık veri seti (3.091 uygulama, Almanya örneklemi)
    2. docs/referans/yesiliz_hesap.py içindeki referans tablo
    3. Elle eklenen Türkiye pazarı uygulamaları

Katman 2'nin ezmesi ŞART: LMU, Snapchat'i "Communication" (ağırlık 0,5),
TikTok'u "Social_Media" diyor. Referans ise ikisini de tüketim tarafına
koyuyor — Snapchat için 0,5 yerine 1,0. Referans doğru: Snapchat'te
geçirilen dakika WhatsApp dakikası gibi değil. Ayrıca `test_vectors.json`
içindeki `kategorilendirme` vektörleri referans tabloya göre üretildi;
LMU kazanırsa testler kırılır.

Çalıştırma:
    python3 tools/kategori_tablosu_uret.py

Veri seti ilk çalıştırmada indirilir, `tools/veri/` altına önbelleklenir.
"""

import csv
import json
import os
import sys
import urllib.request

BURASI = os.path.dirname(os.path.abspath(__file__))
KOK = os.path.dirname(BURASI)
VERI_DIZINI = os.path.join(BURASI, "veri")
CSV_YOLU = os.path.join(VERI_DIZINI, "lmu_app_categorisation_2020_v2.csv")
HEDEF = os.path.join(KOK, "app", "src", "main", "assets", "kategori_tablosu.json")

CSV_URL = "https://pada.psycharchives.org/bitstream/e6d56ab5-8557-4833-8112-9d8e9ed57211"

KAYNAK = {
    "ad": "Systematic Categorisation of 3,091 Smartphone Applications "
          "From a Large-Scale Smartphone Sensing Dataset",
    "yazarlar": "Schoedel, R., Oldemeier, M., Bonauer, L., Sust, L.",
    "makale_doi": "10.5334/jopd.59",
    "veri_doi": "10.23668/psycharchives.5680",
    "not": "Journal of Open Psychology Data — açık erişim. Örneklem Almanya; "
           "Türkiye pazarı uygulamaları veri setinde yok, elle eklendi.",
}

sys.path.insert(0, os.path.join(KOK, "docs", "referans"))
import yesiliz_hesap as ref  # noqa: E402


# =====================================================================
# LMU kategorileri → Yeşilİz kategorileri
# =====================================================================
# Yeşilİz'in 8 kategorisi spec-risk.md §4.1'de sabit. Buna ek olarak
# "sistem" var: risk hesabına HİÇ girmeyen paketler.
#
# Neden ayrı bir "sistem" kategorisi:
# LMU'nun 3.091 uygulamasının 1.426'sı System/Settings — launcher, SystemUI,
# üretici bloatware. Bunlar "ekran süresi" değil. Tabloda olmasalardı
# çalışma anında "other" (ağırlık 0,5) sayılırlardı ki bu tamamen yanlış
# olurdu: telefonun kendi arayüzü kullanıcıya risk yazılamaz.
#
# "sistem" core-risk'e SIZMAZ — app katmanı bu paketleri günlük özet
# oluşturulurken düşürür, motor 8 kategoriyle çalışmaya devam eder
# (CLAUDE.md §6 veri akışı).
LMU_ESLEME = {
    # --- tüketim tarafı ---
    "Social_Media": "social",
    "Dating": "social",
    "Audio_Entertainment": "entertainment",
    "Visual_Entertainment": "entertainment",
    "Gaming": "games",

    # --- iletişim ---
    "Communication": "communication",

    # --- işlevsel ---
    "Tools": "productivity",
    "Creativity": "productivity",
    "Career": "productivity",
    "Time": "productivity",
    "Photo": "productivity",
    "Security": "productivity",
    "Finance": "productivity",
    "Weather": "productivity",
    "Orientation": "productivity",
    "Transportation": "productivity",

    # --- eğitim ---
    "Knowledge": "education",
    "Reading": "education",

    # --- sağlık ---
    "Health": "health",

    # --- belirsiz: 0,5 ağırlıkla ortada bırakılıyor ---
    # News ve Internet (tarayıcı) kompulsif de olabilir işlevsel de;
    # Shopping/Food/Spirituality düşük yoğunluklu. Hepsi "other".
    "News": "other",
    "Internet": "other",
    "Shopping": "other",
    "Food": "other",
    "Spirituality": "other",

    # --- hesaba girmeyenler ---
    "System": "sistem",
    "Settings": "sistem",
}


# =====================================================================
# Katman 3 — Türkiye pazarı
# =====================================================================
# LMU örneklemi Almanya. Türk gencinin telefonunda ağırlıklı yer tutan
# uygulamalar burada. SADECE paket adından emin olunanlar eklendi —
# tahmin edilen paket adı yanlış eşleşme üretir, boş bırakmak daha güvenli.
#
# Bu liste EKSİK ve bilinçli olarak öyle. Gerçek bir telefonda
# `pm list packages -3` çalıştırılıp kapsama ölçülecek; asıl doldurma
# oradan gelecek. Tablodan düşenler için çalışma anı yedeği ayrıca var
# (bkz. KategoriEsleyici — ApplicationInfo.category).
TURKIYE = {
    # sosyal
    "com.linkedin.android": "social",
    "com.pinterest": "social",
    "com.tumblr": "social",
    "com.ss.android.ugc.trill": "entertainment",   # TikTok (bölge varyantı)

    # eğlence — Türkiye yayın platformları
    "com.turkcell.gncplay": "entertainment",       # fizy
    "com.bein.connect": "entertainment",           # beIN CONNECT
    "tv.puhutv": "entertainment",                  # puhutv
    "com.google.android.apps.youtube.music": "entertainment",
    "com.amazon.avod.thirdpartyclient": "entertainment",
    "com.disney.disneyplus": "entertainment",
    "com.wattpad": "entertainment",
    "deezer.android.app": "entertainment",

    # oyun
    "com.supercell.clashroyale": "games",
    "com.supercell.brawlstars": "games",
    "com.king.candycrushsaga": "games",
    "com.tencent.ig": "games",                     # PUBG Mobile

    # iletişim
    "com.turkcell.bip": "communication",           # BiP
    "com.viber.voip": "communication",
    "com.skype.raider": "communication",
    "com.microsoft.teams": "communication",
    "us.zoom.videomeetings": "communication",
    "com.google.android.apps.messaging": "communication",

    # üretim
    "com.microsoft.office.excel": "productivity",
    "com.microsoft.office.powerpoint": "productivity",
    "com.microsoft.office.onenote": "productivity",
    "com.google.android.calendar": "productivity",
    "com.google.android.keep": "productivity",
    "com.todoist": "productivity",
    "com.trello": "productivity",

    # sağlık
    "com.google.android.apps.fitness": "health",
    "com.myfitnesspal.android": "health",
}


def veri_setini_indir():
    if os.path.exists(CSV_YOLU) and os.path.getsize(CSV_YOLU) > 100_000:
        return
    os.makedirs(VERI_DIZINI, exist_ok=True)
    print("LMU veri seti indiriliyor: %s" % CSV_URL)
    istek = urllib.request.Request(CSV_URL, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(istek, timeout=120) as yanit:
        icerik = yanit.read()
    with open(CSV_YOLU, "wb") as f:
        f.write(icerik)
    print("  %d bayt yazıldı" % len(icerik))


def uret():
    veri_setini_indir()

    paketler = {}
    sayac = {"lmu": 0, "referans_yeni": 0, "referans_ezdi": 0,
             "turkiye_yeni": 0, "turkiye_ezdi": 0, "atlanan": 0}
    catismalar = []

    # --- katman 1: LMU ---
    with open(CSV_YOLU, encoding="utf-8") as f:
        for satir in csv.DictReader(f):
            paket = (satir["App_name"] or "").strip()
            lmu_kategori = (satir["Final_Rating"] or "").strip()
            if not paket or lmu_kategori in ("", "NA"):
                sayac["atlanan"] += 1
                continue
            bizim = LMU_ESLEME.get(lmu_kategori)
            if bizim is None:
                raise SystemExit(
                    "LMU kategorisi eşlenmemiş: %r — LMU_ESLEME'ye ekle" % lmu_kategori
                )
            paketler[paket] = bizim
            sayac["lmu"] += 1

    # --- katman 2: referans tablo (EZER) ---
    for paket, kategori in ref.PAKET_KATEGORI.items():
        onceki = paketler.get(paket)
        if onceki is None:
            sayac["referans_yeni"] += 1
        elif onceki != kategori:
            sayac["referans_ezdi"] += 1
            catismalar.append((paket, onceki, kategori))
        paketler[paket] = kategori

    # --- katman 3: Türkiye (EZER) ---
    for paket, kategori in TURKIYE.items():
        onceki = paketler.get(paket)
        if onceki is None:
            sayac["turkiye_yeni"] += 1
        elif onceki != kategori:
            sayac["turkiye_ezdi"] += 1
            catismalar.append((paket, onceki, kategori))
        paketler[paket] = kategori

    tablo = {
        "surum": 1,
        "kaynak": KAYNAK,
        "not": "ÜRETİLEN DOSYA — elle düzenleme. "
               "python3 tools/kategori_tablosu_uret.py ile yeniden üret.",
        "risk_kategorileri": sorted(ref.KATEGORI_AGIRLIK.keys()),
        "hesaba_girmeyen": "sistem",
        "varsayilan": "other",
        "paketler": dict(sorted(paketler.items())),
    }

    os.makedirs(os.path.dirname(HEDEF), exist_ok=True)
    with open(HEDEF, "w", encoding="utf-8") as f:
        json.dump(tablo, f, ensure_ascii=False, indent=1, sort_keys=False)
        f.write("\n")

    # --- rapor ---
    dagilim = {}
    for k in paketler.values():
        dagilim[k] = dagilim.get(k, 0) + 1

    print("\nYazıldı: %s" % HEDEF)
    print("Toplam paket: %d\n" % len(paketler))
    print("Katman katkıları:")
    for k, v in sayac.items():
        print("  %-16s %5d" % (k, v))
    print("\nKategori dağılımı:")
    for k, v in sorted(dagilim.items(), key=lambda x: -x[1]):
        isaret = "  (hesaba girmez)" if k == "sistem" else ""
        print("  %-16s %5d%s" % (k, v, isaret))
    if catismalar:
        print("\nÜst katmanın ezdiği LMU kararları:")
        for paket, eski, yeni in catismalar:
            print("  %-34s %s → %s" % (paket, eski, yeni))


if __name__ == "__main__":
    uret()
