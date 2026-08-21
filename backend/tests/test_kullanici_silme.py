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
#: Bir tablo buraya eklenirken gerekçesi yazılmalı, yoksa muafiyet
#: listesi sessizce silmenin kendisini boşaltır.
MUAF = frozenset(
    {
        # `denetim_kaydi.hedef_id` yabancı anahtar DEĞİL (migration
        # KDoc'u) — burada görünmesinin sebebi `personel_id` kolonu ve o
        # personele bakıyor, kullanıcıya değil. Denetim izi silinemez.
        "denetim_kaydi",
        "personel_oturum",
        "personel_kurtarma_kodu",
        "ozellik_bayragi",
    }
)


def _bakanlar(hedef: str) -> set[str]:
    """Şemada `hedef` tablosuna yabancı anahtarı olan her tablo."""
    bulunan = set()
    for tablo in Taban.metadata.tables.values():
        if tablo.name == hedef:
            continue
        for fk in tablo.foreign_keys:
            if fk.column.table.name == hedef:
                bulunan.add(tablo.name)
    return bulunan


def _kullaniciya_bakan_tablolar() -> set[str]:
    """
    Kullanıcıya **doğrudan ya da başvuru üzerinden** bağlı her tablo.

    Yalnızca doğrudan FK'ya bakmak yetmiyor: `yedam_temas` ve
    `danisman_notu` kullanıcıya hiç bakmıyor, `yedam_basvuru`'ya bakıyor.
    Kullanıcı silinince başvurusu gidiyor ve onlar öksüz kalıyor — tarama
    bir kademe derine inmezse bu sessizce kaçardı.
    """
    dogrudan = _bakanlar("kullanici")
    dolayli = set()
    for ara in dogrudan:
        dolayli |= _bakanlar(ara)
    # Personele bakan tablolar (denetim, oturum) bu zincirde değil.
    return (dogrudan | dolayli) - {"kullanici"}


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



def test_yedam_zinciri_bastan_sona_siliniyor(db):
    """
    Hesap silme rıza metnindeki asimetriyi **tanımıyor**.

    Rıza geri çekilince `danisman_notu` ve `erisim_kaydi` kalıyor; hesabın
    tamamen silinmesi ise KVKK silme hakkı ve her şeyi siliyor.
    """
    from app.modeller import (
        AnketYanit,
        DanismanNotu,
        ErisimKaydi,
        Personel,
        PersonelDurumu,
        PersonelRolu,
        RiskSurukleyici,
        YedamBasvuru,
        YedamRiza,
        YedamTemas,
    )

    simdi = dt.datetime.now(dt.timezone.utc)
    k = Kullanici(eposta="yedam@ornek.com", ad="Y", sifre_ozet="x")
    p = Personel(
        eposta="d@yesilay.org.tr",
        ad="Danışman",
        sifre_ozet="x",
        rol=PersonelRolu.YEDAM_DANISMAN.value,
        durum=PersonelDurumu.AKTIF.value,
    )
    db.add_all([k, p])
    db.flush()

    riza = YedamRiza(
        kullanici_id=k.id,
        metin_surumu="v1",
        kapsam="kimlik,risk",
        son_gecerlilik=simdi + dt.timedelta(days=90),
    )
    db.add(riza)
    db.flush()

    basvuru = YedamBasvuru(
        kullanici_id=k.id,
        riza_id=riza.id,
        iletisim_turu="telefon",
        iletisim_degeri="05550000000",
    )
    db.add(basvuru)
    db.flush()

    db.add_all(
        [
            YedamTemas(basvuru_id=basvuru.id, personel_id=p.id, tur="arandi"),
            DanismanNotu(basvuru_id=basvuru.id, personel_id=p.id, metin="not"),
            ErisimKaydi(kullanici_id=k.id, personel_id=p.id, basvuru_id=basvuru.id),
            RiskSurukleyici(
                kullanici_id=k.id, tarih=dt.date(2026, 8, 20), faktor="social", sira=1
            ),
            AnketYanit(
                kullanici_id=k.id,
                tarih=dt.date(2026, 8, 20),
                madde_puanlari="1,2,3,4,5,6,1,2,3,4",
                ham_puan=31.0,
                r_s=50.0,
            ),
        ]
    )
    db.commit()

    kullaniciyi_tamamen_sil(db, k)
    db.commit()

    for model in (
        YedamRiza,
        YedamBasvuru,
        YedamTemas,
        DanismanNotu,
        ErisimKaydi,
        RiskSurukleyici,
        AnketYanit,
    ):
        assert db.query(model).count() == 0, f"{model.__tablename__} kalıntı bıraktı"
    assert db.get(Personel, p.id) is not None, "Danışman hesabı silinmemeli"


def test_ebeveyn_silinince_cocugun_rizasi_kalir_onayi_duser(db):
    """
    Ebeveynin hesabını kapatması çocuğun rızasını yok etmemeli.

    Ama onay da geçerli sayılamaz: onayı veren hesap artık yok.
    """
    from app.modeller import YedamRiza

    simdi = dt.datetime.now(dt.timezone.utc)
    cocuk = Kullanici(eposta="cocuk@ornek.com", ad="C", sifre_ozet="x")
    ebeveyn = Kullanici(eposta="ebeveyn@ornek.com", ad="E", sifre_ozet="x")
    db.add_all([cocuk, ebeveyn])
    db.flush()

    riza = YedamRiza(
        kullanici_id=cocuk.id,
        metin_surumu="v1",
        kapsam="kimlik",
        son_gecerlilik=simdi + dt.timedelta(days=90),
        ebeveyn_id=ebeveyn.id,
        ebeveyn_onay_tarihi=simdi,
    )
    db.add(riza)
    db.commit()

    kullaniciyi_tamamen_sil(db, ebeveyn)
    db.commit()
    db.expire_all()

    kalan = db.query(YedamRiza).one()
    assert kalan.kullanici_id == cocuk.id
    assert kalan.ebeveyn_id is None
    assert kalan.ebeveyn_onay_tarihi is None
