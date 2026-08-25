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

**Aynı imaj iki servis koşuyor** (`docs/panel-plani.md §4`, CLAUDE.md §12 K57).
`YESILIZ_ROL` hangi router'ların mount edileceğini belirliyor: `api` cihaz
uçlarını, `panel` yönetim ve YEDAM panelini. Panel uçları API alan adında
**hiç** mount edilmiyor, yani yanlış yazılmış bir yetki kontrolü bile
internete açık olmuyor — yol 404 dönüyor.
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

#: Panel oturum çerezinin ve CSRF jetonunun türetildiği sır. Üretimde
#: `panel` rolünde bu değerle başlanamaz.
VARSAYILAN_PANEL_SIRRI = "gelistirme-panel-sirri-uretimde-degistir"

#: Personel TOTP sırlarını şifreleyen Fernet anahtarı. Üretimde `panel`
#: rolünde bu değerle başlanamaz.
#: Üretmek için: `python3 -c "from cryptography.fernet import Fernet;
#: print(Fernet.generate_key().decode())"`
VARSAYILAN_PANEL_TOTP_ANAHTARI = "Z2VsaXN0aXJtZS1hbmFodGFyaS1kZWdpc3Rpci0wMDA="

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


class SunucuRolu(str, enum.Enum):
    """
    Aynı imajı koşan iki servisi ayıran anahtar (`docs/panel-plani.md §4`).

    `api`: cihazın konuştuğu uçlar — mevcut davranış, varsayılan.
    `panel`: yönetim ve YEDAM danışman paneli (SSR).

    **Varsayılanın `api` olması zorunlu:** canlıdaki mevcut Railway servisi
    `YESILIZ_ROL` tanımlamıyor ve dokunulmadan çalışmaya devam etmeli.

    Tanınmayan bir değer pydantic'te `ValidationError` üretiyor, yani servis
    hiç açılmıyor — yanlış yazılmış bir rol sessizce API'ye düşmüyor.
    """

    API = "api"
    PANEL = "panel"


def _veritabani_url_duzelt(url: str) -> str:
    """
    Sağlayıcının verdiği URL'i SQLAlchemy 2.0 sürücüsüne bağlar.

    Railway/Render `postgresql://…` (bazen `postgres://…`) yazıyor;
    SQLAlchemy 2.0 bu şemada psycopg2'yi arıyor ve `requirements.txt`'de
    psycopg **3** var. Sürücü açıkça belirtilmezse kap açılışta
    `ModuleNotFoundError: psycopg2` ile ölüyor — ve bu hata dağıtım
    günü sağlayıcı panelinde okunması en zor hatalardan biri.

    **Çevresi kırpılıyor.** Değer sağlayıcı panelinde elle yapıştırılıyor
    ve baştaki tek bir boşluk şemayı `" postgresql"` yapıp SQLAlchemy'yi
    `ArgumentError: Could not parse SQLAlchemy URL from given URL string`
    ile düşürüyor. Hata **hangi URL olduğunu söyleyemiyor** (parola
    taşıyor), yani panel servisi 25 Ağu 2026'da tam olarak bu yüzden
    açılamadı ve sebebi yalnızca kabın çıktısından bulunabildi. Kırpma
    şema düzeltmesiyle aynı sınıf: sağlayıcıdan gelen değeri normalize
    etmek. Sondaki boşluk zaten ayrıştırılıyordu, baştaki değil —
    yani kusur sessizce yarım çalışıyordu.
    """
    url = url.strip()
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

    rol: SunucuRolu = Field(
        default=SunucuRolu.API,
        validation_alias=AliasChoices("YESILIZ_ROL", "SERVER_ROLE"),
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

    # --- panel (yalnızca `rol = panel` iken anlamlı) ---
    #
    # Bu beş ayar API servisinde tanımsız kalabilir; `dogrula` onları
    # yalnızca panel rolünde arıyor. Böylece canlıdaki mevcut API servisine
    # hiç dokunmadan panel servisi ayağa kalkabiliyor.

    # Oturum çerezinin ve CSRF jetonunun türetildiği sır.
    panel_oturum_sirri: str = Field(
        default=VARSAYILAN_PANEL_SIRRI,
        validation_alias=AliasChoices("YESILIZ_PANEL_SIRRI", "PANEL_SECRET"),
    )

    # Personel TOTP sırlarını şifreleyen Fernet anahtarı (kullanıcı kararı,
    # `docs/panel-plani.md §1.1`): veritabanı yedeğini okuyan biri ikinci
    # faktörü baypas edememeli.
    panel_totp_anahtari: str = Field(
        default=VARSAYILAN_PANEL_TOTP_ANAHTARI,
        validation_alias=AliasChoices("YESILIZ_PANEL_TOTP_ANAHTARI", "PANEL_TOTP_KEY"),
    )

    # Panelin kendi adresi. `Origin`/`Referer` denetimi ve TOTP kurulum
    # bağlantısındaki `otpauth://` issuer'ı bundan geliyor.
    panel_taban_url: str = Field(
        default="",
        validation_alias=AliasChoices("YESILIZ_PANEL_TABAN", "PANEL_BASE_URL"),
    )

    # Yerel geliştirmede http üzerinden çalışabilmek için gevşetilebiliyor;
    # üretimde `dogrula` False'u reddediyor.
    panel_cerez_guvenli: bool = True

    # İkinci faktör zorunlu mu?
    #
    # **Varsayılan `True` ve öyle kalmalı.** Kapatmak, panelin bütün yetki
    # yüzeyini — hesap askıya alma, silme, KVKK dışa aktarma, puan
    # düzeltmesi, YEDAM danışmanlık kayıtları — tek bir parolanın arkasına
    # koyuyor.
    #
    # Kapatılabilir olmasının sebebi geliştirme evresi: panel Yeşilay'a
    # devredilene kadar ekip her girişte doğrulayıcı uygulama açmak
    # istemiyor. Kapalıyken **sessiz kalmıyor**: her girişte uyarı
    # loglanıyor ve panelin her sayfasında şerit görünüyor. Devretmeden
    # önce açmak tek değişkenlik iş, ama unutulursa ekranın kendisi
    # söylüyor.
    panel_2fa_zorunlu: bool = Field(
        default=True,
        validation_alias=AliasChoices("YESILIZ_PANEL_2FA_ZORUNLU", "PANEL_2FA_REQUIRED"),
    )

    # Geliştirmede ikinci faktörün yerine geçen sabit kod.
    #
    # **Yalnızca geliştirme kolaylığı.** TOTP zaman tabanlı olduğu için sabit
    # bir kod üretemiyor; panele her bakışta doğrulayıcı uygulama açmak
    # yerel döngüyü yavaşlatıyordu. Boş bırakılırsa (varsayılan) böyle bir
    # yol **yok**: özellik açıkça tanımlanmadan var olmuyor.
    #
    # Üretimde tanımlıysa `dogrula` servisi hiç başlatmıyor — uyarı basıp
    # devam etmek, panelin ikinci faktörünü sessizce kapalı bırakırdı.
    panel_sabit_totp: str = Field(
        default="",
        validation_alias=AliasChoices("YESILIZ_PANEL_SABIT_TOTP", "PANEL_FIXED_TOTP"),
    )

    # Vardiya uzunluğu. `personel_oturum.mutlak_bitis` bu değerden **satır
    # yazılırken** hesaplanıyor: ayar sonradan büyütülürse yaşayan oturumlar
    # geriye dönük uzamıyor.
    panel_oturum_mutlak_saat: int = 8
    panel_oturum_hareketsizlik_dakika: int = 30

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
    def panel_mi(self) -> bool:
        return self.rol is SunucuRolu.PANEL

    @property
    def api_mi(self) -> bool:
        return self.rol is SunucuRolu.API

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

        # **Rolden önce sorulan tek kural.** Diğer panel kuralları
        # `panel_mi` dalının içinde, ama bu değişken yanlışlıkla API
        # servisine tanımlanmış olsa bile dağıtımın kırmızıya dönmesi
        # gerekiyor: ikinci faktörü kapatan bir anahtarın üretim ortamında
        # bulunması, hangi serviste olduğundan bağımsız olarak bir kusur.
        if self.panel_sabit_totp:
            sorunlar.append(
                "PANEL_FIXED_TOTP üretimde tanımlanamaz: panelin ikinci "
                "faktörünü devre dışı bırakır. Değişkeni silin."
            )

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

        # `allow_credentials=True` ile joker kaynak kullanmak, gelecekteki
        # tarayıcı panelini herhangi bir sitenin kimlikli isteğine açar.
        if "*" in self.izinli_kaynak_listesi:
            sorunlar.append(
                "Üretimde ALLOWED_ORIGINS joker (*) olamaz; izin verilen "
                "panel kaynaklarını açıkça yazın."
            )

        # Panel rolüne özel kurallar. **API rolüne yeni kural eklenmiyor** —
        # canlıdaki mevcut servisin üretim kapısı bit-birebir aynı kalıyor.
        if self.panel_mi:
            sorunlar.extend(self._panel_sorunlari())

        if sorunlar:
            raise YapilandirmaHatasi(
                "Üretim yapılandırması eksik ya da güvensiz:\n- "
                + "\n- ".join(sorunlar)
            )


    def _panel_sorunlari(self) -> list[str]:
        """
        Panel servisinin üretimde reddettikleri.

        Panel, personel kimliği ve sağlık verisine yakın bir yüzey taşıyor;
        API'den daha sıkı bir kapıyı hak ediyor. Ayrı bir yardımcıya
        çıkarılmasının sebebi [dogrula]'nın okunabilir kalması.
        """
        sorunlar: list[str] = []

        if self.panel_oturum_sirri == VARSAYILAN_PANEL_SIRRI:
            sorunlar.append(
                "Panel oturum sırrı depodaki varsayılan değerde. "
                "PANEL_SECRET'i ortam değişkeni olarak tanımlayın "
                "(`python3 -c \"import secrets;print(secrets.token_urlsafe(32))\"`)."
            )
        elif len(self.panel_oturum_sirri) < ASGARI_ANAHTAR_UZUNLUGU:
            sorunlar.append(
                f"Panel oturum sırrı {ASGARI_ANAHTAR_UZUNLUGU} karakterden kısa "
                f"({len(self.panel_oturum_sirri)})."
            )

        if self.panel_totp_anahtari == VARSAYILAN_PANEL_TOTP_ANAHTARI:
            sorunlar.append(
                "Panel TOTP anahtarı depodaki varsayılan değerde. "
                "PANEL_TOTP_KEY'i tanımlayın (`python3 -c \"from "
                'cryptography.fernet import Fernet;'
                "print(Fernet.generate_key().decode())\"`)."
            )

        # Çerez `Secure` olmazsa oturum jetonu şifresiz bir bağlantıda
        # yakalanabilir; panelin bütün yetki modeli o çereze bağlı.
        if not self.panel_cerez_guvenli:
            sorunlar.append(
                "Üretimde panel çerezi Secure olmalı; "
                "YESILIZ_PANEL_CEREZ_GUVENLI=false yalnızca yerel geliştirme için."
            )

        # `Origin`/`Referer` denetimi bu adrese dayanıyor. Boşsa CSRF
        # savunmasının ikinci ayağı sessizce devre dışı kalırdı.
        if not self.panel_taban_url:
            sorunlar.append(
                "Panel adresi tanımsız; PANEL_BASE_URL panelin kendi HTTPS "
                "adresi olmalı (Origin denetimi ve TOTP kurulumu bunu kullanıyor)."
            )
        elif not self.panel_taban_url.startswith("https://"):
            sorunlar.append("Üretimde PANEL_BASE_URL https:// ile başlamalı.")

        return sorunlar


@lru_cache
def ayarlar() -> Ayarlar:
    a = Ayarlar()
    a.dogrula()
    return a
