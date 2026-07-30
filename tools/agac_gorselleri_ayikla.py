"""6 evrelik ağaç setini (tek görsel, 3x2 ızgara) tekil şeffaf PNG'lere ayırır.

Önceki set (`tools/agac_gorselleri_hazirla.py`) Stitch'in ürettiği tek-konu
ekran görüntülerindeki damalı-şeffaflık sorununu çözüyordu. Bu görsel farklı
bir kaynaktan geliyor: her hücrede kendi gökyüzü/tepe/çit zemini olan
kart illüstrasyonu var — kaldırılması gereken şey damalı desen değil, gerçek
bir arka plan sahnesi. Bu yüzden ayrı bir script: hücreleri kesip, her
hücrede GrabCut (OpenCV) ile ağaç/dal/yaprak/gövdeyi zeminden ayırıyor.

Kullanım:
    python3 tools/agac_gorselleri_ayikla.py <kaynak.png>

Çıktı: `app/src/main/res/drawable-nodpi/agac_evreN_<ad>.png` (768x768, RGBA).
"""

import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image
from scipy import ndimage

CIKTI_KLASORU = Path("app/src/main/res/drawable-nodpi")
HEDEF_BOYUT = 768
UPSCALE = 3

# Izgara sırasına göre (soldan sağa, yukarıdan aşağıya) evre adları.
# Bu set büyüme yoğunluğu değil mevsimsel/meyve teması ile ilerliyor —
# CLAUDE.md K2/A7'de not düşüldü.
HUCRELER = [
    "evre1_cicek",     # r0c0 — beyaz çiçek, kuş
    "evre2_narenciye",  # r0c1 — turuncu meyve, tepeler
    "evre3_pembe",      # r0c2 — pembe çiçek
    "evre4_sari",       # r1c0 — sarı meyve, çit
    "evre5_erik",       # r1c1 — mor erik
    "evre6_elma",       # r1c2 — kırmızı elma
]


def hucreyi_ayikla(hucre_rgb: np.ndarray) -> np.ndarray:
    """Bir hücrenin RGBA (şeffaf zeminli) halini döndürür."""
    H, W = hucre_rgb.shape[:2]
    bgr = cv2.cvtColor(hucre_rgb, cv2.COLOR_RGB2BGR)
    hsv = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)
    sat = hsv[:, :, 1].astype(int)
    val = hsv[:, :, 2].astype(int)

    mask = np.full((H, W), cv2.GC_PR_BGD, dtype=np.uint8)
    olasi_on_plan = (sat > 35) | (val < 90)
    mask[olasi_on_plan] = cv2.GC_PR_FGD
    kenar = 4
    mask[:kenar, :] = cv2.GC_BGD
    mask[-kenar:, :] = cv2.GC_BGD
    mask[:, :kenar] = cv2.GC_BGD
    mask[:, -kenar:] = cv2.GC_BGD

    bgd_model = np.zeros((1, 65), np.float64)
    fgd_model = np.zeros((1, 65), np.float64)
    cv2.grabCut(bgr, mask, None, bgd_model, fgd_model, 6, cv2.GC_INIT_WITH_MASK)

    on_plan = np.where((mask == cv2.GC_FGD) | (mask == cv2.GC_PR_FGD), 1, 0).astype("uint8")
    on_plan = ndimage.binary_fill_holes(on_plan).astype("uint8")

    alpha = (on_plan * 255).astype("uint8")
    kernel = np.ones((3, 3), np.uint8)
    alpha = cv2.morphologyEx(alpha, cv2.MORPH_CLOSE, kernel, iterations=2)
    alpha = cv2.morphologyEx(alpha, cv2.MORPH_OPEN, kernel, iterations=1)
    alpha = cv2.GaussianBlur(alpha, (3, 3), 0)

    return np.dstack([hucre_rgb, alpha])


def kareye_otur(rgba: np.ndarray, boyut: int) -> Image.Image:
    """Şeffaf piksellerin bounding box'ını bulup kare tuvale ortalar."""
    alpha = rgba[:, :, 3]
    ys, xs = np.where(alpha > 10)
    top, bottom, left, right = ys.min(), ys.max(), xs.min(), xs.max()
    kesit = Image.fromarray(rgba[top : bottom + 1, left : right + 1], "RGBA")

    pay = int(boyut * 0.04)
    hedef = boyut - 2 * pay
    olcek = min(hedef / kesit.width, hedef / kesit.height)
    yeni_boyut = (int(kesit.width * olcek), int(kesit.height * olcek))
    kesit = kesit.resize(yeni_boyut, Image.LANCZOS)

    tuval = Image.new("RGBA", (boyut, boyut), (0, 0, 0, 0))
    ofset = ((boyut - kesit.width) // 2, boyut - pay - kesit.height)
    tuval.paste(kesit, ofset, kesit)
    return tuval


def main() -> None:
    kaynak = Path(sys.argv[1])
    img = Image.open(kaynak).convert("RGB")
    w, h = img.size
    cw, ch = w // 3, h // 2

    CIKTI_KLASORU.mkdir(parents=True, exist_ok=True)

    for i, ad in enumerate(HUCRELER):
        r, c = divmod(i, 3)
        hucre = img.crop((c * cw + 6, r * ch + 6, (c + 1) * cw - 6, (r + 1) * ch - 6))
        hucre = hucre.resize((hucre.width * UPSCALE, hucre.height * UPSCALE), Image.LANCZOS)
        rgba = hucreyi_ayikla(np.array(hucre))
        sonuc = kareye_otur(rgba, HEDEF_BOYUT)
        cikti_yolu = CIKTI_KLASORU / f"agac_{ad}.png"
        sonuc.save(cikti_yolu)
        print(f"{ad} -> {cikti_yolu}")


if __name__ == "__main__":
    main()
