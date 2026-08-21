"""
Rıza süresi bakımı — arka plan döngüsü.

Bu iş **erişimin yerine geçmiyor**: erişim her okumada zaten kapanıyor
(`gecerli_riza`). Buranın işi **silmek** — rıza metni "silinir" diyor,
"artık göstermiyoruz" demiyor. İki sorumluluğun ayrı olduğunu test de
söylüyor.
"""

from __future__ import annotations

import asyncio
import datetime as dt

import pytest

from app import yedam_bakim
from app.modeller import (
    AnketYanit,
    Kullanici,
    RiskSurukleyici,
    YedamBasvuru,
    YedamRiza,
)
from app.yedam_riza_yonetimi import GECERLILIK_GUN, gecerli_riza

SIMDI = dt.datetime.now(dt.timezone.utc)


def _dolmus_riza(db) -> Kullanici:
    k = Kullanici(eposta="dolan@ornek.com", ad="D", sifre_ozet="x")
    db.add(k)
    db.flush()
    verilme = SIMDI - dt.timedelta(days=GECERLILIK_GUN + 2)
    riza = YedamRiza(
        kullanici_id=k.id,
        metin_surumu="v1",
        kapsam="kimlik,risk,surukleyici,anket",
        verilme=verilme,
        son_gecerlilik=verilme + dt.timedelta(days=GECERLILIK_GUN),
    )
    db.add(riza)
    db.flush()
    db.add_all(
        [
            YedamBasvuru(
                kullanici_id=k.id,
                riza_id=riza.id,
                iletisim_turu="telefon",
                iletisim_degeri="0555",
            ),
            RiskSurukleyici(
                kullanici_id=k.id, tarih=dt.date(2026, 5, 1), faktor="social", katki=0.4
            ),
            AnketYanit(
                kullanici_id=k.id,
                tarih=dt.date(2026, 5, 1),
                madde_puanlari="3,3,3,3,3,3,3,3,3,3",
                ham_puan=30.0,
                r_s=50.0,
            ),
        ]
    )
    db.commit()
    return k


def test_erisim_is_kosmadan_da_kapali(db):
    """Bakım gecikse bile danışman veri göremiyor — güvenli taraf."""
    k = _dolmus_riza(db)
    assert gecerli_riza(db, k.id) is None
    # Ama veri hâlâ duruyor: silme işin sorumluluğu.
    assert db.query(RiskSurukleyici).count() == 1


def test_bir_tur_veriyi_siliyor(db, monkeypatch):
    k = _dolmus_riza(db)

    # `bir_tur` kendi oturumunu açıyor; testte fixture oturumunu veriyoruz.
    class SahteFabrika:
        def __call__(self):
            return self

        def __enter__(self):
            return db

        def __exit__(self, *_):
            return False

    monkeypatch.setattr(yedam_bakim, "OturumFabrikasi", SahteFabrika())

    assert yedam_bakim.bir_tur() == 1
    db.expire_all()
    assert db.query(RiskSurukleyici).count() == 0
    assert db.query(AnketYanit).count() == 0
    assert db.query(YedamBasvuru).one().durum == "suresi_doldu"


def test_ilk_tur_hemen_kosmuyor():
    """
    Açılışta yapılacak iş trafiği karşılamak, bakım değil.

    Sıfır gecikme, hazır olmayan bir veritabanına ilk saniyede gidip
    gereksiz hata logluyordu (testlerde de görüldü).
    """
    assert yedam_bakim.ILK_GECIKME_SN > 0


def test_dongu_hatada_olmuyor(monkeypatch):
    """Geçici bir kesinti bakımı kalıcı olarak durdurmamalı."""
    cagri = {"n": 0}

    def patlayan():
        cagri["n"] += 1
        raise RuntimeError("veritabanı yok")

    monkeypatch.setattr(yedam_bakim, "bir_tur", patlayan)
    monkeypatch.setattr(yedam_bakim, "ILK_GECIKME_SN", 0)

    async def kos():
        gorev = asyncio.create_task(yedam_bakim.dongu(aralik_sn=0))
        # Birkaç turluk fırsat ver, sonra iptal et.
        for _ in range(20):
            await asyncio.sleep(0)
            if cagri["n"] >= 2:
                break
        gorev.cancel()
        try:
            await gorev
        except asyncio.CancelledError:
            pass

    asyncio.run(kos())
    assert cagri["n"] >= 2, "Döngü ilk hatadan sonra durmamalı"
