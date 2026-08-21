"""
İçerik ve duyuru (`docs/panel-plani.md §5.5, §5.6` · S5 kararı).

Bitiş ölçütü: bir yazı düzeltmesi APK yayını olmadan telefona iniyor.
Testler iki sınırı koruyor:

- **Gömülü set yedek kalıyor.** Sunucuda içerik yoksa uç boş dönüyor ve
  cihaz gömülü sete düşüyor; uydurma yazı üretilmiyor.
- **Yayımlamak geri alınamaz.** Duyuru bir kez indiğinde bildirim
  defterine yazılıyor; taslak varsayılan ve ikinci kez yayımlanamıyor.
"""

from __future__ import annotations

import datetime as dt

import pytest

from app.modeller import (
    BilgiYazisi,
    DenetimKaydi,
    Duyuru,
    Personel,
    PersonelDurumu,
    PersonelOturum,
    PersonelRolu,
)
from app.panel.oturum import CEREZ_ADI

GEREKCE = "Yesilay icerik ekibinden gelen duzeltme talebi."


@pytest.fixture
def girisli(db, panel_istemci):
    from app.guvenlik import jeton_ozetle, rastgele_jeton, sifre_ozetle

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
    from app.panel.oturum import csrf_jetonu

    return csrf_jetonu(jeton_ozetle(panel_istemci.cookies[CEREZ_ADI]))


def _post(panel_istemci, yol, **alanlar):
    return panel_istemci.post(yol, data={**alanlar, "_csrf": _csrf(panel_istemci)})


YAZI = {
    "yazi_id": "ekran-ve-uyku",
    "baslik": "Ekran süresi ve uyku",
    "ozet": "Yatmadan önceki yarım saat.",
    "kategori": "Uyku",
    "okuma_dk": 2,
    "govde": "İlk paragraf.\nİkinci paragraf.",
    "yayinda": "evet",
    "gerekce": GEREKCE,
}


# --- cihaz ucu ---


def test_sunucuda_icerik_yoksa_bos_donuyor(istemci):
    """Cihaz bunu görünce gömülü sete düşüyor (S5); uç uydurma yazı üretmiyor."""
    yanit = istemci.get("/icerik/bilgi-bankasi")
    assert yanit.status_code == 200
    assert yanit.json() == {"surum": 0, "yazilar": []}


def test_kimlik_istemiyor(istemci, db):
    """Misafir kullanıcı da okuyor; kimlik şartı onu okuyamaz hâle getirirdi."""
    assert istemci.get("/icerik/bilgi-bankasi").status_code == 200
    assert istemci.get("/icerik/duyuru").status_code == 200


def test_govde_paragraf_listesine_cevriliyor(db, panel_istemci, girisli, istemci):
    girisli()
    _post(panel_istemci, "/yonetim/icerik", **YAZI)

    yazi = istemci.get("/icerik/bilgi-bankasi").json()["yazilar"][0]
    assert yazi["govde"] == ["İlk paragraf.", "İkinci paragraf."]
    assert yazi["okumaDk"] == 2


def test_yayindan_kaldirilan_listeden_dusuyor_tekil_okuma_calisiyor(
    db, panel_istemci, girisli, istemci
):
    """Önbellekte kalmış bir yazının bağlantısını kırmak ayrı bir şey."""
    girisli()
    _post(panel_istemci, "/yonetim/icerik", **{**YAZI, "yayinda": ""})

    assert istemci.get("/icerik/bilgi-bankasi").json()["yazilar"] == []
    tekil = istemci.get("/icerik/bilgi-bankasi/ekran-ve-uyku")
    assert tekil.status_code == 200
    assert tekil.json()["baslik"] == "Ekran süresi ve uyku"


def test_olmayan_yazi_404(istemci):
    assert istemci.get("/icerik/bilgi-bankasi/yok").status_code == 404


def test_surum_yazi_degisince_degisiyor(db, panel_istemci, girisli, istemci):
    """Cihazın önbelleği bununla geçersizleşiyor."""
    girisli()
    _post(panel_istemci, "/yonetim/icerik", **YAZI)
    ilk = istemci.get("/icerik/bilgi-bankasi").json()["surum"]

    _post(panel_istemci, "/yonetim/icerik", **{**YAZI, "baslik": "Düzeltilmiş başlık"})
    ikinci = istemci.get("/icerik/bilgi-bankasi").json()["surum"]

    assert ikinci > ilk
    assert db.get(BilgiYazisi, "ekran-ve-uyku").surum == 2


def test_surum_yazi_eklenince_de_degisiyor(db, panel_istemci, girisli, istemci):
    girisli()
    _post(panel_istemci, "/yonetim/icerik", **YAZI)
    ilk = istemci.get("/icerik/bilgi-bankasi").json()["surum"]
    _post(panel_istemci, "/yonetim/icerik", **{**YAZI, "yazi_id": "ikinci-yazi"})
    assert istemci.get("/icerik/bilgi-bankasi").json()["surum"] > ilk


# --- panel: yazı ---


def test_gerekcesiz_yazi_kaydedilemiyor(db, panel_istemci, girisli):
    girisli()
    yanit = _post(panel_istemci, "/yonetim/icerik", **{**YAZI, "gerekce": "kısa"})
    assert yanit.status_code == 422
    assert db.query(BilgiYazisi).count() == 0


def test_yazi_denetim_kaydi_biraliyor(db, panel_istemci, girisli):
    girisli()
    _post(panel_istemci, "/yonetim/icerik", **YAZI)
    kayit = db.query(DenetimKaydi).one()
    assert kayit.eylem == "icerik.yazi_ekle"
    assert '"surum": 1' in kayit.detay


def test_gozlemci_icerik_duzenleyemiyor(db, panel_istemci, girisli):
    girisli(PersonelRolu.GOZLEMCI)
    assert _post(panel_istemci, "/yonetim/icerik", **YAZI).status_code == 403


# --- duyuru ---


DUYURU = {"baslik": "Yeni özellik", "metin": "Aktiviteler sekmesi yenilendi.", "gerekce": GEREKCE}


def test_taslak_duyuru_cihaza_inmiyor(db, panel_istemci, girisli, istemci):
    """Yayımlanmamış kayıt henüz bir karar değil."""
    girisli()
    _post(panel_istemci, "/yonetim/duyuru", **DUYURU)

    assert db.query(Duyuru).one().yayin_zamani is None
    assert istemci.get("/icerik/duyuru").json() == []


def test_yayimlanan_duyuru_iniyor(db, panel_istemci, girisli, istemci):
    girisli()
    _post(panel_istemci, "/yonetim/duyuru", **{**DUYURU, "yayimla": "evet"})

    kayitlar = istemci.get("/icerik/duyuru").json()
    assert len(kayitlar) == 1
    assert kayitlar[0]["baslik"] == "Yeni özellik"


def test_sonrasi_ile_yalnizca_yeniler_geliyor(db, panel_istemci, girisli, istemci):
    """Aynı duyuruyu ikinci kez bildirmek, aynı şeyi iki kez söylemek olurdu."""
    girisli()
    _post(panel_istemci, "/yonetim/duyuru", **{**DUYURU, "yayimla": "evet"})
    _post(panel_istemci, "/yonetim/duyuru", **{**DUYURU, "baslik": "İkinci", "yayimla": "evet"})

    hepsi = istemci.get("/icerik/duyuru").json()
    assert len(hepsi) == 2
    sonrakiler = istemci.get("/icerik/duyuru", params={"sonrasi": hepsi[0]["id"]}).json()
    assert [d["baslik"] for d in sonrakiler] == ["İkinci"]


def test_taslak_sonradan_yayimlanabiliyor(db, panel_istemci, girisli, istemci):
    girisli()
    _post(panel_istemci, "/yonetim/duyuru", **DUYURU)
    did = db.query(Duyuru).one().id

    assert _post(
        panel_istemci, f"/yonetim/duyuru/{did}/yayimla", gerekce=GEREKCE
    ).status_code in (200, 303)
    assert len(istemci.get("/icerik/duyuru").json()) == 1


def test_ikinci_kez_yayimlanamiyor(db, panel_istemci, girisli):
    """Duyuru bir kez indiğinde geri çekmenin yolu yok."""
    girisli()
    _post(panel_istemci, "/yonetim/duyuru", **{**DUYURU, "yayimla": "evet"})
    did = db.query(Duyuru).one().id
    assert _post(
        panel_istemci, f"/yonetim/duyuru/{did}/yayimla", gerekce=GEREKCE
    ).status_code == 409


def test_emir_kipi_uyari_biraliyor_ama_engellemiyor(db, panel_istemci, girisli):
    """§4 kuralı serbest metinde kodla zorlanamıyor; uyarmak zorlamaktan farklı."""
    girisli()
    yanit = _post(
        panel_istemci,
        "/yonetim/duyuru",
        **{**DUYURU, "metin": "Hemen uygulamayı aç, geç kalma!"},
    )
    assert yanit.status_code in (200, 303), "Kayıt engellenmemeli"

    kayit = db.query(DenetimKaydi).one()
    assert "hemen" in kayit.detay and "geç kalma" in kayit.detay


def test_aksansiz_yazim_da_yakalaniyor(db, panel_istemci, girisli):
    """
    Gerçek doğrulamada çıktı: "geç kalma" aranırken "gec kalma" kaçıyordu.

    Tarama aksana duyarlı olsaydı işe yaramazdı — insanlar aksansız
    yazıyor ve uyarı tam da o zaman gerekiyor.
    """
    girisli()
    _post(
        panel_istemci,
        "/yonetim/duyuru",
        **{**DUYURU, "metin": "Hemen uygulamayi ac, gec kalma!"},
    )
    detay = db.query(DenetimKaydi).one().detay
    assert "hemen" in detay and "geç kalma" in detay


def test_temiz_metinde_uyari_yok(db, panel_istemci, girisli):
    girisli()
    _post(panel_istemci, "/yonetim/duyuru", **DUYURU)
    assert db.query(DenetimKaydi).one().detay == ""
