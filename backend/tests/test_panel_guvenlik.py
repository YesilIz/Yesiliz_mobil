"""
Panel güvenlik kapıları — CSRF, başlıklar ve yetki kapsamı.

Buradaki iki test (`test_yetki_kapsami_*`, `test_denetim...`) davranış değil
**kapsam** sınıyor: router listesini gezip her yazma yolunun kapıdan geçtiğini
doğruluyor. Amaç bir hatayı yakalamak değil, gelecekte yetkisiz eklenecek bir
ucun CI'da kırmızıya dönmesi.
"""

from __future__ import annotations

import pytest
from fastapi.routing import APIRoute
from fastapi.testclient import TestClient

from app.guvenlik import jeton_ozetle, sifre_ozetle, sir_sifrele
from app.modeller import Personel, PersonelDurumu, PersonelRolu
from app.panel import oturum as panel_oturum
from app.panel.basliklar import CSP
from app.panel.oturum import CEREZ_ADI, csrf_jetonu, personel_oturumu
from app.totp import kod_uret, sayac, sir_uret

SIFRE = "panel-parolasi-2026"

#: Oturum açmadan erişilebilen yollar. Liste **kısa kalmalı**; her yeni
#: satır bir muafiyet demek.
KAPI_DISI = ("/oturum/", "/statik", "/health", "/")


@pytest.fixture
def girisli(panel_istemci, db):
    """TOTP'yi de geçmiş, panele girmiş bir admin oturumu."""
    import time

    sir = sir_uret()
    db.add(
        Personel(
            eposta="admin@yesilay.org.tr",
            ad="Panel Yöneticisi",
            sifre_ozet=sifre_ozetle(SIFRE),
            rol=PersonelRolu.ADMIN.value,
            durum=PersonelDurumu.AKTIF.value,
            totp_sir=sir_sifrele(sir),
        )
    )
    db.commit()
    panel_istemci.post(
        "/oturum/giris", data={"eposta": "admin@yesilay.org.tr", "sifre": SIFRE}
    )
    jeton = csrf_jetonu(jeton_ozetle(panel_istemci.cookies[CEREZ_ADI]))
    panel_istemci.post(
        "/oturum/totp",
        data={"kod": kod_uret(sir, sayac(int(time.time()))), "_csrf": jeton},
    )
    return panel_istemci


def _csrf(istemci) -> str:
    return csrf_jetonu(jeton_ozetle(istemci.cookies[CEREZ_ADI]))


# --- CSRF ---


def test_jetonsuz_post_reddediliyor(girisli):
    """Muafiyet listesi boş: güvensiz metotların hepsi varsayılan olarak red."""
    yanit = girisli.post("/oturum/cikis", data={})
    assert yanit.status_code == 403
    assert "Oturum doğrulaması" in yanit.text


def test_yanlis_jetonlu_post_reddediliyor(girisli):
    yanit = girisli.post("/oturum/cikis", data={"_csrf": "a" * 64})
    assert yanit.status_code == 403


def test_dogru_jetonlu_post_geciyor(girisli):
    assert girisli.post("/oturum/cikis", data={"_csrf": _csrf(girisli)}).status_code == 303


def test_jeton_baslikta_da_kabul_ediliyor(girisli):
    """HTMX form alanı gönderemiyor; `hx-headers` ile başlıkta taşıyor."""
    yanit = girisli.post("/oturum/cikis", headers={"X-CSRF": _csrf(girisli)})
    assert yanit.status_code == 303


def test_yabanci_origin_reddediliyor(girisli, monkeypatch):
    a = panel_oturum.ayarlar()
    monkeypatch.setattr(a, "panel_taban_url", "https://panel.yesilay.org.tr")
    yanit = girisli.post(
        "/oturum/cikis",
        data={"_csrf": _csrf(girisli)},
        headers={"Origin": "https://saldirgan.example"},
    )
    assert yanit.status_code == 403


def test_kendi_originimiz_kabul_ediliyor(girisli, monkeypatch):
    a = panel_oturum.ayarlar()
    monkeypatch.setattr(a, "panel_taban_url", "https://panel.yesilay.org.tr")
    yanit = girisli.post(
        "/oturum/cikis",
        data={"_csrf": _csrf(girisli)},
        headers={"Origin": "https://panel.yesilay.org.tr"},
    )
    assert yanit.status_code == 303


def test_basliksiz_oturumsuz_post_normal_yonlendirmeye_dusuyor(
    panel_istemci, monkeypatch
):
    """
    `Origin` yokluğu tek başına red sebebi değil.

    Red sayılsaydı panele atılan her tarayıcı dışı POST — var olmayan bir
    yola atılan dahil — 404 yerine 403 dönerdi ve rol ayrımının "cihaz ucu
    panelde yok" iddiası dışarıdan doğrulanamazdı.
    """
    a = panel_oturum.ayarlar()
    monkeypatch.setattr(a, "panel_taban_url", "https://panel.yesilay.org.tr")
    assert panel_istemci.post("/auth/giris", json={}).status_code == 404


def test_oturumlu_istekte_jeton_hala_zorunlu(girisli, monkeypatch):
    """Origin gevşetmesi jeton kontrolünü zayıflatmamalı."""
    a = panel_oturum.ayarlar()
    monkeypatch.setattr(a, "panel_taban_url", "https://panel.yesilay.org.tr")
    assert girisli.post("/oturum/cikis", data={}).status_code == 403


def test_giris_postu_jetonsuz_gecebiliyor(panel_istemci):
    """
    Giriş oturum açılmadan geldiği için CSRF jetonu taşıyamaz.

    Onu koruyan şey `Origin` denetimi ve parolanın kendisi; kimlik
    bilgisi olmadan bir giriş isteği zaten bir şey yapamıyor.
    """
    yanit = panel_istemci.post(
        "/oturum/giris", data={"eposta": "yok@ornek.com", "sifre": "x"}
    )
    assert yanit.status_code == 401


# --- güvenlik başlıkları ---


def test_csp_unsafe_inline_icermiyor(panel_istemci):
    yanit = panel_istemci.get("/oturum/giris")
    assert yanit.headers["content-security-policy"] == CSP
    assert "unsafe-inline" not in CSP
    assert "default-src 'none'" in CSP


@pytest.mark.parametrize(
    "baslik, deger",
    [
        ("x-frame-options", "DENY"),
        ("x-content-type-options", "nosniff"),
        ("referrer-policy", "no-referrer"),
        ("cache-control", "no-store"),
    ],
)
def test_sabit_basliklar(panel_istemci, baslik, deger):
    assert panel_istemci.get("/oturum/giris").headers[baslik] == deger


def test_statik_dosya_no_store_almiyor(panel_istemci):
    """htmx ve CSS her sayfa açılışında yeniden indirilmemeli."""
    yanit = panel_istemci.get("/statik/panel.css")
    assert yanit.status_code == 200
    assert yanit.headers.get("cache-control") != "no-store"


def test_htmx_depoda_servis_ediliyor(panel_istemci):
    """CDN yok: panel VPN arkasında dışa çıkışsız olabilir (§5)."""
    assert panel_istemci.get("/statik/htmx.min.js").status_code == 200


# --- yetki kapsamı ---


def _bagimliliklar(dependant) -> set:
    """Bir route'un bağımlılık ağacındaki bütün çağrılabilirler."""
    bulunan = set()
    yigin = list(dependant.dependencies)
    while yigin:
        alt = yigin.pop()
        if alt.call is not None:
            bulunan.add(alt.call)
        yigin.extend(alt.dependencies)
    return bulunan


def test_yetki_kapsami_her_panel_yolunu_kapsiyor(panel_uygulamasi):
    """
    Kapı dışında bırakılmış bir panel yolu CI'da kırmızıya dönmeli.

    `personel_gerek(...)` ürettiği kapı `personel_oturumu`'na bağlı, yani
    ağaçta o görünüyorsa yol korunuyor demektir.
    """
    korumasiz = []
    for yol in panel_uygulamasi.routes:
        if not isinstance(yol, APIRoute) or yol.path.startswith(KAPI_DISI):
            continue
        if personel_oturumu not in _bagimliliklar(yol.dependant):
            korumasiz.append(f"{yol.methods} {yol.path}")

    assert not korumasiz, f"Yetki kapısı olmayan panel yolu: {korumasiz}"


def test_rol_kapisi_yanlis_role_403_veriyor(panel_istemci, db):
    """
    En az yetki: danışman yönetim göstergesini göremiyor.

    Ekranda o bağlantı zaten çizilmiyor; buraya gelmek doğrudan URL denemesi
    demek ve 403 dönmesi gerekiyor.
    """
    import time

    sir = sir_uret()
    db.add(
        Personel(
            eposta="danisman@yesilay.org.tr",
            ad="Danışman",
            sifre_ozet=sifre_ozetle(SIFRE),
            rol=PersonelRolu.YEDAM_DANISMAN.value,
            durum=PersonelDurumu.AKTIF.value,
            totp_sir=sir_sifrele(sir),
        )
    )
    db.commit()

    panel_istemci.post(
        "/oturum/giris", data={"eposta": "danisman@yesilay.org.tr", "sifre": SIFRE}
    )
    jeton = csrf_jetonu(jeton_ozetle(panel_istemci.cookies[CEREZ_ADI]))
    girisi = panel_istemci.post(
        "/oturum/totp",
        data={"kod": kod_uret(sir, sayac(int(time.time()))), "_csrf": jeton},
    )
    # Danışman havuza düşüyor, göstergeye değil.
    assert girisi.headers["location"] == "/yedam/havuz"
    assert panel_istemci.get("/yedam/havuz").status_code == 200
    assert panel_istemci.get("/yonetim/gosterge").status_code == 403


def test_api_rolunde_panel_middlewarei_yuklenmiyor(istemci):
    """
    CSRF kapısı ve panel başlıkları cihaz API'sine sızmamalı.

    Sızsaydı Android istemcisi (CSRF jetonu taşımıyor) bütün yazma
    isteklerinde 403 alırdı.
    """
    yanit = istemci.post("/auth/giris", json={"eposta": "yok@ornek.com", "sifre": "x"})
    assert yanit.status_code == 401
    assert "content-security-policy" not in yanit.headers


def test_menu_linkleri_rollerle_ortusuyor(panel_uygulamasi, db):
    """
    Menüde görünen her link o rolde **açılabilmeli**, ve erişilebilen her
    ekranın menüde bir yolu olmalı.

    İkisi ayrışınca iki ayrı kusur çıkıyor: açılmayan link (kullanıcı
    403 yiyor) ya da **var olan ama ulaşılamayan özellik**. İkincisi
    gerçek bir denetimde bulundu — `operasyon` denetim kaydını görebiliyor
    ama menüde linki yoktu.
    """
    import re

    from fastapi.routing import APIRoute

    from app.panel.sablon import SABLON_DIZINI

    taban = (SABLON_DIZINI / "taban.html").read_text(encoding="utf-8")
    menu = taban.split("{% block yan_menu %}")[1].split("{% endblock %}")[0]

    # Menüdeki her yol gerçekten bir route olmalı.
    yollar = {
        y.path
        for y in panel_uygulamasi.routes
        if isinstance(y, APIRoute) and "GET" in y.methods
    }
    for link in re.findall(r'<a href="(/[^"]+)"', menu):
        assert link in yollar, f"Menüde olmayan yola link var: {link}"
