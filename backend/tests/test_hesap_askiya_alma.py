"""
Hesap askıya alma (`docs/panel-plani.md §5.2`).

Askı iki tarafta birden yaşıyor: panel yazıyor, cihaz API'si uyguluyor.
Buradaki testlerin çoğu **ikinci tarafı** koruyor — kapı `mevcut_kullanici`
bağımlılığında tek yerde durduğu için her uç ayrı ayrı doğru yazılmak
zorunda değil, ama o tek yerin sessizce kalkması da mümkün.

403 gövdesinin `hata: hesap_askida` alanı Android'in çevrimdışıyla askıyı
ayırdığı yer (CLAUDE.md §12 K18/K42): adı değişirse cihaz sessizce yanlış
ekranı gösterir, o yüzden ayrıca sınanıyor.
"""

from __future__ import annotations

import datetime as dt

import pytest

from app.guvenlik import ASKIDA_KODU
from app.modeller import (
    Kullanici,
    KullaniciDurumu,
    Personel,
    PersonelDurumu,
    PersonelOturum,
    PersonelRolu,
    YenilemeJetonu,
)

GEREKCE = "Hesap paylaşımı tespit edildi, doğrulama bekleniyor."


def _kid(db) -> int:
    """Tek kullanıcılı testlerde o kullanıcının numarası."""
    return db.query(Kullanici).one().id


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
    """Oturuma bağlı CSRF jetonu — şablonun forma gömdüğü değerin aynısı."""
    from app.guvenlik import jeton_ozetle
    from app.panel.oturum import CEREZ_ADI, csrf_jetonu

    return csrf_jetonu(jeton_ozetle(panel_istemci.cookies[CEREZ_ADI]))


def _gonder(panel_istemci, yol, gerekce=GEREKCE):
    return panel_istemci.post(
        yol, data={"gerekce": gerekce, "_csrf": _csrf(panel_istemci)}
    )


def _askiya_al(panel_istemci, kullanici_id, gerekce=GEREKCE):
    return _gonder(
        panel_istemci, f"/yonetim/kullanici/{kullanici_id}/askiya-al", gerekce
    )


# --- panel tarafı ---


def test_gerekce_kisaysa_reddediliyor(db, panel_istemci, panel_girisli, kullanici_kur):
    panel_girisli()
    _, veri = kullanici_kur()
    kid = _kid(db)

    yanit = _askiya_al(panel_istemci, kid, gerekce="kısa")
    assert yanit.status_code == 422
    assert db.get(Kullanici, kid).durum == "aktif"


def test_askiya_alma_denetim_kaydi_yaziyor(db, panel_istemci, panel_girisli, kullanici_kur):
    from app.modeller import DenetimKaydi

    panel_girisli()
    kullanici_kur()
    kid = _kid(db)

    assert _askiya_al(panel_istemci, kid).status_code in (200, 303)

    db.expire_all()
    k = db.get(Kullanici, kid)
    assert k.durum == KullaniciDurumu.ASKIDA.value
    assert k.askiya_alma_sebebi == GEREKCE
    assert k.askiya_alan_id is not None

    kayit = db.query(DenetimKaydi).one()
    assert kayit.eylem == "kullanici_askiya_al"
    assert kayit.hedef_id == kid
    assert kayit.gerekce == GEREKCE


def test_ikinci_askiya_alma_sebebi_ezmiyor(db, panel_istemci, panel_girisli, kullanici_kur):
    panel_girisli()
    kullanici_kur()
    kid = _kid(db)

    _askiya_al(panel_istemci, kid)
    yanit = _askiya_al(panel_istemci, kid, gerekce="Bambaşka bir gerekçe yazıldı burada.")
    assert yanit.status_code == 409

    db.expire_all()
    assert db.get(Kullanici, kid).askiya_alma_sebebi == GEREKCE


def test_askiyi_kaldirma_sebebi_temizliyor(db, panel_istemci, panel_girisli, kullanici_kur):
    panel_girisli()
    kullanici_kur()
    kid = _kid(db)
    _askiya_al(panel_istemci, kid)

    yanit = _gonder(
        panel_istemci,
        f"/yonetim/kullanici/{kid}/askiyi-kaldir",
        "Doğrulama tamamlandı, hesap iade ediliyor.",
    )
    assert yanit.status_code in (200, 303)

    db.expire_all()
    k = db.get(Kullanici, kid)
    assert k.durum == "aktif"
    assert k.askiya_alma_sebebi == ""
    assert k.askiya_alan_id is None


def test_gozlemci_askiya_alamiyor(db, panel_istemci, panel_girisli, kullanici_kur):
    panel_girisli(PersonelRolu.GOZLEMCI)
    kullanici_kur()
    kid = _kid(db)
    yanit = _askiya_al(panel_istemci, kid)
    assert yanit.status_code == 403
    # CSRF değil ROL reddi olduğunu doğrula: jeton geçerli gönderildi.
    assert "yetkiniz yok" in yanit.text.lower()
    assert db.get(Kullanici, kid).durum == "aktif"


# --- cihaz tarafı: kapı gerçekten uygulanıyor mu ---


def _askiya_al_dogrudan(db, kid):
    k = db.get(Kullanici, kid)
    k.durum = KullaniciDurumu.ASKIDA.value
    k.askiya_alma_sebebi = GEREKCE
    db.commit()


def test_askidaki_kullanici_korumali_uca_giremiyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    kid = _kid(db)
    _askiya_al_dogrudan(db, kid)

    yanit = istemci.get("/lig", headers=baslik)
    assert yanit.status_code == 403
    govde = yanit.json()["detail"]
    assert govde["hata"] == ASKIDA_KODU
    assert govde["sebep"] == GEREKCE


def test_askidaki_kullanici_giris_yapamiyor(db, istemci, kullanici_kur):
    kullanici_kur(eposta="askida@ornek.com", sifre="parola123")
    kid = _kid(db)
    _askiya_al_dogrudan(db, kid)

    yanit = istemci.post(
        "/auth/giris", json={"eposta": "askida@ornek.com", "sifre": "parola123"}
    )
    assert yanit.status_code == 403
    assert yanit.json()["detail"]["hata"] == ASKIDA_KODU


def test_yanlis_parola_askiyi_sizdirmiyor(db, istemci, kullanici_kur):
    """Askı bilgisi ancak parola doğrulandıktan sonra veriliyor."""
    kullanici_kur(eposta="askida@ornek.com", sifre="parola123")
    kid = _kid(db)
    _askiya_al_dogrudan(db, kid)

    yanit = istemci.post(
        "/auth/giris", json={"eposta": "askida@ornek.com", "sifre": "YANLIS-parola"}
    )
    assert yanit.status_code == 401
    assert "askida" not in yanit.text


def test_askiya_alma_yenileme_jetonlarini_iptal_ediyor(
    db, panel_istemci, panel_girisli, istemci, kullanici_kur
):
    panel_girisli()
    _, veri = kullanici_kur()
    kid = _kid(db)

    _askiya_al(panel_istemci, kid)

    db.expire_all()
    assert db.query(YenilemeJetonu).count() > 0
    assert all(j.iptal for j in db.query(YenilemeJetonu).all())

    yanit = istemci.post("/auth/yenile", json={"yenileme": veri["yenileme"]})
    assert yanit.status_code == 401


def test_aktif_hesap_etkilenmiyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    assert istemci.get("/lig", headers=baslik).status_code == 200
