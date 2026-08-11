"""Ekran görüntüsündeki başlık gerçekten ortalı mı — göz kararı değil, ölçüm.

`YesilBaslikliEkran`'ın yeşil başlık şeridindeki beyaz piksel kümelerini
bulup metnin merkezini ekran merkeziyle karşılaştırır. `kubragorev.md` §C —
A1 düzeltmesini (`ui/bilesen/UstBaslik.kt`) doğrulamak için yazıldı.

Kullanım:
    adb exec-out screencap -p > ekran.png
    python3 tools/kontrol_baslik.py ekran.png
"""

import sys

from PIL import Image


def main() -> None:
    im = Image.open(sys.argv[1]).convert("RGB")
    genislik, _ = im.size
    piksel = im.load()

    # Yeşil başlık şeridi: durum çubuğunun altı, içerik yüzeyinin üstü.
    sutunlar = []
    for x in range(genislik):
        beyaz = sum(
            1
            for y in range(95, 150)
            if piksel[x, y][0] > 200 and piksel[x, y][1] > 200 and piksel[x, y][2] > 200
        )
        sutunlar.append(beyaz)

    xler = [x for x, c in enumerate(sutunlar) if c > 0]
    if not xler:
        print("başlık şeridinde beyaz piksel yok")
        sys.exit(1)

    # Kümelere ayır: ilk küme geri oku, en genişi başlık metni.
    kumeler = []
    bas = onceki = xler[0]
    for x in xler[1:]:
        if x - onceki > 25:
            kumeler.append((bas, onceki))
            bas = x
        onceki = x
    kumeler.append((bas, onceki))

    a, b = max(kumeler, key=lambda k: k[1] - k[0])
    merkez = (a + b) / 2
    sapma = merkez - genislik / 2
    print(f"başlık merkezi={merkez:.1f}  ekran merkezi={genislik / 2}  sapma={sapma:+.1f}px")


if __name__ == "__main__":
    main()
