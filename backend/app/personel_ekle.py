"""
İlk personel hesabını açan komut satırı aracı.

    python -m app.personel_ekle --eposta admin@yesilay.org.tr --ad "Ad Soyad" --rol admin

Üretimde `railway run python -m app.personel_ekle …` ile koşuyor.

**Panelde kayıt formu yok ve olmayacak.** Personel hesabı açmak bir yetki
işlemi: ilk hesap bu komutla, sonrakiler panelden `admin` rolüyle açılıyor.
İnternete açık bir kayıt formu, panelin bütün yetki modelini anlamsız
kılardı.

Parola argümanla **alınmıyor** — kabuk geçmişine ve süreç listesine düşerdi.
`getpass` ile soruluyor; boru hattından koşmak gerekirse ortam değişkeni
(`YESILIZ_PERSONEL_SIFRE`) kabul ediliyor ve bu durumda uyarı basılıyor.
"""

from __future__ import annotations

import argparse
import getpass
import os
import secrets
import sys

from sqlalchemy import select

from app.ayarlar import ayarlar
from app.guvenlik import jeton_ozetle, sifre_ozetle, sir_sifrele
from app.modeller import Personel, PersonelKurtarmaKodu, PersonelDurumu, PersonelRolu
from app.totp import otpauth_uri, sir_uret
from app.veritabani import OturumFabrikasi

#: Kurulumda bir kez gösterilen tek kullanımlık giriş kodu sayısı.
KURTARMA_KODU_SAYISI = 8

#: Panel parolası için asgari uzunluk. Son kullanıcıdan (8) daha sıkı:
#: personel hesabı sağlık verisine yakın bir yüzeyi açıyor.
ASGARI_SIFRE = 12


def _sifre_al() -> str:
    ortamdan = os.environ.get("YESILIZ_PERSONEL_SIFRE")
    if ortamdan:
        print(
            "UYARI: parola ortam değişkeninden alındı. Etkileşimli kullanımda "
            "bunu tanımlamayın.",
            file=sys.stderr,
        )
        return ortamdan

    sifre = getpass.getpass("Parola: ")
    if sifre != getpass.getpass("Parola (tekrar): "):
        sys.exit("Parolalar eşleşmiyor.")
    return sifre


def _kurtarma_kodu() -> str:
    """Okunması kolay, 10 karakterlik tek kullanımlık kod."""
    return f"{secrets.token_hex(2)}-{secrets.token_hex(2)}".upper()


def main() -> None:
    ayristirici = argparse.ArgumentParser(description="Panel personel hesabı açar.")
    ayristirici.add_argument("--eposta", required=True)
    ayristirici.add_argument("--ad", required=True)
    ayristirici.add_argument(
        "--rol",
        required=True,
        choices=[r.value for r in PersonelRolu],
    )
    argumanlar = ayristirici.parse_args()

    sifre = _sifre_al()
    if len(sifre) < ASGARI_SIFRE:
        sys.exit(f"Parola en az {ASGARI_SIFRE} karakter olmalı.")

    eposta = argumanlar.eposta.strip().lower()
    sir = sir_uret()
    kodlar = [_kurtarma_kodu() for _ in range(KURTARMA_KODU_SAYISI)]

    with OturumFabrikasi() as db:
        if db.scalar(select(Personel).where(Personel.eposta == eposta)):
            sys.exit(f"{eposta} zaten kayıtlı.")

        personel = Personel(
            eposta=eposta,
            ad=argumanlar.ad.strip(),
            sifre_ozet=sifre_ozetle(sifre),
            rol=argumanlar.rol,
            durum=PersonelDurumu.AKTIF.value,
            totp_sir=sir_sifrele(sir),
        )
        db.add(personel)
        db.flush()

        for kod in kodlar:
            db.add(
                PersonelKurtarmaKodu(
                    personel_id=personel.id, kod_ozet=jeton_ozetle(kod)
                )
            )
        db.commit()
        personel_id = personel.id

    taban = ayarlar().panel_taban_url or "Yeşilİz Panel"
    print(f"\nPersonel açıldı: #{personel_id} · {eposta} · {argumanlar.rol}")
    print("\n--- İKİ FAKTÖRLÜ KURULUM (bir kez gösteriliyor) ---")
    print(f"Sır (base32) : {sir}")
    print(f"Bağlantı     : {otpauth_uri(sir, eposta, 'Yesiliz Panel')}")
    print(f"Panel adresi : {taban}")
    print("\nDoğrulayıcı uygulamaya sırrı ya da bağlantıyı elle girin.")
    print("\n--- KURTARMA KODLARI (bir kez gösteriliyor, her biri tek kullanımlık) ---")
    for kod in kodlar:
        print(f"  {kod}")
    print(
        "\nBu ekranı kapatınca sır ve kodlar bir daha gösterilemez; "
        "veritabanında yalnızca şifreli sır ve kod özetleri duruyor.\n"
    )


if __name__ == "__main__":  # pragma: no cover
    main()
