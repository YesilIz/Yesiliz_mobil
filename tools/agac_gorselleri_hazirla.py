# -*- coding: utf-8 -*-
"""
YEŞİLİZ — AĞAÇ İLLÜSTRASYONLARINI ANDROID'E HAZIRLA

Stitch'ten dışa aktarılan illüstrasyonların arka planı **şeffaf değil**:
üretim sırasında "transparent background" isteği yanlış yorumlanmış ve
Photoshop'un şeffaflık damalı deseni resmin İÇİNE çizilmiş
(`#000000` ve `#afafaf` kareler, alfa kanalı yok).

Bu script o deseni kaldırıp gerçek alfa kanalı olan PNG üretir.

> **30 Tem 2026 — ağaç evreleri için artık kullanılmıyor.** 6 evrelik yeni set
> (`docs/tasarim/...` yerine doğrudan WhatsApp'tan gelen tek görsel, farklı
> kaynak/sorun) `tools/agac_gorselleri_ayikla.py` ile üretiliyor — orada
> sorun damalı desen değil, gerçek bir arka plan sahnesi (gökyüzü/tepe).
> Bu script Stitch kaynaklı **başka** illüstrasyonlar için (örn.
> `yedam_iki_figur.png`) hâlâ geçerli, o yüzden siliniyor.

    python3 tools/agac_gorselleri_hazirla.py

Girdi : stitch_markdown_design_system/flat_vector_*/screen.png
Çıktı : app/src/main/res/drawable-nodpi/agac_*.png

## Nasıl ayırt ediliyor

Damalı desen **tam akromatik**: doygunluk (maks kanal − min kanal) = 0.
Ağacın gövdesi soluk gri-yeşil ama doygunluğu 18. Eşik 10 seçildi:

| Öğe            | Renk      | Doygunluk |
|----------------|-----------|-----------|
| dama gri       | `#afafaf` | 0         |
| dama siyah     | `#000000` | 0         |
| **gövde**      | `#b1c3b5` | **18**    |
| dal            | `#164028` | 42        |
| yaprak         | `#319755` | 102       |

Sadece renk eşiği yetmiyor — ağacın içindeki gri tonlar da yakalanırdı.
Bu yüzden **kenardan taşma dolgusu** yapılıyor: yalnızca resmin kenarına
bağlı akromatik bölge silinir. Ağacın içi kenara bağlı olmadığı için güvende.

Son adım: maskeyi 2 piksel genişletmek. Dama ile ağaç sınırındaki
yumuşatma pikselleri griye/siyaha karışmış durumda; genişletme bu halkayı
temizler, karşılığında ağacın kenarından 2 piksel götürür (1024 px'lik
görselde göze görünmez).
"""

import os
import sys

try:
    import numpy as np
    from PIL import Image
    from scipy import ndimage
except ImportError as e:
    sys.exit("Gerekli paket yok (%s). Kurulum: pip3 install pillow numpy scipy" % e)

BURASI = os.path.dirname(os.path.abspath(__file__))
KOK = os.path.dirname(BURASI)
KAYNAK = os.path.join(KOK, "stitch_markdown_design_system")
HEDEF = os.path.join(KOK, "app", "src", "main", "res", "drawable-nodpi")

DOYGUNLUK_ESIGI = 10   # bunun altı "renksiz" → dama adayı
KENAR_SERIDI = 8       # ton öğrenmek için kenardan kaç piksel
TON_TOLERANSI = 22     # öğrenilen tona bu kadar yakın olanlar dama sayılır
GENISLETME_PX = 2      # yumuşatma halkasını temizlemek için
CIKTI_BOYUT = 768      # xxhdpi'de ~256dp; 1024 gereksiz büyük

# Stitch klasör adı parçası → Android kaynak adı
ESLEME = {
    "growth_stage_1_sparse": "agac_evre1_seyrek",
    "growth_stage_2_half": "agac_evre2_yari",
    "growth_stage_3_full": "agac_evre3_dolgun",
    "growth_stage_4_blossom": "agac_evre4_cicekli",
    "growth_stage_5_budding": "agac_evre5_tomurcuk",
    "two_abstract_human_figures": "yedam_iki_figur",
}


def arka_plani_ayikla(dizi):
    """
    Damalı desen maskesi.

    Kenardan taşma dolgusu YETMİYOR: bazı görsellerde desen ağacın dalları
    arasında kalan boşluklara da düşmüş, oralara kenardan ulaşılamıyor
    (evre 2'de tacın ortasında bir parça kalmıştı).

    Bunun yerine **renk imzasıyla** ayıklanıyor. İki koşul birden:

      doygunluk ≤ 10   → akromatik (gövde 18 ile kurtulur)
      parlaklık < 60 veya 150–200 → damanın iki tonu

    Parlaklık koşulu şart: evre 4'teki **beyaz çiçekler** de akromatik
    (doygunluk 0, parlaklık 255). Aralık kısıtı olmasaydı silinirlerdi.
    """
    doygunluk = dizi.max(axis=2) - dizi.min(axis=2)
    parlaklik = dizi.max(axis=2)
    akromatik = doygunluk <= DOYGUNLUK_ESIGI

    # Damanın tonları görselden görsele DEĞİŞİYOR (kimi açık gri, kimi koyu).
    # Sabit aralık kırılgan; tonları her görselin KENAR ŞERİDİNDEN öğreniyoruz.
    # Kenar şeridi daima arka plan — ağaç ortada.
    kenar = np.zeros(dizi.shape[:2], bool)
    kenar[:KENAR_SERIDI, :] = kenar[-KENAR_SERIDI:, :] = True
    kenar[:, :KENAR_SERIDI] = kenar[:, -KENAR_SERIDI:] = True

    kenar_tonlari = parlaklik[kenar & akromatik]
    if kenar_tonlari.size == 0:
        return np.zeros(dizi.shape[:2], bool)   # dama yok (ör. YEDAM görseli)

    # Histogramın tepe noktaları = damanın tonları
    sayim = np.bincount(kenar_tonlari, minlength=256)
    tepeler = [t for t in range(256) if sayim[t] > kenar_tonlari.size * 0.02]

    maske = np.zeros(dizi.shape[:2], bool)
    for tepe in tepeler:
        maske |= akromatik & (np.abs(parlaklik - tepe) <= TON_TOLERANSI)

    # Ton öğrenme tek başına da yetmiyor: evre 2'de tacın altındaki şerit
    # kenarda hiç görünmeyen bir tonda. Kenara BAĞLI tüm akromatik bölgeyi
    # ayrıca ekliyoruz. Doygunluk eşiği 10 olduğu için gövde (18) güvende —
    # eşik 28 denendiğinde gövde de silinmişti.
    etiket, adet = ndimage.label(akromatik)
    if adet > 0:
        kenar_etiketleri = set(etiket[kenar & akromatik].tolist()) - {0}
        if kenar_etiketleri:
            maske |= np.isin(etiket, list(kenar_etiketleri))

    # Yumuşatma halkasını temizle
    if GENISLETME_PX > 0:
        maske = ndimage.binary_dilation(maske, iterations=GENISLETME_PX)
    return maske


def isle(kaynak_yol, hedef_ad):
    goruntu = Image.open(kaynak_yol).convert("RGB")
    dizi = np.asarray(goruntu).astype(np.int16)
    maske = arka_plani_ayikla(dizi)

    rgba = np.dstack([dizi, np.full(dizi.shape[:2], 255, np.int16)])
    rgba[maske] = [0, 0, 0, 0]

    cikti = Image.fromarray(rgba.astype(np.uint8))
    if CIKTI_BOYUT and cikti.width != CIKTI_BOYUT:
        cikti = cikti.resize((CIKTI_BOYUT, CIKTI_BOYUT), Image.LANCZOS)

    os.makedirs(HEDEF, exist_ok=True)
    hedef_yol = os.path.join(HEDEF, hedef_ad + ".png")
    cikti.save(hedef_yol, optimize=True)
    return hedef_yol, 100 * maske.mean(), os.path.getsize(hedef_yol)


def main():
    if not os.path.isdir(KAYNAK):
        sys.exit("Stitch dışa aktarımı yok: %s" % KAYNAK)

    klasorler = sorted(
        d for d in os.listdir(KAYNAK)
        if d.startswith("flat_vector") and
        os.path.isfile(os.path.join(KAYNAK, d, "screen.png"))
    )
    if not klasorler:
        sys.exit("İllüstrasyon bulunamadı")

    print("%-26s %-9s %9s" % ("kaynak", "arka plan", "boyut"))
    print("-" * 48)
    islenen = 0
    for klasor in klasorler:
        ad = next((v for k, v in ESLEME.items() if k in klasor), None)
        if ad is None:
            print("  ATLANDI (eşleşme yok): %s" % klasor[:60])
            continue
        yol, oran, bayt = isle(os.path.join(KAYNAK, klasor, "screen.png"), ad)
        print("%-26s %8.1f%% %8.0f KB" % (ad, oran, bayt / 1024))
        islenen += 1

    print("\n%d görsel → %s" % (islenen, HEDEF))


if __name__ == "__main__":
    main()
