"""Ebeveyn ve çocuk hesaplarını tek etkin eşleşmeyle sınırla.

Revision ID: c84f32a1e907
Revises: d7e5c1a93f42
"""

from typing import Sequence, Union

from alembic import op


revision: str = "c84f32a1e907"
down_revision: Union[str, None] = "d7e5c1a93f42"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Eski sürüm bir hesabı birden fazla bağlantıya sokabiliyordu.
    # En yeni bağlantıyı tutup onunla ebeveyn veya çocuk kimliğini
    # paylaşan daha eski satırları temizle; ardından yarışa dayanıklı
    # veritabanı garantilerini kur.
    op.execute(
        """
        DELETE FROM ebeveyn_baglanti AS eski
        USING ebeveyn_baglanti AS yeni
        WHERE eski.id < yeni.id
          AND (
            eski.ebeveyn_id IN (yeni.ebeveyn_id, yeni.cocuk_id)
            OR eski.cocuk_id IN (yeni.ebeveyn_id, yeni.cocuk_id)
          )
        """
    )
    op.drop_constraint("uq_ebeveyn_cocuk", "ebeveyn_baglanti", type_="unique")
    op.create_unique_constraint("uq_ebeveyn_tek_cocuk", "ebeveyn_baglanti", ["ebeveyn_id"])
    op.create_unique_constraint("uq_cocuk_tek_ebeveyn", "ebeveyn_baglanti", ["cocuk_id"])


def downgrade() -> None:
    op.drop_constraint("uq_cocuk_tek_ebeveyn", "ebeveyn_baglanti", type_="unique")
    op.drop_constraint("uq_ebeveyn_tek_cocuk", "ebeveyn_baglanti", type_="unique")
    op.create_unique_constraint(
        "uq_ebeveyn_cocuk", "ebeveyn_baglanti", ["ebeveyn_id", "cocuk_id"]
    )
