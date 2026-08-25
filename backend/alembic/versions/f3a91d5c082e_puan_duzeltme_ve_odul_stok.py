"""puan_duzeltme, odul_stok, odul_teslimat

`docs/panel-plani.md §5.3, §5.4, §9` · `CLAUDE.md §12 K56, K28`.

**`puan_duzeltme` delta saklıyor, yeni toplam değil.** Toplam cihazda
hesaplanıyor; sunucunun bildiği değer bilgilendirici (U7, A13). Yeni
toplamı yazmak sunucuyu olmadığı bir otoriteye dönüştürürdü.

`islem_anahtari` tekil: aynı düzeltmenin iki kez yazılması geri dönülemez
bir kullanıcı zararı. Panelin CSRF'i oturuma bağlı HMAC (U2) ve tekrar
gönderimi engellemiyor — bu, U2'nin bilinçli istisnası.

`odul_stok` K28'in kapısını taşıyor: parti yüklenince kademe açılıyor,
stok bitince kapanıyor. A4'ün teknik ayağı; finansman ayağı Yeşilay'da.

Revision ID: f3a91d5c082e
Revises: e8b2f45c1907
Create Date: 2026-08-21 13:00:00.000000
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "f3a91d5c082e"
down_revision: Union[str, None] = "e8b2f45c1907"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "puan_duzeltme",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("kullanici_id", sa.Integer(), nullable=False),
        sa.Column("delta", sa.Integer(), nullable=False),
        sa.Column("gerekce", sa.String(length=500), nullable=False),
        sa.Column("personel_id", sa.Integer(), nullable=False),
        sa.Column("islem_anahtari", sa.String(length=64), nullable=False),
        sa.Column("zaman", sa.DateTime(timezone=True), nullable=False),
        sa.Column(
            "cihaz_uyguladi", sa.Boolean(), nullable=False, server_default=sa.text("false")
        ),
        sa.ForeignKeyConstraint(["kullanici_id"], ["kullanici.id"]),
        sa.ForeignKeyConstraint(["personel_id"], ["personel.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("islem_anahtari", name="uq_duzeltme_islem_anahtari"),
    )
    op.create_index("ix_puan_duzeltme_kullanici_id", "puan_duzeltme", ["kullanici_id"])
    op.create_index("ix_puan_duzeltme_zaman", "puan_duzeltme", ["zaman"])

    op.create_table(
        "odul_stok",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("kademe_id", sa.String(length=40), nullable=False),
        sa.Column("secenek_id", sa.String(length=40), nullable=False),
        sa.Column("toplam", sa.Integer(), nullable=False, server_default=sa.text("0")),
        sa.Column("kalan", sa.Integer(), nullable=False, server_default=sa.text("0")),
        sa.Column("son_kullanma", sa.Date(), nullable=True),
        sa.Column("guncelleme", sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("kademe_id", "secenek_id", name="uq_stok_kademe_secenek"),
    )

    op.create_table(
        "odul_teslimat",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("odul_secimi_id", sa.Integer(), nullable=False),
        sa.Column("durum", sa.String(length=20), nullable=False),
        sa.Column("kod", sa.String(length=120), nullable=False, server_default=""),
        sa.Column("gonderim_zamani", sa.DateTime(timezone=True), nullable=True),
        sa.Column("personel_id", sa.Integer(), nullable=True),
        sa.Column("guncelleme", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["odul_secimi_id"], ["odul_secimi.id"]),
        sa.ForeignKeyConstraint(["personel_id"], ["personel.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("odul_secimi_id", name="uq_teslimat_secim"),
    )
    op.create_index("ix_odul_teslimat_secim", "odul_teslimat", ["odul_secimi_id"])
    op.create_index("ix_odul_teslimat_durum", "odul_teslimat", ["durum"])


def downgrade() -> None:
    op.drop_table("odul_teslimat")
    op.drop_table("odul_stok")
    op.drop_table("puan_duzeltme")
