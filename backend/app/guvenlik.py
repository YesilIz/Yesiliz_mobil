"""Şifre özetleme, JWT ve panel sırlarının şifrelenmesi."""

from __future__ import annotations

import datetime as dt
import hashlib
import secrets
from functools import lru_cache

import jwt
from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError
from cryptography.fernet import Fernet, InvalidToken
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ayarlar import ayarlar
from app.modeller import Kullanici
from app.veritabani import oturum

# argon2id — bcrypt yerine: bellek-zor, GPU saldırısına dirençli ve
# OWASP'ın güncel önerisi.
_ozetleyici = PasswordHasher()

# Girişte bulunmayan e-posta da gerçek bir Argon2id doğrulaması çalıştırır.
# Bu değer hiçbir hesaba ait değildir ve sır değildir; yalnızca var/yok
# yollarının hesaplama maliyetini eşitleyerek zamanlama ile e-posta
# sorgulanmasını zorlaştırır. Parametreleri PasswordHasher varsayılanlarıyla
# aynı tutulmuştur.
_SAHTE_SIFRE_OZETI = (
    "$argon2id$v=19$m=65536,t=3,p=4$AGQ7bdOUcoA+qk/I5pVPCg$"
    "VCxMPxnGWiJ8ZGepbBMSJipUxzc5TZ6SIixryVUTcIM"
)

_tasiyici = HTTPBearer(auto_error=False)


def sifre_ozetle(sifre: str) -> str:
    return _ozetleyici.hash(sifre)


def sifre_dogrula(ozet: str | None, sifre: str) -> bool:
    gercek_kullanici = ozet is not None
    try:
        eslesti = _ozetleyici.verify(ozet or _SAHTE_SIFRE_OZETI, sifre)
        return gercek_kullanici and eslesti
    except VerifyMismatchError:
        return False
    except Exception:
        # Bozuk özet (elle düzenlenmiş kayıt) da başarısız sayılır;
        # istisna fırlatmak giriş ucunu 500'e düşürürdü.
        return False


def rastgele_jeton() -> str:
    """Yenileme ve sıfırlama jetonları — 32 bayt, URL güvenli."""
    return secrets.token_urlsafe(32)[:64]


def jeton_ozetle(jeton: str) -> str:
    """Tek kullanımlı ve yenileme sırlarını veritabanında düz tutma."""
    return hashlib.sha256(jeton.encode("utf-8")).hexdigest()


class SirCozulemedi(RuntimeError):
    """Şifreli sır bu anahtarla açılamıyor — anahtar değişmiş olabilir."""


@lru_cache
def _fernet() -> Fernet:
    return Fernet(ayarlar().panel_totp_anahtari.encode("ascii"))


def sir_sifrele(duz: str) -> str:
    """
    Personel TOTP sırrını veritabanına yazmadan önce şifreler.

    Parola `argon2id` ile **özetleniyor** çünkü geri okunması gerekmiyor;
    TOTP sırrı ise her doğrulamada okunmak zorunda, yani özet işe yaramaz —
    tersine çevrilebilir şifreleme gerekiyor. Anahtar veritabanında değil
    ortam değişkeninde (`PANEL_TOTP_KEY`), böylece veritabanı yedeğini
    okuyan biri ikinci faktörü baypas edemiyor.

    `jeton_ozetle` ile karıştırılmaz: o tek yönlü, bu iki yönlü. Seçim
    verinin okunmaya ihtiyacı olup olmamasına göre yapılıyor.
    """
    return _fernet().encrypt(duz.encode("utf-8")).decode("ascii")


def sir_coz(sifreli: str) -> str:
    """
    [sir_sifrele] ile yazılmış değeri açar.

    Anahtar döndürülmüş ya da kaybolmuşsa açık bir hata veriyor; sessizce
    boş sır dönmek, TOTP doğrulamasının **her kodu reddetmesi** demek olurdu
    ve sebebi hiçbir yerde görünmezdi (§9 `runCatching{}.getOrDefault`
    tuzağının sunucu karşılığı).
    """
    try:
        return _fernet().decrypt(sifreli.encode("ascii")).decode("utf-8")
    except (InvalidToken, ValueError) as hata:
        raise SirCozulemedi(
            "TOTP sırrı çözülemedi; PANEL_TOTP_KEY değişmiş olabilir. "
            "Anahtar geri getirilemiyorsa personelin 2FA'yı yeniden kurması "
            "gerekiyor (docs/dagitim.md §1)."
        ) from hata


def erisim_jetonu(kullanici_id: int) -> str:
    a = ayarlar()
    simdi = dt.datetime.now(dt.timezone.utc)
    return jwt.encode(
        {
            "sub": str(kullanici_id),
            "iat": simdi,
            "exp": simdi + dt.timedelta(minutes=a.erisim_dakika),
        },
        a.jwt_anahtari,
        algorithm=a.jwt_algoritma,
    )


def jetonu_coz(jeton: str) -> int | None:
    a = ayarlar()
    try:
        veri = jwt.decode(jeton, a.jwt_anahtari, algorithms=[a.jwt_algoritma])
        return int(veri["sub"])
    except (jwt.PyJWTError, KeyError, ValueError):
        return None


#: Askıya alınmış hesabın 403 gövdesindeki ayırt edici alan. Cihaz bu
#: değere bakıp çevrimdışı ile askıyı ayırıyor; yeniden adlandırılırsa
#: Android tarafı da değişmeli.
ASKIDA_KODU = "hesap_askida"


def askiya_alma_govdesi(kullanici: Kullanici) -> dict[str, str]:
    """
    403'ün **makine okunur** gövdesi.

    Düz metin bir mesaj yeterli değil: cihaz 403'ü ağ hatasından ayırmak
    zorunda, yoksa `Sunucu*Deposu` onu sessizce "çevrimdışı"ya indirger ve
    kullanıcı sebebini hiç öğrenmez (CLAUDE.md §12 K18/K42 ailesi).
    """
    return {
        "hata": ASKIDA_KODU,
        "sebep": kullanici.askiya_alma_sebebi or "",
    }


def mevcut_kullanici(
    kimlik: HTTPAuthorizationCredentials | None = Depends(_tasiyici),
    db: Session = Depends(oturum),
) -> Kullanici:
    """Korumalı uçların bağımlılığı."""
    if kimlik is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Oturum gerekiyor")

    kullanici_id = jetonu_coz(kimlik.credentials)
    if kullanici_id is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Jeton geçersiz ya da süresi dolmuş")

    kullanici = db.scalar(select(Kullanici).where(Kullanici.id == kullanici_id))
    if kullanici is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Kullanıcı bulunamadı")
    if kullanici.askida_mi:
        raise HTTPException(
            status.HTTP_403_FORBIDDEN,
            detail=askiya_alma_govdesi(kullanici),
        )
    return kullanici


def kullaniciyi_kilitle(db: Session, kullanici_id: int) -> Kullanici:
    """Korunan yazma boyunca hesabın silinemeyeceğini garanti eder.

    Kimlik bağımlılığının ilk SELECT'i ile uçtaki yazma arasında başka bir
    istek hesabı silebilir. Kilit sorgusunun boş sonucunu yok saymak,
    ardından yabancı anahtarlı satır ekleyip 500 üretmek demekti.
    """
    kullanici = db.scalar(
        select(Kullanici)
        .where(Kullanici.id == kullanici_id)
        .with_for_update()
    )
    if kullanici is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Kullanıcı bulunamadı")
    return kullanici
