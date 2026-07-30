# -*- coding: utf-8 -*-
"""
YEŞİLİZ — STITCH EKRAN DENETLEYİCİSİ

DESIGN.md §7 kontrol listesinin **otomatikleştirilebilir** maddelerini üretilen
Stitch HTML çıktıları üzerinde tarar:

  ✓ Hiç kırmızı yok            → sıcak hex + isimli renk + Tailwind sıcak sınıf
  ✓ Uyarı/ünlem ikonu yok      → Material Symbols ligature adları
  ✓ Teşhis dili yok            → "bağımlı", "riskli grup", "tedavi" vb.
  ✓ Emir kipi yok              → "azalt!", "bırak!", "hemen kapat" vb.
  ✓ Türkçe karakterler doğru   → mojibake tespiti + Türkçe harf varlığı

İnsan gözü gereken maddeler (bu script BAKMAZ, DESIGN.md §7'de elle işaretlenir):
  · Ağaç ölü görünmüyor
  · Beyaz alan yeterli, ekran kalabalık değil

Kullanım:
    1. Ekranların HTML'ini indir — Stitch MCP `list_screens` çıktısındaki
       her ekranın `htmlCode.downloadUrl` adresi. Bir klasöre kaydet.
    2. python3 tests/kontrol_stitch.py <klasor>

Çıktı: ekran başına özet tablo + yalnızca bulgu olan ekranlar için detay.
Bulgu varsa çıkış kodu 1.
"""

import glob
import os
import re
import sys

# --- Kırmızı / sıcak renk ---
HEX = re.compile(r"#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})\b")
ISIMLI_SICAK = re.compile(
    r"\b(red|crimson|firebrick|tomato|orangered|orange|amber|maroon|salmon|coral)\b",
    re.I)
TAILWIND_SICAK = re.compile(r"\b(bg|text|border|from|to|via|ring|fill|stroke)-"
                            r"(red|orange|amber|rose|yellow)-\d{2,3}\b", re.I)

# --- Uyarı ikonları (Material Symbols ligature adı olarak) ---
UYARI_IKON = re.compile(
    r">\s*(warning|warning_amber|error_outline|report|report_problem|"
    r"priority_high|dangerous|crisis_alert|new_releases|gpp_bad)\s*<")

# --- Teşhis dili ---
# "teşhis" kelimesi TEK BAŞINA ihlal değil: "Bu bir teşhis değil" cümlesi
# DESIGN.md §5.5 ve §5.12'de bilinçli olarak kullanılıyor. Olumsuzlanmamış
# hâlini ve gerçek teşhis sözcüklerini arıyoruz.
TESHIS = re.compile(
    r"(bağımlısın|bağımlısınız|bağımlılığın var|müptela|hastasın|hastalığın|"
    r"riskli grup|tedavi\w*|tanı koy\w*|teşhis(?!\s+değil))", re.I)

# --- Emir kipi / korkutma ---
EMIR = re.compile(
    r"(azalt!|bırak!|yapma!|dur!|kes!|hemen kapat|derhal|zorunlu olarak)", re.I)

# --- Türkçe karakter bozulması ---
MOJIBAKE = re.compile(r"(Ã[¼§¶±ŸŞ]|Å[ŸŞ]|Ä[±ŸŒ]|â€™|â€œ|�)")
TR_HARF = set("ıİğĞşŞçÇöÖüÜ")

# --- Tema tanım bloğu ---
# Stitch, tasarım sisteminin TÜM tokenlarını her ekranın Tailwind config
# bloğuna basıyor — kullanılmasa bile. Kırmızı bulgusunu "tanımlı ama
# kullanılmıyor" ile "fiilen kullanılıyor" diye ayırmak için gerekli.
ERROR_KULLANIM = re.compile(
    r"(?:bg|text|border|ring|fill|stroke|from|to|via)-(?:on-)?error(?:-container)?\b"
    r"|var\(--(?:on-)?error")


def hex_ac(h):
    h = h.lstrip("#")
    if len(h) == 3:
        h = "".join(c * 2 for c in h)
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


def sicak_mi(h):
    """Kırmızı/turuncu ailesi mi? Kırmızı kanal belirgin farkla önde mi?"""
    r, g, b = hex_ac(h)
    if r == g == b:
        return False
    if r <= g:
        return False
    return (r - max(g, b)) >= 40


def govde_metni(html):
    """Script/style çıkar, etiketleri soy, yalnızca görünen metni döndür."""
    h = re.sub(r"<(script|style)\b.*?</\1>", " ", html, flags=re.S | re.I)
    h = re.sub(r"<!--.*?-->", " ", h, flags=re.S)
    h = re.sub(r"<[^>]+>", " ", h)
    h = re.sub(r"&nbsp;?", " ", h)
    return re.sub(r"\s+", " ", h)


def ekran_denetle(yol):
    with open(yol, encoding="utf-8") as f:
        ham = f.read()
    metin = govde_metni(ham)
    return {
        "ad": os.path.basename(yol).replace(".html", ""),
        "sicak_hex": sorted({m.group(0).lower() for m in HEX.finditer(ham)
                             if sicak_mi(m.group(0))}),
        "isimli": sorted({m.group(0).lower() for m in ISIMLI_SICAK.finditer(metin)}),
        "tailwind": sorted({m.group(0).lower() for m in TAILWIND_SICAK.finditer(ham)}),
        "error_kullanim": sorted({m.group(0) for m in ERROR_KULLANIM.finditer(ham)}),
        "ikon": sorted({m.group(1) for m in UYARI_IKON.finditer(ham)}),
        "teshis": sorted({m.group(0) for m in TESHIS.finditer(metin)}),
        "emir": sorted({m.group(0) for m in EMIR.finditer(metin)}),
        "moji": sorted({m.group(0) for m in MOJIBAKE.finditer(ham)}),
        "tr_var": bool(TR_HARF & set(metin)),
    }


def main():
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    klasor = sys.argv[1]
    yollar = sorted(glob.glob(os.path.join(klasor, "*.html")))
    if not yollar:
        print("HTML bulunamadı: %s" % klasor)
        return 2

    sonuclar = [ekran_denetle(y) for y in yollar]

    print("%-24s %-8s %-6s %-7s %-6s %-6s %-5s" % (
        "ekran", "kırmızı", "ikon", "teşhis", "emir", "moji", "tr"))
    print("-" * 70)
    ihlal = 0
    for s in sonuclar:
        # Tanımlı-ama-kullanılmayan token ihlal SAYILMAZ; fiilen kullanım sayılır.
        kirmizi = len(s["isimli"]) + len(s["tailwind"]) + len(s["error_kullanim"])
        gizli = bool(s["sicak_hex"]) and not kirmizi
        satir_ihlal = (kirmizi or s["ikon"] or s["teshis"]
                       or s["emir"] or s["moji"] or not s["tr_var"])
        if satir_ihlal:
            ihlal += 1
        print("%-24s %-8s %-6s %-7s %-6s %-6s %-5s" % (
            s["ad"],
            "!%d" % kirmizi if kirmizi else ("~gizli" if gizli else "ok"),
            "!%d" % len(s["ikon"]) if s["ikon"] else "ok",
            "!%d" % len(s["teshis"]) if s["teshis"] else "ok",
            "!%d" % len(s["emir"]) if s["emir"] else "ok",
            "!%d" % len(s["moji"]) if s["moji"] else "ok",
            "var" if s["tr_var"] else "YOK"))

    print("\n~gizli = tema bloğunda sıcak hex TANIMLI ama hiçbir öğe kullanmıyor.")
    print("         Ekranda kırmızı görünmez; yine de tasarım sisteminden")
    print("         temizlenmeli — sonraki üretim onu çekebilir.")

    detay = [s for s in sonuclar if s["sicak_hex"] or s["isimli"] or s["tailwind"]
             or s["error_kullanim"] or s["ikon"] or s["teshis"] or s["emir"]
             or s["moji"] or not s["tr_var"]]
    if detay:
        print("\n--- DETAY ---")
        for s in detay:
            print("\n%s" % s["ad"])
            for etiket, deger in (("tanımlı sıcak hex", s["sicak_hex"]),
                                  ("isimli renk", s["isimli"]),
                                  ("tailwind sıcak sınıf", s["tailwind"]),
                                  ("error tokenı KULLANIMI", s["error_kullanim"]),
                                  ("uyarı ikonu", s["ikon"]),
                                  ("teşhis dili", s["teshis"]),
                                  ("emir kipi", s["emir"]),
                                  ("mojibake", s["moji"])):
                if deger:
                    print("   - %s: %s" % (etiket, ", ".join(deger)))
            if not s["tr_var"]:
                print("   - Türkçe harf YOK — metinler İngilizce kalmış olabilir")

    print("\nİhlal içeren ekran: %d / %d" % (ihlal, len(sonuclar)))
    return 1 if ihlal else 0


if __name__ == "__main__":
    sys.exit(main())
