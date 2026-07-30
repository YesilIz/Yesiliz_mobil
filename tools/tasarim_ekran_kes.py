"""Teslim edilen tasarım sayfasından (grid halinde birden çok ekran içeren
tek PNG) tekil ekran görsellerini keser.

Girdi sayfası şeffaf zeminli bir PNG ızgarasıdır: her hücrede üstte küçük
bir "NN — İsim" etiketi, altında telefon mockup'ı bulunur. Izgara satır/sütun
sınırları whitespace (neredeyse tamamen zemin rengi) taramasıyla otomatik
bulunur — sayfa yeniden düzenlense bile GRID sabiti yeniden hesaplanabilir
(bkz. `satir_sutun_tespit_et`). Çıktılar `docs/tasarim/ekranlar/NN_ad.png`.

Kullanım:
    python3 tools/tasarim_ekran_kes.py <kaynak.png> <hucre1:NN_ad> [<hucre2:NN_ad> ...]
    hucre = "satir,sutun" (0-indeksli)

Örnek:
    python3 tools/tasarim_ekran_kes.py "Yeşilİz Mobil Arayüz-selection.png" \
        3,2:08b_ana_ekran_ebeveyn.png 6,2:20_ebeveyn_ilk_kurulum_rol.png
"""

import sys
from pathlib import Path

import numpy as np
from PIL import Image

CIKTI_KLASORU = Path("docs/tasarim/ekranlar")
GAP_ORAN_ESIK = 0.01  # bir satır/sütunun "boş" sayılması için ink oranı eşiği


def zemine_otur(img: Image.Image) -> Image.Image:
    """Şeffaf pikselleri beyaza oturtur — ink taraması için gerekli."""
    rgba = img.convert("RGBA")
    zemin = Image.new("RGBA", rgba.size, (255, 255, 255, 255))
    return Image.alpha_composite(zemin, rgba).convert("RGB")


def bosluk_bantlari(
    ink_1d: np.ndarray, uzunluk_esik: int, birlestir_esik: int = 0
) -> list[tuple[int, int]]:
    bantlar = []
    icinde = False
    basla = 0
    for i, bos in enumerate(ink_1d):
        if bos and not icinde:
            icinde = True
            basla = i
        elif not bos and icinde:
            icinde = False
            if i - basla > uzunluk_esik:
                bantlar.append((basla, i))
    if icinde:
        bantlar.append((basla, len(ink_1d)))

    if birlestir_esik <= 0 or len(bantlar) < 2:
        return bantlar

    # birbirine yakın bantları tek bant olarak birleştir (etiket/gölge kırıntıları
    # gerçek satır/sütun boşluğunu iki parçaya bölebiliyor)
    birlesmis = [bantlar[0]]
    for basla, bitis in bantlar[1:]:
        onceki_basla, onceki_bitis = birlesmis[-1]
        if basla - onceki_bitis < birlestir_esik:
            birlesmis[-1] = (onceki_basla, bitis)
        else:
            birlesmis.append((basla, bitis))
    return birlesmis


def satir_sutun_tespit_et(arr: np.ndarray) -> tuple[list[int], list[tuple[int, int]]]:
    h, w, _ = arr.shape
    gray = arr.mean(axis=2)
    ink = gray < 245

    satir_ink = ink.sum(axis=1)
    satir_bos = satir_ink < (w * GAP_ORAN_ESIK)
    satir_bantlari = bosluk_bantlari(satir_bos, 10, birlestir_esik=25)
    # her satırın gerçek başlangıcı: bant bitiminden ~20px önce (etiket üst kenarı için pay)
    aday_baslari = [0] + [b[1] - 20 for b in satir_bantlari if b[1] < h]

    # bir satırın gerçek yüksekliği en az bu kadar olmalı — daha yakın adaylar
    # (üst kenar boşluğu kırıntısı veya bir ekranın kendi iç boşluğu) elenir
    MIN_SATIR_YUKSEKLIGI = 850
    satir_baslari = [aday_baslari[0]]
    for basla in aday_baslari[1:]:
        if basla - satir_baslari[-1] >= MIN_SATIR_YUKSEKLIGI:
            satir_baslari.append(basla)

    # sütunları tespit etmek için tam dolu (3 sütunu da içeren) bir satır
    # aralığı kullan — ilk satır (0:satir_baslari[1]) bunun için güvenilir
    sutun_arr = ink[satir_baslari[0] : satir_baslari[1], :]
    sutun_ink = sutun_arr.sum(axis=0)
    sutun_bos = sutun_ink < (sutun_arr.shape[0] * GAP_ORAN_ESIK)
    sutun_bantlari_raw = bosluk_bantlari(sutun_bos, 5)
    # sayfanın sol/sağ dış kenar boşluğu bir "sütun ayracı" değildir
    sutun_bantlari_raw = [
        b for b in sutun_bantlari_raw if b[0] > 5 and b[1] < w - 5
    ]
    sutunlar = []
    onceki = 0
    for b in sutun_bantlari_raw:
        orta = (b[0] + b[1]) // 2
        sutunlar.append((onceki, orta))
        onceki = orta
    sutunlar.append((onceki, w))

    return satir_baslari, sutunlar


def main() -> None:
    kaynak = Path(sys.argv[1])
    istekler = sys.argv[2:]

    img = Image.open(kaynak)
    duz = zemine_otur(img)
    arr = np.array(duz)
    h, _, _ = arr.shape

    satir_baslari, sutunlar = satir_sutun_tespit_et(arr)
    satir_baslari.append(h)

    CIKTI_KLASORU.mkdir(parents=True, exist_ok=True)

    for istek in istekler:
        hucre, ad = istek.split(":")
        satir, sutun = map(int, hucre.split(","))
        y0, y1 = satir_baslari[satir], satir_baslari[satir + 1]
        x0, x1 = sutunlar[sutun]
        kesit = img.crop((x0, y0, x1, y1))
        cikti_yolu = CIKTI_KLASORU / ad
        kesit.save(cikti_yolu)
        print(f"{hucre} -> {cikti_yolu}  ({x1 - x0}x{y1 - y0})")


if __name__ == "__main__":
    main()
