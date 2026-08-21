"""
Kullanıcıya bağlı her satırın silinmesi — **tek kaynak**.

Bu mantık bir zamanlar `uclar/hesap.py`'nin `DELETE /auth/hesap` ucunun
gövdesinde duruyordu. Panel de aynı silmeyi yapmak zorunda
(`docs/panel-plani.md §5.2`, KVKK silme talebi) ve listeyi kopyalamak,
F3'ün yedi YEDAM tablosu ya da F4'ün `puan_duzeltme`'si eklendiğinde
birinin güncellenip diğerinin unutulmasıyla **öksüz kişisel veri**
bırakırdı. Unutulan satır, silme talebinin karşılanmaması demek.

Unutmaya karşı `tests/test_kullanici_silme.py` şemayı tarıyor: kullanıcıya
işaret eden yabancı anahtarı olan her tablo aşağıdaki listede geçmek
zorunda, geçmiyorsa test düşer.

**Commit etmiyor.** Commit'i çağıran yapıyor; panel silmesi denetim
kaydını aynı transaction'a yazabilsin diye (`panel/denetim.py` ile aynı
gerekçe: gerçekleşmiş ama kaydı olmayan eylem mümkün olmasın).
"""

from __future__ import annotations

from sqlalchemy import delete, or_, select, update
from sqlalchemy.orm import Session

from app.modeller import (
    AnketYanit,
    Arkadaslik,
    DanismanNotu,
    DavetKodu,
    EbeveynBaglanti,
    ErisimKaydi,
    EslestirmeKodu,
    OdulTeslimat,
    GunlukOzet,
    Heartbeat,
    Kullanici,
    LigUyelik,
    OdulSecimi,
    PuanDuzeltme,
    RiskSonuc,
    RiskSurukleyici,
    SifreSifirlama,
    YedamBasvuru,
    YedamRiza,
    YedamTemas,
    YenilemeJetonu,
)

#: Tek bir `kullanici_id` kolonuyla bağlanan tablolar. Sıra önemli:
#: ORM `cascade` tanımlı değil, yabancı anahtar önce çocukları istiyor.
_TEK_KOLONLU = (
    (Heartbeat, Heartbeat.kullanici_id),
    (RiskSonuc, RiskSonuc.kullanici_id),
    (GunlukOzet, GunlukOzet.kullanici_id),
    (LigUyelik, LigUyelik.kullanici_id),
    (OdulSecimi, OdulSecimi.kullanici_id),
    (DavetKodu, DavetKodu.kullanici_id),
    (EslestirmeKodu, EslestirmeKodu.ureten_id),
    (SifreSifirlama, SifreSifirlama.kullanici_id),
    (YenilemeJetonu, YenilemeJetonu.kullanici_id),
    (RiskSurukleyici, RiskSurukleyici.kullanici_id),
    (AnketYanit, AnketYanit.kullanici_id),
    (ErisimKaydi, ErisimKaydi.kullanici_id),
    (PuanDuzeltme, PuanDuzeltme.kullanici_id),
)

#: Başvuruya bağlı tablolar. Kullanıcıya doğrudan bakmıyorlar ama
#: başvurusu silinince öksüz kalırlar; `yedam_basvuru`'dan ÖNCE
#: silinmeleri gerekiyor (yabancı anahtar).
#:
#: Rıza metni §3 bunların rıza geri çekilince **kalmasını** söylüyor —
#: ama hesabın tamamen silinmesi o ayrımı tanımıyor: KVKK silme hakkı her
#: şeyi siler, danışman notu dahil.
_BASVURUYA_BAGLI = (
    (YedamTemas, YedamTemas.basvuru_id),
    (DanismanNotu, DanismanNotu.basvuru_id),
)

#: Ödül seçimine bağlı tablolar. Kullanıcıya doğrudan bakmıyorlar;
#: `odul_secimi` silinince öksüz kalırlar, ondan ÖNCE silinmeleri gerek.
_SECIME_BAGLI = (
    (OdulTeslimat, OdulTeslimat.odul_secimi_id),
)

#: İki uçlu bağlar: kullanıcı her iki kolonda da geçebilir, ikisinden de
#: silinmeli — yoksa karşı taraf öksüz bir satıra bakmaya devam eder.
_CIFT_KOLONLU = (
    (Arkadaslik, Arkadaslik.kullanici_id, Arkadaslik.arkadas_id),
    (EbeveynBaglanti, EbeveynBaglanti.ebeveyn_id, EbeveynBaglanti.cocuk_id),
)

#: Nöbetçi testin şemayı tararken kullandığı ad listesi.
KAPSANAN_TABLOLAR = frozenset(
    [model.__tablename__ for model, _ in _TEK_KOLONLU]
    + [model.__tablename__ for model, _, _ in _CIFT_KOLONLU]
    + [model.__tablename__ for model, _ in _BASVURUYA_BAGLI]
    + [model.__tablename__ for model, _ in _SECIME_BAGLI]
    + [YedamBasvuru.__tablename__, YedamRiza.__tablename__]
)


def kullaniciyi_tamamen_sil(db: Session, kullanici: Kullanici) -> None:
    """
    Kullanıcıyı ve ona bağlı **her satırı** siler; commit etmez.

    Çağıran, kullanıcıyı `guvenlik.kullaniciyi_kilitle` ile kilitlemiş
    olmalı: temizlik sürerken yeni bir alt satır eklenirse yabancı anahtar
    hatası ya da silme sonrası kalıntı oluşuyor. Kilidi burada almıyoruz
    çünkü "kullanıcı yok" durumunun doğru yanıtı çağırana göre değişiyor
    (cihazda 401, panelde 404).
    """
    kid = kullanici.id

    _yedami_sil(db, kid)
    _odulu_sil(db, kid)

    for model, kolon in _TEK_KOLONLU:
        db.execute(delete(model).where(kolon == kid))

    for model, sol, sag in _CIFT_KOLONLU:
        db.execute(delete(model).where(or_(sol == kid, sag == kid)))

    db.delete(kullanici)


def _odulu_sil(db: Session, kid: int) -> None:
    """
    Teslimat kayıtları — `odul_secimi`'nden önce.

    **Stok geri verilmiyor.** Hesabını silen kullanıcının kuponu zaten
    ayrılmıştı; onu havuza geri koymak, teslim edilmiş bir ödülü ikinci
    kez dağıtmaya yol açabilirdi. Sayım farkı panelden görülüyor.
    """
    secimler = select(OdulSecimi.id).where(OdulSecimi.kullanici_id == kid)
    for model, kolon in _SECIME_BAGLI:
        db.execute(delete(model).where(kolon.in_(secimler)))


def _yedami_sil(db: Session, kid: int) -> None:
    """
    YEDAM zinciri — yabancı anahtar sırası: temas/not → başvuru → rıza.

    Bir incelik: `yedam_riza.ebeveyn_id` de kullanıcıya bakıyor. Silinen
    kişi **başkasının ebeveyni** ise o çocuğun rıza satırı silinmemeli,
    yalnızca ebeveyn alanı boşaltılmalı — çocuğun rızası kendi kaydı ve
    ebeveynin hesabını kapatması onu yok etmez. Onay tarihi de düşüyor:
    onayı veren hesap artık yok, o onayın geçerli sayılması yanlış olurdu.
    """
    basvurular = select(YedamBasvuru.id).where(YedamBasvuru.kullanici_id == kid)

    for model, kolon in _BASVURUYA_BAGLI:
        db.execute(delete(model).where(kolon.in_(basvurular)))

    db.execute(delete(YedamBasvuru).where(YedamBasvuru.kullanici_id == kid))
    db.execute(delete(YedamRiza).where(YedamRiza.kullanici_id == kid))

    db.execute(
        update(YedamRiza)
        .where(YedamRiza.ebeveyn_id == kid)
        .values(ebeveyn_id=None, ebeveyn_onay_tarihi=None)
    )
