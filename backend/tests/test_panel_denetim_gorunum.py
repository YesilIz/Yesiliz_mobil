"""
Denetim kaydı görüntüleyici (`docs/panel-plani.md §5.8`).

Görüntüleyici yalnızca okuyor; testlerin işi filtrelerin **doğru kesişmesi**
ve rol daraltmasının gerçekten uygulanması. Yanlış filtre, denetim izini
işe yaramaz kılar: aranan kayıt bulunamayınca "kayıt yok" sanılır.
"""

from __future__ import annotations

import datetime as dt

import pytest

from app.modeller import (
    DenetimKaydi,
    Personel,
    PersonelDurumu,
    PersonelOturum,
    PersonelRolu,
)
from app.panel.denetim_gorunum import Filtre, eylem_secenekleri, listele

SIMDI = dt.datetime(2026, 8, 18, 12, 0, tzinfo=dt.timezone.utc)


@pytest.fixture
def personel(db):
    from app.guvenlik import sifre_ozetle

    sayac = {"n": 0}

    def kur(rol=PersonelRolu.ADMIN) -> Personel:
        sayac["n"] += 1
        p = Personel(
            eposta=f"p{sayac['n']}@yesilay.org.tr",
            ad=f"Personel {sayac['n']}",
            sifre_ozet=sifre_ozetle("parola"),
            rol=rol.value,
            durum=PersonelDurumu.AKTIF.value,
            totp_sir="",
        )
        db.add(p)
        db.commit()
        return p

    return kur


def _kayit(db, personel_id, eylem="kullanici.sil", hedef_id=1, zaman=SIMDI,
           hedef_tur="kullanici"):
    k = DenetimKaydi(
        personel_id=personel_id,
        eylem=eylem,
        hedef_tur=hedef_tur,
        hedef_id=hedef_id,
        gerekce="Yeterince uzun bir gerekçe metni.",
        ip="127.0.0.1",
        zaman=zaman,
    )
    db.add(k)
    db.commit()
    return k


def test_filtresiz_hepsi_geliyor(db, personel):
    p = personel()
    _kayit(db, p.id)
    _kayit(db, p.id, eylem="kullanici.askiya_al")
    satirlar, toplam = listele(db, Filtre())
    assert toplam == 2 and len(satirlar) == 2


def test_personel_adi_satira_geliyor(db, personel):
    p = personel()
    _kayit(db, p.id)
    satirlar, _ = listele(db, Filtre())
    assert satirlar[0].personel_ad == p.ad


def test_personel_filtresi(db, personel):
    a, b = personel(), personel()
    _kayit(db, a.id)
    _kayit(db, b.id)
    satirlar, toplam = listele(db, Filtre(personel_id=a.id))
    assert toplam == 1 and satirlar[0].personel_ad == a.ad


def test_kullanici_filtresi_hedef_turunu_de_ariyor(db, personel):
    """7 numaralı kullanıcı ile 7 numaralı başvuru aynı şey değil."""
    p = personel()
    _kayit(db, p.id, hedef_id=7, hedef_tur="kullanici")
    _kayit(db, p.id, hedef_id=7, hedef_tur="yedam_basvuru", eylem="yedam.ata")

    satirlar, toplam = listele(db, Filtre(kullanici_id=7))
    assert toplam == 1
    assert satirlar[0].hedef_tur == "kullanici"


def test_eylem_filtresi_onek_esliyor(db, personel):
    p = personel()
    _kayit(db, p.id, eylem="kullanici.sil")
    _kayit(db, p.id, eylem="kullanici.askiya_al")
    _kayit(db, p.id, eylem="yedam.ata", hedef_tur="yedam_basvuru")

    _, toplam = listele(db, Filtre(eylem="kullanici."))
    assert toplam == 2


def test_bitis_gunu_dahil(db, personel):
    """"18–20 Ağustos" diyen kullanıcı 20'yi de kastediyor."""
    p = personel()
    _kayit(db, p.id, zaman=dt.datetime(2026, 8, 20, 23, 30, tzinfo=dt.timezone.utc))

    _, toplam = listele(
        db, Filtre(baslangic=dt.date(2026, 8, 18), bitis=dt.date(2026, 8, 20))
    )
    assert toplam == 1


def test_aralik_disi_dusuyor(db, personel):
    p = personel()
    _kayit(db, p.id, zaman=dt.datetime(2026, 8, 21, 0, 30, tzinfo=dt.timezone.utc))
    _, toplam = listele(
        db, Filtre(baslangic=dt.date(2026, 8, 18), bitis=dt.date(2026, 8, 20))
    )
    assert toplam == 0


def test_filtreler_kesisiyor(db, personel):
    a, b = personel(), personel()
    _kayit(db, a.id, eylem="kullanici.sil", hedef_id=1)
    _kayit(db, a.id, eylem="kullanici.askiya_al", hedef_id=1)
    _kayit(db, b.id, eylem="kullanici.sil", hedef_id=1)

    _, toplam = listele(db, Filtre(personel_id=a.id, eylem="kullanici.sil"))
    assert toplam == 1


def test_en_yeni_ustte_ve_esitlik_bozucu_var(db, personel):
    """Aynı ana düşen iki kayıtta sıra `id`'ye göre belirleniyor."""
    p = personel()
    ilk = _kayit(db, p.id, eylem="kullanici.sil")
    ikinci = _kayit(db, p.id, eylem="kullanici.askiya_al")
    satirlar, _ = listele(db, Filtre())
    assert [s.id for s in satirlar] == [ikinci.id, ilk.id]


def test_eylem_secenekleri_veriden_geliyor(db, personel):
    p = personel()
    _kayit(db, p.id, eylem="kullanici.sil")
    _kayit(db, p.id, eylem="kullanici.sil")
    _kayit(db, p.id, eylem="kullanici.askiya_al")
    assert eylem_secenekleri(db) == ["kullanici.askiya_al", "kullanici.sil"]


# --- HTTP: rol daraltması ---


@pytest.fixture
def girisli(db, panel_istemci, personel):
    from app.guvenlik import jeton_ozetle, rastgele_jeton
    from app.panel.oturum import CEREZ_ADI

    def kur(rol=PersonelRolu.ADMIN) -> Personel:
        p = personel(rol)
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


def test_operasyon_yalnizca_kendi_kayitlarini_goruyor(db, panel_istemci, girisli, personel):
    baskasi = personel(PersonelRolu.ADMIN)
    _kayit(db, baskasi.id, eylem="kullanici.sil")

    ben = girisli(PersonelRolu.OPERASYON)
    _kayit(db, ben.id, eylem="kullanici.askiya_al")

    sayfa = panel_istemci.get("/yonetim/denetim").text
    assert "kullanici.askiya_al" in sayfa
    assert baskasi.ad not in sayfa


def test_operasyon_filtreyi_ezerek_baskasini_goremiyor(
    db, panel_istemci, girisli, personel
):
    """Sorgu parametresi rol daraltmasını atlayamamalı."""
    baskasi = personel(PersonelRolu.ADMIN)
    _kayit(db, baskasi.id, eylem="kullanici.sil")
    girisli(PersonelRolu.OPERASYON)

    sayfa = panel_istemci.get(f"/yonetim/denetim?personel_id={baskasi.id}").text
    assert baskasi.ad not in sayfa
    assert "eşleşen kayıt yok" in sayfa


def test_admin_hepsini_goruyor(db, panel_istemci, girisli, personel):
    baskasi = personel(PersonelRolu.OPERASYON)
    _kayit(db, baskasi.id, eylem="kullanici.sil")
    girisli(PersonelRolu.ADMIN)

    sayfa = panel_istemci.get("/yonetim/denetim").text
    assert baskasi.ad in sayfa


def test_gozlemci_denetime_giremiyor(panel_istemci, girisli):
    girisli(PersonelRolu.GOZLEMCI)
    assert panel_istemci.get("/yonetim/denetim").status_code == 403
