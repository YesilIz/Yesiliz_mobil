"""
Hesap silme — kaskadın eksiksizliği.

Buradaki asıl test davranış değil **kapsam**: şemaya yeni bir tablo
eklenip `kullanici_silme.py`'ye eklenmezse, silme talebi sessizce eksik
karşılanır ve kullanıcının verisi sunucuda kalır. F3 yedi YEDAM tablosu,
F4 `puan_duzeltme` getiriyor; nöbetçi onları hatırlatmak için var.
"""

from __future__ import annotations

import datetime as dt

from app.kullanici_silme import KAPSANAN_TABLOLAR, kullaniciyi_tamamen_sil
from app.modeller import (
    Arkadaslik,
    EbeveynBaglanti,
    GunlukOzet,
    Heartbeat,
    Kullanici,
    RiskSonuc,
    YenilemeJetonu,
)
from app.veritabani import Taban

#: Kullanıcıya işaret ettiği hâlde silinmesi **istenmeyen** tablolar.
#: Şu an boş; bir tablo buraya eklenirken gerekçesi yazılmalı, yoksa
#: muafiyet listesi sessizce silmenin kendisini boşaltır.
MUAF = frozenset()


def _kullaniciya_bakan_tablolar() -> set[str]:
    """Şemada `kullanici.id`'ye yabancı anahtarı olan her tablo."""
    bulunan = set()
    for tablo in Taban.metadata.tables.values():
        if tablo.name == "kullanici":
            continue
        for fk in tablo.foreign_keys:
            if fk.column.table.name == "kullanici":
                bulunan.add(tablo.name)
    return bulunan


def test_kullaniciya_bakan_her_tablo_silmede_geciyor():
    eksik = _kullaniciya_bakan_tablolar() - KAPSANAN_TABLOLAR - MUAF
    assert not eksik, (
        f"Bu tablolar kullanıcıya bağlı ama silinmiyor: {sorted(eksik)}. "
        "app/kullanici_silme.py'ye ekle — yoksa KVKK silme talebi eksik "
        "karşılanıyor ve öksüz kişisel veri kalıyor."
    )


def test_kapsanan_liste_hayali_tablo_saymiyor():
    """Ters yön: listede şemada olmayan bir ad kalmasın."""
    hayali = KAPSANAN_TABLOLAR - set(Taban.metadata.tables)
    assert not hayali, f"Şemada olmayan tablo adları: {sorted(hayali)}"


def test_silme_bagli_satirlari_birakmiyor(db):
    simdi = dt.datetime.now(dt.timezone.utc)
    a = Kullanici(eposta="a@ornek.com", ad="A", sifre_ozet="x")
    b = Kullanici(eposta="b@ornek.com", ad="B", sifre_ozet="y")
    db.add_all([a, b])
    db.flush()

    db.add_all(
        [
            Heartbeat(kullanici_id=a.id, son_sinyal=simdi),
            RiskSonuc(
                kullanici_id=a.id,
                tarih=dt.date(2026, 8, 1),
                s=10.0,
                r=40.0,
                gosterilen_bant="ORTA",
                trend="SABIT",
            ),
            GunlukOzet(kullanici_id=a.id, tarih=dt.date(2026, 8, 1)),
            YenilemeJetonu(kullanici_id=a.id, jeton="ozet", son_gecerlilik=simdi),
            # İki uçlu bağlar: kullanıcı KARŞI kolonda duruyor.
            Arkadaslik(kullanici_id=b.id, arkadas_id=a.id),
            EbeveynBaglanti(ebeveyn_id=b.id, cocuk_id=a.id),
        ]
    )
    db.commit()

    kullaniciyi_tamamen_sil(db, a)
    db.commit()

    assert db.get(Kullanici, a.id) is None
    assert db.get(Kullanici, b.id) is not None, "Karşı taraf silinmemeli"
    for model in (Heartbeat, RiskSonuc, GunlukOzet, YenilemeJetonu, Arkadaslik, EbeveynBaglanti):
        kalan = db.query(model).all()
        assert not kalan, f"{model.__tablename__} tablosunda kalıntı var: {kalan}"
