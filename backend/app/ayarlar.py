"""Ortam ayarları — hepsi `YESILIZ_` önekli ortam değişkeninden."""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Ayarlar(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="YESILIZ_", env_file=".env")

    # Varsayılan SQLite: Docker yokken de testler koşabilsin. docker-compose
    # bunu PostgreSQL URL'iyle eziyor (CLAUDE.md §5: sunucu DB'si PostgreSQL).
    veritabani_url: str = "sqlite+pysqlite:///./yesiliz.db"

    jwt_anahtari: str = "gelistirme-anahtari-uretimde-degistir"
    jwt_algoritma: str = "HS256"

    # Erişim jetonu kısa, yenileme jetonu uzun: telefon çalınırsa erişim
    # penceresi dar olsun, ama kullanıcı her gün yeniden giriş yapmasın.
    erisim_dakika: int = 30
    yenileme_gun: int = 30

    # Eşleştirme kodu 10 dakika — tasarım 21'deki metinle birebir.
    eslestirme_kodu_dakika: int = 10

    # Şifre sıfırlama bağlantısı 1 saat.
    sifirlama_saat: int = 1

    # Lig grubu üst sınırı ve yükselme/düşme sayıları (CLAUDE.md §12 K1).
    lig_grup_boyu: int = 10
    lig_yukselen: int = 3
    lig_dusen: int = 3


@lru_cache
def ayarlar() -> Ayarlar:
    return Ayarlar()
