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

from sqlalchemy import delete, or_
from sqlalchemy.orm import Session

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

    for model, kolon in _TEK_KOLONLU:
        db.execute(delete(model).where(kolon == kid))

    for model, sol, sag in _CIFT_KOLONLU:
        db.execute(delete(model).where(or_(sol == kid, sag == kid)))

    db.delete(kullanici)
