"""personel.parola_degistirmeli

Panelden açılan personel hesabı **geçici parolayla** geliyor
(`docs/panel-plani.md §5`, S1 kararı: hesapları Yeşilay kendi açıyor).
Geçici parola bir insan kanalından geçtiği için ilk girişte değiştirilmesi
zorunlu; aksi hâlde o kanalda süresiz geçerli bir kimlik bilgisi kalır.

E-posta daveti yerine geçici parola seçildi çünkü sistemde e-posta
sağlayıcısı yok (`CLAUDE.md` A15) ve Y13'ü ona bağlamak, panelin
kullanılabilirliğini çözülmemiş bir kaleme rehin bırakırdı.

Var olan hesaplar `false`: CLI ile açılmış hesapların parolasını sahibi
zaten kendi belirledi.

Revision ID: d1e6c4a728b3
Revises: c7d3b81e5a94
Create Date: 2026-08-21 11:00:00.000000
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "d1e6c4a728b3"
down_revision: Union[str, None] = "c7d3b81e5a94"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "personel",
        sa.Column(
            "parola_degistirmeli",
            sa.Boolean(),
            nullable=False,
            server_default=sa.text("false"),
        ),
    )


def downgrade() -> None:
    op.drop_column("personel", "parola_degistirmeli")
