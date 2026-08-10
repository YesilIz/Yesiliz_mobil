"""Sunucu takvim sınırları.

Anlık zamanlar veritabanında UTC tutulur. Buna karşılık lig haftası ve
"bugün" gibi ürün kavramları Türkiye yerel takvimine göre hesaplanır;
Railway/Docker süreç saatinin UTC olması günün ilk üç saatini bir önceki
gün saymamalıdır.
"""

from __future__ import annotations

import datetime as dt
from zoneinfo import ZoneInfo

from app.ayarlar import ayarlar


def yerel_bugun(simdi: dt.datetime | None = None) -> dt.date:
    """Yapılandırılmış iş saat dilimindeki takvim gününü döndürür."""
    an = simdi or dt.datetime.now(dt.timezone.utc)
    if an.tzinfo is None:
        an = an.replace(tzinfo=dt.timezone.utc)
    return an.astimezone(ZoneInfo(ayarlar().saat_dilimi)).date()
