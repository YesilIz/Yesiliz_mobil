"""
YEDAM başvurusunun açılabilirlik kapısı (`docs/panel-plani.md §14 S2`).

**Tek fonksiyon, tek karar.** 18 yaş altı kullanıcıda ebeveyn onayının
gerekip gerekmediği kuralı burada; Yeşilay farklı bir şey söylerse
değişen tek şey `bayraklar.YEDAM_EBEVEYN_ONAYI`'nin değeri oluyor, kod
değil.

S2 kararı (18 Ağu): varsayılan **eşleşmiş ebeveyn varsa onayı da gerekli**,
eşleşme yoksa çocuğun rızası yeterli. Gerekçe rıza metninde yazılı:
*"desteğe erişimin bir eşleştirme yapmadığın için engellenmemeli."*
Eşleştirmesi olmayan bir çocuğun YEDAM'a ulaşamaması, korumanın kendisini
zarara çevirirdi.

Yaş **beyan usulü** ve doğrulanamıyor (`CLAUDE.md §12 K26`). Bu bir kusur
değil bilinçli kabul: doğrulanabilir yaş tespiti kimlik doğrulaması ister
ve o, koruduğundan fazlasını toplar.
"""

from __future__ import annotations

import enum
from dataclasses import dataclass

from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app import bayraklar
from app.modeller import EbeveynBaglanti

#: Reşitlik sınırı. Beyan edilen yaş bunun altındaysa kapı devreye giriyor.
RESIT_YAS = 18


class KapiSonucu(str, enum.Enum):
    ACIK = "acik"
    #: Ebeveyn onayı bekleniyor: başvuru açılıyor ama danışman görmüyor.
    EBEVEYN_ONAYI_GEREKLI = "ebeveyn_onayi_gerekli"
    #: Kural "her_zaman" ve bağlı ebeveyn yok: onay alınacak kimse yok.
    EBEVEYN_YOK = "ebeveyn_yok"
    #: Köprü panelden kapatılmış.
    KAPALI = "kapali"


@dataclass(frozen=True)
class Karar:
    sonuc: KapiSonucu
    #: Onayı beklenen ebeveyn hesabı; yoksa `None`.
    ebeveyn_id: int | None = None

    @property
    def acilabilir(self) -> bool:
        """Başvuru **kaydı** oluşturulabilir mi?

        `EBEVEYN_ONAYI_GEREKLI` de açılabilir sayılıyor: başvuru bekleyen
        durumda oluşuyor ve hiçbir danışman görmüyor. Kullanıcıya "hiç
        başvuramazsın" demek yerine "onay bekleniyor" demek, hem doğru hem
        ebeveynin onaylayabileceği bir kayıt üretiyor.
        """
        return self.sonuc in (KapiSonucu.ACIK, KapiSonucu.EBEVEYN_ONAYI_GEREKLI)


def _bagli_ebeveyn(db: Session, kullanici_id: int) -> int | None:
    """Kullanıcının **ebeveyni** varsa onun id'si.

    Yalnızca `cocuk_id` tarafına bakılıyor: kullanıcı kendisi ebeveyn
    olabilir (kendi çocuğunu bağlamış bir yetişkin), o durumda onay
    beklenecek bir üst hesap yok.
    """
    return db.scalar(
        select(EbeveynBaglanti.ebeveyn_id).where(EbeveynBaglanti.cocuk_id == kullanici_id)
    )


def degerlendir(db: Session, kullanici_id: int, beyan_yas: int | None) -> Karar:
    """
    Başvuru açılabilir mi, ebeveyn onayı gerekli mi?

    `beyan_yas` `None` ise (eski sürümden gelen, yaşını hiç girmemiş
    kullanıcı) reşit **sayılmıyor**: bilinmeyen yaşı "18 üstü" kabul etmek,
    korumayı tam da korunması gerekenlerde kapatırdı. Ölçülmeyen değerin
    "iyi" tarafa yorumlanması (`CLAUDE.md §9`) burada da geçerli — cihaz
    yaşı gönderiyor, göndermiyorsa çocuk muamelesi görüyor.
    """
    if not bayraklar.acik_mi(db, bayraklar.YEDAM_ACIK):
        return Karar(KapiSonucu.KAPALI)

    resit = beyan_yas is not None and beyan_yas >= RESIT_YAS
    if resit:
        return Karar(KapiSonucu.ACIK)

    kural = bayraklar.oku(db, bayraklar.YEDAM_EBEVEYN_ONAYI)
    if kural == "hicbir_zaman":
        return Karar(KapiSonucu.ACIK)

    ebeveyn_id = _bagli_ebeveyn(db, kullanici_id)

    if kural == "her_zaman":
        if ebeveyn_id is None:
            return Karar(KapiSonucu.EBEVEYN_YOK)
        return Karar(KapiSonucu.EBEVEYN_ONAYI_GEREKLI, ebeveyn_id)

    # "eslesmisse" — varsayılan. Eşleşme yoksa çocuğun rızası yeterli:
    # eşleştirme yapmamış olmak desteğe erişimi engellememeli.
    if ebeveyn_id is None:
        return Karar(KapiSonucu.ACIK)
    return Karar(KapiSonucu.EBEVEYN_ONAYI_GEREKLI, ebeveyn_id)
