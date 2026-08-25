"""
Gösterge metrikleri ve SVG grafikler.

Yanlış sayan bir metrik, panelde fark edilmesi en zor hata türü: ekranda
makul görünen bir sayı duruyor ve kimse sorgulamıyor. Bu yüzden sorgular
router'dan ayrı bir modülde ve tek tek sınanıyor.
"""

from __future__ import annotations

import datetime as dt

import pytest

from app.modeller import (
    EbeveynBaglanti,
    GunlukOzet,
    Heartbeat,
    Kullanici,
    LigGrup,
    LigUyelik,
    RiskSonuc,
)
from app.panel.gosterge import gostergeyi_hesapla
from app.panel.grafik import BOS_METIN, bant_dagilimi_svg, risk_cizgisi_svg
from app.zaman import yerel_bugun

SIMDI = dt.datetime(2026, 8, 18, 12, 0, tzinfo=dt.timezone.utc)


@pytest.fixture
def kullanici(db):
    sayac = {"n": 0}

    def kur() -> Kullanici:
        sayac["n"] += 1
        k = Kullanici(
            eposta=f"k{sayac['n']}@ornek.com",
            ad=f"Kullanıcı {sayac['n']}",
            sifre_ozet="ozet",
        )
        db.add(k)
        db.commit()
        return k

    return kur


def _risk(db, kullanici_id, gun_once, bant, durum="HESAPLANDI"):
    db.add(
        RiskSonuc(
            kullanici_id=kullanici_id,
            tarih=yerel_bugun() - dt.timedelta(days=gun_once),
            s=10.0,
            r=40.0,
            gosterilen_bant=bant,
            trend="SABIT",
            veri_durumu=durum,
        )
    )
    db.commit()


def _ozet(db, kullanici_id, gun_once, dakika):
    db.add(
        GunlukOzet(
            kullanici_id=kullanici_id,
            tarih=yerel_bugun() - dt.timedelta(days=gun_once),
            social_dk=dakika,
        )
    )
    db.commit()


# --- boş durum ---


def test_bos_veritabani_sifir_donuyor(db):
    g = gostergeyi_hesapla(db, simdi=SIMDI)
    assert g.kullanici_sayisi == 0
    assert g.bant_toplami == 0
    assert g.ortalama_ekran_dk == 0
    assert g.bant_yuzdesi("AZ") == 0


# --- bant dağılımı ---


def test_yetersiz_veri_banda_sayilmiyor(db, kullanici):
    """
    CLAUDE.md §12 K30: ölçümü yetersiz gün "az riskli" değil.

    Sunucuya yine de satır gelebiliyor; `veri_durumu` ayırıyor.
    """
    k = kullanici()
    _risk(db, k.id, 1, "AZ", durum="YETERSIZ")
    g = gostergeyi_hesapla(db, simdi=SIMDI)
    assert g.bant_toplami == 0
    assert g.bandi_olmayan == 1


def test_kullanici_bir_kez_sayiliyor(db, kullanici):
    """
    Gün gün saymak, uygulamayı çok açan kullanıcıyı birden çok kez saymak
    olurdu. Her kullanıcının penceredeki EN SON bandı alınıyor.
    """
    k = kullanici()
    _risk(db, k.id, 3, "AZ")
    _risk(db, k.id, 2, "ORTA")
    _risk(db, k.id, 1, "COK")

    g = gostergeyi_hesapla(db, simdi=SIMDI)
    assert g.bant_toplami == 1
    assert g.bant_dagilimi["COK"] == 1
    assert g.bant_dagilimi["AZ"] == 0


def test_pencere_disindaki_bant_sayilmiyor(db, kullanici):
    k = kullanici()
    _risk(db, k.id, 30, "COK")
    g = gostergeyi_hesapla(db, simdi=SIMDI, pencere_gun=7)
    assert g.bant_toplami == 0


def test_yuzde_hesabi(db, kullanici):
    for bant in ("AZ", "AZ", "AZ", "COK"):
        _risk(db, kullanici().id, 1, bant)
    g = gostergeyi_hesapla(db, simdi=SIMDI)
    assert g.bant_yuzdesi("AZ") == 75
    assert g.bant_yuzdesi("COK") == 25


# --- kullanım ortalaması ---


def test_sifir_gunler_ortalamayi_dusurmuyor(db, kullanici):
    """'Satır var ≠ veri var' (CLAUDE.md §9) — sunucu tarafı karşılığı."""
    k = kullanici()
    _ozet(db, k.id, 1, 120.0)
    _ozet(db, k.id, 2, 0.0)
    g = gostergeyi_hesapla(db, simdi=SIMDI)
    assert g.ortalama_ekran_dk == 120


# --- aktiflik ---


def test_aktiflik_heartbeat_penceresine_bakiyor(db, kullanici):
    yeni, eski = kullanici(), kullanici()
    db.add(Heartbeat(kullanici_id=yeni.id, son_sinyal=SIMDI - dt.timedelta(days=2)))
    db.add(Heartbeat(kullanici_id=eski.id, son_sinyal=SIMDI - dt.timedelta(days=20)))
    db.commit()

    g = gostergeyi_hesapla(db, simdi=SIMDI)
    assert g.aktif_7 == 1
    assert g.aktif_30 == 2


def test_lig_ve_ebeveyn_sayilari(db, kullanici):
    cocuk, ebeveyn = kullanici(), kullanici()
    grup = LigGrup(hafta_baslangic=yerel_bugun(), seviye=0)
    db.add(grup)
    db.commit()
    db.add(
        LigUyelik(
            kullanici_id=cocuk.id,
            grup_id=grup.id,
            hafta_baslangic=yerel_bugun(),
            haftalik_puan=10,
        )
    )
    db.add(EbeveynBaglanti(ebeveyn_id=ebeveyn.id, cocuk_id=cocuk.id))
    db.commit()

    g = gostergeyi_hesapla(db, simdi=SIMDI)
    assert g.lig_katilimi == 1
    assert g.ebeveyn_eslesmesi == 1


# --- grafikler ---


def test_bos_dagilim_veri_yok_yaziyor():
    """Boş grafik çizmek "herkes az riskli" izlenimi verirdi."""
    assert BOS_METIN in bant_dagilimi_svg({})


def test_dagilim_grafigi_oranlari_tasiyor():
    svg = bant_dagilimi_svg({"AZ": 1, "ORTA": 1, "COK": 2}, genislik=400)
    assert 'viewBox="0 0 400 48"' in svg
    assert svg.count("<rect") == 3
    # Yarısı COK olduğu için son dikdörtgen 200 birim geniş.
    assert 'width="200.00"' in svg


def test_dagilimda_sifir_bant_cizilmiyor():
    svg = bant_dagilimi_svg({"AZ": 3, "ORTA": 0, "COK": 0})
    assert svg.count("<rect") == 1


def test_risk_cizgisi_bos_girdide_veri_yok():
    assert BOS_METIN in risk_cizgisi_svg([])


def test_risk_cizgisi_tek_noktayla_cizilebiliyor():
    svg = risk_cizgisi_svg([(dt.date(2026, 8, 18), 40.0)])
    assert "<polyline" in svg


def test_risk_cizgisi_tavani_asmiyor():
    """Tavanı aşan bir skor çizim alanının dışına taşmamalı (K49 ailesi)."""
    svg = risk_cizgisi_svg(
        [(dt.date(2026, 8, 17), 0.0), (dt.date(2026, 8, 18), 500.0)],
        yukseklik=100,
        tavan=100.0,
    )
    ykoordlar = [float(n.split(",")[1]) for n in svg.split('points="')[1].split('"')[0].split()]
    assert min(ykoordlar) >= 0
    assert max(ykoordlar) <= 100


def test_grafik_kacisi_yapiyor():
    """Bant adı doğrudan SVG'ye gömülüyor; kaçırılmazsa enjeksiyon olurdu."""
    svg = bant_dagilimi_svg({"AZ": 1})
    assert "<title>AZ: 1</title>" in svg


# --- F2/7: trend ve askıdaki hesap ---


def test_trend_onceki_pencereyle_karsilastiriyor(db, kullanici):
    k = kullanici()
    bugun = yerel_bugun()
    # Bu pencere: 100 dk. Önceki pencere: 60 dk.
    db.add(GunlukOzet(kullanici_id=k.id, tarih=bugun, social_dk=100.0))
    db.add(
        GunlukOzet(
            kullanici_id=k.id, tarih=bugun - dt.timedelta(days=8), social_dk=60.0
        )
    )
    db.commit()

    g = gostergeyi_hesapla(db, SIMDI, pencere_gun=7)
    assert g.ortalama_ekran_dk == 100
    assert g.onceki_ortalama_ekran_dk == 60
    assert g.ekran_farki_dk == 40


def test_onceki_pencerede_olcum_yoksa_fark_none(db, kullanici):
    """Sıfır sayılsaydı yeni sistemde "%100 arttı" gibi bir sayı çıkardı."""
    k = kullanici()
    db.add(GunlukOzet(kullanici_id=k.id, tarih=yerel_bugun(), social_dk=100.0))
    db.commit()

    g = gostergeyi_hesapla(db, SIMDI, pencere_gun=7)
    assert g.onceki_ortalama_ekran_dk is None
    assert g.ekran_farki_dk is None


def test_askidaki_hesap_sayiliyor(db, kullanici):
    from app.modeller import KullaniciDurumu

    a, b = kullanici(), kullanici()
    a.durum = KullaniciDurumu.ASKIDA.value
    db.commit()

    g = gostergeyi_hesapla(db, SIMDI)
    assert g.askidaki_hesap == 1
    assert g.kullanici_sayisi == 2, "Askıdaki hesap toplamdan düşmüyor"


# --- F4: açık YEDAM başvurusu ---


def test_acik_yedam_sayiliyor_bekleyen_sayilmiyor(db, kullanici):
    """Ebeveyn onayı bekleyen başvuru hiçbir danışmana görünmüyor."""
    from app.modeller import BasvuruDurumu, YedamBasvuru, YedamRiza

    def basvuru(durum):
        k = kullanici()
        riza = YedamRiza(
            kullanici_id=k.id,
            metin_surumu="v1",
            kapsam="kimlik",
            son_gecerlilik=SIMDI + dt.timedelta(days=90),
        )
        db.add(riza)
        db.flush()
        db.add(
            YedamBasvuru(
                kullanici_id=k.id,
                riza_id=riza.id,
                durum=durum.value,
                iletisim_turu="telefon",
                iletisim_degeri="0555",
                olusturma=SIMDI - dt.timedelta(days=4),
            )
        )
        db.commit()

    basvuru(BasvuruDurumu.YENI)
    basvuru(BasvuruDurumu.ATANDI)
    basvuru(BasvuruDurumu.EBEVEYN_ONAYI_BEKLIYOR)
    basvuru(BasvuruDurumu.KAPANDI)

    g = gostergeyi_hesapla(db, SIMDI)
    assert g.acik_yedam == 2
    assert g.en_eski_yedam_gun == 4


def test_acik_basvuru_yoksa_yas_none(db):
    """"Hiç başvuru yok" ile "bugün gelmiş bir başvuru var" aynı şey değil."""
    g = gostergeyi_hesapla(db, SIMDI)
    assert g.acik_yedam == 0
    assert g.en_eski_yedam_gun is None
