"""personel, oturum, kurtarma kodu, denetim kaydı ve özellik bayrağı tabloları

Panel kimliği son kullanıcıdan **ayrı tabloda** (`docs/panel-plani.md §2`,
CLAUDE.md §12 K57): KVKK'da personel *veri işleyen*, son kullanıcı *veri
sahibi*; personelin ekran süresi/lig/ödül verisi olmamalı; ve `kullanici.rol`
ile ayrım her uçta ayrı ayrı doğru yazılmayı gerektirirken ayrı tablo bunu
yabancı anahtar seviyesinde fiziksel kılıyor.

`denetim_kaydi` **salt eklenebilir**. `hedef_id` bilerek yabancı anahtar
değil: kullanıcı silinince cascade baskısı doğmasın ve denetim satırı hayatta
kalsın diye. Silinemezliği PostgreSQL'de bir tetikleyici de zorluyor —
`psql`'den bile UPDATE/DELETE geçmiyor. SQLite'ta tetikleyici kurulmuyor;
testler `create_all` kullanıyor, migration koşmuyor.

Revision ID: b4c8e1d27f39
Revises: a83f19c2d507
Create Date: 2026-08-18 09:00:00.000000
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "b4c8e1d27f39"
down_revision: Union[str, None] = "a83f19c2d507"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


DENETIM_FONKSIYONU = """
CREATE OR REPLACE FUNCTION denetim_kaydi_degistirilemez() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'denetim_kaydi salt eklenebilir: UPDATE ve DELETE reddedildi';
END;
$$ LANGUAGE plpgsql;
"""

DENETIM_TETIKLEYICISI = """
CREATE TRIGGER denetim_kaydi_koruma
BEFORE UPDATE OR DELETE ON denetim_kaydi
FOR EACH ROW EXECUTE FUNCTION denetim_kaydi_degistirilemez();
"""


def _postgres_mu() -> bool:
    return op.get_bind().dialect.name == "postgresql"


def upgrade() -> None:
    op.create_table(
        "personel",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("eposta", sa.String(length=320), nullable=False),
        sa.Column("ad", sa.String(length=120), nullable=False),
        sa.Column("sifre_ozet", sa.String(length=255), nullable=False),
        sa.Column("rol", sa.String(length=20), nullable=False),
        sa.Column(
            "durum",
            sa.String(length=20),
            nullable=False,
            server_default=sa.text("'aktif'"),
        ),
        sa.Column(
            "totp_sir", sa.String(length=255), nullable=False, server_default=sa.text("''")
        ),
        sa.Column(
            "son_totp_sayaci", sa.Integer(), nullable=False, server_default=sa.text("0")
        ),
        sa.Column(
            "basarisiz_deneme", sa.Integer(), nullable=False, server_default=sa.text("0")
        ),
        sa.Column("kilit_bitis", sa.DateTime(timezone=True), nullable=True),
        sa.Column("son_giris", sa.DateTime(timezone=True), nullable=True),
        sa.Column("olusturma", sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_personel_eposta"), "personel", ["eposta"], unique=True)

    op.create_table(
        "personel_oturum",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("personel_id", sa.Integer(), nullable=False),
        sa.Column("jeton_ozet", sa.String(length=64), nullable=False),
        sa.Column("totp_dogrulandi", sa.Boolean(), nullable=False),
        sa.Column("ip", sa.String(length=45), nullable=False, server_default=sa.text("''")),
        sa.Column(
            "tarayici", sa.String(length=255), nullable=False, server_default=sa.text("''")
        ),
        sa.Column("olusturma", sa.DateTime(timezone=True), nullable=False),
        sa.Column("son_erisim", sa.DateTime(timezone=True), nullable=False),
        sa.Column("mutlak_bitis", sa.DateTime(timezone=True), nullable=False),
        sa.Column("iptal", sa.Boolean(), nullable=False),
        sa.ForeignKeyConstraint(["personel_id"], ["personel.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_personel_oturum_jeton_ozet"), "personel_oturum", ["jeton_ozet"], unique=True
    )
    op.create_index(
        op.f("ix_personel_oturum_personel_id"), "personel_oturum", ["personel_id"]
    )

    op.create_table(
        "personel_kurtarma_kodu",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("personel_id", sa.Integer(), nullable=False),
        sa.Column("kod_ozet", sa.String(length=64), nullable=False),
        sa.Column("kullanildi", sa.Boolean(), nullable=False),
        sa.Column("olusturma", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["personel_id"], ["personel.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_personel_kurtarma_kodu_kod_ozet"),
        "personel_kurtarma_kodu",
        ["kod_ozet"],
        unique=True,
    )
    op.create_index(
        op.f("ix_personel_kurtarma_kodu_personel_id"),
        "personel_kurtarma_kodu",
        ["personel_id"],
    )

    op.create_table(
        "denetim_kaydi",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("personel_id", sa.Integer(), nullable=False),
        sa.Column("eylem", sa.String(length=60), nullable=False),
        sa.Column("hedef_tur", sa.String(length=40), nullable=False),
        # Kasten yabancı anahtar DEĞİL — modül KDoc'unda gerekçesi.
        sa.Column("hedef_id", sa.Integer(), nullable=True),
        sa.Column(
            "gerekce", sa.String(length=500), nullable=False, server_default=sa.text("''")
        ),
        sa.Column("ip", sa.String(length=45), nullable=False, server_default=sa.text("''")),
        sa.Column(
            "detay", sa.String(length=2000), nullable=False, server_default=sa.text("''")
        ),
        sa.Column("zaman", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["personel_id"], ["personel.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_denetim_kaydi_personel_id"), "denetim_kaydi", ["personel_id"])
    op.create_index(op.f("ix_denetim_kaydi_eylem"), "denetim_kaydi", ["eylem"])
    op.create_index(op.f("ix_denetim_kaydi_hedef_id"), "denetim_kaydi", ["hedef_id"])
    op.create_index(op.f("ix_denetim_kaydi_zaman"), "denetim_kaydi", ["zaman"])

    op.create_table(
        "ozellik_bayragi",
        sa.Column("anahtar", sa.String(length=60), nullable=False),
        sa.Column("deger", sa.String(length=200), nullable=False),
        sa.Column(
            "aciklama", sa.String(length=300), nullable=False, server_default=sa.text("''")
        ),
        sa.Column("guncelleyen_id", sa.Integer(), nullable=True),
        sa.Column("guncelleme", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["guncelleyen_id"], ["personel.id"]),
        sa.PrimaryKeyConstraint("anahtar"),
    )

    if _postgres_mu():
        op.execute(DENETIM_FONKSIYONU)
        op.execute(DENETIM_TETIKLEYICISI)


def downgrade() -> None:
    if _postgres_mu():
        op.execute("DROP TRIGGER IF EXISTS denetim_kaydi_koruma ON denetim_kaydi")
        op.execute("DROP FUNCTION IF EXISTS denetim_kaydi_degistirilemez()")

    op.drop_table("ozellik_bayragi")

    op.drop_index(op.f("ix_denetim_kaydi_zaman"), table_name="denetim_kaydi")
    op.drop_index(op.f("ix_denetim_kaydi_hedef_id"), table_name="denetim_kaydi")
    op.drop_index(op.f("ix_denetim_kaydi_eylem"), table_name="denetim_kaydi")
    op.drop_index(op.f("ix_denetim_kaydi_personel_id"), table_name="denetim_kaydi")
    op.drop_table("denetim_kaydi")

    op.drop_index(
        op.f("ix_personel_kurtarma_kodu_personel_id"), table_name="personel_kurtarma_kodu"
    )
    op.drop_index(
        op.f("ix_personel_kurtarma_kodu_kod_ozet"), table_name="personel_kurtarma_kodu"
    )
    op.drop_table("personel_kurtarma_kodu")

    op.drop_index(op.f("ix_personel_oturum_personel_id"), table_name="personel_oturum")
    op.drop_index(op.f("ix_personel_oturum_jeton_ozet"), table_name="personel_oturum")
    op.drop_table("personel_oturum")

    op.drop_index(op.f("ix_personel_eposta"), table_name="personel")
    op.drop_table("personel")
