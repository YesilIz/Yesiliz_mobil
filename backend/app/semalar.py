"""İstek/yanıt modelleri.

Alan adları **cihazdaki Kotlin modelleriyle** aynı yazımda tutuluyor
(`camelCase` değil `snake_case` değil — Kotlin tarafındaki isimlerin
birebiri). Böylece Retrofit DTO'sunda `@SerialName` haritalaması
gerekmiyor ve alan eklerken tek yerde unutma riski azalıyor.
"""

from __future__ import annotations

import datetime as dt
from typing import Literal

from pydantic import BaseModel, EmailStr, Field, field_validator, model_validator


# --- hesap ---

class KayitIstek(BaseModel):
    ad: str = Field(min_length=2, max_length=120)
    eposta: EmailStr
    sifre: str = Field(min_length=8, max_length=128)

    @field_validator("ad", mode="before")
    @classmethod
    def adi_temizle(cls, deger: object) -> object:
        return deger.strip() if isinstance(deger, str) else deger


class GirisIstek(BaseModel):
    eposta: EmailStr
    sifre: str = Field(min_length=1, max_length=128)


class YenileIstek(BaseModel):
    yenileme: str = Field(min_length=1, max_length=256)


class SifreSifirlaIstek(BaseModel):
    eposta: EmailStr


class SifreYenileIstek(BaseModel):
    jeton: str = Field(min_length=1, max_length=256)
    yeniSifre: str = Field(min_length=8, max_length=128)


class ProfilIstek(BaseModel):
    """
    Profil güncelleme — **e-posta yok**.

    E-posta giriş kimliği; doğrulama e-postası gönderilemediği sürece
    (A15, sağlayıcı bağlı değil) değiştirilmesi kullanıcıyı ulaşılamaz bir
    adrese kilitler. Alan sunucuda da istemcide de kapalı.

    ## Kısmi güncelleme

    Uç `PATCH`: gövdede **olmayan alan değişmez**. `avatar` eskiden
    `default=""` taşıyordu ve uç bunu koşulsuz atıyordu, yani avatarını
    göndermeyen bir istek (ör. yalnızca şifre değiştiren bir istemci)
    kayıtlı avatarı **siliyordu** — `PATCH` adı taşıyan bir uç `PUT` gibi
    davranıyordu (4. tarama T4-005). `ad` da aynı sınıftaydı: zorunlu
    olduğu için sadece şifre değiştirmek isteyen istemci onu da göndermek
    zorundaydı.

    `None` ile "gönderilmedi" bilerek aynı anlama geliyor; `model_fields_set`
    ayrımına gerek duyulmadı çünkü bu iki alanın anlamlı bir `null` değeri
    yok. Korunması gereken tek ayrım "gönderilmedi" ile **"boş gönderildi"**
    arasında ve onu `None` zaten taşıyor: `{"avatar": ""}` avatarı bilerek
    temizler, `avatar` alanının hiç bulunmaması ona dokunmaz.
    """

    ad: str | None = Field(default=None, min_length=2, max_length=120)
    avatar: str | None = Field(default=None, max_length=32)
    # Yeni şifre verilirse mevcut şifre ZORUNLU: çalınmış bir telefon
    # yalnızca erişim jetonuyla şifreyi değiştirebilmemeli.
    mevcutSifre: str | None = Field(default=None, max_length=128)
    yeniSifre: str | None = Field(default=None, min_length=8, max_length=128)

    @field_validator("ad", "avatar", mode="before")
    @classmethod
    def bosluklari_temizle(cls, deger: object) -> object:
        return deger.strip() if isinstance(deger, str) else deger


class Profil(BaseModel):
    ad: str
    eposta: str
    avatar: str


class Jetonlar(BaseModel):
    erisim: str
    yenileme: str
    ad: str
    eposta: str
    avatar: str = ""


# --- özet senkronu ---

class GunlukOzetDto(BaseModel):
    tarih: dt.date
    socialDk: float = Field(default=0.0, ge=0, le=1440, allow_inf_nan=False)
    entertainmentDk: float = Field(default=0.0, ge=0, le=1440, allow_inf_nan=False)
    gamesDk: float = Field(default=0.0, ge=0, le=1440, allow_inf_nan=False)
    communicationDk: float = Field(default=0.0, ge=0, le=1440, allow_inf_nan=False)
    productivityDk: float = Field(default=0.0, ge=0, le=1440, allow_inf_nan=False)
    educationDk: float = Field(default=0.0, ge=0, le=1440, allow_inf_nan=False)
    healthDk: float = Field(default=0.0, ge=0, le=1440, allow_inf_nan=False)
    otherDk: float = Field(default=0.0, ge=0, le=1440, allow_inf_nan=False)
    # Risk motorunun örüntü girdileri cihaz modelinde 0..1 oranıdır.
    gece: float = Field(default=0.0, ge=0, le=1, allow_inf_nan=False)
    oturum: float = Field(default=0.0, ge=0, le=1, allow_inf_nan=False)
    acilis: float = Field(default=0.0, ge=0, le=1, allow_inf_nan=False)
    sabah: float = Field(default=0.0, ge=0, le=1, allow_inf_nan=False)
    uykuOncesi: float = Field(default=0.0, ge=0, le=1, allow_inf_nan=False)
    yerDegis: float = Field(default=0.0, ge=0, le=1, allow_inf_nan=False)
    adim: float = Field(default=0.0, ge=0, le=500_000, allow_inf_nan=False)
    sporDk: float = Field(default=0.0, ge=0, le=1440, allow_inf_nan=False)
    S: float = Field(default=0.0, ge=0, le=5000, allow_inf_nan=False)

    @model_validator(mode="after")
    def toplam_ekran_bir_gunu_asamaz(self) -> GunlukOzetDto:
        toplam = sum(
            (
                self.socialDk,
                self.entertainmentDk,
                self.gamesDk,
                self.communicationDk,
                self.productivityDk,
                self.educationDk,
                self.healthDk,
                self.otherDk,
            )
        )
        if toplam > 1440.001:
            raise ValueError("Kategori dakikaları toplamı bir günü aşamaz")
        return self


class RiskSonucDto(BaseModel):
    tarih: dt.date
    S: float = Field(ge=0, le=5000, allow_inf_nan=False)
    R: float = Field(ge=0, le=100, allow_inf_nan=False)
    gosterilenBant: Literal["AZ", "ORTA", "COK"]
    trend: Literal["BELIRSIZ", "IYILESIYOR", "SABIT", "KOTULESIYOR"]
    # Eski istemciler alanı göndermez; onların daha önceki davranışı
    # hesaplanmış kabul edilerek sözleşme geriye uyumlu kalır.
    veriDurumu: Literal["YETERSIZ", "HESAPLANDI", "ESKI"] = "HESAPLANDI"


class SenkronIstek(BaseModel):
    # Cihaz worker'ı yalnızca son yedi günü yollar. Liste tavanı,
    # kimliği doğrulanmış olsa bile tek istekle kaynak tüketimini sınırlar.
    ozetler: list[GunlukOzetDto] = Field(default_factory=list, max_length=7)
    riskler: list[RiskSonucDto] = Field(default_factory=list, max_length=7)
    # Cihazın o günkü toplam puanı — lig sıralaması bunun üzerinden.
    haftalikPuan: int = Field(default=0, ge=0, le=2000)
    # "Ligde görün" tercihi. None = değiştirme (eski istemciler).
    ligdeGorunur: bool | None = None
    # Cihazın TOPLAM puanı — **bilgilendirici, otorite değil** (U7).
    #
    # Panelin puan düzeltme formu "mevcut toplam · delta · önizleme"
    # gösteriyor ve o toplamı sunucu başka türlü bilemiyor: skoru da
    # puanı da cihaz hesaplıyor (K17 ailesi). A13 aynen geçerli — sayı
    # istemci beyanlı ve form bunu ekranda yazıyor.
    #
    # `None` = eski istemci; alanı sıfırla doldurmak "puanı yok" demek
    # olurdu ve panel bunu yanlış okurdu (§9).
    toplamPuan: int | None = Field(default=None, ge=0, le=1_000_000)


class SenkronYanit(BaseModel):
    ozetSayisi: int
    riskSayisi: int


# --- lig ---

class LigSatiriDto(BaseModel):
    sira: int
    isim: str
    puan: int
    benMiyim: bool = False
    arkadasMi: bool = False


class LigYanit(BaseModel):
    ligAdi: str
    kalanGun: int
    yukselen: int
    dusen: int
    siralama: list[LigSatiriDto]
    katildiMi: bool
    # Kullanıcı başkalarının sıralamasında görünüyor mu. False ise ekran
    # bunu söylüyor; sessizce gizlenmek, tercihin çalışıp çalışmadığını
    # belirsiz bırakırdı.
    gorunuyorMuyum: bool = True


class ArkadasDto(BaseModel):
    isim: str
    davetBekliyorMu: bool


class DavetIstek(BaseModel):
    kod: str = Field(min_length=1, max_length=16)


class KodYanit(BaseModel):
    kod: str


class LigKatilIstek(BaseModel):
    """Yeni üyeliğin gizlilik tercihi; eski istemcide gövde olmayabilir."""

    gorunur: bool = True


class LigGorunurlukIstek(BaseModel):
    gorunur: bool


# --- ödül ---

class OdulSecimIstek(BaseModel):
    kademeId: str = Field(min_length=1, max_length=40)
    secenekId: str = Field(min_length=1, max_length=40)


class OdulSecimiDto(BaseModel):
    kademeId: str
    secenekId: str


# --- ebeveyn ---

class EslestirmeKoduIstek(BaseModel):
    # `ebeveyn` | `cocuk`
    rol: str = Field(min_length=1, max_length=10)


class EslestirIstek(BaseModel):
    kod: str = Field(min_length=1, max_length=16)
    rol: str = Field(min_length=1, max_length=10)


class EbeveynOzetYanit(BaseModel):
    """
    Ebeveyne giden **özet** — ham veri değil (`docs/spec-veri.md`).

    Uygulama adı, oturum bilgisi ve saat düzeyinde döküm YOK; yalnızca
    günlük toplam ekran süresi, bant ve trend.
    """

    cocukAdi: str
    gunlukTarihler: list[dt.date]
    gunlukDakikalar: list[int]
    ortalamaDk: int
    bant: str | None
    trend: str | None
    sonSinyalSaatOnce: int | None


# --- YEDAM ---
#
# `docs/panel-plani.md §6` · rıza metni `docs/yedam-riza-metni-v1.md`.
# Metnin sürümü isteğe **cihaz tarafından** konuyor: kullanıcı hangi metni
# okuduysa onun sürümü kaydedilmeli. Sunucu kendi bildiği sürümü yazsaydı,
# eski bir APK yeni metne rıza vermiş gibi görünürdü.


class YedamBasvuruIstek(BaseModel):
    metinSurumu: str = Field(min_length=1, max_length=20)
    #: Kullanıcının onayladığı kapsam anahtarları.
    kapsam: list[str] = Field(min_length=1, max_length=10)
    iletisimTuru: Literal["telefon", "eposta"]
    iletisimDegeri: str = Field(min_length=3, max_length=320)
    kullaniciNotu: str = Field(default="", max_length=1000)
    #: Beyan edilen yaş. `None` ise sunucu **reşit saymıyor** (K26, §9).
    beyanYas: int | None = Field(default=None, ge=5, le=120)

    @field_validator("iletisimDegeri", mode="before")
    @classmethod
    def temizle(cls, deger: object) -> object:
        return deger.strip() if isinstance(deger, str) else deger


class YedamBasvuruYanit(BaseModel):
    basvuruId: int
    durum: str
    rizaSonGecerlilik: dt.datetime
    #: 18 altı ve bağlı ebeveyn varsa onay bekleniyor.
    ebeveynOnayiBekleniyor: bool = False


class YedamDurumYanit(BaseModel):
    """`GET /yedam/basvurum` — cihazın durum takibi.

    Danışmanın adı **yok**: kullanıcıya "sana X atandı" demek, danışmanın
    kimliğini başvuru daha görüşmeye dönüşmeden açardı.
    """

    basvuruId: int | None = None
    durum: str = ""
    olusturma: dt.datetime | None = None
    rizaSonGecerlilik: dt.datetime | None = None
    ebeveynOnayiBekleniyor: bool = False
    #: Görüşme kayıtları — tür ve zaman, içerik yok.
    temaslar: list["YedamTemasDto"] = Field(default_factory=list)


class YedamTemasDto(BaseModel):
    tur: str
    zaman: dt.datetime


class ErisimKaydiDto(BaseModel):
    """"14 Ağustos 10:32'de bakıldı." Danışmanın adı taşınmıyor."""

    zaman: dt.datetime
    kapsam: str


class SurukleyiciDto(BaseModel):
    tarih: dt.date
    faktor: str = Field(min_length=1, max_length=40)
    #: 1 = en çok katkı yapan. Cihazdaki `surukleyiciler` listesinin sırası;
    #: katkı DEĞERİ taşınmıyor (bkz. `modeller.RiskSurukleyici.sira`).
    sira: int = Field(ge=1, le=10)


class SurukleyiciIstek(BaseModel):
    """
    `POST /yedam/surukleyici` — **`/ozet`'ten AYRI uç** (U1).

    `/ozet` "Yeşilay ile paylaşım" rızasına bağlı (K19) ve o kapalıysa
    hiç çağrılmıyor; sürükleyicileri oraya eklemek, özelliği tam da
    korumak istediği kullanıcıda sessizce ölü bırakırdı.
    """

    kayitlar: list[SurukleyiciDto] = Field(min_length=1, max_length=400)


class AnketYanitDto(BaseModel):
    tarih: dt.date
    #: 10 maddenin Likert puanları. Ölçek birebir korunuyor (K12).
    maddePuanlari: list[int] = Field(min_length=10, max_length=10)
    hamPuan: float
    rS: float

    @field_validator("maddePuanlari")
    @classmethod
    def araligi_dogrula(cls, deger: list[int]) -> list[int]:
        if any(p < 1 or p > 6 for p in deger):
            raise ValueError("SAS-SV maddeleri 1-6 aralığında olmalı")
        return deger


class YedamGecmisIstek(BaseModel):
    """Rıza anındaki tek seferlik yükleme — sürükleyici + anket geçmişi."""

    surukleyiciler: list[SurukleyiciDto] = Field(default_factory=list, max_length=400)
    anketler: list[AnketYanitDto] = Field(default_factory=list, max_length=50)


class EbeveynYedamOnayIstek(BaseModel):
    rizaId: int
    onay: bool


# --- puan düzeltmesi (K56) ---


class PuanDuzeltmeDto(BaseModel):
    """
    Panelden yapılmış tekil düzeltme.

    `id` **sunucu id'si** ve cihazda birincil anahtar oluyor: aynı
    deltanın iki kez uygulanması fiziksel olarak imkânsız kalsın diye.

    Personelin adı taşınmıyor: kullanıcıya kimin yaptığı değil, ne
    yapıldığı ve niçin söyleniyor.
    """

    id: int
    delta: int
    gerekce: str
    zaman: dt.datetime
