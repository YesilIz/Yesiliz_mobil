"""kullanici.avatar — profil simgesi seçimi cihaz değişince kaybolmasın

Revision ID: a83f19c2d507
Revises: e39a71c4d206
Create Date: 2026-08-13 12:10:00.000000
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "a83f19c2d507"
down_revision: Union[str, None] = "e39a71c4d206"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Var olan kullanıcılar boş kalıyor; uygulama boş değeri "isim baş harfi"
    # avatarı olarak yorumluyor, yani eski davranış korunuyor.
    op.add_column(
        "kullanici",
        sa.Column(
            "avatar",
            sa.String(length=32),
            nullable=False,
            server_default=sa.text("''"),
        ),
    )


def downgrade() -> None:
    op.drop_column("kullanici", "avatar")
