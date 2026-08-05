"""Ortam ayarları — hepsi `YESILIZ_` önekli ortam değişkeninden."""

from functools import lru_cache
from typing import Literal

from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

VARSAYILAN_JWT_ANAHTARI = "gelistirme-anahtari-uretimde-degistir"


class Ayarlar(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="YESILIZ_", env_file=".env")

    # development: yerel geliştirme · test: CI/pytest · production: canlı.
    # Yalnızca production'da güvensiz varsayılanlar reddediliyor (aşağıdaki
    # validator), diğer ikisi birbirinden davranışça ayrılmıyor.
    ortam: Literal["development", "test", "production"] = "development"

    # Virgülle ayrılmış origin listesi (ör. "https://app.example.com,https://admin.example.com").
    # Boş = hiçbir origin'e izin verilmez (kilitli varsayılan); Android
    # istemcisi Origin başlığı göndermediği için bu yalnızca tarayıcı
    # tabanlı istemcileri (uzman/Yeşilay paneli, Faz 3) ilgilendiriyor.
    izinli_kaynaklar: str = ""

    # Varsayılan SQLite: Docker yokken de testler koşabilsin. docker-compose
    # bunu PostgreSQL URL'iyle eziyor (CLAUDE.md §5: sunucu DB'si PostgreSQL).
    veritabani_url: str = "sqlite+pysqlite:///./yesiliz.db"

    jwt_anahtari: str = VARSAYILAN_JWT_ANAHTARI
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

    @property
    def izinli_kaynak_listesi(self) -> list[str]:
        return [k.strip() for k in self.izinli_kaynaklar.split(",") if k.strip()]

    @model_validator(mode="after")
    def uretimde_guvensiz_deger_reddet(self) -> "Ayarlar":
        """Production'da varsayılan/güvensiz değerlerle başlamayı engeller.

        Sessizce yanlış yapılandırılmış bir production yerine gürültülü
        başlangıç hatası tercih ediliyor (CLAUDE.md §1 görev tanımı).
        """
        if self.ortam != "production":
            return self
        if self.jwt_anahtari == VARSAYILAN_JWT_ANAHTARI:
            raise ValueError(
                "Production ortamında varsayılan JWT anahtarı kullanılamaz — "
                "YESILIZ_JWT_ANAHTARI değişkenini güçlü ve rastgele bir "
                "değerle ayarla."
            )
        if self.veritabani_url.startswith("sqlite"):
            raise ValueError(
                "Production ortamında SQLite kullanılamaz — "
                "YESILIZ_VERITABANI_URL değişkenini PostgreSQL bağlantısıyla ayarla."
            )
        return self


@lru_cache
def ayarlar() -> Ayarlar:
    return Ayarlar()
