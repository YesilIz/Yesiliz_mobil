"""kullanici.durum ve askıya alma alanları

Panelden hesap askıya alma (`docs/panel-plani.md §5.2`). **Veri silinmiyor**
— hesap ve bütün satırları yerinde duruyor, yalnızca API erişimi kapanıyor;
silme ayrı ve sert bir işlem (`app/kullanici_silme.py`).

`askiya_alma_sebebi` kullanıcıya cihazda gösteriliyor: sebebini
öğrenemeyen kullanıcı uygulamayı bozuk sanar ve destek talebi açar
(CLAUDE.md §4). Bu yüzden panelde zorunlu alan.

`askiya_alan_id` personele bakıyor. `denetim_kaydi` zaten kim ne yaptı
sorusunu cevaplıyor ama orası salt eklenebilir bir akış; hesabın **güncel**
durumunun kim tarafından konduğu satırın kendisinde durmalı, yoksa her
hesap ekranı denetim tablosunu taramak zorunda kalır.

Revision ID: c7d3b81e5a94
Revises: b4c8e1d27f39
Create Date: 2026-08-21 10:00:00.000000
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "c7d3b81e5a94"
down_revision: Union[str, None] = "b4c8e1d27f39"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Var olan satırlar `aktif`: server_default ile dolduruluyor, sonra
    # NOT NULL'a çekiliyor. İki adım, çünkü tabloda zaten satır var.
    op.add_column(
        "kullanici",
        sa.Column("durum", sa.String(length=20), nullable=False, server_default="aktif"),
    )
    op.add_column(
        "kullanici",
        sa.Column(
            "askiya_alma_sebebi", sa.String(length=500), nullable=False, server_default=""
        ),
    )
    op.add_column("kullanici", sa.Column("askiya_alan_id", sa.Integer(), nullable=True))
    op.add_column(
        "kullanici",
        sa.Column("askiya_alma_zamani", sa.DateTime(timezone=True), nullable=True),
    )
    op.create_foreign_key(
        "fk_kullanici_askiya_alan", "kullanici", "personel", ["askiya_alan_id"], ["id"]
    )
    # Askıya alınmış hesapları listelemek panelin sık işi; aktif hesaplar
    # ezici çoğunluk olduğu için kısmi indeks yeterli.
    op.create_index(
        "ix_kullanici_askida",
        "kullanici",
        ["durum"],
        postgresql_where=sa.text("durum <> 'aktif'"),
    )


def downgrade() -> None:
    op.drop_index("ix_kullanici_askida", table_name="kullanici")
    op.drop_constraint("fk_kullanici_askiya_alan", "kullanici", type_="foreignkey")
    op.drop_column("kullanici", "askiya_alma_zamani")
    op.drop_column("kullanici", "askiya_alan_id")
    op.drop_column("kullanici", "askiya_alma_sebebi")
    op.drop_column("kullanici", "durum")
