"""
Panel servisinin şema sürümü kapısı (`docs/panel-plani.md §4`).

Panel migration koşturmuyor; yalnızca şemanın head'de olduğunu doğruluyor.
Doğrulayamazsa `baslat.sh`'ın `set -e`'si uvicorn'u hiç başlatmıyor ve
hatalı sürüm devreye alınmıyor.
"""

from __future__ import annotations

import pytest

from app import surum_kapisi
from app.surum_kapisi import SemaUyumsuzlugu, semayi_dogrula

HEAD = "a83f19c2d507"


@pytest.fixture(autouse=True)
def beklemeyi_kapat(monkeypatch):
    """Testler gerçek saniyeleri beklemesin."""
    monkeypatch.setattr(surum_kapisi.time, "sleep", lambda _: None)


def _kur(monkeypatch, head, revizyonlar):
    """`mevcut_revizyon` her çağrıda listeden bir sonrakini döndürür."""
    sira = iter(revizyonlar)
    monkeypatch.setattr(surum_kapisi, "beklenen_revizyon", lambda: head)

    def mevcut():
        deger = next(sira)
        if isinstance(deger, Exception):
            raise deger
        return deger

    monkeypatch.setattr(surum_kapisi, "mevcut_revizyon", mevcut)


def test_sema_headde_ise_geciyor(monkeypatch):
    _kur(monkeypatch, HEAD, [HEAD])
    assert semayi_dogrula(deneme=1) == HEAD


def test_gec_kalan_migration_bekleniyor(monkeypatch):
    """
    Dağıtımda iki servis birlikte kalkıyor.

    Panel, API'nin migration'ını bitirmesini beklemezse ilk saniyede
    uyuşmazlık görüp crash-loop'a girerdi.
    """
    _kur(monkeypatch, HEAD, ["eskirevizyon", None, HEAD])
    assert semayi_dogrula(deneme=3) == HEAD


def test_pencere_dolunca_uyusmazlik_firlatiyor(monkeypatch):
    _kur(monkeypatch, HEAD, ["eskirevizyon"] * 3)
    with pytest.raises(SemaUyumsuzlugu) as hata:
        semayi_dogrula(deneme=3)
    assert "eskirevizyon" in str(hata.value)


def test_damgasiz_veritabani_uyusmazlik(monkeypatch):
    """Hiç migration koşmamış veritabanı da 'hazır' sayılmamalı."""
    _kur(monkeypatch, HEAD, [None])
    with pytest.raises(SemaUyumsuzlugu) as hata:
        semayi_dogrula(deneme=1)
    assert "damgasız" in str(hata.value)


def test_veritabanina_ulasilamamasi_uyusmazlik_sayiliyor(monkeypatch):
    """Paneli veritabanısız açmanın anlamı yok — her ekranı ona bağlı."""
    _kur(monkeypatch, HEAD, [OSError("baglanti yok")])
    with pytest.raises(SemaUyumsuzlugu) as hata:
        semayi_dogrula(deneme=1)
    assert "ulaşılamadı" in str(hata.value)


def test_baglanti_hatasinin_gerekcesi_mesaja_girmiyor(monkeypatch):
    """
    Bağlantı istisnaları veritabanı URL'ini parolasıyla taşıyabiliyor;
    `main.py`'nin `_veritabani_ayakta` kuralıyla aynı gerekçe.
    """
    _kur(monkeypatch, HEAD, [OSError("postgresql://kullanici:GIZLIPAROLA@db/yesiliz")])
    with pytest.raises(SemaUyumsuzlugu) as hata:
        semayi_dogrula(deneme=1)
    assert "GIZLIPAROLA" not in str(hata.value)


def test_beklenen_revizyon_alembic_headini_okuyor():
    """Gerçek alembic dizinine bakıyor — head boş dönerse kapı anlamsızlaşır."""
    assert surum_kapisi.beklenen_revizyon()
