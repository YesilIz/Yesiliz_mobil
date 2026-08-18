"""
Panel servisinin şema sürümü kapısı (`docs/panel-plani.md §4`).

**Panel migration koşturmuyor.** İki servis aynı imajı ve aynı veritabanını
paylaşıyor; ikisi birden `alembic upgrade head` çağırırsa aynı anda aynı
tabloları oluşturmaya kalkarlar. Migration'ın tek sahibi API servisi
(`baslat.sh`), panel yalnızca **doğruluyor**.

Doğrulama `alembic`'in Python API'siyle yapılıyor, alt süreç çağırmadan:
panelin veritabanı kullanıcısına yazma yetkisi vermeye gerek yok.

**Yeniden deneme zorunlu.** Dağıtım anında iki servis birlikte kalkıyor;
panel, API'nin migration'ını bitirmesini beklemezse ilk saniyede uyuşmazlık
görüp crash-loop'a girer. Pencere dolduğunda [SemaUyumsuzlugu] fırlıyor,
`baslat.sh`'ın `set -e`'si uvicorn'u hiç başlatmıyor, sağlık kontrolü
düşüyor ve hatalı sürüm devreye alınmıyor — `alembic upgrade` düştüğünde
API tarafında olan şeyin birebir aynısı.
"""

from __future__ import annotations

import logging
import time
from pathlib import Path

from alembic.config import Config
from alembic.runtime.migration import MigrationContext
from alembic.script import ScriptDirectory

from app.veritabani import motor

gunluk = logging.getLogger(__name__)

#: `alembic.ini` depo içinde `backend/` altında; modül konumundan
#: türetiliyor ki çalışma dizininden bağımsız olsun.
ALEMBIC_INI = Path(__file__).resolve().parent.parent / "alembic.ini"

VARSAYILAN_DENEME = 10
VARSAYILAN_BEKLEME_SN = 6


class SemaUyumsuzlugu(RuntimeError):
    """Veritabanı şeması bu sürümün beklediği revizyonda değil."""


def beklenen_revizyon() -> str:
    """Kod tabanındaki en son alembic revizyonu (head)."""
    betikler = ScriptDirectory.from_config(Config(str(ALEMBIC_INI)))
    return betikler.get_current_head() or ""


def mevcut_revizyon() -> str | None:
    """Veritabanının damgalı revizyonu; hiç migration koşmamışsa `None`."""
    with motor.connect() as baglanti:
        return MigrationContext.configure(baglanti).get_current_revision()


def semayi_dogrula(
    deneme: int = VARSAYILAN_DENEME,
    bekleme_sn: int = VARSAYILAN_BEKLEME_SN,
) -> str:
    """
    Şema head'e ulaşana kadar bekler; ulaşmazsa [SemaUyumsuzlugu] fırlatır.

    Dönen değer doğrulanan revizyon. Veritabanına hiç ulaşılamaması da
    uyuşmazlık sayılıyor: paneli veritabanısız açmanın anlamı yok, her
    ekranı veritabanına bağlı.
    """
    beklenen = beklenen_revizyon()
    son_sorun = "bilinmiyor"

    for tur in range(1, deneme + 1):
        try:
            mevcut = mevcut_revizyon()
        except Exception as hata:  # bağlantı henüz açılmamış olabilir
            # Gerekçe metni loglanmıyor: bağlantı istisnaları veritabanı
            # URL'ini parolasıyla taşıyabiliyor (`main.py` ile aynı kural).
            son_sorun = f"veritabanına ulaşılamadı ({type(hata).__name__})"
        else:
            if mevcut == beklenen:
                gunluk.info(
                    "Şema doğrulandı",
                    extra={"revizyon": beklenen, "tur": tur},
                )
                return beklenen
            son_sorun = f"şema {mevcut or 'damgasız'}, beklenen {beklenen}"

        if tur < deneme:
            gunluk.warning(
                "Şema henüz hazır değil, bekleniyor: %s (tur %s/%s)",
                son_sorun,
                tur,
                deneme,
            )
            time.sleep(bekleme_sn)

    raise SemaUyumsuzlugu(
        f"Panel servisi başlatılamadı — {son_sorun}. "
        "Migration'ları API servisi koşturuyor; önce onun dağıtımının "
        "tamamlanması gerekiyor (docs/panel-plani.md §4)."
    )


if __name__ == "__main__":  # pragma: no cover - `baslat.sh` çağırıyor
    semayi_dogrula()
