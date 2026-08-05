"""
Canlı ortam smoke testleri.

Dağıtım sonrası **gerçek adrese** karşı koşuyor; `tests/` altındaki 76
birim testinden ayrı tutulmasının sebebi bu (bkz. `pytest.ini`,
`testpaths = tests`). Normal `pytest -q` bunları toplamıyor.

    YESILIZ_SMOKE_TABAN=https://<alan-adi> python -m pytest smoke -q

Üretim verisine dokunmuyor:

- Kullanıcı her koşumda **benzersiz** (`smoke-duman-<uuid>@example.com`),
  var olan bir hesaba hiç bakılmıyor.
- Yazılan tek veri o kullanıcıya ait, tarih bugünün özeti.
- Koşum sonunda `DELETE /auth/hesap` ile kullanıcı ve ona bağlı **her
  satır** siliniyor; temizlik `finally` içinde, test düşse bile koşuyor.

Adım listesi `gorev1.md §9` ile birebir.
"""

from __future__ import annotations

import datetime as dt
import os
import uuid

import httpx
import pytest

TABAN = os.environ.get("YESILIZ_SMOKE_TABAN", "").rstrip("/")

# Şifresiz adrese izin kapısı: yerel yığına karşı denemek meşru
# (`YESILIZ_SMOKE_HTTP_TAMAM=1`), ama canlı ortamda kazayla http:// adres
# verilmesi sessizce geçmemeli — görev 1'in tamamlanma ölçütü HTTPS.
HTTP_TAMAM = os.environ.get("YESILIZ_SMOKE_HTTP_TAMAM") == "1"

ZAMAN_ASIMI = 20.0

pytestmark = pytest.mark.skipif(
    not TABAN,
    reason="YESILIZ_SMOKE_TABAN tanımlı değil — smoke testleri canlı adres istiyor",
)


@pytest.fixture(scope="module")
def istemci():
    with httpx.Client(base_url=TABAN, timeout=ZAMAN_ASIMI) as c:
        yield c


def test_adres_https(istemci):
    if HTTP_TAMAM:
        pytest.skip("YESILIZ_SMOKE_HTTP_TAMAM=1 — şifresiz adrese izin verildi")
    assert TABAN.startswith("https://"), (
        f"Canlı adres HTTPS olmalı, gelen: {TABAN}"
    )


def test_health(istemci):
    yanit = istemci.get("/health")
    assert yanit.status_code == 200, yanit.text
    govde = yanit.json()
    assert govde["status"] == "healthy"
    assert govde["database"] == "connected", "Backend ayakta ama veritabanına ulaşamıyor"
    assert govde["version"]


def test_uctan_uca_akis(istemci):
    """Kayıt → giriş → korumalı uç → özet → lig → temizlik."""
    # `example.com` — RFC 2606 ile örnek kullanıma ayrılmış, kimseye
    # posta gitmiyor. `.test`/`.invalid` kullanılamıyor: `email-validator`
    # özel amaçlı üst alan adlarını reddediyor (422).
    eposta = f"smoke-duman-{uuid.uuid4().hex[:12]}@example.com"
    sifre = uuid.uuid4().hex  # 32 karakter; asgari 8'in çok üstünde
    erisim: str | None = None

    try:
        # 2 · geçici test kullanıcısı
        kayit = istemci.post(
            "/auth/kayit", json={"ad": "Duman Testi", "eposta": eposta, "sifre": sifre}
        )
        assert kayit.status_code == 201, kayit.text
        assert kayit.json()["erisim"]

        # 3 · aynı kullanıcıyla giriş
        giris = istemci.post("/auth/giris", json={"eposta": eposta, "sifre": sifre})
        assert giris.status_code == 200, giris.text
        erisim = giris.json()["erisim"]
        assert giris.json()["yenileme"], "Yenileme jetonu dönmüyor — 401 döngüsü olur"

        baslik = {"Authorization": f"Bearer {erisim}"}

        # 4 · dönen jetonla korumalı uç. Önce jetonsuz denenip 401 alınıyor:
        # uç gerçekten korunuyor mu, yoksa herkese mi açık?
        bugun = dt.date.today()
        aralik = {"bas": bugun.isoformat(), "bit": bugun.isoformat()}
        assert istemci.get("/ozet", params=aralik).status_code == 401

        korumali = istemci.get("/ozet", params=aralik, headers=baslik)
        assert korumali.status_code == 200, korumali.text

        # Yazma yolu — cihazın günlük senkronu bu uca gidiyor.
        senkron = istemci.post(
            "/ozet",
            headers=baslik,
            json={
                "ozetler": [{"tarih": bugun.isoformat(), "socialDk": 1.0, "S": 1.0}],
                "riskler": [],
                "haftalikPuan": 1,
            },
        )
        assert senkron.status_code == 200, senkron.text
        assert senkron.json()["ozetSayisi"] == 1

        geri = istemci.get("/ozet", params=aralik, headers=baslik)
        assert geri.status_code == 200
        assert len(geri.json()) == 1, "Yazılan özet geri okunamadı"

        # 5 · lig verisi
        lig = istemci.get("/lig", headers=baslik)
        assert lig.status_code == 200, lig.text
        assert "siralama" in lig.json()

    finally:
        # 6 · temizlik — test düşse bile koşuyor.
        if erisim:
            silme = istemci.request(
                "DELETE", "/auth/hesap", headers={"Authorization": f"Bearer {erisim}"}
            )
            assert silme.status_code == 200, f"Temizlik başarısız: {silme.text}"
            # Silinen hesapla giriş yapılamamalı; yapılabiliyorsa üretimde
            # test kullanıcısı kalmış demektir.
            tekrar = istemci.post("/auth/giris", json={"eposta": eposta, "sifre": sifre})
            assert tekrar.status_code == 401
