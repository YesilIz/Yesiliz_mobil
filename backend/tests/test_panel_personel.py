"""
Personel yönetimi (`docs/panel-plani.md §5`, S1 kararı).

Bu yüzey yetkinin kendisini dağıtıyor, o yüzden testlerin çoğu "ne
YAPILAMAZ" tarafında: admin kendi rolünü düşüremiyor, geçici parola
kalıcıya dönüşemiyor, parola sıfırlamak 2FA'yı atlamıyor.
"""

from __future__ import annotations

import datetime as dt

import pytest

from app.guvenlik import sifre_dogrula, sifre_ozetle, sir_sifrele
from app.modeller import (
    DenetimKaydi,
    Personel,
    PersonelDurumu,
    PersonelOturum,
    PersonelRolu,
)
from app.panel.personel_yonetim import gecici_sifre

SIFRE = "panel-parolasi-2026"


@pytest.fixture
def personel(db):
    sayac = {"n": 0}

    def kur(rol=PersonelRolu.ADMIN, sir="ABCDEFGHIJKLMNOP", **ek) -> Personel:
        sayac["n"] += 1
        p = Personel(
            eposta=ek.pop("eposta", f"p{sayac['n']}@yesilay.org.tr"),
            ad=ek.pop("ad", f"Personel {sayac['n']}"),
            sifre_ozet=sifre_ozetle(SIFRE),
            rol=rol.value,
            durum=PersonelDurumu.AKTIF.value,
            totp_sir=sir_sifrele(sir) if sir else "",
            **ek,
        )
        db.add(p)
        db.commit()
        return p

    return kur


@pytest.fixture
def girisli(db, panel_istemci, personel):
    from app.guvenlik import jeton_ozetle, rastgele_jeton
    from app.panel.oturum import CEREZ_ADI

    def kur(rol=PersonelRolu.ADMIN, p=None) -> Personel:
        p = p or personel(rol)
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


# --- geçici parola ---


def test_gecici_sifre_karisan_karakter_tasimiyor():
    """Parola elle yazılabilmeli: 0/O, 1/l/I ayırt edilemiyor."""
    for _ in range(50):
        assert not (set(gecici_sifre()) & set("0O1lI"))


def test_yeni_personel_gecici_sifreyle_aciliyor(db, panel_istemci, girisli):
    girisli(PersonelRolu.ADMIN)
    yanit = _post(
        panel_istemci,
        "/yonetim/personel",
        eposta="Yeni@Yesilay.org.tr",
        ad="Yeni Personel",
        rol="operasyon",
    )
    assert yanit.status_code == 200

    p = db.query(Personel).filter_by(eposta="yeni@yesilay.org.tr").one()
    assert p.parola_degistirmeli is True
    assert p.totp_sir == "", "2FA ilk girişte kurulmalı"

    # Parola sayfada bir kez gösteriliyor ve gerçekten çalışıyor.
    import re

    m = re.search(r'class="kod-alani">([^<]+)<', yanit.text)
    assert m, "Geçici parola sayfada gösterilmeli"
    assert sifre_dogrula(p.sifre_ozet, m.group(1).strip())


def test_ayni_eposta_ikinci_kez_acilmiyor(db, panel_istemci, girisli, personel):
    girisli(PersonelRolu.ADMIN)
    personel(eposta="dolu@yesilay.org.tr")
    yanit = _post(
        panel_istemci,
        "/yonetim/personel",
        eposta="dolu@yesilay.org.tr",
        ad="Kopya",
        rol="operasyon",
    )
    assert yanit.status_code == 409


def test_gecersiz_rol_reddediliyor(panel_istemci, girisli):
    girisli(PersonelRolu.ADMIN)
    yanit = _post(
        panel_istemci,
        "/yonetim/personel",
        eposta="x@yesilay.org.tr",
        ad="X",
        rol="tanrikral",
    )
    assert yanit.status_code == 422


# --- zorunlu parola değişimi ---


def test_gecici_parolayla_panel_kullanilamiyor(db, panel_istemci, girisli, personel):
    """Bayrak duruyorken her yol parola ekranına düşüyor."""
    p = personel(rol=PersonelRolu.ADMIN, parola_degistirmeli=True)
    girisli(p=p)

    for yol in ("/yonetim/gosterge", "/yonetim/personel", "/yonetim/denetim"):
        yanit = panel_istemci.get(yol)
        assert yanit.status_code in (303, 307), yol
        assert yanit.headers["location"] == "/oturum/parola-degistir", yol


def test_parola_degistirince_bayrak_dusuyor(db, panel_istemci, girisli, personel):
    p = personel(rol=PersonelRolu.ADMIN, parola_degistirmeli=True)
    girisli(p=p)

    yanit = _post(
        panel_istemci,
        "/oturum/parola-degistir",
        mevcut=SIFRE,
        yeni="yepyeni-parola-2026",
        yeni_tekrar="yepyeni-parola-2026",
    )
    assert yanit.status_code in (200, 303)

    db.expire_all()
    p = db.get(Personel, p.id)
    assert p.parola_degistirmeli is False
    assert sifre_dogrula(p.sifre_ozet, "yepyeni-parola-2026")
    # Bütün oturumlar düştü: parola değişimi "bu kimlik artık geçersiz".
    assert all(o.iptal for o in db.query(PersonelOturum).all())


def test_yanlis_mevcut_parola_reddediliyor(db, panel_istemci, girisli, personel):
    p = personel(rol=PersonelRolu.ADMIN, parola_degistirmeli=True)
    girisli(p=p)
    yanit = _post(
        panel_istemci,
        "/oturum/parola-degistir",
        mevcut="alakasiz",
        yeni="yepyeni-parola-2026",
        yeni_tekrar="yepyeni-parola-2026",
    )
    assert yanit.status_code == 422
    db.expire_all()
    assert db.get(Personel, p.id).parola_degistirmeli is True


def test_ayni_parolayi_yeniden_yazmak_bayragi_dusurmuyor(
    db, panel_istemci, girisli, personel
):
    p = personel(rol=PersonelRolu.ADMIN, parola_degistirmeli=True)
    girisli(p=p)
    yanit = _post(
        panel_istemci,
        "/oturum/parola-degistir",
        mevcut=SIFRE,
        yeni=SIFRE,
        yeni_tekrar=SIFRE,
    )
    assert yanit.status_code == 422
    db.expire_all()
    assert db.get(Personel, p.id).parola_degistirmeli is True


def test_kisa_parola_reddediliyor(db, panel_istemci, girisli, personel):
    p = personel(rol=PersonelRolu.ADMIN, parola_degistirmeli=True)
    girisli(p=p)
    yanit = _post(
        panel_istemci,
        "/oturum/parola-degistir",
        mevcut=SIFRE,
        yeni="kisa",
        yeni_tekrar="kisa",
    )
    assert yanit.status_code == 422


# --- rol ve durum ---


def test_admin_kendi_rolunu_degistiremiyor(db, panel_istemci, girisli):
    ben = girisli(PersonelRolu.ADMIN)
    yanit = _post(panel_istemci, f"/yonetim/personel/{ben.id}/rol", rol="gozlemci")
    assert yanit.status_code == 422
    db.expire_all()
    assert db.get(Personel, ben.id).rol == "admin"


def test_admin_kendini_askiya_alamiyor(db, panel_istemci, girisli):
    ben = girisli(PersonelRolu.ADMIN)
    yanit = _post(panel_istemci, f"/yonetim/personel/{ben.id}/durum", durum="askida")
    assert yanit.status_code == 422


def test_rol_degisince_oturumlar_dusuyor(db, panel_istemci, girisli, personel):
    girisli(PersonelRolu.ADMIN)
    hedef = personel(PersonelRolu.OPERASYON)
    db.add(
        PersonelOturum(
            personel_id=hedef.id,
            jeton_ozet="baska-ozet",
            totp_dogrulandi=True,
            ip="10.0.0.1",
            tarayici="test",
            mutlak_bitis=dt.datetime.now(dt.timezone.utc) + dt.timedelta(hours=4),
            iptal=False,
        )
    )
    db.commit()

    assert _post(
        panel_istemci, f"/yonetim/personel/{hedef.id}/rol", rol="gozlemci"
    ).status_code in (200, 303)

    db.expire_all()
    assert db.get(Personel, hedef.id).rol == "gozlemci"
    hedef_oturumlari = db.query(PersonelOturum).filter_by(personel_id=hedef.id).all()
    assert all(o.iptal for o in hedef_oturumlari)


def test_personel_silinmiyor_askiya_aliniyor(db, panel_istemci, girisli, personel):
    girisli(PersonelRolu.ADMIN)
    hedef = personel(PersonelRolu.OPERASYON)

    _post(panel_istemci, f"/yonetim/personel/{hedef.id}/durum", durum="askida")

    db.expire_all()
    kalan = db.get(Personel, hedef.id)
    assert kalan is not None, "Personel silinmemeli: denetim kaydı ona bağlı"
    assert kalan.durum == "askida"


# --- parola sıfırlama ---


def test_sifre_sifirlama_2fa_ya_dokunmuyor(db, panel_istemci, girisli, personel):
    girisli(PersonelRolu.ADMIN)
    hedef = personel(PersonelRolu.OPERASYON)
    eski_sir = hedef.totp_sir

    yanit = _post(panel_istemci, f"/yonetim/personel/{hedef.id}/sifre-sifirla")
    assert yanit.status_code == 200

    db.expire_all()
    hedef = db.get(Personel, hedef.id)
    assert hedef.totp_sir == eski_sir, "Parola sıfırlamak ikinci faktörü kaldırmamalı"
    assert hedef.parola_degistirmeli is True
    assert not sifre_dogrula(hedef.sifre_ozet, SIFRE), "Eski parola geçersiz olmalı"


def test_sifre_sifirlama_kilidi_aciyor(db, panel_istemci, girisli, personel):
    girisli(PersonelRolu.ADMIN)
    hedef = personel(
        PersonelRolu.OPERASYON,
        basarisiz_deneme=5,
        kilit_bitis=dt.datetime.now(dt.timezone.utc) + dt.timedelta(minutes=15),
    )

    _post(panel_istemci, f"/yonetim/personel/{hedef.id}/sifre-sifirla")

    db.expire_all()
    hedef = db.get(Personel, hedef.id)
    assert hedef.basarisiz_deneme == 0 and hedef.kilit_bitis is None


# --- oturum düşürme ---


def test_baskasinin_oturumu_dogrudan_dusurulemiyor(db, panel_istemci, girisli, personel):
    girisli(PersonelRolu.ADMIN)
    baskasi = personel(PersonelRolu.OPERASYON)
    o = PersonelOturum(
        personel_id=baskasi.id,
        jeton_ozet="baskasinin-ozeti",
        totp_dogrulandi=True,
        ip="10.0.0.9",
        tarayici="test",
        mutlak_bitis=dt.datetime.now(dt.timezone.utc) + dt.timedelta(hours=4),
        iptal=False,
    )
    db.add(o)
    db.commit()

    assert _post(panel_istemci, f"/yonetim/oturum/{o.id}/dusur").status_code == 404
    db.expire_all()
    assert db.get(PersonelOturum, o.id).iptal is False


# --- rol kapısı ---


def test_operasyon_personel_yuzeyini_goremiyor(panel_istemci, girisli):
    girisli(PersonelRolu.OPERASYON)
    assert panel_istemci.get("/yonetim/personel").status_code == 403
    assert _post(
        panel_istemci, "/yonetim/personel", eposta="x@y.tr", ad="X", rol="admin"
    ).status_code == 403


def test_personel_ekleme_denetim_kaydi_yaziyor(db, panel_istemci, girisli):
    girisli(PersonelRolu.ADMIN)
    _post(
        panel_istemci,
        "/yonetim/personel",
        eposta="izli@yesilay.org.tr",
        ad="İzli",
        rol="gozlemci",
    )
    kayit = db.query(DenetimKaydi).one()
    assert kayit.eylem == "personel.ekle"
    assert kayit.hedef_tur == "personel"
