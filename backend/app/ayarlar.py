"""
Ortam ayarları.

Üç çalışma ortamı var — `development` · `test` · `production` — ve ayrım
tek bir değişkenden (`ENVIRONMENT`) okunuyor. Ayarların tamamı ortam
değişkeninden geliyor; kodda ne veritabanı parolası ne de üretimde
kullanılabilir bir gizli anahtar duruyor.

**İki isim de kabul ediliyor.** Depo `YESILIZ_` önekli Türkçe adlarla
başladı (`YESILIZ_VERITABANI_URL`); dağıtım sağlayıcıları ise standart
adları kendileri enjekte ediyor — Railway PostgreSQL eklendiği anda
`DATABASE_URL`'i yazıyor ve bunun adı değiştirilemiyor. Her ayar bu yüzden
iki takma adla okunuyor, önek verilen kazanıyor.

**Üretimde açılış doğrulaması var** ([Ayarlar.dogrula]): varsayılan JWT
anahtarı, kısa anahtar ya da SQLite ile başlamak reddediliyor. Sessizce
başlayan bir üretim sunucusu, imzası herkesçe bilinen jetonlar dağıtırdı.

Doğrulama [ayarlar] içinde çağrılıyor, `Ayarlar` kurucusunda değil: ayarları
okuyan **her** yol (uvicorn, alembic, worker) kapıdan geçiyor ama testler
geçersiz bir yapılandırmayı kurup davranışını sınayabiliyor.
"""

from __future__ import annotations

import enum
from functools import lru_cache
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from pydantic import AliasChoices, Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

#: Depoda duran geliştirme anahtarı. Üretimde bu değerle başlanamaz.
VARSAYILAN_JWT_ANAHTARI = "gelistirme-anahtari-uretimde-degistir"

#: 32 bayt (`secrets.token_urlsafe(32)` ≈ 43 karakter) altındaki bir HS256
#: anahtarı kaba kuvvete açık; üretimde asgari sınır.
ASGARI_ANAHTAR_UZUNLUGU = 32

SURUM = "0.1.0"


class YapilandirmaHatasi(RuntimeError):
    """Ayarlar üretimde kullanılamayacak durumda — uygulama başlamamalı."""


class Ortam(str, enum.Enum):
    """
    development: yerel geliştirme · test: CI/pytest · production: canlı.

    Yalnızca `production` güvensiz varsayılanları reddediyor; diğer ikisi
    birbirinden davranışça ayrılmıyor, ayrım okunabilirlik için.
    """

    GELISTIRME = "development"
    TEST = "test"
    URETIM = "production"


def _veritabani_url_duzelt(url: str) -> str:
    """
    Sağlayıcının verdiği URL'i SQLAlchemy 2.0 sürücüsüne bağlar.

    Railway/Render `postgresql://…` (bazen `postgres://…`) yazıyor;
    SQLAlchemy 2.0 bu şemada psycopg2'yi arıyor ve `requirements.txt`'de
    psycopg **3** var. Sürücü açıkça belirtilmezse kap açılışta
    `ModuleNotFoundError: psycopg2` ile ölüyor — ve bu hata dağıtım
    günü sağlayıcı panelinde okunması en zor hatalardan biri.
    """
    for onek in ("postgresql+", "postgres+"):
        if url.startswith(onek):
            return url
    if url.startswith("postgresql://"):
        return "postgresql+psycopg://" + url[len("postgresql://") :]
    if url.startswith("postgres://"):
        return "postgresql+psycopg://" + url[len("postgres://") :]
    return url


class Ayarlar(BaseSettings):
    # `extra="ignore"`: sağlayıcı kaba kendi değişkenlerini de enjekte
    # ediyor (PORT, RAILWAY_*). Bilinmeyen bir değişken yüzünden servis
    # başlamamalı.
    model_config = SettingsConfigDict(
        env_prefix="YESILIZ_",
        env_file=".env",
        extra="ignore",
        case_sensitive=False,
    )

    ortam: Ortam = Field(
        default=Ortam.GELISTIRME,
        validation_alias=AliasChoices("YESILIZ_ORTAM", "ENVIRONMENT"),
    )

    # Varsayılan SQLite: Docker yokken de testler koşabilsin. docker-compose
    # bunu PostgreSQL URL'iyle eziyor (CLAUDE.md §5: sunucu DB'si PostgreSQL).
    # Üretimde SQLite `dogrula` tarafından reddediliyor.
    veritabani_url: str = Field(
        default="sqlite+pysqlite:///./yesiliz.db",
        validation_alias=AliasChoices("YESILIZ_VERITABANI_URL", "DATABASE_URL"),
    )

    jwt_anahtari: str = Field(
        default=VARSAYILAN_JWT_ANAHTARI,
        validation_alias=AliasChoices("YESILIZ_JWT_ANAHTARI", "JWT_SECRET"),
    )
    jwt_algoritma: str = "HS256"

    # Erişim jetonu kısa, yenileme jetonu uzun: telefon çalınırsa erişim
    # penceresi dar olsun, ama kullanıcı her gün yeniden giriş yapmasın.
    erisim_dakika: int = Field(
        default=30,
        validation_alias=AliasChoices(
            "YESILIZ_ERISIM_DAKIKA", "ACCESS_TOKEN_EXPIRE_MINUTES"
        ),
    )
    yenileme_gun: int = 30

    # Virgülle ayrılmış origin listesi (ör. "https://app.example.com").
    # Boş = hiçbir origin'e izin verilmez (kilitli varsayılan); Android
    # istemcisi Origin başlığı göndermediği için bu yalnızca tarayıcı
    # tabanlı istemcileri (uzman/Yeşilay paneli, Faz 3) ilgilendiriyor.
    #
    # Düz `str` olmasının sebebi pydantic-settings'in `list` alanları ortam
    # değişkeninde JSON sanıp çözümlemeye çalışması — `a.com,b.com` yazınca
    # açılışta JSONDecodeError veriyor.
    izinli_kaynaklar: str = Field(
        default="",
        validation_alias=AliasChoices("YESILIZ_IZINLI_KAYNAKLAR", "ALLOWED_ORIGINS"),
    )

    # Eşleştirme kodu 10 dakika — tasarım 21'deki metinle birebir.
    eslestirme_kodu_dakika: int = 10

    # Şifre sıfırlama bağlantısı 1 saat.
    sifirlama_saat: int = 1

    # Takvim tabanlı ürün kuralları (haftalık lig, ebeveynin "bugün"
    # penceresi) sunucunun çoğunlukla UTC olan işletim sistemi saat dilimine
    # bırakılamaz. Yarışma ve hedef kitle Türkiye olduğu için ortak iş günü
    # varsayılan olarak İstanbul takvimidir; dağıtımda gerektiğinde ezilebilir.
    saat_dilimi: str = "Europe/Istanbul"

    # Lig grubu üst sınırı ve yükselme/düşme sayıları (CLAUDE.md §12 K1).
    lig_grup_boyu: int = 10
    lig_yukselen: int = 3
    lig_dusen: int = 3

    # Sürücü düzeltmesi okuma anında yapılıyor: alembic, uvicorn ve testler
    # ayarları aynı yerden alsın, üçünde ayrı ayrı düzeltilmesin.
    _url_duzelt = field_validator("veritabani_url")(
        staticmethod(_veritabani_url_duzelt)
    )

    @field_validator("saat_dilimi")
    @classmethod
    def _saat_dilimini_dogrula(cls, deger: str) -> str:
        try:
            ZoneInfo(deger)
        except ZoneInfoNotFoundError as hata:
            raise ValueError("Geçerli bir IANA saat dilimi olmalı") from hata
        return deger

    @property
    def uretim_mi(self) -> bool:
        return self.ortam is Ortam.URETIM

    @property
    def izinli_kaynak_listesi(self) -> list[str]:
        """CORS için izinli kaynak listesi."""
        return [k.strip() for k in self.izinli_kaynaklar.split(",") if k.strip()]

    def dogrula(self) -> None:
        """
        Üretimde güvensiz yapılandırmayı reddeder.

        Uyarı basıp devam etmiyor: uyarı sağlayıcı log akışında kaybolur ve
        sunucu haftalarca varsayılan anahtarla çalışır. Açık hata verip
        başlamamak, dağıtımın o anda kırmızıya dönmesi demek.
        """
        if not self.uretim_mi:
            return

        sorunlar: list[str] = []

        if self.jwt_anahtari == VARSAYILAN_JWT_ANAHTARI:
            sorunlar.append(
                "JWT anahtarı depodaki varsayılan değerde. "
                "JWT_SECRET'i ortam değişkeni olarak tanımlayın "
                "(`python3 -c \"import secrets;print(secrets.token_urlsafe(32))\"`)."
            )
        elif len(self.jwt_anahtari) < ASGARI_ANAHTAR_UZUNLUGU:
            # Anahtarın kendisi mesaja GİRMİYOR — hata mesajı da bir log.
            sorunlar.append(
                f"JWT anahtarı {ASGARI_ANAHTAR_UZUNLUGU} karakterden kısa "
                f"({len(self.jwt_anahtari)})."
            )

        if self.veritabani_url.startswith("sqlite"):
            sorunlar.append(
                "Üretimde SQLite kullanılamaz; DATABASE_URL bir PostgreSQL "
                "adresi olmalı (CLAUDE.md §5)."
            )

        if sorunlar:
            raise YapilandirmaHatasi(
                "Üretim yapılandırması eksik ya da güvensiz:\n- "
                + "\n- ".join(sorunlar)
            )


@lru_cache
def ayarlar() -> Ayarlar:
    a = Ayarlar()
    a.dogrula()
    return a
