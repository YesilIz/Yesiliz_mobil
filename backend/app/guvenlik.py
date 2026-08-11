"""Şifre özetleme ve JWT."""

from __future__ import annotations

import datetime as dt
import hashlib
import secrets

import jwt
from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError
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

_tasiyici = HTTPBearer(auto_error=False)


def sifre_ozetle(sifre: str) -> str:
    return _ozetleyici.hash(sifre)


def sifre_dogrula(ozet: str, sifre: str) -> bool:
    try:
        return _ozetleyici.verify(ozet, sifre)
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
