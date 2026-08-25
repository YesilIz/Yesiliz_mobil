"""
Sunucuda üretilen SVG grafikler.

**Hazır chart kütüphanesi yok** — CLAUDE.md §12 K5'in gerekçesi burada da
geçerli: her kütüphane kendi paletiyle geliyor ve markanın rengine sürekli
karşı durmak gerekiyor. Canvas da yok: CSP `script-src 'self'` ve panelin
JS'siz de okunabilir kalması isteniyor.

Fonksiyonlar **saf**: girdi → string. Tarayıcısız test edilebiliyorlar,
`OturumCikar`/`Tetikleyici` deseniyle aynı gerekçe.
"""

from __future__ import annotations

import datetime as dt
from html import escape

from markupsafe import Markup

#: Risk bandı renkleri — DESIGN.md §1. Koyulaşan yeşil; kırmızı YOK
#: (CLAUDE.md §4). Panelde kırmızı serbest ama risk bandı damgalayıcı olan
#: tam da o yer, yani buradaki kural son kullanıcı ekranıyla aynı.
BANT_RENGI = {"AZ": "#00AE42", "ORTA": "#70ab70", "COK": "#124734"}

BOS_METIN = "Gösterilecek veri yok"


def _bos(genislik: int, yukseklik: int) -> Markup:
    return Markup(
        f'<svg viewBox="0 0 {genislik} {yukseklik}" width="100%" '
        f'role="img" aria-label="{BOS_METIN}">'
        f'<text x="{genislik // 2}" y="{yukseklik // 2}" text-anchor="middle" '
        f'fill="#7B8A82" font-size="14">{BOS_METIN}</text></svg>'
    )


def bant_dagilimi_svg(
    sayilar: dict[str, int],
    genislik: int = 640,
    yukseklik: int = 48,
) -> Markup:
    """
    Yığılmış tek çubuk: bant dağılımı.

    Pasta grafik değil çünkü üç dilimli bir pasta, yığılmış çubuğun
    verdiğinden fazlasını vermiyor ve oran okumayı zorlaştırıyor.
    """
    toplam = sum(sayilar.values())
    if toplam <= 0:
        return _bos(genislik, yukseklik)

    parcalar = []
    x = 0.0
    for bant in ("AZ", "ORTA", "COK"):
        sayi = sayilar.get(bant, 0)
        if sayi <= 0:
            continue
        w = genislik * sayi / toplam
        parcalar.append(
            f'<rect x="{x:.2f}" y="0" width="{w:.2f}" height="{yukseklik}" '
            f'fill="{BANT_RENGI[bant]}"><title>{escape(bant)}: {sayi}</title></rect>'
        )
        x += w

    return Markup(
        f'<svg viewBox="0 0 {genislik} {yukseklik}" width="100%" '
        f'height="{yukseklik}" role="img" '
        f'aria-label="Risk bandı dağılımı, toplam {toplam} kullanıcı">'
        + "".join(parcalar)
        + "</svg>"
    )


def risk_cizgisi_svg(
    noktalar: list[tuple[dt.date, float]],
    genislik: int = 640,
    yukseklik: int = 160,
    tavan: float = 100.0,
) -> Markup:
    """
    Zaman içindeki risk skoru çizgisi.

    Eksen **veriden türüyor**, çağrıdan sabit gelmiyor: CLAUDE.md §12 K49
    aynı hatayı bir kez cihaz tarafında yaşadı — 8 dakikalık bir gün,
    tepesinde "8s" yazan eksenin altında tam boy çubuk olarak çiziliyordu.
    """
    if not noktalar:
        return _bos(genislik, yukseklik)

    kenar = 8
    ic_g = genislik - 2 * kenar
    ic_y = yukseklik - 2 * kenar

    sirali = sorted(noktalar, key=lambda n: n[0])
    if len(sirali) == 1:
        adim = 0.0
    else:
        adim = ic_g / (len(sirali) - 1)

    koordinatlar = []
    for i, (_, deger) in enumerate(sirali):
        x = kenar + i * adim
        oran = min(max(deger, 0.0), tavan) / tavan
        y = kenar + ic_y * (1 - oran)
        koordinatlar.append(f"{x:.2f},{y:.2f}")

    cizgi = " ".join(koordinatlar)
    ilk_tarih = escape(sirali[0][0].isoformat())
    son_tarih = escape(sirali[-1][0].isoformat())

    return Markup(
        f'<svg viewBox="0 0 {genislik} {yukseklik}" width="100%" '
        f'height="{yukseklik}" role="img" '
        f'aria-label="Risk skoru, {ilk_tarih} - {son_tarih}">'
        f'<line x1="{kenar}" y1="{kenar + ic_y / 2:.2f}" '
        f'x2="{genislik - kenar}" y2="{kenar + ic_y / 2:.2f}" '
        f'stroke="#E3EDE6" stroke-width="1"/>'
        f'<polyline points="{cizgi}" fill="none" stroke="#00AE42" '
        f'stroke-width="2" stroke-linejoin="round" stroke-linecap="round"/>'
        f'<circle cx="{koordinatlar[-1].split(",")[0]}" '
        f'cy="{koordinatlar[-1].split(",")[1]}" r="4" fill="#124734"/>'
        "</svg>"
    )
