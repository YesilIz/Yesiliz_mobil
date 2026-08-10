"""Kimlik doğrulama uçları."""

from __future__ import annotations

import datetime as dt
import logging

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import delete, or_, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.ayarlar import ayarlar
from app.guvenlik import (
    erisim_jetonu,
    jeton_ozetle,
    mevcut_kullanici,
    rastgele_jeton,
    sifre_dogrula,
    sifre_ozetle,
)
from app.modeller import (
    Arkadaslik,
    DavetKodu,
    EbeveynBaglanti,
    EslestirmeKodu,
    GunlukOzet,
    Heartbeat,
    Kullanici,
    LigUyelik,
    OdulSecimi,
    RiskSonuc,
    SifreSifirlama,
    YenilemeJetonu,
)
from app.semalar import (
    GirisIstek,
    Jetonlar,
    KayitIstek,
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
    kullanici = db.scalar(select(Kullanici).where(Kullanici.eposta == istek.eposta.lower().strip()))

    # Kullanıcı yoksa da şifre yanlışsa da AYNI mesaj: hangi e-postaların
    # kayıtlı olduğunu sızdırmamak için.
    if kullanici is None or not sifre_dogrula(kullanici.sifre_ozet, istek.sifre):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "E-posta ya da şifre hatalı")

    return _jetonlar(db, kullanici)


@yonlendirici.post("/yenile", response_model=Jetonlar)
def yenile(istek: YenileIstek, db: Session = Depends(oturum)) -> Jetonlar:
    kayit_ = db.scalar(
        select(YenilemeJetonu)
        .where(
            YenilemeJetonu.jeton == jeton_ozetle(istek.yenileme),
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
    return _jetonlar(db, kullanici)


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

    kullanici = db.scalar(select(Kullanici).where(Kullanici.eposta == istek.eposta.lower().strip()))
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
    kayit_ = db.scalar(
        select(SifreSifirlama)
        .where(
            SifreSifirlama.jeton == jeton_ozetle(istek.jeton),
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

    ORM `cascade` tanımlı olmadığı için bağlı tablolar elle siliniyor;
    yabancı anahtar sırası önemli (önce çocuklar, sonra kullanıcı).

    204 yerine gövdeli 200 dönüyor: API'nin geri kalanı da `{"durum": …}`
    veriyor ve boş gövde istemci tarafında ayrı bir dönüştürücü istiyor.
    """
    kid = kullanici.id

    db.execute(delete(Heartbeat).where(Heartbeat.kullanici_id == kid))
    db.execute(delete(RiskSonuc).where(RiskSonuc.kullanici_id == kid))
    db.execute(delete(GunlukOzet).where(GunlukOzet.kullanici_id == kid))
    db.execute(delete(LigUyelik).where(LigUyelik.kullanici_id == kid))
    db.execute(delete(OdulSecimi).where(OdulSecimi.kullanici_id == kid))
    db.execute(delete(DavetKodu).where(DavetKodu.kullanici_id == kid))
    db.execute(delete(EslestirmeKodu).where(EslestirmeKodu.ureten_id == kid))
    db.execute(delete(SifreSifirlama).where(SifreSifirlama.kullanici_id == kid))
    db.execute(delete(YenilemeJetonu).where(YenilemeJetonu.kullanici_id == kid))

    # Karşılıklı bağlar: iki uçtan da silinmeli, yoksa öksüz satır kalır.
    db.execute(
        delete(Arkadaslik).where(
            or_(Arkadaslik.kullanici_id == kid, Arkadaslik.arkadas_id == kid)
        )
    )
    db.execute(
        delete(EbeveynBaglanti).where(
            or_(EbeveynBaglanti.ebeveyn_id == kid, EbeveynBaglanti.cocuk_id == kid)
        )
    )

    db.delete(kullanici)
    db.commit()
    gunluk.info("Hesap silindi: kullanici=%s", kid)
    return {"durum": "silindi"}


def _gecmis(son: dt.datetime, simdi: dt.datetime) -> bool:
    """SQLite saat dilimi bilgisini düşürüyor; karşılaştırmadan önce eşitle."""
    if son.tzinfo is None:
        son = son.replace(tzinfo=dt.timezone.utc)
    return son < simdi
