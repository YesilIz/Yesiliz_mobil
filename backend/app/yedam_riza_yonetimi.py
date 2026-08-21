"""
YEDAM rızasının yaşam döngüsü — verilme, geçerlilik, geri çekme, silme.

`docs/yedam-riza-metni-v1.md §3`'ün silme tablosu **burada** yaşıyor ve
tek yerde: rıza biterken neyin silinip neyin kalacağı, hem kullanıcının
geri çekmesinde hem 90 günün dolmasında hem de panel tarafında aynı
olmalı. Üç ayrı yerde yazılsaydı biri unutulur ve veri gerekçesi bitmiş
hâlde sunucuda kalırdı.

**Silinen:** `risk_surukleyici`, `anket_yanit` — bunların sunucuda
tutulma gerekçesi rızaydı, gerekçe bitince veri de bitiyor. Cihazda zaten
duruyorlar.

**Kalan:** `yedam_basvuru` (durumu değişiyor), `yedam_temas`,
`danisman_notu`, `erisim_kaydi` — bir danışmanlık ilişkisinin kaydı ve
erişim izi. Silinmeleri, geçmişte ne olduğunu kullanıcının da görememesi
demek olurdu.
"""

from __future__ import annotations

import datetime as dt

from sqlalchemy import delete, select, update
from sqlalchemy.orm import Session

from app.modeller import (
    AnketYanit,
    BasvuruDurumu,
    KAPANMIS,
    RiskSurukleyici,
    YedamBasvuru,
    YedamRiza,
)

#: Rızanın geçerlilik süresi. S3 kararı (18 Ağu): 90 gün, **uzatma yolu
#: yok** — süre dolunca yeniden rıza isteniyor. Sessizce uzayan bir rıza,
#: "ne kadar süre" sorusunun cevabını anlamsız kılardı.
GECERLILIK_GUN = 90


def _simdi() -> dt.datetime:
    return dt.datetime.now(dt.timezone.utc)


def _utc(deger: dt.datetime) -> dt.datetime:
    """SQLite saat dilimi bilgisini düşürüyor; karşılaştırmadan önce eşitle."""
    return deger if deger.tzinfo else deger.replace(tzinfo=dt.timezone.utc)


def gecerli_riza(db: Session, kullanici_id: int, simdi: dt.datetime | None = None) -> YedamRiza | None:
    """
    Kullanıcının **şu an geçerli** rızası; yoksa `None`.

    Geri çekilmiş ya da süresi dolmuş rıza geçerli değil. Sorgu her
    çağrıda tazeleniyor: rızanın süresi bir zamanlanmış işle değil, **her
    okumada** kontrol ediliyor — iş koşmasa da erişim kapanmalı.
    """
    simdi = simdi or _simdi()
    for riza in db.scalars(
        select(YedamRiza)
        .where(YedamRiza.kullanici_id == kullanici_id, YedamRiza.geri_cekme.is_(None))
        .order_by(YedamRiza.verilme.desc(), YedamRiza.id.desc())
    ).all():
        if _utc(riza.son_gecerlilik) > simdi:
            return riza
    return None


def acik_basvuru(db: Session, kullanici_id: int) -> YedamBasvuru | None:
    """Kullanıcının kapanmamış başvurusu; yoksa `None`."""
    return db.scalar(
        select(YedamBasvuru)
        .where(
            YedamBasvuru.kullanici_id == kullanici_id,
            YedamBasvuru.durum.not_in(KAPANMIS),
        )
        .order_by(YedamBasvuru.olusturma.desc(), YedamBasvuru.id.desc())
    )


def riza_kapsamli_veriyi_sil(db: Session, kullanici_id: int) -> int:
    """
    Rıza kapsamındaki veriyi siler; commit etmez. Silinen satır sayısını döndürür.

    Erişimi kapatmakla yetinmiyoruz: rıza metni "**silinir**" diyor ve
    "artık göstermiyoruz" onun karşılığı değil.
    """
    silinen = 0
    for model in (RiskSurukleyici, AnketYanit):
        sonuc = db.execute(delete(model).where(model.kullanici_id == kullanici_id))
        silinen += sonuc.rowcount or 0
    return silinen


def rizayi_bitir(
    db: Session,
    riza: YedamRiza,
    yeni_durum: BasvuruDurumu,
    simdi: dt.datetime | None = None,
) -> int:
    """
    Rızayı kapatır, kapsamlı veriyi siler, açık başvuruyu işaretler.

    Geri çekme ve süre dolumu **aynı** işi yapıyor, yalnızca başvurunun
    aldığı durum farklı (`riza_geri_cekildi` / `suresi_doldu`). Kullanıcı
    ikisini ayırt edebilmeli: biri kendi kararı, öteki zamanın geçmesi.
    """
    simdi = simdi or _simdi()
    if riza.geri_cekme is None:
        riza.geri_cekme = simdi

    db.execute(
        update(YedamBasvuru)
        .where(
            YedamBasvuru.riza_id == riza.id,
            YedamBasvuru.durum.not_in(KAPANMIS),
        )
        .values(durum=yeni_durum.value, kapanis=simdi)
    )
    return riza_kapsamli_veriyi_sil(db, riza.kullanici_id)


def suresi_dolanlari_kapat(db: Session, simdi: dt.datetime | None = None) -> int:
    """
    Süresi dolmuş rızaları toplu kapatır; commit etmez. Kaç rıza kapandığını döndürür.

    Erişim zaten `gecerli_riza` üzerinden **her okumada** kapanıyor — bu
    iş onun yerine geçmiyor, **veriyi siliyor**. İkisi ayrı sorumluluk:
    biri "danışman göremesin", öteki "veri sunucuda durmasın". Yalnızca
    okuma kontrolüne güvenmek, gerekçesi bitmiş veriyi sunucuda süresiz
    bırakırdı.
    """
    simdi = simdi or _simdi()
    dolanlar = db.scalars(
        select(YedamRiza).where(
            YedamRiza.geri_cekme.is_(None),
            YedamRiza.son_gecerlilik <= simdi,
        )
    ).all()

    for riza in dolanlar:
        rizayi_bitir(db, riza, BasvuruDurumu.SURESI_DOLDU, simdi)
    return len(dolanlar)
