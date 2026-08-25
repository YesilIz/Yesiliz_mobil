"""
Panel giriş akışı — parola + TOTP, çerez oturumu (`docs/panel-plani.md §2`).
"""

from __future__ import annotations

import time

import pytest

from app.guvenlik import jeton_ozetle, sifre_ozetle, sir_sifrele
from app.modeller import (
    Personel,
    PersonelDurumu,
    PersonelKurtarmaKodu,
    PersonelOturum,
    PersonelRolu,
)
from app.panel.oturum import CEREZ_ADI, KILIT_ESIGI, csrf_jetonu
from app.totp import kod_uret, sayac, sir_uret

SIFRE = "panel-parolasi-2026"


@pytest.fixture
def personel_kur(db):
    """Panel personeli açar; `sir` verilmezse 2FA kurulmamış sayılıyor."""

    def kur(
        eposta="admin@yesilay.org.tr",
        rol=PersonelRolu.ADMIN,
        sir: str | None = None,
        durum=PersonelDurumu.AKTIF,
    ) -> Personel:
        p = Personel(
            eposta=eposta,
            ad="Panel Yöneticisi",
            sifre_ozet=sifre_ozetle(SIFRE),
            rol=rol.value,
            durum=durum.value,
            totp_sir=sir_sifrele(sir) if sir else "",
        )
        db.add(p)
        db.commit()
        return p

    return kur


def _csrf(istemci) -> str:
    """Oturuma bağlı CSRF jetonu — şablonun forma gömdüğü değerin aynısı."""
    return csrf_jetonu(jeton_ozetle(istemci.cookies[CEREZ_ADI]))


def _giris(istemci, eposta="admin@yesilay.org.tr", sifre=SIFRE):
    return istemci.post("/oturum/giris", data={"eposta": eposta, "sifre": sifre})


def _totp(istemci, sir, kayma=0):
    kod = kod_uret(sir, sayac(int(time.time())) + kayma)
    return istemci.post("/oturum/totp", data={"kod": kod, "_csrf": _csrf(istemci)})


# --- oturumsuz erişim ---


def test_giris_formu_aciliyor(panel_istemci):
    yanit = panel_istemci.get("/oturum/giris")
    assert yanit.status_code == 200
    assert "Giriş yap" in yanit.text


def test_kok_adres_girise_dusuyor(panel_istemci):
    yanit = panel_istemci.get("/")
    assert yanit.status_code == 303
    assert yanit.headers["location"] == "/oturum/giris"


def test_oturumsuz_gosterge_girise_yonlendiriyor(panel_istemci):
    yanit = panel_istemci.get("/yonetim/gosterge")
    assert yanit.status_code == 303
    assert yanit.headers["location"] == "/oturum/giris"


def test_htmx_istegi_hx_redirect_aliyor(panel_istemci):
    """
    HTMX bir 303'ü kendi içinde izleyip giriş sayfasını panelin bir
    parçasının yerine gömerdi.
    """
    yanit = panel_istemci.get("/yonetim/gosterge", headers={"HX-Request": "true"})
    assert yanit.status_code == 204
    assert yanit.headers["hx-redirect"] == "/oturum/giris"


def test_kayit_formu_yok(panel_istemci):
    """Personel hesabı açmak bir yetki işlemi; internete açık form olamaz."""
    assert panel_istemci.post("/oturum/kayit", data={}).status_code == 404


# --- parola adımı ---


def test_yanlis_parola_reddediliyor(panel_istemci, personel_kur):
    personel_kur()
    yanit = _giris(panel_istemci, sifre="yanlis-parola")
    assert yanit.status_code == 401
    assert "E-posta ya da parola hatalı" in yanit.text
    assert CEREZ_ADI not in panel_istemci.cookies


def test_olmayan_hesap_ayni_mesaji_veriyor(panel_istemci, personel_kur):
    """Hangi e-postanın kayıtlı olduğu sızmamalı (hesap.py ile aynı kural)."""
    personel_kur()
    yok = _giris(panel_istemci, eposta="baskasi@ornek.com")
    assert yok.status_code == 401
    assert "E-posta ya da parola hatalı" in yok.text


def test_askiya_alinmis_personel_giremiyor(panel_istemci, personel_kur):
    """Parola doğru olsa da girilemiyor; durum bilgisi de bir sızıntı."""
    personel_kur(durum=PersonelDurumu.ASKIDA)
    yanit = _giris(panel_istemci)
    assert yanit.status_code == 401
    assert "askı" not in yanit.text.lower()


def test_dogru_parola_totp_adimina_gonderiyor(panel_istemci, personel_kur):
    personel_kur(sir=sir_uret())
    yanit = _giris(panel_istemci)
    assert yanit.status_code == 303
    assert yanit.headers["location"] == "/oturum/totp"
    assert CEREZ_ADI in panel_istemci.cookies


def test_totp_kurulmamis_hesap_kuruluma_gidiyor(panel_istemci, personel_kur):
    personel_kur()
    yanit = _giris(panel_istemci)
    assert yanit.headers["location"] == "/oturum/totp-kur"


def test_yarim_oturum_paneli_acmiyor(panel_istemci, personel_kur):
    """Parola geçildi ama TOTP geçilmedi — panel hâlâ kapalı."""
    personel_kur(sir=sir_uret())
    _giris(panel_istemci)
    yanit = panel_istemci.get("/yonetim/gosterge")
    assert yanit.status_code == 303
    assert yanit.headers["location"] == "/oturum/totp"


# --- TOTP adımı ---


def test_dogru_kod_paneli_aciyor(panel_istemci, personel_kur):
    sir = sir_uret()
    personel_kur(sir=sir)
    _giris(panel_istemci)

    yanit = _totp(panel_istemci, sir)
    assert yanit.status_code == 303
    assert yanit.headers["location"] == "/yonetim/gosterge"

    gosterge = panel_istemci.get("/yonetim/gosterge")
    assert gosterge.status_code == 200
    assert "Gösterge" in gosterge.text


def test_yanlis_kod_reddediliyor(panel_istemci, personel_kur):
    personel_kur(sir=sir_uret())
    _giris(panel_istemci)
    yanit = panel_istemci.post(
        "/oturum/totp", data={"kod": "000000", "_csrf": _csrf(panel_istemci)}
    )
    assert yanit.status_code == 401


def test_ayni_kod_ikinci_kez_kullanilamiyor(panel_istemci, personel_kur, db):
    """
    Omuz üstünden okunan bir kod 30 saniye boyunca geçerli kalmamalı.

    İlk kullanımdan sonra `personel.son_totp_sayaci` yazılıyor ve aynı
    sayaç bir daha kabul edilmiyor.
    """
    sir = sir_uret()
    personel = personel_kur(sir=sir)
    _giris(panel_istemci)
    assert _totp(panel_istemci, sir).status_code == 303

    db.refresh(personel)
    assert personel.son_totp_sayaci == sayac(int(time.time()))

    # Yeni bir oturumda aynı koda yeniden bakılıyor.
    panel_istemci.cookies.clear()
    _giris(panel_istemci)
    tekrar = _totp(panel_istemci, sir)
    assert tekrar.status_code == 401
    assert "daha önce kullanıldı" in tekrar.text


def test_iki_adim_geride_kalan_kod_reddediliyor(panel_istemci, personel_kur):
    sir = sir_uret()
    personel_kur(sir=sir)
    _giris(panel_istemci)
    assert _totp(panel_istemci, sir, kayma=-2).status_code == 401


# --- TOTP kurulumu ---


def test_kurulum_akisi_sirri_sifreli_yaziyor(panel_istemci, personel_kur, db):
    personel = personel_kur()
    _giris(panel_istemci)

    form = panel_istemci.get("/oturum/totp-kur")
    assert form.status_code == 200
    assert "İki adımlı doğrulamayı kur" in form.text

    sir = sir_uret()
    yanit = panel_istemci.post(
        "/oturum/totp-kur",
        data={
            "sir": sir,
            "kod": kod_uret(sir, sayac(int(time.time()))),
            "_csrf": _csrf(panel_istemci),
        },
    )
    assert yanit.status_code == 303
    assert yanit.headers["location"] == "/yonetim/gosterge"

    db.refresh(personel)
    assert personel.totp_sir and sir not in personel.totp_sir


def test_kurulumda_yanlis_kod_sirri_yazmiyor(panel_istemci, personel_kur, db):
    personel = personel_kur()
    _giris(panel_istemci)
    yanit = panel_istemci.post(
        "/oturum/totp-kur",
        data={"sir": sir_uret(), "kod": "000000", "_csrf": _csrf(panel_istemci)},
    )
    assert yanit.status_code == 401
    db.refresh(personel)
    assert personel.totp_sir == ""


def test_kurulu_hesap_yeniden_kuramiyor(panel_istemci, personel_kur, db):
    """Yarışan ikinci bir kurulum isteği var olan sırrı ezmemeli."""
    eski_sir = sir_uret()
    personel = personel_kur(sir=eski_sir)
    _giris(panel_istemci)
    eski_kolon = personel.totp_sir

    yeni_sir = sir_uret()
    yanit = panel_istemci.post(
        "/oturum/totp-kur",
        data={
            "sir": yeni_sir,
            "kod": kod_uret(yeni_sir, sayac(int(time.time()))),
            "_csrf": _csrf(panel_istemci),
        },
    )
    assert yanit.headers["location"] == "/oturum/totp"
    db.refresh(personel)
    assert personel.totp_sir == eski_kolon


# --- kurtarma kodu ---


def test_kurtarma_kodu_bir_kez_calisiyor(panel_istemci, personel_kur, db):
    personel = personel_kur(sir=sir_uret())
    db.add(
        PersonelKurtarmaKodu(
            personel_id=personel.id, kod_ozet=jeton_ozetle("ABCD-1234"), kullanildi=False
        )
    )
    db.commit()

    _giris(panel_istemci)
    ilk = panel_istemci.post(
        "/oturum/totp", data={"kod": "abcd-1234", "_csrf": _csrf(panel_istemci)}
    )
    assert ilk.status_code == 303

    panel_istemci.cookies.clear()
    _giris(panel_istemci)
    ikinci = panel_istemci.post(
        "/oturum/totp", data={"kod": "ABCD-1234", "_csrf": _csrf(panel_istemci)}
    )
    assert ikinci.status_code == 401


# --- hesap kilidi ---


def test_bes_basarisiz_deneme_hesabi_kilitliyor(panel_istemci, personel_kur, db):
    """
    Redis yerine veritabanı kilidi (U5): paylaşımlı kurum NAT'ının arkasında
    IP tabanlı bir sınır zaten tek bir adrese bakıyor olurdu.
    """
    personel = personel_kur(sir=sir_uret())
    for _ in range(KILIT_ESIGI):
        assert _giris(panel_istemci, sifre="yanlis").status_code == 401

    db.refresh(personel)
    assert personel.kilit_bitis is not None

    # Kilitliyken doğru parola bile geçmiyor.
    kilitli = _giris(panel_istemci)
    assert kilitli.status_code == 429
    assert "kilitlendi" in kilitli.text


def test_basarili_giris_denemeleri_sifirliyor(panel_istemci, personel_kur, db):
    sir = sir_uret()
    personel = personel_kur(sir=sir)
    _giris(panel_istemci, sifre="yanlis")
    db.refresh(personel)
    assert personel.basarisiz_deneme == 1

    _giris(panel_istemci)
    _totp(panel_istemci, sir)
    db.refresh(personel)
    assert personel.basarisiz_deneme == 0
    assert personel.son_giris is not None


# --- çıkış ---


def test_cikis_oturumu_iptal_ediyor(panel_istemci, personel_kur, db):
    sir = sir_uret()
    personel_kur(sir=sir)
    _giris(panel_istemci)
    _totp(panel_istemci, sir)

    yanit = panel_istemci.post("/oturum/cikis", data={"_csrf": _csrf(panel_istemci)})
    assert yanit.status_code == 303

    kayit = db.query(PersonelOturum).one()
    assert kayit.iptal is True


def test_askiya_alinan_personelin_oturumu_o_anda_dusuyor(
    panel_istemci, personel_kur, db
):
    """
    JWT yerine sunucu tarafı oturumun tek gerekçesi bu.

    JWT ya da imzalı çerez olsaydı askıya alma jeton süresi dolana kadar
    etkisiz kalırdı.
    """
    sir = sir_uret()
    personel = personel_kur(sir=sir)
    _giris(panel_istemci)
    _totp(panel_istemci, sir)
    assert panel_istemci.get("/yonetim/gosterge").status_code == 200

    personel.durum = PersonelDurumu.ASKIDA.value
    db.commit()

    yanit = panel_istemci.get("/yonetim/gosterge")
    assert yanit.status_code == 303
    assert yanit.headers["location"] == "/oturum/giris"
    assert db.query(PersonelOturum).one().iptal is True


def test_eski_cerezle_yeniden_giris_yapilabiliyor(panel_istemci, personel_kur, db):
    """
    Tarayıcıda kalmış bir oturum çerezi girişi **kilitlememeli**.

    Gerçek tarayıcıda bulundu: giriş formu CSRF jetonu taşımıyordu ve
    çerez varken kapı jeton istiyordu, yani yarıda bırakılmış bir TOTP
    adımından sonra kullanıcı bir daha giremiyordu — 403 "Oturum
    doğrulaması başarısız". Betikler bunu görmüyordu çünkü her denemede
    temiz bir istemciyle giriyorlardı.
    """
    import re

    sir = sir_uret()
    personel_kur(sir=sir)

    # İlk giriş: çerez konuyor, TOTP adımı yarıda bırakılıyor.
    assert _giris(panel_istemci).status_code in (200, 303)
    assert panel_istemci.cookies.get(CEREZ_ADI)

    # Kullanıcı giriş sayfasına dönüyor; form artık jeton taşımalı.
    sayfa = panel_istemci.get("/oturum/giris").text
    m = re.search(r'name="_csrf" value="([^"]*)"', sayfa)
    assert m, "Giriş formunda _csrf alanı yok"
    assert m.group(1), "Çerez varken jeton boş kalamaz"

    # Ve o jetonla giriş yeniden yapılabilmeli.
    yanit = panel_istemci.post(
        "/oturum/giris",
        data={"eposta": "admin@yesilay.org.tr", "sifre": SIFRE, "_csrf": m.group(1)},
    )
    assert yanit.status_code in (200, 303), yanit.text[:200]


def test_cerezsiz_giriste_jeton_bos(panel_istemci, personel_kur):
    """Oturumsuz istekte kapı jeton aramıyor; form da boş taşıyor."""
    import re

    personel_kur()
    sayfa = panel_istemci.get("/oturum/giris").text
    m = re.search(r'name="_csrf" value="([^"]*)"', sayfa)
    assert m and m.group(1) == ""


# --- geliştirme kolaylığı: sabit TOTP kodu ---


def _ham_kod(istemci, kod: str):
    """`_totp` sırdan kod üretiyor; burada kodun kendisi gönderiliyor."""
    return istemci.post("/oturum/totp", data={"kod": kod, "_csrf": _csrf(istemci)})


def test_sabit_kod_varsayilan_olarak_yok(panel_istemci, personel_kur):
    """
    Ayar tanımlı değilken sabit kod diye bir şey **yok**.

    Varsayılanın kapalı olması özelliğin tamamı: açıkça tanımlanmadan
    ikinci faktörün yanından geçen bir yol bulunmamalı.
    """
    personel_kur(sir=sir_uret())
    _giris(panel_istemci)
    assert _ham_kod(panel_istemci, "123456").status_code == 401


def test_sabit_kod_gelistirmede_gecebiliyor(panel_istemci, personel_kur, monkeypatch):
    """Ayar tanımlıyken sabit kod doğrulayıcı uygulamanın yerine geçiyor."""
    from app.ayarlar import ayarlar

    monkeypatch.setattr(ayarlar(), "panel_sabit_totp", "123456")

    personel_kur(sir=sir_uret())
    _giris(panel_istemci)
    yanit = _ham_kod(panel_istemci, "123456")
    assert yanit.status_code == 303
    assert yanit.headers["location"] == "/yonetim/gosterge"


def test_sabit_kod_yanlis_girilirse_yine_reddediliyor(
    panel_istemci, personel_kur, monkeypatch
):
    """Ayarın açık olması her kodu kabul etmek demek değil."""
    from app.ayarlar import ayarlar

    monkeypatch.setattr(ayarlar(), "panel_sabit_totp", "123456")

    personel_kur(sir=sir_uret())
    _giris(panel_istemci)
    assert _ham_kod(panel_istemci, "000000").status_code == 401


def test_sabit_kod_uretimde_calismiyor(panel_istemci, personel_kur, monkeypatch):
    """
    Kalkış kapısı atlansa bile doğrulama yolu üretimde kapalı.

    `dogrula` üretimde bu değişkenle servisi hiç başlatmıyor; buradaki
    ikinci kapı, kapının bir gün atlanması ihtimaline karşı — yanlış rol,
    elle koşturulan bir kap.
    """
    from app.ayarlar import Ayarlar, ayarlar

    monkeypatch.setattr(ayarlar(), "panel_sabit_totp", "123456")
    monkeypatch.setattr(Ayarlar, "uretim_mi", property(lambda self: True))

    personel_kur(sir=sir_uret())
    _giris(panel_istemci)
    assert _ham_kod(panel_istemci, "123456").status_code == 401


def test_uretim_kapisi_sabit_kodu_reddediyor():
    """Üretimde tanımlıysa servis hiç açılmıyor."""
    from app.ayarlar import Ayarlar, YapilandirmaHatasi

    ayar = Ayarlar(
        YESILIZ_ORTAM="production",
        YESILIZ_JWT_ANAHTARI="x" * 48,
        YESILIZ_VERITABANI_URL="postgresql+psycopg://a:b@c:5432/d",
        YESILIZ_PANEL_SABIT_TOTP="123456",
    )
    with pytest.raises(YapilandirmaHatasi, match="PANEL_FIXED_TOTP"):
        ayar.dogrula()


# --- ikinci faktör bayrağı (geliştirme evresi) ---


def test_2fa_varsayilan_olarak_zorunlu(panel_istemci, personel_kur):
    """Varsayılan açık: parola tek başına paneli açmıyor."""
    personel_kur(sir=sir_uret())
    yanit = _giris(panel_istemci)
    assert yanit.status_code == 303
    assert yanit.headers["location"] == "/oturum/totp"


def test_2fa_kapaliyken_parola_yetiyor(panel_istemci, personel_kur, monkeypatch):
    """Bayrak kapalıyken TOTP adımı atlanıyor ve oturum tam yetkili."""
    from app.ayarlar import ayarlar

    monkeypatch.setattr(ayarlar(), "panel_2fa_zorunlu", False)

    personel_kur(sir=sir_uret())
    yanit = _giris(panel_istemci)
    assert yanit.status_code == 303
    assert yanit.headers["location"] == "/yonetim/gosterge"

    # Oturum yarım kalmıyor: korumalı sayfa gerçekten açılıyor.
    assert panel_istemci.get("/yonetim/gosterge").status_code == 200


def test_2fa_kapaliyken_yanlis_parola_yine_reddediliyor(
    panel_istemci, personel_kur, monkeypatch
):
    """Bayrak ikinci faktörü kaldırıyor, kimlik doğrulamasını değil."""
    from app.ayarlar import ayarlar

    monkeypatch.setattr(ayarlar(), "panel_2fa_zorunlu", False)

    personel_kur(sir=sir_uret())
    assert _giris(panel_istemci, sifre="yanlis").status_code == 401


def test_2fa_kapaliyken_her_sayfada_serit_var(panel_istemci, personel_kur, monkeypatch):
    """
    Görünmeyen bir kapalı güvenlik kontrolü unutulur.

    Bayrak tek bir ortam değişkeni; Yeşilay'a devretmeden önce açılması
    gerektiğini ekranın kendisi söylemeli.
    """
    from app.ayarlar import ayarlar

    monkeypatch.setattr(ayarlar(), "panel_2fa_zorunlu", False)

    personel_kur(sir=sir_uret())
    _giris(panel_istemci)
    sayfa = panel_istemci.get("/yonetim/gosterge").text
    assert "İki adımlı doğrulama kapalı" in sayfa

    # Açıkken şerit yok.
    monkeypatch.setattr(ayarlar(), "panel_2fa_zorunlu", True)
    assert "İki adımlı doğrulama kapalı" not in panel_istemci.get(
        "/yonetim/gosterge"
    ).text
