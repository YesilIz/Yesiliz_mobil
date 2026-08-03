"""lig_uyelik.gorunur — "Ligde görün" tercihi

`Tercihler.ligdeGorun` cihazda yazılıyor ama hiçbir yerde tüketilmiyordu:
kullanıcı anahtarı kapatsa da ligde görünmeye devam ediyordu. Gizlilik
tercihi olarak sunulduğu için "çalışmayan buton" değil, tutulmayan bir
sözdü (debug.md Y-5).

Var olan üyelikler görünür sayılıyor: tercihin varsayılanı zaten açık.

Revision ID: a1c4f7e29b03
Revises: 62ba78535e84
Create Date: 2026-08-01 00:20:00.000000
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = 'a1c4f7e29b03'
down_revision: Union[str, None] = '62ba78535e84'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        'lig_uyelik',
        sa.Column('gorunur', sa.Boolean(), nullable=False, server_default=sa.text('true')),
    )


def downgrade() -> None:
    op.drop_column('lig_uyelik', 'gorunur')
