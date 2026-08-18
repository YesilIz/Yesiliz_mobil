"""
TOTP — RFC 6238 Ek B resmî test vektörleri.

Bu dosya `pyotp` eklememe kararının (`docs/panel-plani.md §1.1 U4`) bedeli
ve gerekçesi. Elle yazılmış bir algoritmanın doğruluğu iddia edilmiyor,
**kanıtlanıyor**: aşağıdaki altı satır RFC'nin kendi tablosundan alınma.
"""

from __future__ import annotations

import base64

import pytest

from app.totp import (
    ADIM_SN,
    dogrula,
    kod_uret,
    otpauth_uri,
    sayac,
    sir_uret,
)

#: RFC 6238 Ek B'nin SHA-1 tohumu: ASCII "12345678901234567890" (20 bayt).
RFC_SIR = base64.b32encode(b"12345678901234567890").decode("ascii")

#: RFC 6238 Ek B, SHA-1 satırları. Vektörler **8 haneli**.
RFC_VEKTORLERI = [
    (59, "94287082"),
    (1111111109, "07081804"),
    (1111111111, "14050471"),
    (1234567890, "89005924"),
    (2000000000, "69279037"),
    (20000000000, "65353130"),
]


@pytest.mark.parametrize("zaman, beklenen", RFC_VEKTORLERI)
def test_rfc6238_ek_b_vektorleri(zaman, beklenen):
    assert kod_uret(RFC_SIR, sayac(zaman), hane=8) == beklenen


def test_dogrula_rfc_vektorunu_kabul_ediyor():
    assert dogrula(RFC_SIR, "94287082", 59, hane=8) == sayac(59)


# --- pencere ---


def test_bir_adim_geride_kalan_kod_kabul_ediliyor():
    """Cihaz saati birkaç saniye sapabiliyor; ±1 adım tolere ediliyor."""
    simdi = 1_700_000_000
    onceki = kod_uret(RFC_SIR, sayac(simdi) - 1)
    assert dogrula(RFC_SIR, onceki, simdi) == sayac(simdi) - 1


def test_iki_adim_geride_kalan_kod_reddediliyor():
    """Pencereyi genişletmek çalınmış kodun ömrünü uzatır."""
    simdi = 1_700_000_000
    eski = kod_uret(RFC_SIR, sayac(simdi) - 2)
    assert dogrula(RFC_SIR, eski, simdi) is None


def test_sonraki_adimin_kodu_kabul_ediliyor():
    simdi = 1_700_000_000
    sonraki = kod_uret(RFC_SIR, sayac(simdi) + 1)
    assert dogrula(RFC_SIR, sonraki, simdi) == sayac(simdi) + 1


# --- tekrar kullanım ---


def test_dogrula_eslesen_sayaci_donduruyor():
    """
    Bool değil sayaç dönüyor: çağıran onu `personel.son_totp_sayaci`'na
    yazıp aynı kodun ikinci kez kullanılmasını reddedebilsin diye. Omuz
    üstünden okunan bir kod 30 saniye geçerli kalmamalı.
    """
    simdi = 1_700_000_000
    kod = kod_uret(RFC_SIR, sayac(simdi))
    eslesen = dogrula(RFC_SIR, kod, simdi)
    assert eslesen == sayac(simdi)
    # Çağıran bu sayacı kaydedip bir sonraki denemede `<=` ile eleyecek.
    assert eslesen is not None and eslesen >= sayac(simdi)


# --- bozuk girdi ---


@pytest.mark.parametrize("kod", ["", "   ", "abcdef", "12345a", None])
def test_rakam_olmayan_kod_reddediliyor(kod):
    assert dogrula(RFC_SIR, kod, 59) is None


def test_yanlis_kod_reddediliyor():
    assert dogrula(RFC_SIR, "000000", 1_700_000_000) is None


# --- sır biçimi ---


def test_uretilen_sir_kendi_kodunu_dogruluyor():
    sir = sir_uret()
    simdi = 1_700_000_000
    assert dogrula(sir, kod_uret(sir, sayac(simdi)), simdi) is not None


def test_uretilen_sirlar_farkli():
    assert sir_uret() != sir_uret()


def test_bosluklu_ve_kucuk_harfli_sir_kabul_ediliyor():
    """
    Kullanıcı sırrı doğrulayıcı uygulamadan boşluklu kopyalayabiliyor;
    dolgu (`=`) da çoğu yerde atılıyor.
    """
    sir = sir_uret()
    bozuk = " ".join(sir[i : i + 4] for i in range(0, len(sir), 4)).lower()
    simdi = 1_700_000_000
    assert kod_uret(bozuk, sayac(simdi)) == kod_uret(sir, sayac(simdi))


def test_varsayilan_hane_alti():
    assert len(kod_uret(RFC_SIR, sayac(1_700_000_000))) == 6


# --- kurulum bağlantısı ---


def test_otpauth_uri_dogrulayici_bicimini_tasiyor():
    uri = otpauth_uri("ABCDEF", "danisman@yesilay.org.tr", "Yeşilİz Panel")
    assert uri.startswith("otpauth://totp/")
    assert "secret=ABCDEF" in uri
    assert f"period={ADIM_SN}" in uri
    # Etiket ve issuer kaçırılmış olmalı; boşluk ve @ ham geçerse
    # doğrulayıcı uygulamalar bağlantıyı ayrıştıramıyor.
    assert " " not in uri
    assert "danisman%40yesilay.org.tr" in uri
