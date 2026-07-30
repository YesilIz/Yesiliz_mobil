"""Alembic ortamı — URL ve metadata uygulamadan geliyor."""

from logging.config import fileConfig

from alembic import context
from sqlalchemy import engine_from_config, pool

from app.ayarlar import ayarlar
from app.veritabani import Taban

# Modeller import edilmeli ki `Taban.metadata` dolsun.
import app.modeller  # noqa: F401

yapilandirma = context.config
yapilandirma.set_main_option("sqlalchemy.url", ayarlar().veritabani_url)

if yapilandirma.config_file_name is not None:
    fileConfig(yapilandirma.config_file_name)

hedef_metadata = Taban.metadata


def cevrimdisi() -> None:
    context.configure(
        url=yapilandirma.get_main_option("sqlalchemy.url"),
        target_metadata=hedef_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )
    with context.begin_transaction():
        context.run_migrations()


def cevrimici() -> None:
    motor = engine_from_config(
        yapilandirma.get_section(yapilandirma.config_ini_section, {}),
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )
    with motor.connect() as baglanti:
        context.configure(connection=baglanti, target_metadata=hedef_metadata)
        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    cevrimdisi()
else:
    cevrimici()
