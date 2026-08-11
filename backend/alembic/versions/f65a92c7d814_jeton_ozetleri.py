"""Yenileme ve parola sıfırlama jetonlarını özetle.

Revision ID: f65a92c7d814
Revises: c84f32a1e907
"""

from __future__ import annotations

import hashlib
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op


revision: str = "f65a92c7d814"
down_revision: Union[str, None] = "c84f32a1e907"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    baglanti = op.get_bind()
    for tablo in ("yenileme_jetonu", "sifre_sifirlama"):
        satirlar = baglanti.execute(sa.text(f"SELECT id, jeton FROM {tablo}")).mappings()
        for satir in satirlar:
            ozet = hashlib.sha256(satir["jeton"].encode("utf-8")).hexdigest()
            baglanti.execute(
                sa.text(f"UPDATE {tablo} SET jeton = :ozet WHERE id = :id"),
                {"ozet": ozet, "id": satir["id"]},
            )


def downgrade() -> None:
    # Kriptografik özetten eski düz metin sır geri getirilemez. Şema
    # değişmediğinden downgrade mevcut özetleri güvenle yerinde bırakır.
    pass
