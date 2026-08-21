"""
Personel kimliği — şema düzeyindeki sözleşmeler (`docs/panel-plani.md §2`).

Buradaki testler davranış değil **yapı** sınıyor. Sebebi K57'nin kendisi:
personel ile son kullanıcının ayrılığı bir kod disiplini değil, yabancı
anahtar seviyesinde fiziksel bir ayrım olmalı. Yapıyı sınayan test, o
ayrımı bozan bir değişikliği yazıldığı anda yakalıyor.
"""

from __future__ import annotations

import datetime as dt

import pytest
from sqlalchemy.exc import IntegrityError

from app.modeller import (
    DenetimKaydi,
    OzellikBayragi,
    Personel,
    PersonelDurumu,
    PersonelKurtarmaKodu,
    PersonelOturum,
    PersonelRolu,
)
from app.veritabani import Taban

#: Panel tabloları — hiçbiri `kullanici.id`'ye bağlanmamalı.
PANEL_TABLOLARI = {
    "personel",
    "personel_oturum",
    "personel_kurtarma_kodu",
    "denetim_kaydi",
    "ozellik_bayragi",
}


def _personel(db, eposta="admin@yesilay.org.tr", rol=PersonelRolu.ADMIN):
    p = Personel(
        eposta=eposta,
        ad="Ad Soyad",
        sifre_ozet="argon2-ozet",
        rol=rol.value,
        durum=PersonelDurumu.AKTIF.value,
    )
    db.add(p)
    db.commit()
    return p


# --- iki kimlik alanının ayrılığı ---


def test_panel_tablolari_kullaniciya_yabanci_anahtar_vermiyor():
    """
    K57'nin taşıyıcı iddiası.

    Bir panel tablosu `kullanici.id`'ye bağlanırsa iki kimlik alanı
    birleşmeye başlar: personel silme kullanıcı silmeye, kullanıcı silme
    personel kaydına bulaşır. Bağ gerektiğinde (denetim hedefi) düz
    `Integer` kullanılıyor.
    """
    for ad in PANEL_TABLOLARI:
        tablo = Taban.metadata.tables[ad]
        hedefler = {
            fk.column.table.name for kolon in tablo.columns for fk in kolon.foreign_keys
        }
        assert "kullanici" not in hedefler, f"{ad} kullanici tablosuna bağlanmış"


def test_denetim_hedefi_yabanci_anahtar_degil():
    """
    Kullanıcı silinince denetim satırı hayatta kalmalı.

    FK olsaydı `DELETE /auth/hesap` ya cascade ile denetim izini silerdi ya
    da yabancı anahtar hatası verip silmeyi bloklardı; ikisi de kabul
    edilemez.
    """
    kolon = Taban.metadata.tables["denetim_kaydi"].columns["hedef_id"]
    assert not kolon.foreign_keys
    assert kolon.nullable


# --- personel ---


def test_eposta_tekil(db):
    _personel(db)
    with pytest.raises(IntegrityError):
        _personel(db)


def test_varsayilan_durum_aktif(db):
    assert _personel(db).durum == PersonelDurumu.AKTIF.value


def test_yeni_personelin_totp_sirri_bos(db):
    """Boş sır = 2FA henüz kurulmadı; ilk giriş kurulum ekranına gidiyor."""
    p = _personel(db)
    assert p.totp_sir == ""
    assert p.son_totp_sayaci == 0


def test_kilit_alanlari_bos_basliyor(db):
    p = _personel(db)
    assert p.basarisiz_deneme == 0
    assert p.kilit_bitis is None


def test_bes_rol_tanimli():
    assert {r.value for r in PersonelRolu} == {
        "admin",
        "operasyon",
        "yedam_yonetici",
        "yedam_danisman",
        "gozlemci",
    }


# --- oturum ---


def test_oturum_jeton_ozeti_tekil(db):
    p = _personel(db)
    bitis = dt.datetime.now(dt.timezone.utc) + dt.timedelta(hours=8)
    for _ in range(2):
        db.add(
            PersonelOturum(
                personel_id=p.id,
                jeton_ozet="ayni-ozet",
                totp_dogrulandi=False,
                mutlak_bitis=bitis,
                iptal=False,
            )
        )
    with pytest.raises(IntegrityError):
        db.commit()


def test_oturum_mutlak_bitisi_kolon_olarak_tutuluyor(db):
    """
    Ayardan türetilen anlık değer değil.

    `panel_oturum_mutlak_saat` sonradan 8'den 12'ye çıkarılırsa yaşayan
    oturumlar geriye dönük uzamamalı.
    """
    kolon = Taban.metadata.tables["personel_oturum"].columns["mutlak_bitis"]
    assert not kolon.nullable


def test_iki_adimli_giris_tek_satirda(db):
    """
    Parola adımı satırı açıyor, TOTP adımı bayrağı çeviriyor.

    İkinci bir tablo ve ikinci bir çerez yok; yarım kalmış giriş de
    listelenebilir ve düşürülebilir bir oturum.
    """
    p = _personel(db)
    oturum = PersonelOturum(
        personel_id=p.id,
        jeton_ozet="ozet",
        totp_dogrulandi=False,
        mutlak_bitis=dt.datetime.now(dt.timezone.utc) + dt.timedelta(minutes=5),
        iptal=False,
    )
    db.add(oturum)
    db.commit()

    oturum.totp_dogrulandi = True
    oturum.mutlak_bitis = dt.datetime.now(dt.timezone.utc) + dt.timedelta(hours=8)
    db.commit()
    assert oturum.totp_dogrulandi


# --- kurtarma kodu ---


def test_kurtarma_kodu_ozeti_tekil(db):
    p = _personel(db)
    for _ in range(2):
        db.add(PersonelKurtarmaKodu(personel_id=p.id, kod_ozet="ayni", kullanildi=False))
    with pytest.raises(IntegrityError):
        db.commit()


# --- denetim ve bayrak ---


def test_denetim_kaydi_yazilabiliyor(db):
    p = _personel(db)
    db.add(
        DenetimKaydi(
            personel_id=p.id,
            eylem="kullanici.askiya_al",
            hedef_tur="kullanici",
            hedef_id=42,
            gerekce="Destek talebi 118 kapsamında geçici askıya alma",
            ip="10.0.0.1",
        )
    )
    db.commit()
    kayit = db.query(DenetimKaydi).one()
    assert kayit.eylem == "kullanici.askiya_al"
    assert kayit.zaman is not None


def test_ozellik_bayragi_anahtarla_tekil(db):
    db.add(OzellikBayragi(anahtar="maddi_odul_teslimati", deger="kapali"))
    db.commit()
    db.add(OzellikBayragi(anahtar="maddi_odul_teslimati", deger="acik"))
    with pytest.raises(IntegrityError):
        db.commit()
