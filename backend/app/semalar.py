"""İstek/yanıt modelleri.

Alan adları **cihazdaki Kotlin modelleriyle** aynı yazımda tutuluyor
(`camelCase` değil `snake_case` değil — Kotlin tarafındaki isimlerin
birebiri). Böylece Retrofit DTO'sunda `@SerialName` haritalaması
gerekmiyor ve alan eklerken tek yerde unutma riski azalıyor.
"""

from __future__ import annotations

import datetime as dt

from pydantic import BaseModel, EmailStr, Field


# --- hesap ---

class KayitIstek(BaseModel):
    ad: str = Field(min_length=2, max_length=120)
    eposta: EmailStr
    sifre: str = Field(min_length=8, max_length=128)


class GirisIstek(BaseModel):
    eposta: EmailStr
    sifre: str


class YenileIstek(BaseModel):
    yenileme: str


class SifreSifirlaIstek(BaseModel):
    eposta: EmailStr


class SifreYenileIstek(BaseModel):
    jeton: str
    yeniSifre: str = Field(min_length=8, max_length=128)


class Jetonlar(BaseModel):
    erisim: str
    yenileme: str
    ad: str
    eposta: str


# --- özet senkronu ---

class GunlukOzetDto(BaseModel):
    tarih: dt.date
    socialDk: float = 0.0
    entertainmentDk: float = 0.0
    gamesDk: float = 0.0
    communicationDk: float = 0.0
    productivityDk: float = 0.0
    educationDk: float = 0.0
    healthDk: float = 0.0
    otherDk: float = 0.0
    gece: float = 0.0
    oturum: float = 0.0
    acilis: float = 0.0
    sabah: float = 0.0
    uykuOncesi: float = 0.0
    yerDegis: float = 0.0
    adim: float = 0.0
    sporDk: float = 0.0
    S: float = 0.0


class RiskSonucDto(BaseModel):
    tarih: dt.date
    S: float
    R: float
    gosterilenBant: str
    trend: str


class SenkronIstek(BaseModel):
    ozetler: list[GunlukOzetDto] = []
    riskler: list[RiskSonucDto] = []
    # Cihazın o günkü toplam puanı — lig sıralaması bunun üzerinden.
    haftalikPuan: int = 0
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
    kod: str


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
    rol: str


class EslestirIstek(BaseModel):
    kod: str
    rol: str


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
