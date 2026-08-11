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


class Jetonlar(BaseModel):
    erisim: str
    yenileme: str
    ad: str
    eposta: str


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


class SenkronIstek(BaseModel):
    # Cihaz worker'ı yalnızca son yedi günü yollar. Liste tavanı,
    # kimliği doğrulanmış olsa bile tek istekle kaynak tüketimini sınırlar.
    ozetler: list[GunlukOzetDto] = Field(default_factory=list, max_length=7)
    riskler: list[RiskSonucDto] = Field(default_factory=list, max_length=7)
    # Cihazın o günkü toplam puanı — lig sıralaması bunun üzerinden.
    haftalikPuan: int = Field(default=0, ge=0, le=2000)
    # "Ligde görün" tercihi. None = değiştirme (eski istemciler).
    ligdeGorunur: bool | None = None


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
