"""
YEDAM — cihaz uçları (`docs/panel-plani.md §11`, API servisi).

**İki rıza bağımsız.** "Yeşilay ile paylaşım" (K19) günlük özetin sunucuya
gitmesini yönetiyor; buradaki rıza danışmanın görmesini. Paylaşımı kapalı
bir kullanıcı YEDAM desteği isteyebiliyor — bu yüzden sürükleyici gönderimi
`/ozet`'e eklenmedi, **ayrı uç** oldu (U1). `/ozet` o kullanıcıda hiç
çağrılmıyor ve özellik tam da korumak istediği kişide sessizce ölü kalırdı.
"""

from __future__ import annotations

import datetime as dt
import logging

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app import yedam_kapisi
from app.guvenlik import kullaniciyi_kilitle, mevcut_kullanici
from app.modeller import (
    AnketYanit,
    BasvuruDurumu,
    ErisimKaydi,
    Kullanici,
    RiskSurukleyici,
    YedamBasvuru,
    YedamRiza,
    YedamTemas,
)
from app.semalar import (
    ErisimKaydiDto,
    SurukleyiciIstek,
    YedamBasvuruIstek,
    YedamBasvuruYanit,
    YedamDurumYanit,
    YedamGecmisIstek,
    YedamTemasDto,
)
from app.veritabani import oturum
from app.yedam_riza_yonetimi import (
    GECERLILIK_GUN,
    acik_basvuru,
    gecerli_riza,
    rizayi_bitir,
)

gunluk = logging.getLogger(__name__)
yonlendirici = APIRouter(prefix="/yedam", tags=["yedam"])

#: Rıza metninin `kapsam` tablosundaki geçerli anahtarlar
#: (`docs/yedam-riza-metni-v1.md §2`).
GECERLI_KAPSAM = frozenset({"kimlik", "risk", "surukleyici", "anket", "davranis", "not"})

#: Erişim kaydı ve şeffaflık ekranında en fazla kaç satır gösteriliyor.
KAYIT_SINIRI = 100


def _simdi() -> dt.datetime:
    return dt.datetime.now(dt.timezone.utc)


def _riza_gerek(db: Session, kullanici_id: int) -> YedamRiza:
    riza = gecerli_riza(db, kullanici_id)
    if riza is None:
        raise HTTPException(
            status.HTTP_403_FORBIDDEN, "Geçerli bir YEDAM rızası yok"
        )
    return riza


@yonlendirici.post(
    "/basvuru", response_model=YedamBasvuruYanit, status_code=status.HTTP_201_CREATED
)
def basvuru_ac(
    istek: YedamBasvuruIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> YedamBasvuruYanit:
    """
    Rızayı kaydeder ve başvuruyu açar — **tek işlem**.

    İkisi ayrılamaz: rızasız başvuru danışmana veri açardı, başvurusuz
    rıza da kullanıcının hiçbir şey istemediği hâlde veri paylaşmasına yol
    açardı. Aynı transaction'da yazılıyorlar.

    18 altında ve bağlı ebeveyn varsa başvuru **açılıyor ama bekliyor**:
    hiçbir danışman görmüyor. "Hiç başvuramazsın" demek yerine bekleyen
    bir kayıt üretmek hem doğru hem ebeveynin onaylayabileceği bir şey
    bırakıyor.
    """
    bilinmeyen = set(istek.kapsam) - GECERLI_KAPSAM
    if bilinmeyen:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_CONTENT,
            f"Bilinmeyen kapsam anahtarı: {sorted(bilinmeyen)}",
        )

    kullaniciyi_kilitle(db, kullanici.id)

    if acik_basvuru(db, kullanici.id) is not None:
        raise HTTPException(
            status.HTTP_409_CONFLICT, "Zaten açık bir YEDAM başvurun var"
        )

    karar = yedam_kapisi.degerlendir(db, kullanici.id, istek.beyan_yas)
    if not karar.acilabilir:
        # Sebebi söyleniyor: "başvuru açılamadı" deyip susmak, kullanıcının
        # ne yapması gerektiğini bilmemesi demek (§4).
        mesaj = {
            yedam_kapisi.KapiSonucu.KAPALI: (
                "YEDAM köprüsü şu anda kapalı. Doğrudan 115'i arayabilirsin."
            ),
            yedam_kapisi.KapiSonucu.EBEVEYN_YOK: (
                "18 yaşından küçüksen başvuru için bağlı bir ebeveyn hesabı gerekiyor."
            ),
        }[karar.sonuc]
        raise HTTPException(status.HTTP_403_FORBIDDEN, mesaj)

    simdi = _simdi()
    riza = YedamRiza(
        kullanici_id=kullanici.id,
        metin_surumu=istek.metin_surumu,
        kapsam=",".join(sorted(set(istek.kapsam))),
        verilme=simdi,
        son_gecerlilik=simdi + dt.timedelta(days=GECERLILIK_GUN),
        ebeveyn_id=karar.ebeveyn_id,
        beyan_yas=istek.beyan_yas,
    )
    db.add(riza)
    db.flush()

    onay_bekliyor = karar.sonuc is yedam_kapisi.KapiSonucu.EBEVEYN_ONAYI_GEREKLI
    basvuru = YedamBasvuru(
        kullanici_id=kullanici.id,
        riza_id=riza.id,
        durum=(
            BasvuruDurumu.EBEVEYN_ONAYI_BEKLIYOR.value
            if onay_bekliyor
            else BasvuruDurumu.YENI.value
        ),
        iletisim_turu=istek.iletisim_turu,
        iletisim_degeri=istek.iletisim_degeri,
        kullanici_notu=istek.kullanici_notu,
        olusturma=simdi,
    )
    db.add(basvuru)
    db.commit()

    gunluk.info(
        "YEDAM başvurusu açıldı",
        extra={"basvuru": basvuru.id, "onay_bekliyor": onay_bekliyor},
    )
    return YedamBasvuruYanit(
        basvuru_id=basvuru.id,
        durum=basvuru.durum,
        riza_son_gecerlilik=riza.son_gecerlilik,
        ebeveyn_onayi_bekleniyor=onay_bekliyor,
    )


@yonlendirici.get("/basvurum", response_model=YedamDurumYanit)
def basvurum(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> YedamDurumYanit:
    """
    Durum takibi. **Uydurma durum yok** (§4, K37): başvuru yoksa boş dönüyor.

    Danışmanın adı taşınmıyor — kullanıcıya "sana X atandı" demek,
    danışmanın kimliğini başvuru görüşmeye dönüşmeden önce açardı.
    """
    basvuru = db.scalar(
        select(YedamBasvuru)
        .where(YedamBasvuru.kullanici_id == kullanici.id)
        .order_by(YedamBasvuru.olusturma.desc(), YedamBasvuru.id.desc())
    )
    if basvuru is None:
        return YedamDurumYanit()

    riza = db.get(YedamRiza, basvuru.riza_id)
    temaslar = db.scalars(
        select(YedamTemas)
        .where(YedamTemas.basvuru_id == basvuru.id)
        .order_by(YedamTemas.zaman.desc())
        .limit(KAYIT_SINIRI)
    ).all()

    return YedamDurumYanit(
        basvuru_id=basvuru.id,
        durum=basvuru.durum,
        olusturma=basvuru.olusturma,
        riza_son_gecerlilik=riza.son_gecerlilik if riza is not None else None,
        ebeveyn_onayi_bekleniyor=(
            basvuru.durum == BasvuruDurumu.EBEVEYN_ONAYI_BEKLIYOR.value
        ),
        temaslar=[YedamTemasDto(tur=t.tur, zaman=t.zaman) for t in temaslar],
    )


@yonlendirici.delete("/riza")
def rizayi_geri_cek(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> dict[str, str | int]:
    """
    Rızayı geri çeker: erişim **aynı saniyede** kesilir, veri silinir.

    İdempotent: geçerli rıza yoksa da başarı dönüyor. Kullanıcının
    "geri çektim" demesi için sunucunun durumunu bilmesi gerekmemeli.
    """
    kullaniciyi_kilitle(db, kullanici.id)
    riza = gecerli_riza(db, kullanici.id)
    if riza is None:
        return {"durum": "riza_yok", "silinen": 0}

    silinen = rizayi_bitir(db, riza, BasvuruDurumu.RIZA_GERI_CEKILDI)
    db.commit()
    gunluk.info("YEDAM rızası geri çekildi", extra={"silinen_satir": silinen})
    return {"durum": "geri_cekildi", "silinen": silinen}


@yonlendirici.get("/erisim-kaydim", response_model=list[ErisimKaydiDto])
def erisim_kaydim(
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> list[ErisimKaydiDto]:
    """
    "14 Ağustos 10:32'de bakıldı."

    Rıza metninin verdiği sözün karşılığı. **Rıza kapısı yok**: rızasını
    geri çekmiş kullanıcı da geçmişte kimin baktığını görebilmeli — kaydın
    varlık sebebi bu.

    Danışmanın adı taşınmıyor: kullanıcıya kimin baktığını değil,
    bakıldığını bildiriyoruz. Ad, danışmanı korumasız bırakırdı ve
    şeffaflık yükümlülüğü için gerekli değil.
    """
    kayitlar = db.scalars(
        select(ErisimKaydi)
        .where(ErisimKaydi.kullanici_id == kullanici.id)
        .order_by(ErisimKaydi.zaman.desc())
        .limit(KAYIT_SINIRI)
    ).all()
    return [ErisimKaydiDto(zaman=k.zaman, kapsam=k.kapsam) for k in kayitlar]


@yonlendirici.post("/surukleyici")
def surukleyici_gonder(
    istek: SurukleyiciIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> dict[str, int]:
    """
    Günlük sürükleyici gönderimi — **`/ozet`'ten ayrı** (U1).

    Sunucu bunu **üretmiyor**, saklıyor (K17): `core-risk`'in
    açıklanabilirlik çıktısı cihazda hesaplanıp buraya geliyor.

    Rıza yoksa `403` — sessizce yok saymak, cihazın gönderdiğini sanıp
    kaydedilmediğini bilmemesi olurdu.
    """
    riza = _riza_gerek(db, kullanici.id)
    if "surukleyici" not in riza.kapsam.split(","):
        raise HTTPException(
            status.HTTP_403_FORBIDDEN, "Rıza sürükleyicileri kapsamıyor"
        )

    yazilan = _surukleyici_yaz(db, kullanici.id, istek.kayitlar)
    db.commit()
    return {"yazilan": yazilan}


@yonlendirici.post("/gecmis")
def gecmis_yukle(
    istek: YedamGecmisIstek,
    db: Session = Depends(oturum),
    kullanici: Kullanici = Depends(mevcut_kullanici),
) -> dict[str, int]:
    """
    Rıza anındaki **tek seferlik** geçmiş yüklemesi.

    Cihazda 30 günlük `risk_sonuc` ve anket geçmişi zaten var. Yüklenmezse
    danışman ilk gün hiçbir şey görmüyor ve "son 30 günün bandı" sözü
    karşılanmıyor.

    Tekrar çağrılabilir ve tekrar zarar vermiyor: yazma idempotent
    (tekil kısıt + upsert deseni), yani ağda düşen bir istek yeniden
    denenebiliyor.
    """
    riza = _riza_gerek(db, kullanici.id)
    kapsam = set(riza.kapsam.split(","))

    surukleyici = 0
    if istek.surukleyiciler and "surukleyici" in kapsam:
        surukleyici = _surukleyici_yaz(db, kullanici.id, istek.surukleyiciler)

    anket = 0
    if istek.anketler and "anket" in kapsam:
        anket = _anket_yaz(db, kullanici.id, istek.anketler)

    db.commit()
    return {"surukleyici": surukleyici, "anket": anket}


def _surukleyici_yaz(db: Session, kullanici_id: int, kayitlar) -> int:
    """Var olan satırı günceller, yoksa ekler (tekil kısıt: kullanıcı+tarih+faktör)."""
    yazilan = 0
    for kayit in kayitlar:
        satir = db.scalar(
            select(RiskSurukleyici).where(
                RiskSurukleyici.kullanici_id == kullanici_id,
                RiskSurukleyici.tarih == kayit.tarih,
                RiskSurukleyici.faktor == kayit.faktor,
            )
        )
        if satir is None:
            db.add(
                RiskSurukleyici(
                    kullanici_id=kullanici_id,
                    tarih=kayit.tarih,
                    faktor=kayit.faktor,
                    katki=kayit.katki,
                )
            )
        else:
            satir.katki = kayit.katki
        yazilan += 1
    return yazilan


def _anket_yaz(db: Session, kullanici_id: int, kayitlar) -> int:
    yazilan = 0
    for kayit in kayitlar:
        satir = db.scalar(
            select(AnketYanit).where(
                AnketYanit.kullanici_id == kullanici_id, AnketYanit.tarih == kayit.tarih
            )
        )
        puanlar = ",".join(str(p) for p in kayit.madde_puanlari)
        if satir is None:
            db.add(
                AnketYanit(
                    kullanici_id=kullanici_id,
                    tarih=kayit.tarih,
                    madde_puanlari=puanlar,
                    ham_puan=kayit.ham_puan,
                    r_s=kayit.r_s,
                )
            )
        else:
            satir.madde_puanlari = puanlar
            satir.ham_puan = kayit.ham_puan
            satir.r_s = kayit.r_s
        yazilan += 1
    return yazilan
