"""risk_sonuc.veri_durumu — yetersiz ölçüm ebeveyne risk diye gitmesin

Revision ID: b271c6d09e44
Revises: f65a92c7d814
Create Date: 2026-08-11 05:31:00.000000
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "b271c6d09e44"
down_revision: Union[str, None] = "f65a92c7d814"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "risk_sonuc",
        sa.Column(
            "veri_durumu",
            sa.String(length=20),
            nullable=False,
            server_default=sa.text("'HESAPLANDI'"),
        ),
    )


def downgrade() -> None:
    op.drop_column("risk_sonuc", "veri_durumu")
