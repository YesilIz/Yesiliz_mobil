"""
Özellik bayrağı arayüzü (`docs/panel-plani.md §5.7`).

İki sınır: bayrak tanımları **kodda** kalıyor (serbest anahtar yok) ve
**risk motoru sabitleri değiştirilemiyor** (K17 — skorlama cihazda ve
referans Python ile doğrulanmış).
"""

from __future__ import annotations

import datetime as dt

import pytest

from app import bayraklar
from app.modeller import (
    DenetimKaydi,
    OzellikBayragi,
    Personel,
    PersonelDurumu,
    PersonelOturum,
    PersonelRolu,
)
from app.panel.oturum import CEREZ_ADI

GEREKCE = "Hukuk gorusu geldi, danisman notu aciliyor."


@pytest.fixture
def girisli(db, panel_istemci):
    from app.guvenlik import jeton_ozetle, rastgele_jeton, sifre_ozetle

    def kur(rol=PersonelRolu.ADMIN) -> Personel:
        p = Personel(
            eposta=f"{rol.value}@yesilay.org.tr",
            ad="Personel",
            sifre_ozet=sifre_ozetle("parola"),
            rol=rol.value,
            durum=PersonelDurumu.AKTIF.value,
            totp_sir="",
        )
        db.add(p)
        db.flush()
        ham = rastgele_jeton()
        db.add(
            PersonelOturum(
                personel_id=p.id,
                jeton_ozet=jeton_ozetle(ham),
                totp_dogrulandi=True,
                ip="127.0.0.1",
                tarayici="test",
                mutlak_bitis=dt.datetime.now(dt.timezone.utc) + dt.timedelta(hours=4),
                iptal=False,
            )
        )
        db.commit()
        panel_istemci.cookies.set(CEREZ_ADI, ham)
        return p

    return kur


def _csrf(panel_istemci) -> str:
    from app.guvenlik import jeton_ozetle
    from app.panel.oturum import csrf_jetonu

    return csrf_jetonu(jeton_ozetle(panel_istemci.cookies[CEREZ_ADI]))


def _yaz(panel_istemci, anahtar, deger, gerekce=GEREKCE):
    return panel_istemci.post(
        "/yonetim/bayrak",
        data={
            "anahtar": anahtar,
            "deger": deger,
            "gerekce": gerekce,
            "_csrf": _csrf(panel_istemci),
        },
    )


def test_ekran_tum_bayraklari_gosteriyor(db, panel_istemci, girisli):
    girisli()
    sayfa = panel_istemci.get("/yonetim/bayrak").text
    for b in bayraklar.TUMU:
        assert b.anahtar in sayfa


def test_risk_sabitleri_salt_okunur(db, panel_istemci, girisli):
    """K17: sunucudan sabit dağıtmak referans doğrulamasını bozardı."""
    girisli()
    sayfa = panel_istemci.get("/yonetim/bayrak").text
    assert "S_REF" in sayfa
    assert "buradan değiştirilemez" in sayfa
    # Sabitler için form alanı YOK.
    assert 'name="anahtar" value="S_REF"' not in sayfa


def test_bayrak_degistirilebiliyor(db, panel_istemci, girisli):
    girisli()
    assert bayraklar.acik_mi(db, bayraklar.DANISMAN_NOTU) is False

    assert _yaz(panel_istemci, "yedam.danisman_notu", "evet").status_code in (200, 303)

    db.expire_all()
    assert bayraklar.acik_mi(db, bayraklar.DANISMAN_NOTU) is True


def test_bilinmeyen_anahtar_reddediliyor(db, panel_istemci, girisli):
    """Serbest anahtar, yazım hatasıyla sessizce varsayılana düşen bayrak üretir."""
    girisli()
    assert _yaz(panel_istemci, "uydurma.anahtar", "evet").status_code == 422
    assert db.query(OzellikBayragi).count() == 0


def test_gecersiz_deger_reddediliyor(db, panel_istemci, girisli):
    girisli()
    assert _yaz(panel_istemci, "yedam.acik", "belki").status_code == 422


def test_gerekcesiz_degistirilemiyor(db, panel_istemci, girisli):
    """Bir bayrak bir özelliği herkes için açıyor; niçini kayıtta durmalı."""
    girisli()
    assert _yaz(panel_istemci, "yedam.acik", "hayir", gerekce="kısa").status_code == 422
    assert db.query(OzellikBayragi).count() == 0


def test_denetim_kaydi_biraliyor(db, panel_istemci, girisli):
    girisli()
    _yaz(panel_istemci, "yedam.danisman_notu", "evet")
    kayit = db.query(DenetimKaydi).one()
    assert kayit.eylem == "bayrak.yaz"
    assert '"anahtar": "yedam.danisman_notu"' in kayit.detay
    assert kayit.gerekce == GEREKCE


def test_operasyon_bayrak_degistiremiyor(db, panel_istemci, girisli):
    """Bayrak değiştirmek operasyonun günlük işi değil."""
    girisli(PersonelRolu.OPERASYON)
    assert panel_istemci.get("/yonetim/bayrak").status_code == 403
    assert _yaz(panel_istemci, "yedam.acik", "hayir").status_code == 403
