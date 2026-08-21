"""
Hesap arama ve detayı (`docs/panel-plani.md §5.2`).

Buradaki en önemli test davranış değil **sınır**: P9 gereği panel, tekil
kullanıcının davranış verisini görmüyor. `test_detay_davranis_verisine_hic
_dokunmuyor` çalışan SQL'i dinliyor — `gunluk_ozet` ya da `risk_sonuc`
geçen bir sorgu düşürüyor. Gösterge aynı tabloları okumaya devam ediyor
ama kullanıcı ayırt etmeden; ayrım burada.
"""

from __future__ import annotations

import datetime as dt

import pytest
from sqlalchemy import event

from app.modeller import (
    EbeveynBaglanti,
    GunlukOzet,
    Heartbeat,
    Kullanici,
    LigGrup,
    LigUyelik,
    OdulSecimi,
    RiskSonuc,
)
from app.panel.hesaplar import KullaniciDetay, ara, detay

SIMDI = dt.datetime(2026, 8, 18, 12, 0, tzinfo=dt.timezone.utc)

#: Tekil hesap yüzeyinin dokunmaması gereken tablolar.
YASAK_TABLOLAR = ("gunluk_ozet", "risk_sonuc")


@pytest.fixture
def kur(db):
    def yap(eposta="ayse@ornek.com", ad="Ayşe") -> Kullanici:
        k = Kullanici(eposta=eposta, ad=ad, sifre_ozet="ozet")
        db.add(k)
        db.commit()
        return k

    return yap


class SqlDinleyici:
    """Oturumda koşan her SQL metnini biriktirir."""

    def __init__(self, db):
        self.motor = db.get_bind()
        self.ifadeler: list[str] = []

    def __enter__(self):
        event.listen(self.motor, "before_cursor_execute", self._yakala)
        return self

    def __exit__(self, *_):
        event.remove(self.motor, "before_cursor_execute", self._yakala)

    def _yakala(self, conn, cursor, ifade, *_):
        self.ifadeler.append(ifade.lower())

    def gecen(self, ad: str) -> bool:
        return any(ad in i for i in self.ifadeler)


# --- P9 sınırı ---


def test_detay_davranis_verisine_hic_dokunmuyor(db, kur):
    k = kur()
    db.add_all(
        [
            GunlukOzet(kullanici_id=k.id, tarih=dt.date(2026, 8, 17), social_dk=444.0),
            RiskSonuc(
                kullanici_id=k.id,
                tarih=dt.date(2026, 8, 17),
                s=99.0,
                r=88.0,
                gosterilen_bant="COK",
                trend="ARTIYOR",
            ),
        ]
    )
    db.commit()
    db.expire_all()

    with SqlDinleyici(db) as dinleyici:
        d = detay(db, k.id)

    assert d is not None
    for tablo in YASAK_TABLOLAR:
        assert not dinleyici.gecen(tablo), (
            f"Hesap detayı `{tablo}` tablosunu sorguladı — P9 sınırı bu: tekil "
            "kullanıcının davranış verisi panelde görünmüyor "
            "(docs/panel-plani.md §5.2). Toplu istatistik için gosterge.py var."
        )


def test_arama_davranis_verisine_hic_dokunmuyor(db, kur):
    kur()
    with SqlDinleyici(db) as dinleyici:
        ara(db, "ornek.com")
    for tablo in YASAK_TABLOLAR:
        assert not dinleyici.gecen(tablo), f"Arama `{tablo}` tablosunu sorguladı"


def test_detay_modelinde_davranis_alani_yok():
    """Sorgu yapılmasa bile alanın var olması, doldurulmasına davetiye."""
    alanlar = set(KullaniciDetay.__dataclass_fields__)
    yasak = {"risk_bandi", "skor", "ekran_dk", "gece", "kategoriler", "oruntuler"}
    assert not (alanlar & yasak)


# --- arama ---


def test_bos_sorgu_hicbir_hesap_dondurmuyor(db, kur):
    kur()
    kur(eposta="baska@ornek.com")
    satirlar, toplam = ara(db, "   ")
    assert satirlar == [] and toplam == 0


def test_eposta_parcasiyla_buluyor(db, kur):
    k = kur(eposta="ayse.yilmaz@ornek.com")
    kur(eposta="mehmet@baska.com")
    satirlar, toplam = ara(db, "AYSE.yil")
    assert toplam == 1 and satirlar[0].id == k.id


def test_kullanici_numarasiyla_buluyor(db, kur):
    k = kur()
    satirlar, _ = ara(db, str(k.id))
    assert [s.id for s in satirlar] == [k.id]


def test_ad_ile_aranmiyor(db, kur):
    """§5.2: ad benzersiz değil, yanlış hesaba müdahale riski."""
    kur(eposta="a@ornek.com", ad="Zeynep Kaya")
    satirlar, toplam = ara(db, "Zeynep")
    assert satirlar == [] and toplam == 0


def test_son_sinyal_satira_geliyor(db, kur):
    k = kur()
    db.add(Heartbeat(kullanici_id=k.id, son_sinyal=SIMDI))
    db.commit()
    satirlar, _ = ara(db, k.eposta)
    assert satirlar[0].son_sinyal is not None


def test_sinyali_olmayan_hesap_da_listeleniyor(db, kur):
    """OUTER JOIN: paylaşımı kapalı kullanıcı aramadan düşmemeli."""
    k = kur()
    satirlar, _ = ara(db, k.eposta)
    assert len(satirlar) == 1 and satirlar[0].son_sinyal is None


# --- detay ---


def test_olmayan_hesap_none(db):
    assert detay(db, 9999) is None


def test_ebeveyn_bagi_iki_yonden_de_goruluyor(db, kur):
    ebeveyn = kur(eposta="anne@ornek.com")
    cocuk = kur(eposta="cocuk@ornek.com")
    db.add(EbeveynBaglanti(ebeveyn_id=ebeveyn.id, cocuk_id=cocuk.id))
    db.commit()

    assert detay(db, ebeveyn.id).ebeveyn_rolu == "ebeveyn"
    assert detay(db, cocuk.id).ebeveyn_rolu == "çocuk"
    assert detay(db, ebeveyn.id).ebeveyn_bagi_var


def test_bagi_olmayanda_rol_bos(db, kur):
    k = kur()
    d = detay(db, k.id)
    assert not d.ebeveyn_bagi_var and d.ebeveyn_rolu == ""


def test_lig_ve_odul_geliyor(db, kur):
    k = kur()
    grup = LigGrup(hafta_baslangic=dt.date(2026, 8, 17), seviye="FIDAN")
    db.add(grup)
    db.flush()
    db.add_all(
        [
            LigUyelik(
                kullanici_id=k.id,
                grup_id=grup.id,
                hafta_baslangic=dt.date(2026, 8, 17),
                haftalik_puan=49,
            ),
            OdulSecimi(kullanici_id=k.id, kademe_id="kupon_market", secenek_id="fidan_bagisi"),
        ]
    )
    db.commit()

    d = detay(db, k.id)
    assert [u.haftalik_puan for u in d.lig_gecmisi] == [49]
    assert [o.secenek_id for o in d.odul_secimleri] == ["fidan_bagisi"]


# --- HTTP: rol kapısı ve render ---


@pytest.fixture
def girisli(db, panel_istemci):
    """Doğrulanmış panel oturumu kuran istemci — rolü çağıran seçiyor."""
    import datetime as _dt

    from app.guvenlik import jeton_ozetle, rastgele_jeton, sifre_ozetle
    from app.modeller import Personel, PersonelDurumu, PersonelOturum, PersonelRolu
    from app.panel.oturum import CEREZ_ADI

    def kur(rol=PersonelRolu.ADMIN):
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
                mutlak_bitis=_dt.datetime.now(_dt.timezone.utc) + _dt.timedelta(hours=4),
                iptal=False,
            )
        )
        db.commit()
        panel_istemci.cookies.set(CEREZ_ADI, ham)
        return panel_istemci

    return kur


def test_gozlemci_hesap_yuzeyine_giremiyor(girisli, kur):
    """§2 'Roller': gozlemci yalnızca toplu/anonim istatistik görüyor."""
    from app.modeller import PersonelRolu

    k = kur()
    istemci = girisli(PersonelRolu.GOZLEMCI)
    assert istemci.get("/yonetim/kullanici?ara=ornek").status_code == 403
    assert istemci.get(f"/yonetim/kullanici/{k.id}").status_code == 403


def test_operasyon_arama_ve_detayi_goruyor(girisli, kur):
    from app.modeller import PersonelRolu

    k = kur(eposta="destek@ornek.com")
    istemci = girisli(PersonelRolu.OPERASYON)

    liste = istemci.get("/yonetim/kullanici?ara=destek")
    assert liste.status_code == 200
    assert "destek@ornek.com" in liste.text

    d = istemci.get(f"/yonetim/kullanici/{k.id}")
    assert d.status_code == 200
    assert "destek@ornek.com" in d.text


def test_olmayan_hesap_404(girisli):
    from app.modeller import PersonelRolu

    assert girisli(PersonelRolu.ADMIN).get("/yonetim/kullanici/9999").status_code == 404


def test_detay_sayfasi_davranis_verisi_basmiyor(girisli, kur, db):
    """Şablon düzeyinde de sızmasın: ayırt edici sayılar sayfada geçmemeli."""
    import datetime as _dt

    from app.modeller import PersonelRolu

    k = kur()
    db.add_all(
        [
            GunlukOzet(kullanici_id=k.id, tarih=_dt.date(2026, 8, 17), social_dk=444.0),
            RiskSonuc(
                kullanici_id=k.id,
                tarih=_dt.date(2026, 8, 17),
                s=99.0,
                r=88.0,
                gosterilen_bant="COK",
                trend="ARTIYOR",
            ),
        ]
    )
    db.commit()

    sayfa = girisli(PersonelRolu.ADMIN).get(f"/yonetim/kullanici/{k.id}").text

    # CSRF jetonu rastgele hex ve "444" gibi bir rakam dizisi orada da
    # geçebiliyor — testin ilk hâli bu yüzden kararsızdı ve konteynerde
    # rastgele düştü. Jetonlar aranmadan önce çıkarılıyor: aranan şey
    # ekrandaki **veri**, sayfanın rastgele parçaları değil.
    import re as _re

    govde = _re.sub(r'value="[0-9a-f]{16,}"', 'value="..."', sayfa)
    assert "444" not in govde
    assert "COK" not in govde


def test_htmx_istegi_yalnizca_parcayi_donduruyor(girisli, kur):
    from app.modeller import PersonelRolu

    kur(eposta="parca@ornek.com")
    istemci = girisli(PersonelRolu.ADMIN)
    yanit = istemci.get("/yonetim/kullanici?ara=parca", headers={"HX-Request": "true"})
    assert yanit.status_code == 200
    assert "parca@ornek.com" in yanit.text
    assert "<nav" not in yanit.text, "HTMX parçası tam sayfayı döndürmemeli"
