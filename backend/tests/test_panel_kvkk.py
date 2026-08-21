"""
KVKK işlemleri: silme, veri dışa aktarma, ebeveyn bağı kaldırma.

Üçü de geri alınamaz ya da ikinci bir kullanıcıyı etkiliyor; hepsinde
gerekçe ve denetim kaydı zorunlu. Dışa aktarmanın **varsayılan** yolu
panelden değil cihazdan (`GET /auth/verilerim`) — panel yolu gerekçeli bir
istisna ve testler ikisinin aynı gövdeyi ürettiğini doğruluyor, yoksa iki
üretici zamanla ayrışır ve biri eksik yanıt verir.
"""

from __future__ import annotations

import datetime as dt

import pytest

from app.modeller import (
    DenetimKaydi,
    EbeveynBaglanti,
    GunlukOzet,
    Kullanici,
    Personel,
    PersonelDurumu,
    PersonelOturum,
    PersonelRolu,
    RiskSonuc,
)

GEREKCE = "KVKK talebi geldi, kayıt numarası 2026-0142."


@pytest.fixture
def panel_girisli(db, panel_istemci):
    from app.guvenlik import jeton_ozetle, rastgele_jeton, sifre_ozetle
    from app.panel.oturum import CEREZ_ADI

    def kur(rol=PersonelRolu.OPERASYON) -> Personel:
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
    from app.panel.oturum import CEREZ_ADI, csrf_jetonu

    return csrf_jetonu(jeton_ozetle(panel_istemci.cookies[CEREZ_ADI]))


def _post(panel_istemci, yol, **alanlar):
    return panel_istemci.post(yol, data={**alanlar, "_csrf": _csrf(panel_istemci)})


def yanit_metni(govde) -> str:
    import json

    return json.dumps(govde, ensure_ascii=False)


def _kid(db) -> int:
    return db.query(Kullanici).one().id


# --- silme ---


def test_yanlis_eposta_ile_silinmiyor(db, panel_istemci, panel_girisli, kullanici_kur):
    panel_girisli()
    kullanici_kur()
    kid = _kid(db)

    yanit = _post(
        panel_istemci,
        f"/yonetim/kullanici/{kid}/sil",
        gerekce=GEREKCE,
        onay_eposta="baska@ornek.com",
    )
    assert yanit.status_code == 422
    assert db.get(Kullanici, kid) is not None


def test_gerekcesiz_silinmiyor(db, panel_istemci, panel_girisli, kullanici_kur):
    panel_girisli()
    _, veri = kullanici_kur()
    kid = _kid(db)

    yanit = _post(
        panel_istemci,
        f"/yonetim/kullanici/{kid}/sil",
        gerekce="kısa",
        onay_eposta=veri["eposta"],
    )
    assert yanit.status_code == 422
    assert db.get(Kullanici, kid) is not None


def test_silme_bagli_satirlari_da_kaldiriyor(
    db, panel_istemci, panel_girisli, kullanici_kur
):
    panel_girisli()
    _, veri = kullanici_kur()
    kid = _kid(db)
    db.add(GunlukOzet(kullanici_id=kid, tarih=dt.date(2026, 8, 17)))
    db.commit()

    yanit = _post(
        panel_istemci,
        f"/yonetim/kullanici/{kid}/sil",
        gerekce=GEREKCE,
        onay_eposta=veri["eposta"],
    )
    assert yanit.status_code in (200, 303)

    db.expire_all()
    assert db.get(Kullanici, kid) is None
    assert db.query(GunlukOzet).count() == 0


def test_silme_denetim_kaydi_hayatta_kaliyor(
    db, panel_istemci, panel_girisli, kullanici_kur
):
    """`hedef_id` yabancı anahtar değil: kullanıcı gidince kayıt kalmalı."""
    panel_girisli()
    _, veri = kullanici_kur()
    kid = _kid(db)

    _post(
        panel_istemci,
        f"/yonetim/kullanici/{kid}/sil",
        gerekce=GEREKCE,
        onay_eposta=veri["eposta"],
    )

    db.expire_all()
    kayit = db.query(DenetimKaydi).one()
    assert kayit.eylem == "kullanici.sil"
    assert kayit.hedef_id == kid
    assert kayit.gerekce == GEREKCE


# --- dışa aktarma ---


def test_cihaz_kendi_verisini_indirebiliyor(db, istemci, kullanici_kur):
    baslik, veri = kullanici_kur()
    kid = _kid(db)
    db.add(GunlukOzet(kullanici_id=kid, tarih=dt.date(2026, 8, 17), social_dk=90.0))
    db.commit()

    yanit = istemci.get("/auth/verilerim", headers=baslik)
    assert yanit.status_code == 200
    govde = yanit.json()
    assert govde["hesap"]["eposta"] == veri["eposta"]
    assert govde["gunluk_ozetler"][0]["kategori_dk"]["social"] == 90.0


def test_disa_aktarma_karsi_tarafin_kimligini_yazmiyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    kid = _kid(db)
    diger = Kullanici(eposta="ebeveyn@ornek.com", ad="Ebeveyn", sifre_ozet="x")
    db.add(diger)
    db.flush()
    db.add(EbeveynBaglanti(ebeveyn_id=diger.id, cocuk_id=kid))
    db.commit()

    govde = istemci.get("/auth/verilerim", headers=baslik).json()
    assert govde["ebeveyn_bagi"]["rol"] == "cocuk"
    assert "ebeveyn@ornek.com" not in yanit_metni(govde)


def test_panel_disa_aktarma_onaysiz_calismiyor(
    db, panel_istemci, panel_girisli, kullanici_kur
):
    panel_girisli()
    kullanici_kur()
    kid = _kid(db)

    yanit = _post(
        panel_istemci, f"/yonetim/kullanici/{kid}/veri-disa-aktar", gerekce=GEREKCE
    )
    assert yanit.status_code == 422
    assert db.query(DenetimKaydi).count() == 0, "Reddedilen işlem kayıt bırakmamalı"


def test_panel_ve_cihaz_ayni_govdeyi_uretiyor(
    db, istemci, panel_istemci, panel_girisli, kullanici_kur
):
    """Tek üretici: iki yol ayrışırsa biri eksik yanıt verir."""
    baslik, _ = kullanici_kur()
    kid = _kid(db)
    db.add(
        RiskSonuc(
            kullanici_id=kid,
            tarih=dt.date(2026, 8, 17),
            s=1.0,
            r=2.0,
            gosterilen_bant="AZ",
            trend="SABIT",
        )
    )
    db.commit()

    cihaz = istemci.get("/auth/verilerim", headers=baslik).json()

    panel_girisli()
    panel = _post(
        panel_istemci,
        f"/yonetim/kullanici/{kid}/veri-disa-aktar",
        gerekce=GEREKCE,
        onay="evet",
    )
    assert panel.status_code == 200
    assert "attachment" in panel.headers["content-disposition"]

    p = panel.json()
    # Üretim zamanı doğal olarak farklı; gerisi birebir aynı olmalı.
    for alan in ("hesap", "gunluk_ozetler", "risk_sonuclari", "lig_uyelikleri"):
        assert p[alan] == cihaz[alan]


def test_panel_disa_aktarma_denetim_kaydi_yaziyor(
    db, panel_istemci, panel_girisli, kullanici_kur
):
    panel_girisli()
    kullanici_kur()
    kid = _kid(db)

    _post(
        panel_istemci,
        f"/yonetim/kullanici/{kid}/veri-disa-aktar",
        gerekce=GEREKCE,
        onay="evet",
    )
    kayit = db.query(DenetimKaydi).one()
    assert kayit.eylem == "kullanici.veri_disa_aktar"
    assert kayit.gerekce == GEREKCE


# --- ebeveyn bağı ---


def test_ebeveyn_bagi_panelden_kaldiriliyor(
    db, panel_istemci, panel_girisli, kullanici_kur
):
    panel_girisli()
    kullanici_kur()
    cocuk_id = _kid(db)
    ebeveyn = Kullanici(eposta="ebeveyn@ornek.com", ad="Ebeveyn", sifre_ozet="x")
    db.add(ebeveyn)
    db.flush()
    db.add(EbeveynBaglanti(ebeveyn_id=ebeveyn.id, cocuk_id=cocuk_id))
    db.commit()

    yanit = _post(
        panel_istemci,
        f"/yonetim/kullanici/{cocuk_id}/ebeveyn-bagi-kaldir",
        gerekce=GEREKCE,
    )
    assert yanit.status_code in (200, 303)

    db.expire_all()
    assert db.query(EbeveynBaglanti).count() == 0
    # İki kullanıcı da yerinde: bağ kalktı, hesaplar silinmedi.
    assert db.query(Kullanici).count() == 2


def test_gozlemci_kvkk_islemi_yapamiyor(db, panel_istemci, panel_girisli, kullanici_kur):
    panel_girisli(PersonelRolu.GOZLEMCI)
    _, veri = kullanici_kur()
    kid = _kid(db)

    for yol, alanlar in (
        (f"/yonetim/kullanici/{kid}/sil", {"gerekce": GEREKCE, "onay_eposta": veri["eposta"]}),
        (f"/yonetim/kullanici/{kid}/veri-disa-aktar", {"gerekce": GEREKCE, "onay": "evet"}),
        (f"/yonetim/kullanici/{kid}/ebeveyn-bagi-kaldir", {"gerekce": GEREKCE}),
    ):
        assert _post(panel_istemci, yol, **alanlar).status_code == 403

    assert db.get(Kullanici, kid) is not None
