"""bilgi_yazisi ve duyuru tabloları

`docs/panel-plani.md §5.5, §5.6` · S5 kararı.

**Bilgi bankası sunucudan servis + cihazda önbellek**; APK'ya gömülü set
çevrimdışı yedek olarak kalıyor. Bir yazı düzeltmesi için yayın beklemek
gerekmiyor, çevrimdışı garantisi de bozulmuyor.

**Duyuruda FCM yok:** cihaz senkronla çekiyor ve bildirim defterine
yazıyor. Push altyapısı ayrı bir iş kalemi.

Yayından kaldırılan yazı **silinmiyor**, `yayinda = false` oluyor: cihazın
önbelleğinde kalmış olabilir ve tekil okumanın çalışmaya devam etmesi,
açılmış bir bağlantıyı kırmaktan iyi.

Revision ID: a2c48f19e6b0
Revises: f3a91d5c082e
Create Date: 2026-08-21 15:00:00.000000
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "a2c48f19e6b0"
down_revision: Union[str, None] = "f3a91d5c082e"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "bilgi_yazisi",
        sa.Column("id", sa.String(length=60), nullable=False),
        sa.Column("baslik", sa.String(length=200), nullable=False),
        sa.Column("ozet", sa.String(length=500), nullable=False),
        sa.Column("kategori", sa.String(length=60), nullable=False),
        sa.Column("okuma_dk", sa.Integer(), nullable=False, server_default=sa.text("1")),
        sa.Column("govde", sa.Text(), nullable=False),
        sa.Column("surum", sa.Integer(), nullable=False, server_default=sa.text("1")),
        sa.Column("yayinda", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("sira", sa.Integer(), nullable=False, server_default=sa.text("0")),
        sa.Column("guncelleme", sa.DateTime(timezone=True), nullable=False),
        sa.Column("guncelleyen_id", sa.Integer(), nullable=True),
        sa.ForeignKeyConstraint(["guncelleyen_id"], ["personel.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_bilgi_yazisi_yayinda", "bilgi_yazisi", ["yayinda", "sira"])

    op.create_table(
        "duyuru",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("baslik", sa.String(length=120), nullable=False),
        sa.Column("metin", sa.String(length=1000), nullable=False),
        sa.Column("hedef", sa.String(length=40), nullable=False, server_default="hepsi"),
        sa.Column("yayin_zamani", sa.DateTime(timezone=True), nullable=True),
        sa.Column("personel_id", sa.Integer(), nullable=False),
        sa.Column("olusturma", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["personel_id"], ["personel.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_duyuru_yayin", "duyuru", ["yayin_zamani"])


def downgrade() -> None:
    op.drop_table("duyuru")
    op.drop_table("bilgi_yazisi")
