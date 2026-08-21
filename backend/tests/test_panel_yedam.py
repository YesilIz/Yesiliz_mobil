"""
YEDAM danışman paneli (`docs/panel-plani.md §6`).

Testlerin ağırlığı **danışmanın göremediklerinde**: rıza metni "danışmanlar
kullanıcı arayamaz; yalnızca kendilerine atanan başvuruyu açabilirler"
diye söz veriyor ve bu sözün tek koruması sorgu daraltması.

Detayı açmak `erisim_kaydi` yazıyor — rıza metninin ikinci sözü. Yazmayı
atlamak sözü tutmamak olurdu, o yüzden ayrıca sınanıyor.
"""

from __future__ import annotations

import datetime as dt

import pytest

from app.modeller import (
    AnketYanit,
    BasvuruDurumu,
    DanismanNotu,
    DenetimKaydi,
    ErisimKaydi,
    GunlukOzet,
    Kullanici,
    OzellikBayragi,
    Personel,
    PersonelDurumu,
    PersonelOturum,
    PersonelRolu,
    RiskSonuc,
    RiskSurukleyici,
    YedamBasvuru,
    YedamRiza,
    YedamTemas,
)
from app.panel.oturum import CEREZ_ADI

SIMDI = dt.datetime.now(dt.timezone.utc)
GEREKCE = "Görüşme tamamlandı, kullanıcı YEDAM'a yönlendirildi."


@pytest.fixture
def personel(db):
    from app.guvenlik import sifre_ozetle

    sayac = {"n": 0}

    def kur(rol=PersonelRolu.YEDAM_DANISMAN, ad=None) -> Personel:
        sayac["n"] += 1
        p = Personel(
            eposta=f"p{sayac['n']}@yesilay.org.tr",
            ad=ad or f"Personel {sayac['n']}",
            sifre_ozet=sifre_ozetle("parola"),
            rol=rol.value,
            durum=PersonelDurumu.AKTIF.value,
            totp_sir="",
        )
        db.add(p)
        db.commit()
        return p

    return kur


@pytest.fixture
def girisli(db, panel_istemci, personel):
    from app.guvenlik import jeton_ozetle, rastgele_jeton

    def kur(rol=PersonelRolu.YEDAM_DANISMAN, p=None) -> Personel:
        p = p or personel(rol)
        ham = rastgele_jeton()
        db.add(
            PersonelOturum(
                personel_id=p.id,
                jeton_ozet=jeton_ozetle(ham),
                totp_dogrulandi=True,
                ip="127.0.0.1",
                tarayici="test",
                mutlak_bitis=SIMDI + dt.timedelta(hours=4),
                iptal=False,
            )
        )
        db.commit()
        panel_istemci.cookies.set(CEREZ_ADI, ham)
        return p

    return kur


@pytest.fixture
def basvuru(db):
    sayac = {"n": 0}

    def kur(
        durum=BasvuruDurumu.YENI,
        atanan=None,
        kapsam="kimlik,risk,surukleyici,anket",
        riza_gun_once=1,
        geri_cekildi=False,
        beyan_yas=25,
    ) -> YedamBasvuru:
        sayac["n"] += 1
        k = Kullanici(
            eposta=f"kul{sayac['n']}@ornek.com", ad=f"Kullanıcı {sayac['n']}", sifre_ozet="x"
        )
        db.add(k)
        db.flush()
        verilme = SIMDI - dt.timedelta(days=riza_gun_once)
        riza = YedamRiza(
            kullanici_id=k.id,
            metin_surumu="v1",
            kapsam=kapsam,
            verilme=verilme,
            son_gecerlilik=verilme + dt.timedelta(days=90),
            geri_cekme=SIMDI if geri_cekildi else None,
            beyan_yas=beyan_yas,
        )
        db.add(riza)
        db.flush()
        b = YedamBasvuru(
            kullanici_id=k.id,
            riza_id=riza.id,
            durum=durum.value,
            iletisim_turu="telefon",
            iletisim_degeri="05551112233",
            kullanici_notu="Gece kullanımım arttı.",
            atanan_personel_id=atanan.id if atanan is not None else None,
            olusturma=SIMDI - dt.timedelta(days=riza_gun_once),
        )
        db.add(b)
        db.commit()
        return b

    return kur


def _csrf(panel_istemci) -> str:
    from app.guvenlik import jeton_ozetle
    from app.panel.oturum import csrf_jetonu

    return csrf_jetonu(jeton_ozetle(panel_istemci.cookies[CEREZ_ADI]))


def _post(panel_istemci, yol, **alanlar):
    return panel_istemci.post(yol, data={**alanlar, "_csrf": _csrf(panel_istemci)})


# --- danışmanın göremedikleri (D20-D22) ---


def test_danisman_atanmamis_basvuruyu_havuzda_gormuyor(db, panel_istemci, girisli, basvuru):
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    basvuru()  # atanmamış
    benimki = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI)

    sayfa = panel_istemci.get("/yedam/havuz").text
    assert f"/yedam/basvuru/{benimki.id}" in sayfa
    assert db.query(YedamBasvuru).count() == 2
    assert sayfa.count("/yedam/basvuru/") == 1


def test_danisman_baskasinin_basvurusunu_acamiyor(db, panel_istemci, girisli, basvuru, personel):
    girisli(PersonelRolu.YEDAM_DANISMAN)
    baskasi = personel(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=baskasi, durum=BasvuruDurumu.ATANDI)

    yanit = panel_istemci.get(f"/yedam/basvuru/{b.id}")
    assert yanit.status_code == 404, "403 dönmek başvurunun varlığını sızdırırdı"
    assert db.query(ErisimKaydi).count() == 0, "Erişemediği kayıt için iz yazılmamalı"


def test_ebeveyn_onayi_bekleyen_basvuru_hicbir_danismana_gorunmuyor(
    db, panel_istemci, girisli, basvuru
):
    girisli(PersonelRolu.ADMIN)
    b = basvuru(durum=BasvuruDurumu.EBEVEYN_ONAYI_BEKLIYOR)

    assert panel_istemci.get(f"/yedam/basvuru/{b.id}").status_code == 404
    assert f"/yedam/basvuru/{b.id}" not in panel_istemci.get("/yedam/havuz").text


def test_havuzda_iletisim_bilgisi_gosterilmiyor(db, panel_istemci, girisli, basvuru):
    """Bilgi ancak başvuru açılınca görünüyor — ve o an kullanıcıya bildiriliyor."""
    girisli(PersonelRolu.YEDAM_YONETICI)
    basvuru()
    assert "05551112233" not in panel_istemci.get("/yedam/havuz").text


def test_operasyon_yedam_havuzuna_giremiyor(panel_istemci, girisli):
    girisli(PersonelRolu.OPERASYON)
    assert panel_istemci.get("/yedam/havuz").status_code == 403


def test_detayda_uygulama_adi_ve_teshis_yok(db, panel_istemci, girisli, basvuru):
    """§6.2: uygulama dökümü sunucuda hiç yok; teşhis etiketi panelde de yasak."""
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI)
    db.add(
        RiskSonuc(
            kullanici_id=b.kullanici_id,
            tarih=dt.date(2026, 8, 20),
            s=50.0,
            r=70.0,
            gosterilen_bant="COK",
            trend="ARTIYOR",
        )
    )
    db.commit()

    sayfa = panel_istemci.get(f"/yedam/basvuru/{b.id}").text
    # Teşhis ETİKETİ yasak; "teşhis değildir" feragati tam tersine şart.
    for yasak in ("bağımlı", "Bağımlı", "com.instagram", "com.zhiliaoapp"):
        assert yasak not in sayfa, f"Ekranda görünmemeli: {yasak}"
    assert "teşhis değildir" in sayfa and "tanı koymaz" in sayfa


# --- erişim kaydı ---


def test_detayi_acmak_erisim_kaydi_yaziyor(db, panel_istemci, girisli, basvuru):
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI)

    assert panel_istemci.get(f"/yedam/basvuru/{b.id}").status_code == 200

    kayit = db.query(ErisimKaydi).one()
    assert kayit.kullanici_id == b.kullanici_id
    assert kayit.personel_id == ben.id
    assert "risk" in kayit.kapsam


def test_her_acilis_ayri_kayit(db, panel_istemci, girisli, basvuru):
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI)

    panel_istemci.get(f"/yedam/basvuru/{b.id}")
    panel_istemci.get(f"/yedam/basvuru/{b.id}")
    assert db.query(ErisimKaydi).count() == 2


def test_riza_gecersizken_de_erisim_kaydi_yaziliyor(db, panel_istemci, girisli, basvuru):
    """Boş bir ekranı açmak da bir erişim denemesidir."""
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI, geri_cekildi=True)

    panel_istemci.get(f"/yedam/basvuru/{b.id}")
    kayit = db.query(ErisimKaydi).one()
    assert kayit.kapsam == "", "Rıza yokken kapsam boş olmalı"


# --- rıza kapsamı ---


def test_rizasi_gecersizse_veri_gosterilmiyor(db, panel_istemci, girisli, basvuru):
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI, geri_cekildi=True)
    db.add(
        RiskSurukleyici(
            kullanici_id=b.kullanici_id, tarih=dt.date(2026, 8, 20), faktor="social", sira=1
        )
    )
    db.commit()

    sayfa = panel_istemci.get(f"/yedam/basvuru/{b.id}").text
    assert "Rıza geçerli değil" in sayfa
    assert "social" not in sayfa
    # Görüşme geçmişi ve kimlik kalıyor (rıza metni §3).
    assert "05551112233" in sayfa


def test_kapsam_disindaki_bolum_bos(db, panel_istemci, girisli, basvuru):
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI, kapsam="kimlik,risk")
    db.add(
        AnketYanit(
            kullanici_id=b.kullanici_id,
            tarih=dt.date(2026, 8, 1),
            madde_puanlari="6,6,6,6,6,6,6,6,6,6",
            ham_puan=60.0,
            r_s=100.0,
        )
    )
    db.commit()

    sayfa = panel_istemci.get(f"/yedam/basvuru/{b.id}").text
    assert "anket sonuçlarını paylaşmayı seçmedi" in sayfa
    assert "6 · 6 · 6" not in sayfa


def test_davranis_ayri_rizaya_bagli(db, panel_istemci, girisli, basvuru):
    """K19: YEDAM rızası "Yeşilay ile paylaşım" kapısını açmıyor."""
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(
        atanan=ben, durum=BasvuruDurumu.ATANDI, kapsam="kimlik,risk,davranis"
    )

    sayfa = panel_istemci.get(f"/yedam/basvuru/{b.id}").text
    assert "davranış özetini paylaşmıyor" in sayfa

    db.add(
        GunlukOzet(kullanici_id=b.kullanici_id, tarih=dt.date.today(), social_dk=120.0)
    )
    db.commit()
    sayfa = panel_istemci.get(f"/yedam/basvuru/{b.id}").text
    assert "120 dk" in sayfa


# --- atama ---


def test_danisman_kendine_atayamiyor(db, panel_istemci, girisli, basvuru):
    """İş yükü dağıtımı yöneticinin kararı (§2 Roller)."""
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru()
    assert _post(panel_istemci, f"/yedam/basvuru/{b.id}/ata", danisman_id=ben.id).status_code == 403


def test_yonetici_atayabiliyor(db, panel_istemci, girisli, basvuru, personel):
    girisli(PersonelRolu.YEDAM_YONETICI)
    danisman = personel(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru()

    assert _post(
        panel_istemci, f"/yedam/basvuru/{b.id}/ata", danisman_id=danisman.id
    ).status_code in (200, 303)

    db.expire_all()
    b = db.get(YedamBasvuru, b.id)
    assert b.atanan_personel_id == danisman.id and b.durum == "atandi"
    assert db.query(DenetimKaydi).filter_by(eylem="yedam.ata").count() == 1


def test_danisman_olmayan_personele_atanamiyor(db, panel_istemci, girisli, basvuru, personel):
    girisli(PersonelRolu.YEDAM_YONETICI)
    operasyon = personel(PersonelRolu.OPERASYON)
    b = basvuru()
    assert _post(
        panel_istemci, f"/yedam/basvuru/{b.id}/ata", danisman_id=operasyon.id
    ).status_code == 422


# --- temas ve durum makinesi ---


def test_gorusme_durumu_ilerletiyor(db, panel_istemci, girisli, basvuru):
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI)

    assert _post(
        panel_istemci, f"/yedam/basvuru/{b.id}/temas", tur="gorusuldu"
    ).status_code in (200, 303)

    db.expire_all()
    assert db.get(YedamBasvuru, b.id).durum == "temas_kuruldu"
    assert db.query(YedamTemas).count() == 1


def test_uc_ulasilamadidan_sonra_durum_degisiyor(db, panel_istemci, girisli, basvuru):
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI)

    for _ in range(2):
        _post(panel_istemci, f"/yedam/basvuru/{b.id}/temas", tur="ulasilamadi")
    db.expire_all()
    assert db.get(YedamBasvuru, b.id).durum == "atandi"

    _post(panel_istemci, f"/yedam/basvuru/{b.id}/temas", tur="ulasilamadi")
    db.expire_all()
    assert db.get(YedamBasvuru, b.id).durum == "ulasilamadi"
    # Başvuru KAPANMADI: ulaşılamamak vazgeçmek değil.
    assert db.get(YedamBasvuru, b.id).kapanis is None


def test_gorusme_ulasilamadi_sayacini_sifirliyor(db, panel_istemci, girisli, basvuru):
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI)

    for _ in range(2):
        _post(panel_istemci, f"/yedam/basvuru/{b.id}/temas", tur="ulasilamadi")
    _post(panel_istemci, f"/yedam/basvuru/{b.id}/temas", tur="gorusuldu")

    db.expire_all()
    assert db.get(YedamBasvuru, b.id).ulasilamadi_sayisi == 0


# --- kapatma ---


def test_gerekcesiz_kapatilamiyor(db, panel_istemci, girisli, basvuru):
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI)

    assert _post(panel_istemci, f"/yedam/basvuru/{b.id}/kapat", gerekce="kısa").status_code == 422
    db.expire_all()
    assert db.get(YedamBasvuru, b.id).durum == "atandi"


def test_kapatma_rizayi_bitirmiyor(db, panel_istemci, girisli, basvuru):
    """Rızayı yalnızca kullanıcı geri çekebilir."""
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI)

    _post(panel_istemci, f"/yedam/basvuru/{b.id}/kapat", gerekce=GEREKCE)

    db.expire_all()
    assert db.get(YedamBasvuru, b.id).durum == "kapandi"
    assert db.query(YedamRiza).one().geri_cekme is None


def test_kapanmis_basvuruya_temas_eklenemiyor(db, panel_istemci, girisli, basvuru):
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.KAPANDI)
    assert _post(panel_istemci, f"/yedam/basvuru/{b.id}/temas", tur="arandi").status_code == 409


# --- danışman notu (S4: bayrakla kapalı) ---


def test_not_varsayilan_kapali(db, panel_istemci, girisli, basvuru):
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI)

    yanit = _post(panel_istemci, f"/yedam/basvuru/{b.id}/not", metin="Bir not")
    assert yanit.status_code == 503
    assert db.query(DanismanNotu).count() == 0, "Kapalıyken hiçbir satır yazılmamalı"

    sayfa = panel_istemci.get(f"/yedam/basvuru/{b.id}").text
    assert "Danışman notu şu anda <strong>kapalı</strong>" in sayfa


def test_bayrak_acilinca_not_yazilabiliyor(db, panel_istemci, girisli, basvuru):
    db.add(OzellikBayragi(anahtar="yedam.danisman_notu", deger="evet"))
    db.commit()
    ben = girisli(PersonelRolu.YEDAM_DANISMAN)
    b = basvuru(atanan=ben, durum=BasvuruDurumu.ATANDI)

    assert _post(
        panel_istemci, f"/yedam/basvuru/{b.id}/not", metin="Görüşme planlandı."
    ).status_code in (200, 303)
    assert db.query(DanismanNotu).one().metin == "Görüşme planlandı."
