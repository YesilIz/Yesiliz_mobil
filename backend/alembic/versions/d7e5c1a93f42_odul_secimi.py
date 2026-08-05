"""odul_secimi tablosu — market kuponu yerine fidan bağışı gibi alternatif ödül seçimi.

Bir kademe için seçim kalıcı: (kullanici_id, kademe_id) üzerinde tekil
kısıt, ikinci seçim denemesi uç tarafında 409 ile reddediliyor (CLAUDE.md
§4 "yalnızca kazanç" — geri alma/değiştirme yolu yok).

Revision ID: d7e5c1a93f42
Revises: a1c4f7e29b03
Create Date: 2026-08-04 12:00:00.000000
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = 'd7e5c1a93f42'
down_revision: Union[str, None] = 'a1c4f7e29b03'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        'odul_secimi',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('kullanici_id', sa.Integer(), nullable=False),
        sa.Column('kademe_id', sa.String(length=40), nullable=False),
        sa.Column('secenek_id', sa.String(length=40), nullable=False),
        sa.Column('tarih', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['kullanici_id'], ['kullanici.id'], ),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('kullanici_id', 'kademe_id', name='uq_odul_kullanici_kademe'),
    )
    op.create_index(
        op.f('ix_odul_secimi_kullanici_id'), 'odul_secimi', ['kullanici_id'], unique=False,
    )


def downgrade() -> None:
    op.drop_index(op.f('ix_odul_secimi_kullanici_id'), table_name='odul_secimi')
    op.drop_table('odul_secimi')
