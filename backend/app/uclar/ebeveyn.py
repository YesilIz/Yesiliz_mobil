"""Ebeveyn eşleştirme ve özet."""

from __future__ import annotations

import datetime as dt
import secrets

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import delete, or_, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.ayarlar import ayarlar
from app.guvenlik import kullaniciyi_kilitle, mevcut_kullanici
from app.modeller import (
    BasvuruDurumu,
    EbeveynBaglanti,
    EslestirmeKodu,
    GunlukOzet,
    Heartbeat,
    Kullanici,
    RiskSonuc,
    YedamBasvuru,
    YedamRiza,
)
from app.semalar import (
    EbeveynOzetYanit,
    EbeveynYedamOnayIstek,
    EslestirIstek,
    EslestirmeKoduIstek,
    KodYanit,
)
from app.veritabani import oturum
from app.yedam_riza_yonetimi import rizayi_bitir
from app.zaman import yerel_bugun

yonlendirici = APIRouter(prefix="/ebeveyn", tags=["ebeveyn"])

ROLLER = {"ebeveyn", "cocuk"}


@yonlendirici.post("/kod", response_model=KodYanit)
def kod_uret(
    istek: EslestirmeKoduIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> KodYanit:
    """6 haneli kod, **10 dakika** geçerli (tasarım 21'deki metinle birebir)."""
    if istek.rol not in ROLLER:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Rol 'ebeveyn' ya da 'cocuk' olmalı")

    a = ayarlar()

    # Paralel kod isteklerinde yalnızca son üretilen kod geçerli kalsın.
    kullaniciyi_kilitle(db, kullanici.id)

    # Aynı kullanıcının bekleyen eski kodları iptal ediliyor: aynı anda iki
    # geçerli kod, hangisinin kullanıldığını belirsizleştirirdi.
    for eski in db.scalars(
        select(EslestirmeKodu).where(
            EslestirmeKodu.ureten_id == kullanici.id, EslestirmeKodu.kullanildi == False  # noqa: E712
        )
    ):
        eski.kullanildi = True

    kod = _kod_uret(db)
    db.add(
        EslestirmeKodu(
            kod=kod,
            ureten_id=kullanici.id,
            ureten_rol=istek.rol,
            son_gecerlilik=dt.datetime.now(dt.timezone.utc)
            + dt.timedelta(minutes=a.eslestirme_kodu_dakika),
        )
    )
    db.commit()
    return KodYanit(kod=kod)


@yonlendirici.post("/eslestir")
def eslestir(
    istek: EslestirIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> dict[str, str]:
    """
    Kodu **gerçekten doğrular**.

    Cihaz tarafında eski hâli yalnızca "6 hane mi" diye bakıyordu; artık
    kodun var olması, süresinin dolmamış olması, daha önce kullanılmamış
    olması ve **karşı rolde** üretilmiş olması aranıyor.
    """
    if istek.rol not in ROLLER:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Rol 'ebeveyn' ya da 'cocuk' olmalı")

    ham_kod = istek.kod.strip()
    ilk_okuma = db.scalar(select(EslestirmeKodu).where(EslestirmeKodu.kod == ham_kod))
    if ilk_okuma is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Kod geçersiz")
    if ilk_okuma.ureten_id == kullanici.id:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Kendi kodunu giremezsin")
    if ilk_okuma.ureten_rol == istek.rol:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            "İki cihaz da aynı rolde — biri ebeveyn, biri çocuk olmalı",
        )

    ebeveyn_id = kullanici.id if istek.rol == "ebeveyn" else ilk_okuma.ureten_id
    cocuk_id = ilk_okuma.ureten_id if istek.rol == "ebeveyn" else kullanici.id

    # Hesap silme ve yeni kod üretimiyle kilit sırası kullanıcılar→kod.
    # Kilitten sonra kod yeniden okunur; arada silindiyse eski ORM nesnesiyle
    # bağlantı kurulmaz.
    kilitlenenler = db.scalars(
        select(Kullanici.id)
        .where(Kullanici.id.in_(sorted((ebeveyn_id, cocuk_id))))
        .order_by(Kullanici.id)
        .with_for_update()
    ).all()
    if len(kilitlenenler) != 2:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Kod geçersiz")

    kayit = db.scalar(
        select(EslestirmeKodu)
        .where(EslestirmeKodu.kod == ham_kod)
        .with_for_update()
    )
    simdi = dt.datetime.now(dt.timezone.utc)

    if kayit is None or kayit.kullanildi:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Kod geçersiz")
    if _gecmis(kayit.son_gecerlilik, simdi):
        raise HTTPException(status.HTTP_410_GONE, "Kodun süresi dolmuş")
    if kayit.ureten_id == kullanici.id:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Kendi kodunu giremezsin")
    if kayit.ureten_rol == istek.rol:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            "İki cihaz da aynı rolde — biri ebeveyn, biri çocuk olmalı",
        )

    # Uygulama tek bir cihaz rolü/bağlantısı saklıyor ve özet ucu tek
    # çocuk döndürüyor. Aynı hesabı ikinci bir bağlantıya eklemek,
    # hangi çocuğun gösterildiğini belirsiz ve kaldırma sonucunu sürprizli
    # yapardı. Katılımcılardan herhangi biri zaten bağlıysa reddet.
    var = db.scalar(
        select(EbeveynBaglanti).where(
            or_(
                EbeveynBaglanti.ebeveyn_id.in_((ebeveyn_id, cocuk_id)),
                EbeveynBaglanti.cocuk_id.in_((ebeveyn_id, cocuk_id)),
            )
        )
    )
    if var is not None:
        raise HTTPException(status.HTTP_409_CONFLICT, "Cihazlardan biri zaten eşleştirilmiş")

    db.add(EbeveynBaglanti(ebeveyn_id=ebeveyn_id, cocuk_id=cocuk_id))

    kayit.kullanildi = True
    try:
        db.commit()
    except IntegrityError as hata:
        # Ayrı istekler aynı anda ön kontrolden geçerse son söz veritabanı
        # tekillik kısıtınındır.
        db.rollback()
        raise HTTPException(
            status.HTTP_409_CONFLICT,
            "Cihazlardan biri zaten eşleştirilmiş",
        ) from hata
    return {"durum": "eslesti"}


def bagi_kaldir(db: Session, kullanici_id: int) -> None:
    """
    Kullanıcının ebeveyn–çocuk bağını iki uçtan da siler; commit etmez.

    Panel de aynı işi yapıyor (`docs/panel-plani.md §5.2`, destek talebi
    senaryosu). Tek tanım, çünkü "hangi satırlar bu bağa ait" sorusunun
    iki cevabı olmamalı.
    """
    db.execute(
        delete(EbeveynBaglanti).where(
            or_(
                EbeveynBaglanti.ebeveyn_id == kullanici_id,
                EbeveynBaglanti.cocuk_id == kullanici_id,
            )
        )
    )


@yonlendirici.delete("/eslesme")
def eslesmeyi_kaldir(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> dict[str, str]:
    """
    Ebeveyn–çocuk bağını iki taraftan herhangi biri için kaldırır.

    İşlem idempotenttir: bağ zaten yoksa da başarı döner. Cihazdaki
    yerel bayrak ancak bu yanıt geldikten sonra temizlenir; ağ kesintisinde
    diğer cihazın erişimi sürerken "bağ kesildi" denmez.
    """
    kullaniciyi_kilitle(db, kullanici.id)
    bagi_kaldir(db, kullanici.id)
    db.commit()
    return {"durum": "kaldirildi"}


@yonlendirici.get("/ozet", response_model=EbeveynOzetYanit)
def ozet(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> EbeveynOzetYanit:
    """
    Çocuğun **özeti** — ham veri değil.

    `docs/spec-veri.md`: ebeveyne uygulama adı, oturum dökümü ya da saat
    düzeyinde ayrıntı gitmiyor. Yalnızca günlük toplam ekran süresi, bant
    ve trend. Bu uç bilerek dar tutuldu; genişletmek veri politikasını
    değiştirmek demek.
    """
    baglanti = db.scalar(
        select(EbeveynBaglanti).where(EbeveynBaglanti.ebeveyn_id == kullanici.id)
    )
    if baglanti is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Eşleşmiş çocuk cihazı yok")

    cocuk = db.get(Kullanici, baglanti.cocuk_id)
    if cocuk is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Çocuk hesabı bulunamadı")

    bugun = yerel_bugun()
    bas = bugun - dt.timedelta(days=6)
    ozetler = db.scalars(
        select(GunlukOzet)
        .where(
            GunlukOzet.kullanici_id == cocuk.id,
            GunlukOzet.tarih >= bas,
            GunlukOzet.tarih <= bugun,
        )
        .order_by(GunlukOzet.tarih)
    ).all()

    dakikalar = [round(o.toplam_ekran_dk) for o in ozetler]
    ortalama = round(sum(dakikalar) / len(dakikalar)) if dakikalar else 0

    son_risk = db.scalar(
        select(RiskSonuc)
        .where(
            RiskSonuc.kullanici_id == cocuk.id,
            RiskSonuc.tarih >= bas,
            RiskSonuc.tarih <= bugun,
            RiskSonuc.veri_durumu == "HESAPLANDI",
        )
        .order_by(RiskSonuc.tarih.desc())
        .limit(1)
    )

    return EbeveynOzetYanit(
        cocukAdi=cocuk.ad,
        gunlukTarihler=[o.tarih for o in ozetler],
        gunlukDakikalar=dakikalar,
        ortalamaDk=ortalama,
        bant=son_risk.gosterilen_bant if son_risk else None,
        trend=son_risk.trend if son_risk else None,
        sonSinyalSaatOnce=_sinyal_saat(db, cocuk.id),
    )


def _sinyal_saat(db: Session, kullanici_id: int) -> int | None:
    """
    Son sinyalden bu yana geçen saat.

    Ebeveyn paneli bunu "çocuk uygulamayı silmiş olabilir" uyarısına
    çeviriyor — doğrudan silme tespiti güvenilir olmadığı için
    (`docs/spec-veri.md`).
    """
    kayit = db.get(Heartbeat, kullanici_id)
    if kayit is None:
        return None
    son = kayit.son_sinyal
    if son.tzinfo is None:
        son = son.replace(tzinfo=dt.timezone.utc)
    return int((dt.datetime.now(dt.timezone.utc) - son).total_seconds() // 3600)


def _kod_uret(db: Session) -> str:
    for _ in range(20):
        kod = f"{secrets.randbelow(900000) + 100000}"
        if db.scalar(select(EslestirmeKodu).where(EslestirmeKodu.kod == kod)) is None:
            return kod
    raise HTTPException(status.HTTP_500_INTERNAL_SERVER_ERROR, "Kod üretilemedi")


def _gecmis(son: dt.datetime, simdi: dt.datetime) -> bool:
    if son.tzinfo is None:
        son = son.replace(tzinfo=dt.timezone.utc)
    return son < simdi


@yonlendirici.post("/yedam-onay")
def yedam_onayi(
    istek: EbeveynYedamOnayIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> dict[str, str]:
    """
    18 altı çocuğun YEDAM başvurusuna **eşleşmiş ebeveynin** onayı.

    Kapı `app/yedam_kapisi.py`'de; buradaki uç yalnızca onayı yazıyor.
    Onay gelene kadar başvuru `ebeveyn_onayi_bekliyor` durumunda ve
    **hiçbir danışman görmüyor** (rıza metni "18 yaşından küçüksen"
    bölümü).

    Yalnızca **rızada adı yazan** ebeveyn onaylayabiliyor: bağın o anda
    var olması yetmiyor, rıza verildiğinde bağlı olan hesap olmalı. Aksi
    hâlde araya giren yeni bir eşleşme, çocuğun daha önce açtığı bir
    başvuruyu onaylayabilirdi.

    Reddetme de bir cevap: `onay=false` başvuruyu kapatıyor ve rızayı
    bitiriyor. Belirsiz bekleyen bir kayıt bırakmak, çocuğun "başvurum ne
    oldu" sorusunu cevapsız bırakırdı.
    """
    riza = db.get(YedamRiza, istek.rizaId)
    if riza is None or riza.ebeveyn_id != kullanici.id:
        # 404: onaylayamayacağı bir rızanın var olup olmadığını da
        # söylemiyoruz.
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Onaylanacak başvuru bulunamadı")

    basvuru = db.scalar(
        select(YedamBasvuru).where(
            YedamBasvuru.riza_id == riza.id,
            YedamBasvuru.durum == BasvuruDurumu.EBEVEYN_ONAYI_BEKLIYOR.value,
        )
    )
    if basvuru is None:
        raise HTTPException(status.HTTP_409_CONFLICT, "Bekleyen bir başvuru yok")

    simdi = dt.datetime.now(dt.timezone.utc)
    if not istek.onay:
        rizayi_bitir(db, riza, BasvuruDurumu.RIZA_GERI_CEKILDI, simdi)
        db.commit()
        return {"durum": "reddedildi"}

    riza.ebeveyn_onay_tarihi = simdi
    basvuru.durum = BasvuruDurumu.YENI.value
    db.commit()
    return {"durum": "onaylandi"}
