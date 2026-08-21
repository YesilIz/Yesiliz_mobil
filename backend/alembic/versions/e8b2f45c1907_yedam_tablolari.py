"""YEDAM: rıza, başvuru, temas, danışman notu, erişim kaydı, sürükleyici, anket

`docs/panel-plani.md §7` · rıza metni `docs/yedam-riza-metni-v1.md`.

Yedi tablo tek revizyonda: hepsi aynı özelliğin parçası ve yarısı kurulmuş
bir YEDAM zinciri işe yaramıyor.

**Silme asimetrik** (rıza metni §3): rıza bitince `risk_surukleyici` ve
`anket_yanit` siliniyor; `yedam_basvuru`, `yedam_temas`, `danisman_notu`
ve `erisim_kaydi` kalıyor. Sonuncular bir danışmanlık ilişkisinin kaydı;
silinmeleri geçmişte ne olduğunu kullanıcının da görememesi demek olurdu.
Hesabın tamamen silinmesi bu ayrımı tanımıyor (`app/kullanici_silme.py`).

`erisim_kaydi` kullanıcıya gösterilen şeffaflık kaydı, `denetim_kaydi` ise
personeli denetleyen iz. Aynı olayın iki izleyicisi var ve tek tabloda
tutmak birini diğerinin erişim kurallarına tabi kılardı.

Revision ID: e8b2f45c1907
Revises: d1e6c4a728b3
Create Date: 2026-08-21 12:00:00.000000
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "e8b2f45c1907"
down_revision: Union[str, None] = "d1e6c4a728b3"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "yedam_riza",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("kullanici_id", sa.Integer(), nullable=False),
        sa.Column("metin_surumu", sa.String(length=20), nullable=False),
        sa.Column("kapsam", sa.String(length=300), nullable=False),
        sa.Column("verilme", sa.DateTime(timezone=True), nullable=False),
        sa.Column("son_gecerlilik", sa.DateTime(timezone=True), nullable=False),
        sa.Column("geri_cekme", sa.DateTime(timezone=True), nullable=True),
        sa.Column("ebeveyn_id", sa.Integer(), nullable=True),
        sa.Column("ebeveyn_onay_tarihi", sa.DateTime(timezone=True), nullable=True),
        sa.Column("beyan_yas", sa.Integer(), nullable=True),
        sa.ForeignKeyConstraint(["kullanici_id"], ["kullanici.id"]),
        sa.ForeignKeyConstraint(["ebeveyn_id"], ["kullanici.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_yedam_riza_kullanici_id", "yedam_riza", ["kullanici_id"])
    # Süre dolumu işi bu indeksi tarıyor.
    op.create_index("ix_yedam_riza_son_gecerlilik", "yedam_riza", ["son_gecerlilik"])

    op.create_table(
        "yedam_basvuru",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("kullanici_id", sa.Integer(), nullable=False),
        sa.Column("riza_id", sa.Integer(), nullable=False),
        sa.Column("durum", sa.String(length=30), nullable=False),
        sa.Column("iletisim_turu", sa.String(length=20), nullable=False),
        sa.Column("iletisim_degeri", sa.String(length=320), nullable=False),
        sa.Column("kullanici_notu", sa.String(length=1000), nullable=False, server_default=""),
        sa.Column("atanan_personel_id", sa.Integer(), nullable=True),
        sa.Column("olusturma", sa.DateTime(timezone=True), nullable=False),
        sa.Column("kapanis", sa.DateTime(timezone=True), nullable=True),
        sa.Column("kapanis_nedeni", sa.String(length=300), nullable=False, server_default=""),
        sa.Column("ulasilamadi_sayisi", sa.Integer(), nullable=False, server_default=sa.text("0")),
        sa.ForeignKeyConstraint(["kullanici_id"], ["kullanici.id"]),
        sa.ForeignKeyConstraint(["riza_id"], ["yedam_riza.id"]),
        sa.ForeignKeyConstraint(["atanan_personel_id"], ["personel.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_yedam_basvuru_kullanici_id", "yedam_basvuru", ["kullanici_id"])
    op.create_index("ix_yedam_basvuru_durum", "yedam_basvuru", ["durum"])
    op.create_index("ix_yedam_basvuru_atanan", "yedam_basvuru", ["atanan_personel_id"])
    op.create_index("ix_yedam_basvuru_olusturma", "yedam_basvuru", ["olusturma"])

    op.create_table(
        "yedam_temas",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("basvuru_id", sa.Integer(), nullable=False),
        sa.Column("personel_id", sa.Integer(), nullable=False),
        sa.Column("tur", sa.String(length=20), nullable=False),
        sa.Column("zaman", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["basvuru_id"], ["yedam_basvuru.id"]),
        sa.ForeignKeyConstraint(["personel_id"], ["personel.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_yedam_temas_basvuru_id", "yedam_temas", ["basvuru_id"])

    op.create_table(
        "danisman_notu",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("basvuru_id", sa.Integer(), nullable=False),
        sa.Column("personel_id", sa.Integer(), nullable=False),
        sa.Column("metin", sa.Text(), nullable=False),
        sa.Column("zaman", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["basvuru_id"], ["yedam_basvuru.id"]),
        sa.ForeignKeyConstraint(["personel_id"], ["personel.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_danisman_notu_basvuru_id", "danisman_notu", ["basvuru_id"])

    op.create_table(
        "erisim_kaydi",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("kullanici_id", sa.Integer(), nullable=False),
        sa.Column("personel_id", sa.Integer(), nullable=False),
        sa.Column("basvuru_id", sa.Integer(), nullable=True),
        sa.Column("kapsam", sa.String(length=300), nullable=False, server_default=""),
        sa.Column("zaman", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["kullanici_id"], ["kullanici.id"]),
        sa.ForeignKeyConstraint(["personel_id"], ["personel.id"]),
        sa.ForeignKeyConstraint(["basvuru_id"], ["yedam_basvuru.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_erisim_kaydi_kullanici_id", "erisim_kaydi", ["kullanici_id"])
    op.create_index("ix_erisim_kaydi_zaman", "erisim_kaydi", ["zaman"])

    op.create_table(
        "risk_surukleyici",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("kullanici_id", sa.Integer(), nullable=False),
        sa.Column("tarih", sa.Date(), nullable=False),
        sa.Column("faktor", sa.String(length=40), nullable=False),
        sa.Column("sira", sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(["kullanici_id"], ["kullanici.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "kullanici_id", "tarih", "faktor", name="uq_surukleyici_kullanici_tarih_faktor"
        ),
    )
    op.create_index("ix_risk_surukleyici_kullanici_id", "risk_surukleyici", ["kullanici_id"])
    op.create_index("ix_risk_surukleyici_tarih", "risk_surukleyici", ["tarih"])

    op.create_table(
        "anket_yanit",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("kullanici_id", sa.Integer(), nullable=False),
        sa.Column("tarih", sa.Date(), nullable=False),
        sa.Column("madde_puanlari", sa.String(length=60), nullable=False),
        sa.Column("ham_puan", sa.Float(), nullable=False),
        sa.Column("r_s", sa.Float(), nullable=False),
        sa.ForeignKeyConstraint(["kullanici_id"], ["kullanici.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("kullanici_id", "tarih", name="uq_anket_kullanici_tarih"),
    )
    op.create_index("ix_anket_yanit_kullanici_id", "anket_yanit", ["kullanici_id"])
    op.create_index("ix_anket_yanit_tarih", "anket_yanit", ["tarih"])


def downgrade() -> None:
    # Yabancı anahtar sırası: yaprakları önce.
    for tablo in (
        "anket_yanit",
        "risk_surukleyici",
        "erisim_kaydi",
        "danisman_notu",
        "yedam_temas",
        "yedam_basvuru",
        "yedam_riza",
    ):
        op.drop_table(tablo)
