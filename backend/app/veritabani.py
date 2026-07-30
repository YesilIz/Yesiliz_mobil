"""SQLAlchemy oturum yönetimi."""

from collections.abc import Iterator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.ayarlar import ayarlar


class Taban(DeclarativeBase):
    """Tüm tabloların ortak tabanı."""


def _motor():
    url = ayarlar().veritabani_url
    # SQLite tek bağlantıyı tek iş parçacığına kilitliyor; testlerde ve
    # Docker'sız çalıştırmada bu kısıtı gevşetmek gerekiyor.
    ek = {"connect_args": {"check_same_thread": False}} if url.startswith("sqlite") else {}
    return create_engine(url, pool_pre_ping=True, **ek)


motor = _motor()
OturumFabrikasi = sessionmaker(bind=motor, autoflush=False, expire_on_commit=False)


def oturum() -> Iterator[Session]:
    """FastAPI bağımlılığı — istek başına bir oturum."""
    with OturumFabrikasi() as o:
        yield o
