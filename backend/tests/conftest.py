"""Test altyapısı.

Testler **SQLite bellek veritabanında** koşuyor: her test dosyası saniyeler
içinde bitiyor ve PostgreSQL kabına bağımlı değil. Şema aynı SQLAlchemy
modellerinden üretildiği için PostgreSQL'e özgü olmayan her davranış
burada da geçerli.
"""

from __future__ import annotations

import os

import pytest

# Ayarlar import edilmeden ÖNCE ezilmeli; `lru_cache` ilk okumayı saklıyor.
os.environ["YESILIZ_ORTAM"] = "test"
os.environ["YESILIZ_VERITABANI_URL"] = "sqlite+pysqlite:///:memory:"
os.environ["YESILIZ_JWT_ANAHTARI"] = "test-anahtari"

from fastapi.testclient import TestClient  # noqa: E402
from sqlalchemy import create_engine  # noqa: E402
from sqlalchemy.orm import sessionmaker  # noqa: E402
from sqlalchemy.pool import StaticPool  # noqa: E402

from app.main import uygulama  # noqa: E402
from app.veritabani import Taban, oturum  # noqa: E402
import app.modeller  # noqa: F401,E402


@pytest.fixture
def db():
    # StaticPool: bellek veritabanı tek bağlantıda yaşamalı, yoksa her
    # bağlantı boş bir veritabanı açar.
    motor = create_engine(
        "sqlite+pysqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Taban.metadata.create_all(motor)
    Fabrika = sessionmaker(bind=motor, autoflush=False, expire_on_commit=False)
    with Fabrika() as o:
        yield o
    Taban.metadata.drop_all(motor)


@pytest.fixture
def istemci(db):
    uygulama.dependency_overrides[oturum] = lambda: db
    with TestClient(uygulama) as c:
        yield c
    uygulama.dependency_overrides.clear()


@pytest.fixture
def kullanici_kur(istemci):
    """Kayıt olup Authorization başlığı hazır bir istemci döndürür."""

    def kur(ad="Turan", eposta="turan@example.com", sifre="parola123"):
        yanit = istemci.post(
            "/auth/kayit", json={"ad": ad, "eposta": eposta, "sifre": sifre}
        )
        assert yanit.status_code == 201, yanit.text
        veri = yanit.json()
        return {"Authorization": f"Bearer {veri['erisim']}"}, veri

    return kur
