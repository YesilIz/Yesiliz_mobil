"""
TOTP — zaman tabanlı tek kullanımlık parola (RFC 6238).

**Niçin kütüphane değil.** `pyotp` eklemek yerine bu modül yazıldı; gerekçe
`CLAUDE.md §12 K5`'in grafik kütüphanesi için verdiği kararla aynı: az kod,
tam kontrol, bağımlılık disiplini. "Elle kripto yazma" itirazı burada
geçerli değil, çünkü TOTP bir kripto *tasarımı* değil — HMAC'in üstüne
oturan, tamamen belirtilmiş bir sayaç kesme algoritması. Ve RFC 6238 **Ek
B'de resmî test vektörleri** veriyor: doğruluk kanıtlanabiliyor
(`tests/test_totp.py`), tıpkı `core-risk`'in referans Python ile birebir
eşleşmesi gibi.

**Bu modül saf.** Ayar okumuyor, veritabanı görmüyor, saat bile parametre.
Sırrın şifrelenerek saklanması `guvenlik.py`'nin işi.
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import secrets
import struct
from urllib.parse import quote

#: RFC 6238'in varsayılanı ve bütün doğrulayıcı uygulamalarının beklediği
#: adım. Değiştirmek Google/Microsoft Authenticator'ı uyumsuz kılar.
ADIM_SN = 30

#: Kullanıcıya gösterilen hane sayısı. RFC 6238 Ek B vektörleri 8 hane
#: kullanıyor; doğrulayıcı uygulamaların yerleşik varsayılanı 6.
VARSAYILAN_HANE = 6

#: `sir_uret` 20 bayt üretiyor — RFC 4226 §4 asgarisi 16, önerisi 20.
SIR_BAYT = 20

#: Kabul edilen adım kayması. 1 = önceki ve sonraki adım da geçerli, yani
#: cihaz saati ±30 sn sapabilir. Daha geniş bir pencere, çalınmış bir kodun
#: kullanılabilir kaldığı süreyi uzatıyor.
VARSAYILAN_PENCERE = 1


def sir_uret() -> str:
    """Yeni bir personel sırrı — base32, dolgusuz."""
    return base64.b32encode(secrets.token_bytes(SIR_BAYT)).decode("ascii").rstrip("=")


def _sirri_coz(sir: str) -> bytes:
    """
    Base32 metni bayta çevirir.

    Doğrulayıcı uygulamalar sırrı boşluklu ve küçük harfle gösterebiliyor;
    kullanıcı da öyle yapıştırabiliyor. Dolgu (`=`) çoğu yerde atılıyor,
    `b32decode` ise dolgusuz girdiyi reddediyor — burada geri ekleniyor.
    """
    temiz = sir.replace(" ", "").replace("-", "").upper()
    dolgu = (-len(temiz)) % 8
    return base64.b32decode(temiz + "=" * dolgu, casefold=True)


def kod_uret(sir: str, sayac: int, hane: int = VARSAYILAN_HANE) -> str:
    """
    Verilen sayaç için TOTP kodu (RFC 4226 §5.3 dinamik kesme).

    `sayac` = unix zamanı // [ADIM_SN]. Ayrı parametre olması test
    vektörlerinin doğrudan koşabilmesi için.
    """
    ozet = hmac.new(_sirri_coz(sir), struct.pack(">Q", sayac), hashlib.sha1).digest()
    # Son yarım baytın işaret ettiği yerden 4 bayt oku, işaret bitini at.
    konum = ozet[-1] & 0x0F
    (parca,) = struct.unpack(">I", ozet[konum : konum + 4])
    return str((parca & 0x7FFFFFFF) % (10**hane)).zfill(hane)


def sayac(simdi_epoch: int) -> int:
    return simdi_epoch // ADIM_SN


def dogrula(
    sir: str,
    kod: str,
    simdi_epoch: int,
    pencere: int = VARSAYILAN_PENCERE,
    hane: int = VARSAYILAN_HANE,
) -> int | None:
    """
    Kod geçerliyse **eşleşen sayacı**, değilse `None` döndürür.

    Bool yerine sayaç dönüyor çünkü çağıran onu `personel.son_totp_sayaci`'na
    yazmak zorunda: aynı kodun ikinci kez kullanılması reddedilmeli, yoksa
    omuz üstünden okunan bir kod 30 saniye boyunca geçerli kalır.

    Karşılaştırma `hmac.compare_digest` ile — kod doğrulaması da bir sır
    karşılaştırması ve zamanlama farkı sızdırmamalı.
    """
    if not kod or not kod.isdigit():
        return None

    simdiki = sayac(simdi_epoch)
    for kayma in range(-pencere, pencere + 1):
        aday = simdiki + kayma
        if aday < 0:
            continue
        if hmac.compare_digest(kod_uret(sir, aday, hane), kod):
            return aday
    return None


def otpauth_uri(sir: str, eposta: str, issuer: str) -> str:
    """
    Doğrulayıcı uygulamanın kabul ettiği kurulum bağlantısı.

    QR kodu **üretilmiyor** (`docs/panel-plani.md §1.1 U4`): `qrcode` +
    `pillow` bağımlılığı bir avuç personel hesabı için orantısız. Kurulum
    ekranı hem base32 sırrı hem bu bağlantıyı kopyalanabilir metin olarak
    veriyor; Google ve Microsoft Authenticator ikisini de elle kabul ediyor.
    """
    etiket = quote(f"{issuer}:{eposta}", safe="")
    return (
        f"otpauth://totp/{etiket}"
        f"?secret={sir}&issuer={quote(issuer, safe='')}"
        f"&algorithm=SHA1&digits={VARSAYILAN_HANE}&period={ADIM_SN}"
    )
