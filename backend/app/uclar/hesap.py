"""Kimlik doğrulama uçları."""

from __future__ import annotations

import datetime as dt
import logging

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.ayarlar import ayarlar
from app.guvenlik import (
    askiya_alma_govdesi,
    erisim_jetonu,
    jeton_ozetle,
    kullaniciyi_kilitle,
    mevcut_kullanici,
    rastgele_jeton,
    sifre_dogrula,
    sifre_ozetle,
)
from app.kullanici_silme import kullaniciyi_tamamen_sil
from app.veri_disa_aktar import kullanici_verisi
from app.modeller import Kullanici, SifreSifirlama, YenilemeJetonu
from app.semalar import (
    GirisIstek,
    Jetonlar,
    KayitIstek,
    Profil,
    ProfilIstek,
    SifreSifirlaIstek,
    SifreYenileIstek,
    YenileIstek,
)
from app.veritabani import oturum

gunluk = logging.getLogger(__name__)
yonlendirici = APIRouter(prefix="/auth", tags=["hesap"])


def _jetonlar(db: Session, kullanici: Kullanici) -> Jetonlar:
    a = ayarlar()
    yenileme = rastgele_jeton()
    db.add(
        YenilemeJetonu(
            kullanici_id=kullanici.id,
            jeton=jeton_ozetle(yenileme),
            son_gecerlilik=dt.datetime.now(dt.timezone.utc) + dt.timedelta(days=a.yenileme_gun),
        )
    )
    db.commit()
    return Jetonlar(
        erisim=erisim_jetonu(kullanici.id),
        yenileme=yenileme,
        ad=kullanici.ad,
        eposta=kullanici.eposta,
        avatar=kullanici.avatar or "",
    )


@yonlendirici.post("/kayit", response_model=Jetonlar, status_code=status.HTTP_201_CREATED)
def kayit(istek: KayitIstek, db: Session = Depends(oturum)) -> Jetonlar:
    eposta = istek.eposta.lower().strip()
    if db.scalar(select(Kullanici).where(Kullanici.eposta == eposta)):
        raise HTTPException(status.HTTP_409_CONFLICT, "Bu e-posta zaten kayıtlı")

    kullanici = Kullanici(
        eposta=eposta,
        ad=istek.ad.strip(),
        sifre_ozet=sifre_ozetle(istek.sifre),
    )
    db.add(kullanici)
    try:
        db.commit()
    except IntegrityError as hata:
        # Eşzamanlı aynı e-posta istekleri ön kontrolden birlikte
        # geçebilir; tekillik ihlali bir sunucu hatası değil çakışmadır.
        db.rollback()
        raise HTTPException(status.HTTP_409_CONFLICT, "Bu e-posta zaten kayıtlı") from hata
    db.refresh(kullanici)
    return _jetonlar(db, kullanici)


@yonlendirici.post("/giris", response_model=Jetonlar)
def giris(istek: GirisIstek, db: Session = Depends(oturum)) -> Jetonlar:
    # Hesap silme ile giriş yarışırsa, silinmiş kullanıcıya yenileme
    # jetonu eklenmeye çalışılmasın. Kilit jeton yazılana kadar tutulur.
    kullanici = db.scalar(
        select(Kullanici)
        .where(Kullanici.eposta == istek.eposta.lower().strip())
        .with_for_update()
    )

    # Kullanıcı yoksa da gerçek bir Argon2 doğrulaması yapılır. Yalnızca aynı
    # mesajı vermek yetmez: hızlı "kullanıcı yok" yolu süre ölçümüyle kayıtlı
    # e-postaları sızdırırdı.
    sifre_gecerli = sifre_dogrula(
        kullanici.sifre_ozet if kullanici is not None else None,
        istek.sifre,
    )
    if kullanici is None or not sifre_gecerli:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "E-posta ya da şifre hatalı")

    # Parola doğrulandıktan SONRA bakılıyor: önce bakmak, yanlış parolayla
    # bile bir e-postanın askıya alınmış olduğunu sızdırırdı.
    if kullanici.askida_mi:
        raise HTTPException(
            status.HTTP_403_FORBIDDEN, detail=askiya_alma_govdesi(kullanici)
        )

    return _jetonlar(db, kullanici)


@yonlendirici.post("/yenile", response_model=Jetonlar)
def yenile(istek: YenileIstek, db: Session = Depends(oturum)) -> Jetonlar:
    jeton_ozeti = jeton_ozetle(istek.yenileme)
    ilk_okuma = db.scalar(
        select(YenilemeJetonu).where(
            YenilemeJetonu.jeton == jeton_ozeti,
            YenilemeJetonu.iptal == False,  # noqa: E712
        )
    )
    if ilk_okuma is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Yenileme jetonu geçersiz")

    # Hesap silme/çıkış ile kilit sırası her zaman kullanıcı→jeton.
    # Ters sıra, biri kullanıcıyı diğeri jetonu tuttuğunda deadlock
    # üretebilirdi. Kilitten sonra jeton yeniden okunup doğrulanır.
    db.scalar(
        select(Kullanici.id)
        .where(Kullanici.id == ilk_okuma.kullanici_id)
        .with_for_update()
    )
    kayit_ = db.scalar(
        select(YenilemeJetonu)
        .where(
            YenilemeJetonu.jeton == jeton_ozeti,
            YenilemeJetonu.iptal == False,  # noqa: E712
        )
        .with_for_update()
    )
    simdi = dt.datetime.now(dt.timezone.utc)

    if kayit_ is None or _gecmis(kayit_.son_gecerlilik, simdi):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Yenileme jetonu geçersiz")

    # Dönüşümlü jeton: eski jeton iptal edilip yenisi veriliyor. Çalınan bir
    # yenileme jetonu ikinci kez kullanılamıyor.
    kayit_.iptal = True
    kullanici = db.get(Kullanici, kayit_.kullanici_id)
    if kullanici is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Kullanıcı bulunamadı")
    # Askıya alma jetonları iptal ediyor, ama iptalle bu istek yarışabilir.
    # Yenilemenin kendisi de kapıdan geçmeli, yoksa 30 dakikalık bir
    # pencerede askı fiilen uygulanmıyor.
    if kullanici.askida_mi:
        db.commit()
        raise HTTPException(
            status.HTTP_403_FORBIDDEN, detail=askiya_alma_govdesi(kullanici)
        )
    return _jetonlar(db, kullanici)


# `exclude_none`: `jetonlar` yalnızca şifre değiştiğinde gövdede görünüyor.
# Alanı her yanıtta `null` olarak taşımak, ad/avatar güncellemesinin
# sözleşmesini de değiştirirdi; bu yolda gövde bit-birebir eskisi kalıyor.
@yonlendirici.patch("/profil", response_model=Profil, response_model_exclude_none=True)
def profil_guncelle(
    istek: ProfilIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> Profil:
    """
    Ad, profil simgesi ve şifre güncellemesi.

    Bu uç yazılmadan önce istemci "profil güncellendi" diyip yalnızca
    DataStore'a yazıyordu: sunucudaki ad değişmiyor, girilen yeni şifre ise
    hiçbir yere gitmiyordu — kullanıcı şifresini değiştirdiğini sanıp eski
    şifreyle giriş yapmaya devam ediyordu (CLAUDE.md §4, K37).

    Şifre değişimi **mevcut şifreyi** şart koşuyor. Erişim jetonu tek başına
    yetseydi, telefonu eline geçiren biri hesabı kalıcı olarak devralırdı.

    Ve şifre değişince **bütün yenileme jetonları iptal ediliyor.** Bu uç
    şifreyi değiştiren üç yoldan biriydi ve tek iptal etmeyeniydi: `cikis`
    ve `sifre-yenile` ediyor, panel de ediyor (K60). Yani ucun kendi tarif
    ettiği tehdit karşılanmıyordu — telefonu ele geçiren birinin 30 günlük
    yenileme jetonu, kullanıcı şifresini değiştirse de yaşamaya devam
    ediyordu. İptalden sonra bu cihaza yeni bir jeton çifti dönüyor
    ([Profil.jetonlar]), yoksa kullanıcı kendi oturumundan da düşerdi.

    Uç `PATCH` semantiğinde: gövdede olmayan alan değişmiyor. Ayrıntı ve
    gerekçe [ProfilIstek] KDoc'unda.
    """
    kilitli = kullaniciyi_kilitle(db, kullanici.id)

    if istek.yeniSifre is not None:
        if not istek.mevcutSifre:
            raise HTTPException(
                status.HTTP_400_BAD_REQUEST,
                "Şifre değiştirmek için mevcut şifreni de gir",
            )
        if not sifre_dogrula(kilitli.sifre_ozet, istek.mevcutSifre):
            raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Mevcut şifre hatalı")
        kilitli.sifre_ozet = sifre_ozetle(istek.yeniSifre)

    # Kısmi güncelleme: gövdede olmayan alan değişmiyor (T4-005).
    # `{"avatar": ""}` bilerek temizlemek anlamına gelmeye devam ediyor;
    # ayrımın gerekçesi `ProfilIstek` KDoc'unda.
    if istek.ad is not None:
        kilitli.ad = istek.ad
    if istek.avatar is not None:
        kilitli.avatar = istek.avatar

    if istek.yeniSifre is None:
        db.commit()
        db.refresh(kilitli)
        return Profil(ad=kilitli.ad, eposta=kilitli.eposta, avatar=kilitli.avatar or "")

    # Sıra önemli: iptal `_jetonlar`'dan ÖNCE, yoksa yeni jeton da iptal
    # edilir. `_jetonlar` ad ve avatarı okuduğu için alan yazımlarından
    # sonra geliyor ve commit'i kendi yapıyor.
    for jeton in db.scalars(
        select(YenilemeJetonu).where(
            YenilemeJetonu.kullanici_id == kilitli.id,
            YenilemeJetonu.iptal == False,  # noqa: E712
        )
    ):
        jeton.iptal = True

    yeni_jetonlar = _jetonlar(db, kilitli)
    db.refresh(kilitli)
    return Profil(
        ad=kilitli.ad,
        eposta=kilitli.eposta,
        avatar=kilitli.avatar or "",
        jetonlar=yeni_jetonlar,
    )


@yonlendirici.post("/cikis")
def cikis(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> dict[str, str]:
    """
    Hesabın yenileme oturumlarını sunucuda iptal eder.

    Erişim JWT'si durumsuzdur ve en fazla 30 dakika daha geçerlidir; uzun
    ömürlü (30 gün) yenileme jetonları ise bu işlemden sonra yeni erişim
    jetonu üretemez. Tüm oturumları kapatmak, kopyalanmış eski bir cihaz
    jetonunun kullanıcı "çıkış" dedikten sonra yaşamasını önler.
    """
    kullaniciyi_kilitle(db, kullanici.id)
    for jeton in db.scalars(
        select(YenilemeJetonu).where(
            YenilemeJetonu.kullanici_id == kullanici.id,
            YenilemeJetonu.iptal == False,  # noqa: E712
        )
    ):
        jeton.iptal = True
    db.commit()
    return {"durum": "cikis"}


@yonlendirici.post("/sifre-sifirla", status_code=status.HTTP_202_ACCEPTED)
def sifre_sifirla(istek: SifreSifirlaIstek, db: Session = Depends(oturum)) -> dict[str, str]:
    """
    Sıfırlama jetonu üretir.

    E-posta sağlayıcısı bu sürümde bağlı değil. Üretimde başarı taklidi
    yapmak kullanıcının gelmeyecek bir bağlantıyı beklemesine yol açar;
    bu yüzden hizmet açıkça 503 döner ve sıfırlama jetonu dahi üretilmez.
    Geliştirme/test ortamında uçtan uca sözleşme test edilebilsin diye jeton
    üretilir, ancak yeniden kullanılabilir sır **asla günlüğe yazılmaz**.

    Kayıtlı olmayan e-postada da **202** dönüyor: hangi adreslerin kayıtlı
    olduğunu sızdırmamak için.
    """
    a = ayarlar()
    if a.uretim_mi:
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "Şifre sıfırlama e-postası henüz etkin değil",
        )

    kullanici = db.scalar(
        select(Kullanici)
        .where(Kullanici.eposta == istek.eposta.lower().strip())
        .with_for_update()
    )
    if kullanici is None:
        return {"durum": "kabul"}

    jeton = rastgele_jeton()
    db.add(
        SifreSifirlama(
            kullanici_id=kullanici.id,
            jeton=jeton_ozetle(jeton),
            son_gecerlilik=dt.datetime.now(dt.timezone.utc)
            + dt.timedelta(hours=a.sifirlama_saat),
        )
    )
    db.commit()
    gunluk.info("Şifre sıfırlama isteği kaydedildi: kullanici=%s", kullanici.id)
    return {"durum": "kabul"}


@yonlendirici.post("/sifre-yenile")
def sifre_yenile(istek: SifreYenileIstek, db: Session = Depends(oturum)) -> dict[str, str]:
    jeton_ozeti = jeton_ozetle(istek.jeton)
    ilk_okuma = db.scalar(
        select(SifreSifirlama).where(
            SifreSifirlama.jeton == jeton_ozeti,
            SifreSifirlama.kullanildi == False,  # noqa: E712
        )
    )
    if ilk_okuma is None:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Sıfırlama bağlantısı geçersiz")

    # Hesap silmeyle aynı kullanıcı→jeton kilit sırası.
    db.scalar(
        select(Kullanici.id)
        .where(Kullanici.id == ilk_okuma.kullanici_id)
        .with_for_update()
    )
    kayit_ = db.scalar(
        select(SifreSifirlama)
        .where(
            SifreSifirlama.jeton == jeton_ozeti,
            SifreSifirlama.kullanildi == False,  # noqa: E712
        )
        .with_for_update()
    )
    simdi = dt.datetime.now(dt.timezone.utc)

    if kayit_ is None or _gecmis(kayit_.son_gecerlilik, simdi):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Sıfırlama bağlantısı geçersiz")

    kullanici = db.get(Kullanici, kayit_.kullanici_id)
    if kullanici is None:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Kullanıcı bulunamadı")

    kullanici.sifre_ozet = sifre_ozetle(istek.yeniSifre)
    kayit_.kullanildi = True

    # Şifre değişince tüm oturumlar kapanıyor — hesabı ele geçiren varsa
    # erişimi de bitsin.
    for jeton in db.scalars(
        select(YenilemeJetonu).where(YenilemeJetonu.kullanici_id == kullanici.id)
    ):
        jeton.iptal = True

    db.commit()
    return {"durum": "tamam"}


@yonlendirici.get("/verilerim")
def verilerim(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> dict:
    """
    KVKK erişim hakkı — sunucudaki kendi verisi.

    **Varsayılan yol bu.** Dosya panele hiç düşmüyor: kullanıcı kendi
    cihazından, kendi oturumuyla indiriyor (`docs/panel-plani.md §5.2`).
    Personelin indirmesi ayrı ve gerekçeli bir istisna.

    Cihazdaki yerel veri buraya girmiyor — `DisaAktarDeposu` onu Room'dan
    üretiyor ve ham olaylar sunucuya hiç çıkmıyor (CLAUDE.md §4).
    """
    return kullanici_verisi(db, kullanici)


@yonlendirici.delete("/hesap")
def hesabi_sil(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> dict[str, str]:
    """
    Hesabı ve ona bağlı **her satırı** kalıcı olarak siler.

    Cihaz "Hesabımı kalıcı sil" dediğinde yereli zaten temizliyor; bu uç
    olmadan sunucudaki kopya yerinde kalırdı — KVKK'nın silme hakkı
    karşılanmamış olurdu ve ekrandaki "kalıcı" sözü yalan olurdu.

    Yumuşak silme (`silindi` bayrağı) **bilinçli olarak tercih edilmedi**:
    kullanıcı verisinin sunucuda kalmaya devam etmesi, silme talebinin
    karşılanmaması demek. Aynı e-posta ile yeniden kayıt olmak serbest.

    Silmenin kendisi `app/kullanici_silme.py`'de — panel de aynı silmeyi
    yapıyor (`docs/panel-plani.md §5.2`) ve iki kopya, yeni bir tablo
    eklendiğinde birinin unutulmasıyla öksüz kişisel veri bırakırdı.

    204 yerine gövdeli 200 dönüyor: API'nin geri kalanı da `{"durum": …}`
    veriyor ve boş gövde istemci tarafında ayrı bir dönüştürücü istiyor.
    """
    kid = kullanici.id

    # Giriş/senkron/eşleştirme gibi kullanıcıya yazan uçlarla aynı
    # kilit. Temizlik sürerken yeni bir alt satır eklenip FK hatası ya da
    # silme sonrasında kalıntı oluşmasın.
    kullanici = kullaniciyi_kilitle(db, kid)

    kullaniciyi_tamamen_sil(db, kullanici)
    db.commit()
    gunluk.info("Hesap silindi: kullanici=%s", kid)
    return {"durum": "silindi"}


def _gecmis(son: dt.datetime, simdi: dt.datetime) -> bool:
    """SQLite saat dilimi bilgisini düşürüyor; karşılaştırmadan önce eşitle."""
    if son.tzinfo is None:
        son = son.replace(tzinfo=dt.timezone.utc)
    return son < simdi
