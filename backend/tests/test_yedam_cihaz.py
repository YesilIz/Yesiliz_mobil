"""
YEDAM cihaz uçları (`docs/panel-plani.md §11`).

Buradaki testlerin çoğu **rıza sınırını** koruyor: rızasız veri
gitmemeli, rıza bitince silinmeli, iki rıza birbirine karışmamalı.
Rıza metni (`docs/yedam-riza-metni-v1.md`) verilen sözü tek tek yazıyor;
testler o sözleri kontrol ediyor.
"""

from __future__ import annotations

import datetime as dt

import pytest

from app.modeller import (
    AnketYanit,
    BasvuruDurumu,
    EbeveynBaglanti,
    ErisimKaydi,
    Kullanici,
    OzellikBayragi,
    Personel,
    PersonelDurumu,
    PersonelRolu,
    RiskSurukleyici,
    YedamBasvuru,
    YedamRiza,
    YedamTemas,
)

BASVURU = {
    "metinSurumu": "v1",
    "kapsam": ["kimlik", "risk", "surukleyici", "anket", "not"],
    "iletisimTuru": "telefon",
    "iletisimDegeri": "05551112233",
    "kullaniciNotu": "Gece kullanımım arttı.",
    "beyanYas": 25,
}


def _kid(db) -> int:
    return db.query(Kullanici).filter_by(eposta="turan@example.com").one().id


def _basvur(istemci, baslik, **ek):
    return istemci.post("/yedam/basvuru", json={**BASVURU, **ek}, headers=baslik)


# --- başvuru açma ---


def test_resit_kullanici_basvuru_acabiliyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    yanit = _basvur(istemci, baslik)
    assert yanit.status_code == 201

    govde = yanit.json()
    assert govde["durum"] == "yeni"
    assert govde["ebeveynOnayiBekleniyor"] is False

    riza = db.query(YedamRiza).one()
    assert riza.metin_surumu == "v1"
    assert riza.kapsam == "anket,kimlik,not,risk,surukleyici"
    # 90 gün, uzatma yolu yok (S3).
    assert (riza.son_gecerlilik - riza.verilme).days == 90


def test_ikinci_acik_basvuru_reddediliyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    assert _basvur(istemci, baslik).status_code == 201
    assert _basvur(istemci, baslik).status_code == 409


def test_bilinmeyen_kapsam_reddediliyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    yanit = _basvur(istemci, baslik, kapsam=["kimlik", "her_sey"])
    assert yanit.status_code == 422
    assert db.query(YedamRiza).count() == 0


def test_kopru_kapaliyken_basvuru_acilmiyor(db, istemci, kullanici_kur):
    db.add(OzellikBayragi(anahtar="yedam.acik", deger="hayir"))
    db.commit()
    baslik, _ = kullanici_kur()

    yanit = _basvur(istemci, baslik)
    assert yanit.status_code == 403
    assert "115" in yanit.json()["detail"], "Alternatif yol söylenmeli"
    assert db.query(YedamBasvuru).count() == 0


def test_metin_surumu_cihazdan_geliyor(db, istemci, kullanici_kur):
    """Kullanıcı hangi metni okuduysa onun sürümü kaydedilmeli."""
    baslik, _ = kullanici_kur()
    _basvur(istemci, baslik, metinSurumu="v2")
    assert db.query(YedamRiza).one().metin_surumu == "v2"


# --- 18 altı ---


def test_18_alti_eslesme_yoksa_dogrudan_aciliyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    yanit = _basvur(istemci, baslik, beyanYas=15)
    assert yanit.status_code == 201
    assert yanit.json()["durum"] == "yeni"


def test_18_alti_eslesme_varsa_onay_bekliyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    cocuk_id = _kid(db)
    ebeveyn = Kullanici(eposta="anne@ornek.com", ad="Anne", sifre_ozet="x")
    db.add(ebeveyn)
    db.flush()
    db.add(EbeveynBaglanti(ebeveyn_id=ebeveyn.id, cocuk_id=cocuk_id))
    db.commit()

    yanit = _basvur(istemci, baslik, beyanYas=15)
    assert yanit.status_code == 201
    assert yanit.json()["ebeveynOnayiBekleniyor"] is True
    assert db.query(YedamBasvuru).one().durum == "ebeveyn_onayi_bekliyor"


def test_yas_gonderilmezse_cocuk_muamelesi(db, istemci, kullanici_kur):
    """§9: ölçülmeyen değer "iyi" tarafa yorumlanmaz."""
    baslik, _ = kullanici_kur()
    cocuk_id = _kid(db)
    ebeveyn = Kullanici(eposta="anne@ornek.com", ad="Anne", sifre_ozet="x")
    db.add(ebeveyn)
    db.flush()
    db.add(EbeveynBaglanti(ebeveyn_id=ebeveyn.id, cocuk_id=cocuk_id))
    db.commit()

    yanit = _basvur(istemci, baslik, beyanYas=None)
    assert yanit.json()["ebeveynOnayiBekleniyor"] is True


# --- sürükleyici ve geçmiş ---


def _surukleyici(tarih="2026-08-20", faktor="social", sira=1):
    return {"tarih": tarih, "faktor": faktor, "sira": sira}


def test_rizasiz_surukleyici_reddediliyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    yanit = istemci.post(
        "/yedam/surukleyici", json={"kayitlar": [_surukleyici()]}, headers=baslik
    )
    assert yanit.status_code == 403
    assert db.query(RiskSurukleyici).count() == 0


def test_rizali_surukleyici_yaziliyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    _basvur(istemci, baslik)

    yanit = istemci.post(
        "/yedam/surukleyici", json={"kayitlar": [_surukleyici()]}, headers=baslik
    )
    assert yanit.status_code == 200
    assert db.query(RiskSurukleyici).one().faktor == "social"


def test_kapsam_disinda_surukleyici_reddediliyor(db, istemci, kullanici_kur):
    """Rıza "sürükleyici" maddesini içermiyorsa gönderilemez."""
    baslik, _ = kullanici_kur()
    _basvur(istemci, baslik, kapsam=["kimlik", "risk"])

    yanit = istemci.post(
        "/yedam/surukleyici", json={"kayitlar": [_surukleyici()]}, headers=baslik
    )
    assert yanit.status_code == 403


def test_ayni_surukleyici_iki_kez_gonderilince_cogalmiyor(db, istemci, kullanici_kur):
    """Ağda düşen istek yeniden denenebilmeli."""
    baslik, _ = kullanici_kur()
    _basvur(istemci, baslik)

    for _ in range(2):
        istemci.post(
            "/yedam/surukleyici", json={"kayitlar": [_surukleyici(sira=2)]}, headers=baslik
        )
    assert db.query(RiskSurukleyici).count() == 1
    assert db.query(RiskSurukleyici).one().sira == 2


def test_gecmis_yuklemesi_anketi_de_yaziyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    _basvur(istemci, baslik)

    yanit = istemci.post(
        "/yedam/gecmis",
        json={
            "surukleyiciler": [_surukleyici(), _surukleyici(faktor="gece", sira=2)],
            "anketler": [
                {
                    "tarih": "2026-08-01",
                    "maddePuanlari": [3, 4, 2, 5, 1, 6, 3, 2, 4, 1],
                    "hamPuan": 31.0,
                    "rS": 52.5,
                }
            ],
        },
        headers=baslik,
    )
    assert yanit.status_code == 200
    assert yanit.json() == {"surukleyici": 2, "anket": 1}
    assert db.query(AnketYanit).one().madde_puanlari == "3,4,2,5,1,6,3,2,4,1"


def test_gecersiz_likert_reddediliyor(db, istemci, kullanici_kur):
    """SAS-SV 1-6 aralığında; ölçek birebir korunuyor (K12)."""
    baslik, _ = kullanici_kur()
    _basvur(istemci, baslik)

    yanit = istemci.post(
        "/yedam/gecmis",
        json={
            "anketler": [
                {
                    "tarih": "2026-08-01",
                    "maddePuanlari": [9, 4, 2, 5, 1, 6, 3, 2, 4, 1],
                    "hamPuan": 37.0,
                    "rS": 60.0,
                }
            ]
        },
        headers=baslik,
    )
    assert yanit.status_code == 422


# --- geri çekme ---


def test_geri_cekme_veriyi_siliyor(db, istemci, kullanici_kur):
    """Rıza metni: "risk ve anket bilgilerin sunucudan silinir"."""
    baslik, _ = kullanici_kur()
    _basvur(istemci, baslik)
    istemci.post(
        "/yedam/gecmis",
        json={
            "surukleyiciler": [_surukleyici()],
            "anketler": [
                {
                    "tarih": "2026-08-01",
                    "maddePuanlari": [3] * 10,
                    "hamPuan": 30.0,
                    "rS": 50.0,
                }
            ],
        },
        headers=baslik,
    )

    yanit = istemci.delete("/yedam/riza", headers=baslik)
    assert yanit.status_code == 200
    assert yanit.json()["durum"] == "geri_cekildi"

    db.expire_all()
    assert db.query(RiskSurukleyici).count() == 0
    assert db.query(AnketYanit).count() == 0
    assert db.query(YedamBasvuru).one().durum == "riza_geri_cekildi"


def test_geri_cekme_basvuru_ve_erisim_kaydini_silmiyor(db, istemci, kullanici_kur):
    """Rıza metni §3: bunlar bir danışmanlık ilişkisinin kaydı, kalıyor."""
    baslik, _ = kullanici_kur()
    _basvur(istemci, baslik)
    kid = _kid(db)
    p = Personel(
        eposta="d@yesilay.org.tr",
        ad="Danışman",
        sifre_ozet="x",
        rol=PersonelRolu.YEDAM_DANISMAN.value,
        durum=PersonelDurumu.AKTIF.value,
    )
    db.add(p)
    db.flush()
    basvuru_id = db.query(YedamBasvuru).one().id
    db.add_all(
        [
            ErisimKaydi(kullanici_id=kid, personel_id=p.id, basvuru_id=basvuru_id),
            YedamTemas(basvuru_id=basvuru_id, personel_id=p.id, tur="arandi"),
        ]
    )
    db.commit()

    istemci.delete("/yedam/riza", headers=baslik)

    db.expire_all()
    assert db.query(ErisimKaydi).count() == 1
    assert db.query(YedamTemas).count() == 1
    assert db.query(YedamBasvuru).count() == 1


def test_geri_cekme_idempotent(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    assert istemci.delete("/yedam/riza", headers=baslik).json()["durum"] == "riza_yok"


def test_geri_cekince_yeni_basvuru_acilabiliyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    _basvur(istemci, baslik)
    istemci.delete("/yedam/riza", headers=baslik)
    assert _basvur(istemci, baslik).status_code == 201


# --- durum takibi ve şeffaflık ---


def test_basvuru_yoksa_bos_donuyor(db, istemci, kullanici_kur):
    """Uydurma durum yok (§4, K37)."""
    baslik, _ = kullanici_kur()
    govde = istemci.get("/yedam/basvurum", headers=baslik).json()
    assert govde["basvuruId"] is None and govde["durum"] == ""


def test_durum_temaslari_tasiyor_ama_danisman_adini_tasimiyor(
    db, istemci, kullanici_kur
):
    baslik, _ = kullanici_kur()
    _basvur(istemci, baslik)
    p = Personel(
        eposta="ayse.danisman@yesilay.org.tr",
        ad="Ayşe Danışman",
        sifre_ozet="x",
        rol=PersonelRolu.YEDAM_DANISMAN.value,
        durum=PersonelDurumu.AKTIF.value,
    )
    db.add(p)
    db.flush()
    db.add(
        YedamTemas(
            basvuru_id=db.query(YedamBasvuru).one().id, personel_id=p.id, tur="arandi"
        )
    )
    db.commit()

    yanit = istemci.get("/yedam/basvurum", headers=baslik)
    assert [t["tur"] for t in yanit.json()["temaslar"]] == ["arandi"]
    assert "Ayşe Danışman" not in yanit.text


def test_erisim_kaydi_riza_geri_cekilse_de_gorunuyor(db, istemci, kullanici_kur):
    """Kaydın varlık sebebi şeffaflık; rızayla birlikte kaybolamaz."""
    baslik, _ = kullanici_kur()
    _basvur(istemci, baslik)
    kid = _kid(db)
    p = Personel(
        eposta="d@yesilay.org.tr",
        ad="D",
        sifre_ozet="x",
        rol=PersonelRolu.YEDAM_DANISMAN.value,
        durum=PersonelDurumu.AKTIF.value,
    )
    db.add(p)
    db.flush()
    db.add(ErisimKaydi(kullanici_id=kid, personel_id=p.id, kapsam="risk,surukleyici"))
    db.commit()

    istemci.delete("/yedam/riza", headers=baslik)

    kayitlar = istemci.get("/yedam/erisim-kaydim", headers=baslik).json()
    assert len(kayitlar) == 1
    assert kayitlar[0]["kapsam"] == "risk,surukleyici"


def test_erisim_kaydi_danisman_adini_tasimiyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    kid = _kid(db)
    p = Personel(
        eposta="gizli@yesilay.org.tr",
        ad="Gizli Danışman",
        sifre_ozet="x",
        rol=PersonelRolu.YEDAM_DANISMAN.value,
        durum=PersonelDurumu.AKTIF.value,
    )
    db.add(p)
    db.flush()
    db.add(ErisimKaydi(kullanici_id=kid, personel_id=p.id, kapsam="risk"))
    db.commit()

    yanit = istemci.get("/yedam/erisim-kaydim", headers=baslik)
    assert "Gizli Danışman" not in yanit.text


def test_baskasinin_erisim_kaydi_gorunmuyor(db, istemci, kullanici_kur):
    baslik, _ = kullanici_kur()
    baska = Kullanici(eposta="baska@ornek.com", ad="B", sifre_ozet="x")
    p = Personel(
        eposta="d@yesilay.org.tr",
        ad="D",
        sifre_ozet="x",
        rol=PersonelRolu.YEDAM_DANISMAN.value,
        durum=PersonelDurumu.AKTIF.value,
    )
    db.add_all([baska, p])
    db.flush()
    db.add(ErisimKaydi(kullanici_id=baska.id, personel_id=p.id, kapsam="risk"))
    db.commit()

    assert istemci.get("/yedam/erisim-kaydim", headers=baslik).json() == []
