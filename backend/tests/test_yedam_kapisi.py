"""
YEDAM kapısı ve özellik bayrakları.

Kapı bir **erişim** kararı veriyor: yanlış tarafa düşerse ya korunması
gereken bir çocuğun verisi danışmana gider, ya da desteğe ihtiyacı olan
biri kapıda kalır. İkisi de sessiz hatalar, o yüzden her dal ayrı
sınanıyor.
"""

from __future__ import annotations

import pytest

from app import bayraklar
from app.modeller import EbeveynBaglanti, Kullanici, OzellikBayragi
from app.yedam_kapisi import KapiSonucu, degerlendir


@pytest.fixture
def kullanici(db):
    sayac = {"n": 0}

    def kur() -> Kullanici:
        sayac["n"] += 1
        k = Kullanici(
            eposta=f"k{sayac['n']}@ornek.com", ad=f"K{sayac['n']}", sifre_ozet="x"
        )
        db.add(k)
        db.commit()
        return k

    return kur


def _bayrak(db, anahtar, deger):
    db.add(OzellikBayragi(anahtar=anahtar, deger=deger))
    db.commit()


# --- bayrak okuma ---


def test_satir_yoksa_varsayilan(db):
    assert bayraklar.oku(db, bayraklar.YEDAM_EBEVEYN_ONAYI) == "eslesmisse"
    assert bayraklar.acik_mi(db, bayraklar.YEDAM_ACIK) is True
    assert bayraklar.acik_mi(db, bayraklar.DANISMAN_NOTU) is False


def test_gecersiz_deger_varsayilana_dusuyor(db):
    """Elle düzenlenmiş satır kapıyı tanımsız bir duruma sokmamalı."""
    _bayrak(db, "yedam.ebeveyn_onayi", "saçmalık")
    assert bayraklar.oku(db, bayraklar.YEDAM_EBEVEYN_ONAYI) == "eslesmisse"


def test_gecerli_deger_varsayilani_eziyor(db):
    _bayrak(db, "yedam.ebeveyn_onayi", "her_zaman")
    assert bayraklar.oku(db, bayraklar.YEDAM_EBEVEYN_ONAYI) == "her_zaman"


def test_bilinmeyen_anahtar_yazilamiyor(db):
    with pytest.raises(ValueError):
        bayraklar.yaz(db, "uydurma.anahtar", "evet", personel_id=1)


def test_gecersiz_deger_yazilamiyor(db):
    with pytest.raises(ValueError):
        bayraklar.yaz(db, "yedam.acik", "belki", personel_id=1)


# --- kapı: reşit ---


def test_resit_kullanici_dogrudan_aciliyor(db, kullanici):
    k = kullanici()
    assert degerlendir(db, k.id, 18).sonuc is KapiSonucu.ACIK


def test_kopru_kapaliysa_resit_de_acamiyor(db, kullanici):
    _bayrak(db, "yedam.acik", "hayir")
    k = kullanici()
    karar = degerlendir(db, k.id, 30)
    assert karar.sonuc is KapiSonucu.KAPALI
    assert not karar.acilabilir


# --- kapı: 18 altı, varsayılan kural (eslesmisse) ---


def test_18_alti_eslesme_yoksa_kendi_rizasi_yetiyor(db, kullanici):
    """Rıza metni: eşleştirme yapmamış olmak desteğe erişimi engellememeli."""
    k = kullanici()
    assert degerlendir(db, k.id, 15).sonuc is KapiSonucu.ACIK


def test_18_alti_eslesme_varsa_ebeveyn_onayi(db, kullanici):
    cocuk, ebeveyn = kullanici(), kullanici()
    db.add(EbeveynBaglanti(ebeveyn_id=ebeveyn.id, cocuk_id=cocuk.id))
    db.commit()

    karar = degerlendir(db, cocuk.id, 15)
    assert karar.sonuc is KapiSonucu.EBEVEYN_ONAYI_GEREKLI
    assert karar.ebeveyn_id == ebeveyn.id
    assert karar.acilabilir, "Başvuru bekleyen durumda açılabilmeli"


def test_kendisi_ebeveyn_olan_kullanici_onay_beklemiyor(db, kullanici):
    """Bağın `ebeveyn` tarafındaki kullanıcının üstünde bir hesap yok."""
    ebeveyn, cocuk = kullanici(), kullanici()
    db.add(EbeveynBaglanti(ebeveyn_id=ebeveyn.id, cocuk_id=cocuk.id))
    db.commit()

    # Yaşını 16 beyan etmiş bir "ebeveyn" hesabı: yine de onay beklenecek
    # bir üst hesabı yok.
    assert degerlendir(db, ebeveyn.id, 16).sonuc is KapiSonucu.ACIK


# --- kapı: her_zaman ve hicbir_zaman ---


def test_her_zaman_kuralinda_eslesme_yoksa_acilamiyor(db, kullanici):
    _bayrak(db, "yedam.ebeveyn_onayi", "her_zaman")
    k = kullanici()
    karar = degerlendir(db, k.id, 15)
    assert karar.sonuc is KapiSonucu.EBEVEYN_YOK
    assert not karar.acilabilir


def test_hicbir_zaman_kuralinda_18_alti_da_aciliyor(db, kullanici):
    _bayrak(db, "yedam.ebeveyn_onayi", "hicbir_zaman")
    cocuk, ebeveyn = kullanici(), kullanici()
    db.add(EbeveynBaglanti(ebeveyn_id=ebeveyn.id, cocuk_id=cocuk.id))
    db.commit()
    assert degerlendir(db, cocuk.id, 15).sonuc is KapiSonucu.ACIK


# --- yaş bilinmiyorsa ---


def test_yas_bilinmiyorsa_cocuk_muamelesi(db, kullanici):
    """Ölçülmeyen değer "iyi" tarafa yorumlanmaz (CLAUDE.md §9)."""
    cocuk, ebeveyn = kullanici(), kullanici()
    db.add(EbeveynBaglanti(ebeveyn_id=ebeveyn.id, cocuk_id=cocuk.id))
    db.commit()
    assert degerlendir(db, cocuk.id, None).sonuc is KapiSonucu.EBEVEYN_ONAYI_GEREKLI
