"""kalıcı arkadaş davet kodlarını taramaya dayanıklı uzunluğa geçir

Revision ID: e39a71c4d206
Revises: b271c6d09e44
Create Date: 2026-08-11 06:20:00.000000
"""
from typing import Sequence, Union

from alembic import op


revision: str = "e39a71c4d206"
down_revision: Union[str, None] = "b271c6d09e44"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Dört karakterli kalıcı kodlar yaklaşık 20 bit entropiliydi. Onları
    # anında geçersizleştir; sahip sonraki GET'te sekiz karakterli yeni kod
    # alır. Arkadaşlık satırlarına dokunulmaz.
    op.execute("DELETE FROM davet_kodu WHERE length(kod) <> 12")


def downgrade() -> None:
    # Güvenlik için iptal edilmiş sırlar geri getirilemez.
    pass
