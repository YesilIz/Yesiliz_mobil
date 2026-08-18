"""
Panel oturumu — sunucu tarafı, çerez taşımalı (`docs/panel-plani.md §2`).

**JWT değil.** Personel hesabı askıya alındığında oturumun *o anda* düşmesi
gerekiyor; JWT tarayıcıda iptal edilemez, imzalı bir çerez de edilemez.
`personel_oturum` tablosu bunu tek `UPDATE` ile yapıyor. Bu aynı zamanda
`itsdangerous` bağımlılığını gereksiz kılıyor.

Jeton çereze **ham** yazılıyor, veritabanına SHA-256 özeti — `YenilemeJetonu`
ile aynı sözleşme (CLAUDE.md §12 K36).
"""

from __future__ import annotations

import datetime as dt
import hashlib
import hmac
import logging
from collections.abc import Callable

from fastapi import Depends, HTTPException, Request, Response, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ayarlar import ayarlar
from app.guvenlik import jeton_ozetle, rastgele_jeton
from app.modeller import Personel, PersonelDurumu, PersonelOturum, PersonelRolu
from app.veritabani import oturum as db_oturumu

gunluk = logging.getLogger(__name__)

CEREZ_ADI = "yesiliz_panel"

GIRIS_YOLU = "/oturum/giris"
TOTP_YOLU = "/oturum/totp"

#: Parola doğrulandıktan sonra TOTP adımı için tanınan süre. Kısa: yarım
#: kalmış bir giriş saatlerce açık bir kapı bırakmamalı.
TOTP_ADIMI_DAKIKA = 5

#: `son_erisim` yazımı bu aralıkta bir kez yapılıyor. Her HTMX isteğinde
#: bir UPDATE, tablo sayfalanırken dakikada onlarca yazma demek.
SON_ERISIM_ARALIGI_SN = 60

#: Kaba kuvvet kilidi (`docs/panel-plani.md §1.1 U5`). Hem parola hem TOTP
#: adımı sayıyor.
KILIT_ESIGI = 5
KILIT_DAKIKA = 15


def istemci_ip(istek: Request) -> str:
    """
    İstemcinin adresi — Railway'in ters vekili arkasında.

    `X-Forwarded-For` zincirinin **ilk** öğesi istemci; sonrakiler vekiller.
    Başlık istemci tarafından uydurulabiliyor, o yüzden yalnızca denetim
    kaydında bilgi olarak duruyor, hiçbir yetki kararına girmiyor.
    """
    iletilen = istek.headers.get("x-forwarded-for", "")
    if iletilen:
        return iletilen.split(",")[0].strip()[:45]
    return (istek.client.host if istek.client else "")[:45]


def tarayici_ozeti(istek: Request) -> str:
    return istek.headers.get("user-agent", "")[:255]


def csrf_jetonu(jeton_ozet: str) -> str:
    """
    Oturuma bağlı CSRF jetonu.

    Double-submit cookie **değil**: o, JS'in okuyabildiği ikinci bir çerez
    gerektiriyor ve okunabilir çerez XSS'ten bir adım uzakta. Bunun yerine
    jeton oturum özetinden HMAC ile türetiliyor — durumsuz, ekstra kolon yok.

    **Tek kullanımlık değil** (`docs/panel-plani.md §1.1 U2`): tek kullanımlık
    jeton HTMX kısmi güncellemeleriyle ve iki açık sekmeyle çalışmıyor. Tek
    kullanımlık olan tek şey puan düzeltme formunun işlem anahtarı.
    """
    sir = ayarlar().panel_oturum_sirri.encode("utf-8")
    return hmac.new(sir, jeton_ozet.encode("ascii"), hashlib.sha256).hexdigest()


def _simdi() -> dt.datetime:
    return dt.datetime.now(dt.timezone.utc)


def _utc(deger: dt.datetime) -> dt.datetime:
    """SQLite saat dilimi bilgisini düşürüyor; karşılaştırmadan önce eşitle."""
    return deger if deger.tzinfo else deger.replace(tzinfo=dt.timezone.utc)


# --- yaşam döngüsü ---


def oturum_ac(db: Session, personel: Personel, istek: Request) -> tuple[str, PersonelOturum]:
    """
    Parola adımından sonra oturumu açar; TOTP henüz doğrulanmadı.

    Çerez **şimdi** konuyor: TOTP adımı da aynı oturumu kullanıyor, ikinci
    bir geçici çerez taşımaya gerek yok.
    """
    ham = rastgele_jeton()
    kayit = PersonelOturum(
        personel_id=personel.id,
        jeton_ozet=jeton_ozetle(ham),
        totp_dogrulandi=False,
        ip=istemci_ip(istek),
        tarayici=tarayici_ozeti(istek),
        mutlak_bitis=_simdi() + dt.timedelta(minutes=TOTP_ADIMI_DAKIKA),
        iptal=False,
    )
    db.add(kayit)
    return ham, kayit


def totp_dogrulandi_isaretle(kayit: PersonelOturum) -> None:
    """İkinci faktör geçildi — oturum vardiya uzunluğuna çekiliyor."""
    kayit.totp_dogrulandi = True
    kayit.mutlak_bitis = _simdi() + dt.timedelta(
        hours=ayarlar().panel_oturum_mutlak_saat
    )


def cerezi_yaz(yanit: Response, ham_jeton: str) -> None:
    """
    `Max-Age` **verilmiyor**: tarayıcı oturum çerezi.

    Sürenin tek otoritesi veritabanı (`mutlak_bitis` + hareketsizlik). Çerezde
    ikinci bir süre tutmak, ikisinin ayrışabileceği bir durum yaratırdı.
    """
    yanit.set_cookie(
        CEREZ_ADI,
        ham_jeton,
        httponly=True,
        secure=ayarlar().panel_cerez_guvenli,
        samesite="strict",
        path="/",
    )


def cerezi_sil(yanit: Response) -> None:
    yanit.delete_cookie(CEREZ_ADI, path="/")


def oturumu_kapat(db: Session, kayit: PersonelOturum) -> None:
    kayit.iptal = True
    db.commit()


# --- okuma ---


def _oturumu_coz(db: Session, istek: Request) -> PersonelOturum | None:
    ham = istek.cookies.get(CEREZ_ADI)
    if not ham:
        return None

    kayit = db.scalar(
        select(PersonelOturum).where(
            PersonelOturum.jeton_ozet == jeton_ozetle(ham),
            PersonelOturum.iptal.is_(False),
        )
    )
    if kayit is None:
        return None

    simdi = _simdi()
    if _utc(kayit.mutlak_bitis) <= simdi:
        return None

    hareketsizlik = dt.timedelta(minutes=ayarlar().panel_oturum_hareketsizlik_dakika)
    if _utc(kayit.son_erisim) + hareketsizlik <= simdi:
        return None

    # Yazma kısılıyor; her istekte UPDATE atmanın bir faydası yok.
    if (simdi - _utc(kayit.son_erisim)).total_seconds() >= SON_ERISIM_ARALIGI_SN:
        kayit.son_erisim = simdi
        db.commit()

    return kayit


def _yonlendir(istek: Request, yol: str) -> HTTPException:
    """
    Tarayıcıya 303, HTMX'e `HX-Redirect`.

    HTMX bir 303'ü kendi içinde izleyip giriş sayfasını panelin bir
    parçasının yerine gömerdi; `HX-Redirect` tam sayfa yönlendirme yapıyor.
    """
    if istek.headers.get("hx-request"):
        return HTTPException(
            status.HTTP_204_NO_CONTENT, headers={"HX-Redirect": yol}
        )
    return HTTPException(status.HTTP_303_SEE_OTHER, headers={"Location": yol})


def personel_oturumu(
    istek: Request,
    db: Session = Depends(db_oturumu),
) -> Personel:
    """
    Korumalı panel yollarının bağımlılığı.

    Personelin **durumu her istekte** kontrol ediliyor. Askıya alma zaten
    oturumları iptal ediyor; bu ikinci savunma hattı, iptal edilmesi
    unutulan ya da yarışan bir oturumu da kapatıyor.
    """
    kayit = _oturumu_coz(db, istek)
    if kayit is None:
        raise _yonlendir(istek, GIRIS_YOLU)

    personel = db.get(Personel, kayit.personel_id)
    if personel is None or personel.durum != PersonelDurumu.AKTIF.value:
        kayit.iptal = True
        db.commit()
        raise _yonlendir(istek, GIRIS_YOLU)

    if not kayit.totp_dogrulandi:
        raise _yonlendir(istek, TOTP_YOLU)

    # İstek boyunca gereken yan bilgiler; ayrı bir dependency açmaya değmez.
    # `sablon.yanit` ikisini de buradan okuyor, route'lar tekrarlamıyor.
    istek.state.oturum = kayit
    istek.state.personel = personel
    istek.state.csrf = csrf_jetonu(kayit.jeton_ozet)
    return personel


def personel_gerek(*roller: PersonelRolu) -> Callable[..., Personel]:
    """
    Rol kapısı üreten bağımlılık fabrikası.

    Her panel route'u rolünü **açıkça beyan ediyor**; kapsamı
    `test_panel_yetki_kapsami` router listesini tarayarak garanti ediyor,
    yani yetkisiz eklenmiş bir uç CI'da yakalanıyor.
    """
    izinli = {r.value for r in roller}

    def kapi(personel: Personel = Depends(personel_oturumu)) -> Personel:
        if personel.rol not in izinli:
            # Ekranda o bölüm hiç çizilmiyor; buraya gelmek ya doğrudan URL
            # denemesi ya da bir arayüz hatası demek. İkisi de loglanmalı.
            gunluk.warning(
                "Yetkisiz panel erişimi",
                extra={"personel": personel.id, "rol": personel.rol},
            )
            raise HTTPException(status.HTTP_403_FORBIDDEN, "Bu bölüme yetkiniz yok")
        return personel

    return kapi


# --- kaba kuvvet kilidi ---


def kilitli_mi(personel: Personel, simdi: dt.datetime | None = None) -> bool:
    if personel.kilit_bitis is None:
        return False
    return _utc(personel.kilit_bitis) > (simdi or _simdi())


def basarisiz_deneme_kaydet(db: Session, personel: Personel) -> None:
    personel.basarisiz_deneme += 1
    if personel.basarisiz_deneme >= KILIT_ESIGI:
        personel.kilit_bitis = _simdi() + dt.timedelta(minutes=KILIT_DAKIKA)
        personel.basarisiz_deneme = 0
    db.commit()


def denemeleri_sifirla(db: Session, personel: Personel) -> None:
    personel.basarisiz_deneme = 0
    personel.kilit_bitis = None
    personel.son_giris = _simdi()
    db.commit()
