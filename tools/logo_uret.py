#!/usr/bin/env python3
"""Launcher ikonu üreticisi — `docs/tasarim/yenilogo.png` → adaptive icon.

Neden bir betik gerekiyor:

Teslim edilen logo 1600x1600 **kare** bir PNG ve alfa kanalı yok. İçinde üç
katman var: dışta açık yeşil yuvarlatılmış kare, ortada daha koyu bir yeşil
disk, en içte asıl marka işareti (telefon + hilal + piksel motifi).

Android'in adaptive icon'unda foreground katmanı 108dp'lik bir tuvale çiziliyor
ama launcher bunun **orta 66dp**'sini garanti ediyor; gerisi maskeye göre
kırpılıyor. Dosya olduğu gibi foreground yapılırsa yuvarlak maske altında disk
kesiliyor — kullanıcının "yuvarlak tam olmalı" uyarısı tam olarak bu.

Çözüm: dış zeminler ATILIYOR (zaten maskenin işi), yalnızca asıl işaret
kırpılıp güvenli alana ölçekleniyor. Zemin rengi adaptive icon'un kendi
background katmanına veriliyor, yani maske ne şekilde olursa olsun (daire,
squircle, kare) zemin kenardan kenara doluyor ve işaret hiçbir zaman
kırpılmıyor.

Kullanım:
    python3 tools/logo_uret.py

Üretilenler (elle düzenlenmez):
    app/src/main/res/mipmap-*/ic_launcher_foreground.png
"""
from __future__ import annotations

import sys
from pathlib import Path

try:
    import numpy as np
    from PIL import Image
except ImportError:  # pragma: no cover
    sys.exit("pillow ve numpy gerekiyor: pip install pillow numpy")

KOK = Path(__file__).resolve().parent.parent
KAYNAK = KOK / "docs" / "tasarim" / "yenilogo.png"
RES = KOK / "app" / "src" / "main" / "res"

# Adaptive icon tuvali 108dp; launcher yalnızca orta 66dp'yi garanti ediyor.
# İşareti 66/108'in biraz altında tutuyoruz ki en agresif maskede bile
# kenarına değmesin.
TUVAL_DP = 108
GUVENLI_DP = 60

# mipmap yoğunlukları — 48dp'lik ikonun katları (adaptive icon 108dp tuval).
YOGUNLUKLAR = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}


def isaret_kutusu(im: Image.Image) -> tuple[int, int, int, int]:
    """Asıl marka işaretinin sınır kutusu.

    Zeminler açık ve düşük doygunlukta; işaret (telefon çerçevesi, hilal,
    pikseller) belirgin şekilde daha koyu ve daha yeşil. Ayrım bu kontrasttan
    yapılıyor — sabit koordinat yazmak, logo güncellenince sessizce yanlış
    kırpardı.
    """
    a = np.asarray(im.convert("RGB")).astype(int)
    r, g, b = a[:, :, 0], a[:, :, 1], a[:, :, 2]
    maske = (g > r + 15) & (g > b + 15) & (g < 210)
    ys, xs = np.nonzero(maske)
    if len(xs) == 0:
        sys.exit("İşaret bulunamadı — kaynak görsel beklenenden farklı.")
    return int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1


def zemin_saydamla(kirpik: Image.Image) -> Image.Image:
    """Zemin tonlarını saydama çevirir.

    Kaynakta alfa yok; kırpılan parçanın zemini olduğu gibi bırakılırsa
    foreground katmanı, background katmanının üstüne opak bir dikdörtgen
    basar ve maske ne olursa olsun köşeli görünür.
    """
    rgba = kirpik.convert("RGBA")
    a = np.asarray(rgba).astype(int)
    r, g, b = a[:, :, 0], a[:, :, 1], a[:, :, 2]
    # Açık ve az doygun her şey zemin sayılıyor. Telefon ekranındaki beyaz da
    # buna giriyor — bilinçli: hilal beyazın üstünde duruyor, beyazı saydam
    # yapmak işareti daha hafif ve maskeden bağımsız kılıyor.
    zemin = (r > 200) & (g > 200) & (b > 200)
    zemin |= (abs(r - g) < 25) & (abs(g - b) < 25) & (r > 180)
    yeni = a.copy()
    yeni[:, :, 3] = np.where(zemin, 0, 255)
    return Image.fromarray(yeni.astype("uint8"), "RGBA")


def uret() -> None:
    if not KAYNAK.exists():
        sys.exit(f"Kaynak yok: {KAYNAK}")

    kaynak = Image.open(KAYNAK)
    kutu = isaret_kutusu(kaynak)
    isaret = zemin_saydamla(kaynak.crop(kutu))

    # Kare tuvale oturt: işaret 739x856 gibi dikdörtgen, ölçek uzun kenardan.
    kenar = max(isaret.size)
    kare = Image.new("RGBA", (kenar, kenar), (0, 0, 0, 0))
    kare.paste(
        isaret,
        ((kenar - isaret.width) // 2, (kenar - isaret.height) // 2),
        isaret,
    )

    for ad, boyut in YOGUNLUKLAR.items():
        hedef_klasor = RES / f"mipmap-{ad}"
        hedef_klasor.mkdir(parents=True, exist_ok=True)

        guvenli_px = round(boyut * GUVENLI_DP / TUVAL_DP)
        olcekli = kare.resize((guvenli_px, guvenli_px), Image.LANCZOS)

        tuval = Image.new("RGBA", (boyut, boyut), (0, 0, 0, 0))
        pay = (boyut - guvenli_px) // 2
        tuval.paste(olcekli, (pay, pay), olcekli)

        hedef = hedef_klasor / "ic_launcher_foreground.png"
        tuval.save(hedef, "PNG", optimize=True)
        print(f"yazıldı: {hedef.relative_to(KOK)}  ({boyut}x{boyut})")


if __name__ == "__main__":
    uret()
