"""
Panel girişi — iki adımlı, çerez oturumlu (`docs/panel-plani.md §2`).

**Kayıt formu ve parola sıfırlama YOK.** Personel hesabı açmak bir yetki
işlemi: ilk hesap `python -m app.personel_ekle`, sonrakiler panelden `admin`
rolüyle. İnternete açık bir kayıt formu panelin bütün yetki modelini
anlamsız kılardı; sıfırlama da e-posta sağlayıcısı olmadan (A15) dürüstçe
sunulamaz.
"""

from __future__ import annotations

import logging
import time

from fastapi import APIRouter, Depends, Form, Request, Response, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ayarlar import ayarlar
from app.guvenlik import (
    jeton_ozetle,
    sifre_dogrula,
    sifre_ozetle,
    sir_coz,
    sir_sifrele,
)
from app.modeller import (
    Personel,
    PersonelDurumu,
    PersonelKurtarmaKodu,
    PersonelRolu,
)
from app.panel import oturum as panel_oturum
from app.panel.sablon import yanit, yonlendir
from app.totp import dogrula as totp_dogrula
from app.totp import otpauth_uri, sir_uret
from app.personel_ekle import ASGARI_SIFRE as ASGARI_PANEL_SIFRE
from app.veritabani import oturum

gunluk = logging.getLogger(__name__)

yonlendirici = APIRouter(prefix="/oturum", tags=["panel-oturum"])

#: Kimlik bilgisi hatalarında **tek** mesaj. Hangi e-postanın kayıtlı
#: olduğunu sızdırmamak için: `hesap.py`'nin giriş ucuyla aynı kural.
KIMLIK_HATASI = "E-posta ya da parola hatalı."
KILIT_MESAJI = (
    "Çok fazla başarısız deneme yapıldı. Hesap geçici olarak kilitlendi; "
    "birkaç dakika sonra tekrar deneyin."
)

#: YEDAM rolleri havuza, geri kalanı göstergeye düşüyor.
_YEDAM_ROLLERI = {PersonelRolu.YEDAM_YONETICI.value, PersonelRolu.YEDAM_DANISMAN.value}


def giris_sonrasi_yol(rol: str) -> str:
    return "/yedam/havuz" if rol in _YEDAM_ROLLERI else "/yonetim/gosterge"


def _giris_hedefi(personel: Personel) -> str:
    """
    TOTP geçildikten sonra nereye?

    Geçici parolayla açılmış hesap önce parolasını değiştiriyor
    (`docs/panel-plani.md §5`, S1). `personel_oturumu` bağımlılığı bunu
    zaten zorluyor; buradaki dal yalnızca kullanıcıyı gereksiz bir
    yönlendirme sekmesinden kurtarıyor.
    """
    if personel.parola_degistirmeli:
        return panel_oturum.PAROLA_YOLU
    return giris_sonrasi_yol(personel.rol)


# --- 1. adım: parola ---


@yonlendirici.get("/giris")
def giris_formu(istek: Request, db: Session = Depends(oturum)):
    return yanit(istek, "giris.html", {"hata": None, "eposta": ""})


@yonlendirici.post("/giris")
def giris(
    istek: Request,
    eposta: str = Form(...),
    sifre: str = Form(...),
    db: Session = Depends(oturum),
):
    temiz = eposta.strip().lower()
    personel = db.scalar(select(Personel).where(Personel.eposta == temiz))

    if personel is not None and panel_oturum.kilitli_mi(personel):
        return yanit(
            istek,
            "giris.html",
            {"hata": KILIT_MESAJI, "eposta": temiz},
            durum=status.HTTP_429_TOO_MANY_REQUESTS,
        )

    # Bulunmayan e-posta da gerçek bir Argon2id doğrulaması çalıştırıyor;
    # `guvenlik.sifre_dogrula` bunu `None` özetle kendi içinde yapıyor.
    dogru = sifre_dogrula(personel.sifre_ozet if personel else None, sifre)

    if personel is None or not dogru:
        if personel is not None:
            panel_oturum.basarisiz_deneme_kaydet(db, personel)
        gunluk.warning("Panel giriş denemesi başarısız", extra={"yol": "/oturum/giris"})
        return yanit(
            istek,
            "giris.html",
            {"hata": KIMLIK_HATASI, "eposta": temiz},
            durum=status.HTTP_401_UNAUTHORIZED,
        )

    if personel.durum != PersonelDurumu.AKTIF.value:
        # Askıya alınmış hesaba parolası doğru olsa da oturum açılmıyor ve
        # sebebi söylenmiyor — durum bilgisi de bir sızıntı.
        return yanit(
            istek,
            "giris.html",
            {"hata": KIMLIK_HATASI, "eposta": temiz},
            durum=status.HTTP_401_UNAUTHORIZED,
        )

    ham, _ = panel_oturum.oturum_ac(db, personel, istek)
    db.commit()

    hedef = "/oturum/totp" if personel.totp_sir else "/oturum/totp-kur"
    cevap = yonlendir(istek, hedef)
    panel_oturum.cerezi_yaz(cevap, ham)
    return cevap


# --- 2. adım: TOTP ---


def _yarim_oturum(istek: Request, db: Session):
    """
    Parola adımını geçmiş ama TOTP'yi geçmemiş oturumu döndürür.

    `personel_oturumu` bağımlılığı kullanılamıyor: o, TOTP doğrulanmamış
    oturumu bu sayfaya yönlendiriyor — burada kullanılırsa döngü olurdu.
    """
    ham = istek.cookies.get(panel_oturum.CEREZ_ADI)
    if not ham:
        return None, None
    kayit = db.scalar(
        select(panel_oturum.PersonelOturum).where(
            panel_oturum.PersonelOturum.jeton_ozet == jeton_ozetle(ham),
            panel_oturum.PersonelOturum.iptal.is_(False),
        )
    )
    if kayit is None:
        return None, None
    return kayit, db.get(Personel, kayit.personel_id)


def _csrf(kayit) -> str:
    return panel_oturum.csrf_jetonu(kayit.jeton_ozet)


@yonlendirici.get("/totp")
def totp_formu(istek: Request, db: Session = Depends(oturum)):
    kayit, personel = _yarim_oturum(istek, db)
    if kayit is None or personel is None:
        return yonlendir(istek, "/oturum/giris")
    if not personel.totp_sir:
        return yonlendir(istek, "/oturum/totp-kur")
    if kayit.totp_dogrulandi:
        return yonlendir(istek, _giris_hedefi(personel))
    return yanit(istek, "totp.html", {"hata": None, "csrf": _csrf(kayit)})


@yonlendirici.post("/totp")
def totp_dogrulama(
    istek: Request,
    kod: str = Form(...),
    db: Session = Depends(oturum),
):
    kayit, personel = _yarim_oturum(istek, db)
    if kayit is None or personel is None or not personel.totp_sir:
        return yonlendir(istek, "/oturum/giris")

    if panel_oturum.kilitli_mi(personel):
        return yanit(
            istek,
            "totp.html",
            {"hata": KILIT_MESAJI, "csrf": _csrf(kayit)},
            durum=status.HTTP_429_TOO_MANY_REQUESTS,
        )

    temiz = kod.strip()
    sayac = totp_dogrula(sir_coz(personel.totp_sir), temiz, int(time.time()))

    if sayac is not None:
        # Aynı kodun ikinci kez kullanılması reddediliyor: omuz üstünden
        # okunan bir kod 30 saniye boyunca geçerli kalmamalı.
        if sayac <= personel.son_totp_sayaci:
            panel_oturum.basarisiz_deneme_kaydet(db, personel)
            return yanit(
                istek,
                "totp.html",
                {"hata": "Bu kod daha önce kullanıldı. Yeni kodu bekleyin.",
                 "csrf": _csrf(kayit)},
                durum=status.HTTP_401_UNAUTHORIZED,
            )
        personel.son_totp_sayaci = sayac
    elif not _kurtarma_koduyla_gec(db, personel, temiz):
        panel_oturum.basarisiz_deneme_kaydet(db, personel)
        return yanit(
            istek,
            "totp.html",
            {"hata": "Kod doğrulanamadı.", "csrf": _csrf(kayit)},
            durum=status.HTTP_401_UNAUTHORIZED,
        )

    panel_oturum.totp_dogrulandi_isaretle(kayit)
    panel_oturum.denemeleri_sifirla(db, personel)
    return yonlendir(istek, _giris_hedefi(personel))


def _kurtarma_koduyla_gec(db: Session, personel: Personel, kod: str) -> bool:
    """Telefonunu kaybeden personelin tek kullanımlık çıkış yolu."""
    kayit = db.scalar(
        select(PersonelKurtarmaKodu).where(
            PersonelKurtarmaKodu.personel_id == personel.id,
            PersonelKurtarmaKodu.kod_ozet == jeton_ozetle(kod.strip().upper()),
            PersonelKurtarmaKodu.kullanildi.is_(False),
        )
    )
    if kayit is None:
        return False
    kayit.kullanildi = True
    db.commit()
    gunluk.warning(
        "Kurtarma koduyla giriş", extra={"personel": personel.id}
    )
    return True


# --- ilk kurulum ---


@yonlendirici.get("/totp-kur")
def totp_kurulum_formu(istek: Request, db: Session = Depends(oturum)):
    """
    Hesapta ikinci faktör tanımlı değilken gösteriliyor.

    Sır form alanında taşınıyor; sunucu tarafında saklamak için ayrı bir
    tablo açmaya değmiyor. Yarım oturumu ele geçiren biri kendi sırrını
    kurabilir — ama o kişi parolayı zaten biliyor, yani kurulum aşamasında
    ikinci faktörün sağladığı ek koruma doğası gereği yok. Kurulum bir kez
    yapılıyor ve sonrası korunuyor.
    """
    kayit, personel = _yarim_oturum(istek, db)
    if kayit is None or personel is None:
        return yonlendir(istek, "/oturum/giris")
    if personel.totp_sir:
        return yonlendir(istek, "/oturum/totp")

    sir = sir_uret()
    return yanit(
        istek,
        "totp_kurulum.html",
        {
            "hata": None,
            "sir": sir,
            "uri": otpauth_uri(sir, personel.eposta, "Yesiliz Panel"),
            "csrf": _csrf(kayit),
        },
    )


@yonlendirici.post("/totp-kur")
def totp_kurulum(
    istek: Request,
    sir: str = Form(...),
    kod: str = Form(...),
    db: Session = Depends(oturum),
):
    kayit, personel = _yarim_oturum(istek, db)
    if kayit is None or personel is None:
        return yonlendir(istek, "/oturum/giris")
    if personel.totp_sir:
        # Yarışan ikinci bir kurulum isteği var olan sırrı ezmemeli.
        return yonlendir(istek, "/oturum/totp")

    sayac = totp_dogrula(sir, kod.strip(), int(time.time()))
    if sayac is None:
        return yanit(
            istek,
            "totp_kurulum.html",
            {
                "hata": "Kod doğrulanamadı. Sırrı doğru eklediğinden emin ol.",
                "sir": sir,
                "uri": otpauth_uri(sir, personel.eposta, "Yesiliz Panel"),
                "csrf": _csrf(kayit),
            },
            durum=status.HTTP_401_UNAUTHORIZED,
        )

    personel.totp_sir = sir_sifrele(sir)
    personel.son_totp_sayaci = sayac
    panel_oturum.totp_dogrulandi_isaretle(kayit)
    panel_oturum.denemeleri_sifirla(db, personel)
    return yonlendir(istek, _giris_hedefi(personel))


# --- çıkış ---


@yonlendirici.post("/cikis")
def cikis(istek: Request, db: Session = Depends(oturum)):
    kayit, _ = _yarim_oturum(istek, db)
    if kayit is not None:
        panel_oturum.oturumu_kapat(db, kayit)
    cevap = yonlendir(istek, "/oturum/giris")
    panel_oturum.cerezi_sil(cevap)
    return cevap


# --- zorunlu parola değişimi ---


@yonlendirici.get(panel_oturum.PAROLA_YOLU.removeprefix("/oturum"))
def parola_degistir_formu(
    istek: Request,
    personel: Personel = Depends(panel_oturum.personel_oturumu),
):
    """
    Geçici parolayla açılmış hesabın ilk durağı.

    Bu route rol kapısı **taşımıyor** ve taşımamalı: her rolden personel
    buraya düşebiliyor ve kapıdan geçmeden panelin hiçbir yerine
    gidemiyor.
    """
    return yanit(istek, "parola_degistir.html", {"hata": None})


@yonlendirici.post(panel_oturum.PAROLA_YOLU.removeprefix("/oturum"))
def parola_degistir(
    istek: Request,
    mevcut: str = Form(...),
    yeni: str = Form(...),
    yeni_tekrar: str = Form(...),
    db: Session = Depends(oturum),
    personel: Personel = Depends(panel_oturum.personel_oturumu),
):
    """
    Parolayı değiştirir ve `parola_degistirmeli` bayrağını düşürür.

    **Mevcut parola şart.** Oturum çerezi tek başına yetseydi, panelin
    açık kaldığı bir bilgisayarı eline geçiren biri hesabı devralırdı
    (`CLAUDE.md` §12 K38'in panel karşılığı).

    Bütün oturumlar düşürülüyor — **bu oturum dahil**. Parola değişimi
    "bu kimlik bilgisi artık geçersiz" demek; geçici parolayı bilen
    başkasının açık bir oturumu varsa o da bitmeli.
    """
    hata = _parola_hatasi(personel, mevcut, yeni, yeni_tekrar)
    if hata is not None:
        return yanit(
            istek,
            "parola_degistir.html",
            {"hata": hata},
            durum=status.HTTP_422_UNPROCESSABLE_CONTENT,
        )

    personel.sifre_ozet = sifre_ozetle(yeni)
    personel.parola_degistirmeli = False
    panel_oturum.oturumlari_dusur(db, personel.id)
    db.commit()

    cevap = yonlendir(istek, panel_oturum.GIRIS_YOLU)
    panel_oturum.cerezi_sil(cevap)
    return cevap


def _parola_hatasi(personel: Personel, mevcut: str, yeni: str, tekrar: str) -> str | None:
    if not sifre_dogrula(personel.sifre_ozet, mevcut):
        return "Mevcut parola hatalı."
    if yeni != tekrar:
        return "Yeni parolalar birbirini tutmuyor."
    if len(yeni) < ASGARI_PANEL_SIFRE:
        return f"Yeni parola en az {ASGARI_PANEL_SIFRE} karakter olmalı."
    if yeni == mevcut:
        # Geçici parolayı "değiştirdim" diye yeniden yazmak, bayrağı
        # düşürüp asıl amacı boşa çıkarırdı.
        return "Yeni parola geçici parolayla aynı olamaz."
    return None
