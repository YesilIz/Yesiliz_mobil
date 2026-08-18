"""
Şablon katmanı — Jinja2, sunucu tarafında render.

Panel bir SPA değil (`docs/panel-plani.md §3`): işi form ve tablo, yani
SSR'ın güçlü olduğu yer. Tarayıcıda jeton tutulmuyor, ayrı bir Node araç
zinciri ve dağıtım hattı yok.

**HTMX ayrımı tek yerde.** Tam sayfa isteği `taban.html`'i genişleten
şablonu render ediyor; `HX-Request` başlığı varsa yalnızca parça render
ediliyor. Route'lar bu dallanmayı tekrarlamıyor.
"""

from __future__ import annotations

from pathlib import Path

from fastapi import Request, status
from fastapi.responses import HTMLResponse, RedirectResponse, Response
from fastapi.templating import Jinja2Templates

from app.ayarlar import SURUM

SABLON_DIZINI = Path(__file__).resolve().parent / "sablonlar"
STATIK_DIZINI = Path(__file__).resolve().parent / "statik"

sablonlar = Jinja2Templates(directory=str(SABLON_DIZINI))

# Jinja varsayılan olarak yalnızca HTML/XML kaçırıyor; panelin bütün
# şablonları .html, yani autoescape zaten açık. Açıkça yazılıyor ki bir
# gün .txt şablon eklendiğinde sessizce kapanmasın.
sablonlar.env.autoescape = True
sablonlar.env.globals["surum"] = SURUM


def htmx_mi(istek: Request) -> bool:
    return bool(istek.headers.get("hx-request"))


def yanit(
    istek: Request,
    ad: str,
    baglam: dict | None = None,
    *,
    parca: str | None = None,
    durum: int = status.HTTP_200_OK,
) -> HTMLResponse:
    """
    Şablonu render eder; HTMX isteğinde `parca` verilmişse onu kullanır.

    `baglam`'a her zaman `istek`, oturumdaki personel ve CSRF jetonu
    ekleniyor — şablonlar bunları her sayfada istiyor ve route başına
    tekrarlamanın faydası yok.
    """
    tam = dict(baglam or {})
    tam["request"] = istek
    tam.setdefault("csrf", getattr(istek.state, "csrf", ""))
    tam.setdefault("personel", getattr(istek.state, "personel", None))

    secilen = parca if (parca and htmx_mi(istek)) else ad
    return sablonlar.TemplateResponse(request=istek, name=secilen, context=tam, status_code=durum)


def yonlendir(istek: Request, yol: str) -> Response:
    """
    Form başarısından sonra: Post/Redirect/Get.

    HTMX bir 303'ü kendi içinde izleyip hedef sayfayı parçanın yerine
    gömerdi; `HX-Redirect` tam sayfa yönlendirme yaptırıyor.
    """
    if htmx_mi(istek):
        return Response(status_code=status.HTTP_204_NO_CONTENT, headers={"HX-Redirect": yol})
    return RedirectResponse(yol, status_code=status.HTTP_303_SEE_OTHER)
