"""
Denetim kaydı **kapsamı** — yazan her panel ucu iz bırakmalı.

`docs/panel-plani.md §5.8`: "Her personel eylemi yazılır ve silinemez."
Bu bir disiplin sözü ve disiplin unutulur — yeni bir uç eklenirken
`denetle(...)` çağrısını yazmamak, hiçbir testin göremeyeceği sessiz bir
boşluk üretir: eylem çalışır, izi olmaz.

Bu yüzden test davranışa değil **kaynağa** bakıyor: panel router'larındaki
her yazma handler'ının gövdesinde `denetle(` geçiyor mu? Kaba ama doğru
soruyu soruyor ve yanlış negatif vermiyor.

`denetim.py`'nin KDoc'u bu dosyaya işaret ediyordu ama dosya hiç
yazılmamıştı (CLAUDE.md §12 K55'in "yalan yorum" sınıfı). Artık var.
"""

from __future__ import annotations

import inspect

from fastapi.routing import APIRoute

from app.ayarlar import SunucuRolu
from app.main import uygulama_kur

YAZAN_METOTLAR = {"POST", "PUT", "PATCH", "DELETE"}

#: Denetim kaydı yazmayan yazma uçları ve **niçin**.
#:
#: Muafiyet listesi gerekçesiz büyürse testin kendisi anlamsızlaşır; her
#: satır bir karar olmalı.
MUAF = {
    # Oturum akışının kendisi `personel_oturum` tablosunda zaten iz
    # bırakıyor (kim, ne zaman, hangi IP, hangi tarayıcı) ve başarısız
    # giriş `gunluk.py`'ye yazılıyor. Denetim kaydı "personelin bir
    # KULLANICIYA ne yaptığı" içindir; kendi oturumunu açmak o sınıfta
    # değil.
    ("POST", "/oturum/giris"),
    ("POST", "/oturum/totp"),
    ("POST", "/oturum/totp-kur"),
    ("POST", "/oturum/cikis"),
}


def _yazma_yollari(uygulama):
    for yol in uygulama.routes:
        if not isinstance(yol, APIRoute):
            continue
        for metot in yol.methods & YAZAN_METOTLAR:
            yield metot, yol


def test_yazan_her_panel_ucu_denetim_kaydi_yaziyor():
    uygulama = uygulama_kur(SunucuRolu.PANEL)
    eksik = []

    for metot, yol in _yazma_yollari(uygulama):
        if (metot, yol.path) in MUAF:
            continue
        kaynak = inspect.getsource(yol.endpoint)
        if "denetle(" not in kaynak:
            eksik.append(f"{metot} {yol.path}")

    assert not eksik, (
        f"Bu uçlar yazıyor ama denetim kaydı bırakmıyor: {sorted(eksik)}. "
        "`panel/denetim.py`'deki `denetle(...)` çağrısını db.commit()'ten "
        "ÖNCE ekle, ya da gerekçesiyle MUAF listesine yaz."
    )


def test_muafiyet_listesi_olu_satir_tasimiyor():
    """Kaldırılan bir uç muafiyette kalırsa liste yanıltıcı büyür."""
    uygulama = uygulama_kur(SunucuRolu.PANEL)
    mevcut = {(metot, yol.path) for metot, yol in _yazma_yollari(uygulama)}
    olu = MUAF - mevcut
    assert not olu, f"MUAF listesinde artık var olmayan uçlar: {sorted(olu)}"


def test_denetim_kaydi_yazan_kod_silme_ya_da_guncelleme_yapmiyor():
    """
    Kayıt salt eklenebilir olmalı.

    Veritabanı tetikleyicisi bunu PostgreSQL'de zorluyor ama testler
    SQLite'ta koşuyor ve tetikleyici orada kurulmuyor. Kaynak taraması
    üçüncü katman.
    """
    import app.panel.denetim as denetim
    import app.panel.denetim_gorunum as gorunum

    for modul in (denetim, gorunum):
        kaynak = inspect.getsource(modul)
        for yasak in ("delete(DenetimKaydi", "update(DenetimKaydi", "db.delete("):
            assert yasak not in kaynak, (
                f"{modul.__name__} denetim kaydını değiştiriyor: {yasak}"
            )
