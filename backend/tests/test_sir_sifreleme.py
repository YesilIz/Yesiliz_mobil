"""
Panel sırlarının şifrelenmesi (`guvenlik.sir_sifrele` / `sir_coz`).

Kullanıcı kararı (`docs/panel-plani.md §1.1`): TOTP sırrı veritabanında düz
metin durmayacak. Parola argon2id ile **özetleniyor** çünkü geri okunması
gerekmiyor; TOTP sırrı her doğrulamada okunmak zorunda, yani özet işe
yaramaz ve tersine çevrilebilir şifreleme gerekiyor.
"""

from __future__ import annotations

import pytest
from cryptography.fernet import Fernet

from app import guvenlik
from app.guvenlik import SirCozulemedi, sir_coz, sir_sifrele

SIR = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"


@pytest.fixture(autouse=True)
def _fernet_onbellegini_temizle():
    """`_fernet` lru_cache'li; anahtarı değiştiren testler için sıfırlanmalı."""
    guvenlik._fernet.cache_clear()
    yield
    guvenlik._fernet.cache_clear()


def test_sifrelenen_sir_geri_okunuyor():
    assert sir_coz(sir_sifrele(SIR)) == SIR


def test_sifreli_deger_duz_sirri_icermiyor():
    """Veritabanı yedeğini okuyan biri ikinci faktörü baypas edememeli."""
    assert SIR not in sir_sifrele(SIR)


def test_ayni_sir_her_seferinde_farkli_sifreleniyor():
    """Fernet rastgele IV kullanıyor; aynı çıktı, tekrar saldırısına açık olurdu."""
    assert sir_sifrele(SIR) != sir_sifrele(SIR)


def test_bozuk_deger_sessizce_bos_donmuyor():
    """
    Sessizce boş sır dönmek, TOTP doğrulamasının HER kodu reddetmesi demek
    olurdu ve sebebi hiçbir yerde görünmezdi — §9'un
    `runCatching{}.getOrDefault` tuzağının sunucu karşılığı.
    """
    with pytest.raises(SirCozulemedi):
        sir_coz("gAAAAABbozukdeger")


def test_anahtar_degisince_acik_hata_veriyor(monkeypatch):
    sifreli = sir_sifrele(SIR)

    guvenlik._fernet.cache_clear()
    yeni_anahtar = Fernet.generate_key().decode("ascii")
    a = guvenlik.ayarlar()
    monkeypatch.setattr(a, "panel_totp_anahtari", yeni_anahtar)

    with pytest.raises(SirCozulemedi) as hata:
        sir_coz(sifreli)
    assert "PANEL_TOTP_KEY" in str(hata.value)


def test_hata_mesaji_sirri_icermiyor():
    """Hata mesajı da bir log (JWT anahtarı kuralıyla aynı)."""
    with pytest.raises(SirCozulemedi) as hata:
        sir_coz("gAAAAABbozukdeger")
    assert SIR not in str(hata.value)
