"""
Sunucu şeması.

## MAHREMİYET SÖZLEŞMESİ (CLAUDE.md §4)

Bu şemada **`kullanim_olay` yok ve olmayacak**. Yani hangi uygulamanın ne
zaman açıldığı, oturum başlangıç/bitişleri, uygulama adları sunucuya HİÇ
gelmiyor. Mesaj, fotoğraf, rehber, arama geçmişi, konum zaten hiçbir yerde
toplanmıyor.

Sunucuya çıkan tek davranış verisi **günlük özet** (kategori dakikaları +
örüntü değerleri) ve **risk sonucu** (skor, bant). İkisi de cihazda
hesaplanıyor; sunucu yalnızca saklıyor ve ebeveyn/lig için okuyor.

Bu, cihaz şemasının (`DB.md`) kopyası **değil**: burada kullanıcı kimliği,
lig üyeliği ve ebeveyn bağlantısı var; orada ham olaylar var.
"""

from __future__ import annotations

import datetime as dt

from sqlalchemy import (
    text,
    Boolean,
    Date,
    DateTime,
    Float,
    ForeignKey,
    Index,
    Integer,
    String,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.veritabani import Taban


def _simdi() -> dt.datetime:
    return dt.datetime.now(dt.timezone.utc)


class Kullanici(Taban):
    __tablename__ = "kullanici"

    id: Mapped[int] = mapped_column(primary_key=True)
    eposta: Mapped[str] = mapped_column(String(320), unique=True, index=True)
    ad: Mapped[str] = mapped_column(String(120))
    # argon2 özeti. Düz şifre hiçbir yere yazılmıyor.
    sifre_ozet: Mapped[str] = mapped_column(String(255))
    # `kullanici` | `uzman` — uzman paneli arayüzü Faz 3, rol şimdiden var.
    rol: Mapped[str] = mapped_column(String(20), default="kullanici")
    # Profil simgesi kimliği (`HARF`, `YAPRAK`, …). Görselin kendisi
    # uygulamada; sunucu yalnızca seçimi taşıyor ki kullanıcı telefon
    # değiştirdiğinde simgesi de gelsin.
    avatar: Mapped[str] = mapped_column(String(32), default="")
    olusturma: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi)

    ozetler: Mapped[list[GunlukOzet]] = relationship(back_populates="kullanici")


class YenilemeJetonu(Taban):
    """Uzun ömürlü yenileme jetonunun SHA-256 özeti ve iptal hali."""

    __tablename__ = "yenileme_jetonu"

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    jeton: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    son_gecerlilik: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True))
    iptal: Mapped[bool] = mapped_column(Boolean, default=False)


class SifreSifirlama(Taban):
    """Tek kullanımlık sıfırlama jetonunun SHA-256 özeti."""

    __tablename__ = "sifre_sifirlama"

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    jeton: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    son_gecerlilik: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True))
    kullanildi: Mapped[bool] = mapped_column(Boolean, default=False)


class GunlukOzet(Taban):
    """
    Cihazdan gelen günlük özet.

    Alan adları cihazdaki `GunlukOzetEntity` ile birebir — iki taraf
    arasında çeviri katmanı olmaması, alan eklerken tek yerde unutma
    riskini azaltıyor.
    """

    __tablename__ = "gunluk_ozet"
    __table_args__ = (UniqueConstraint("kullanici_id", "tarih", name="uq_ozet_kullanici_tarih"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    tarih: Mapped[dt.date] = mapped_column(Date, index=True)

    # kategori dakikaları
    social_dk: Mapped[float] = mapped_column(Float, default=0.0)
    entertainment_dk: Mapped[float] = mapped_column(Float, default=0.0)
    games_dk: Mapped[float] = mapped_column(Float, default=0.0)
    communication_dk: Mapped[float] = mapped_column(Float, default=0.0)
    productivity_dk: Mapped[float] = mapped_column(Float, default=0.0)
    education_dk: Mapped[float] = mapped_column(Float, default=0.0)
    health_dk: Mapped[float] = mapped_column(Float, default=0.0)
    other_dk: Mapped[float] = mapped_column(Float, default=0.0)

    # örüntüler
    gece: Mapped[float] = mapped_column(Float, default=0.0)
    oturum: Mapped[float] = mapped_column(Float, default=0.0)
    acilis: Mapped[float] = mapped_column(Float, default=0.0)
    sabah: Mapped[float] = mapped_column(Float, default=0.0)
    uyku_oncesi: Mapped[float] = mapped_column(Float, default=0.0)
    yer_degis: Mapped[float] = mapped_column(Float, default=0.0)

    # aktivite
    adim: Mapped[float] = mapped_column(Float, default=0.0)
    spor_dk: Mapped[float] = mapped_column(Float, default=0.0)

    # skor
    s: Mapped[float] = mapped_column(Float, default=0.0)

    guncelleme: Mapped[dt.datetime] = mapped_column(
        DateTime(timezone=True), default=_simdi, onupdate=_simdi
    )

    kullanici: Mapped[Kullanici] = relationship(back_populates="ozetler")

    @property
    def toplam_ekran_dk(self) -> float:
        return (
            self.social_dk
            + self.entertainment_dk
            + self.games_dk
            + self.communication_dk
            + self.productivity_dk
            + self.education_dk
            + self.health_dk
            + self.other_dk
        )


class RiskSonuc(Taban):
    __tablename__ = "risk_sonuc"
    __table_args__ = (UniqueConstraint("kullanici_id", "tarih", name="uq_risk_kullanici_tarih"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    tarih: Mapped[dt.date] = mapped_column(Date, index=True)

    s: Mapped[float] = mapped_column(Float)
    r: Mapped[float] = mapped_column(Float)
    gosterilen_bant: Mapped[str] = mapped_column(String(10))
    trend: Mapped[str] = mapped_column(String(20))
    veri_durumu: Mapped[str] = mapped_column(String(20), default="HESAPLANDI")

    guncelleme: Mapped[dt.datetime] = mapped_column(
        DateTime(timezone=True), default=_simdi, onupdate=_simdi
    )


class LigGrup(Taban):
    """
    Haftalık lig grubu.

    Seviye adları CLAUDE.md §12 K1: Fidan → Filiz → Ağaç → Koru → Orman.
    """

    __tablename__ = "lig_grup"

    id: Mapped[int] = mapped_column(primary_key=True)
    hafta_baslangic: Mapped[dt.date] = mapped_column(Date, index=True)
    seviye: Mapped[int] = mapped_column(Integer, default=0)


class LigUyelik(Taban):
    __tablename__ = "lig_uyelik"
    __table_args__ = (
        UniqueConstraint("kullanici_id", "hafta_baslangic", name="uq_uyelik_kullanici_hafta"),
        Index("ix_uyelik_grup", "grup_id"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    grup_id: Mapped[int] = mapped_column(ForeignKey("lig_grup.id"))
    hafta_baslangic: Mapped[dt.date] = mapped_column(Date, index=True)
    haftalik_puan: Mapped[int] = mapped_column(Integer, default=0)

    # "Ligde görün" tercihi (cihazda `Tercihler.ligdeGorun`).
    #
    # Tercih yazılıyor ve yalnızca kendi anahtarını çizmek için okunuyordu;
    # lig kodu, senkron ve sunucu hiçbiri tüketmiyordu. Kullanıcı anahtarı
    # kapatsa da ligde görünmeye devam ediyordu -- gizlilik tercihi olarak
    # sunulduğu için "çalışmayan buton" değil, tutulmayan bir sözdü
    # (debug.md Y-5).
    gorunur: Mapped[bool] = mapped_column(Boolean, default=True, server_default=text("true"))


class DavetKodu(Taban):
    """Arkadaş daveti — kullanıcı başına tek, kalıcı kod."""

    __tablename__ = "davet_kodu"

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), unique=True)
    kod: Mapped[str] = mapped_column(String(16), unique=True, index=True)


class Arkadaslik(Taban):
    """
    Tek yönlü kayıt; karşılıklı olduğunda iki satır bulunur.

    `durum`: `bekliyor` | `kabul`
    """

    __tablename__ = "arkadaslik"
    __table_args__ = (UniqueConstraint("kullanici_id", "arkadas_id", name="uq_arkadaslik"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    arkadas_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    durum: Mapped[str] = mapped_column(String(20), default="bekliyor")
    olusturma: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi)


class EslestirmeKodu(Taban):
    """
    Ebeveyn eşleştirme kodu — **10 dakika** geçerli, tek kullanımlık
    (tasarım 21'deki "Kod 10 dakika geçerlidir" metniyle birebir).
    """

    __tablename__ = "eslestirme_kodu"

    id: Mapped[int] = mapped_column(primary_key=True)
    kod: Mapped[str] = mapped_column(String(6), unique=True, index=True)
    ureten_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    # `ebeveyn` | `cocuk` — kodu üreten cihazın rolü
    ureten_rol: Mapped[str] = mapped_column(String(10))
    son_gecerlilik: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True))
    kullanildi: Mapped[bool] = mapped_column(Boolean, default=False)


class EbeveynBaglanti(Taban):
    __tablename__ = "ebeveyn_baglanti"
    __table_args__ = (
        UniqueConstraint("ebeveyn_id", name="uq_ebeveyn_tek_cocuk"),
        UniqueConstraint("cocuk_id", name="uq_cocuk_tek_ebeveyn"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    ebeveyn_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    cocuk_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    onay_tarihi: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi)


class OdulSecimi(Taban):
    """
    Kullanıcının ödül seçimi — bazı kademelerde birden çok seçenek var
    (örn. "Market kuponu" veya "Fidan bağışı"), kazanılınca kullanıcı biri
    arasında seçim yapıyor.

    Bir kademe için seçim **bir kez** yapılır ve değiştirilemez — tekil
    kısıt bunu garanti ediyor, ikinci istek uç tarafında 409 ile reddediliyor
    (CLAUDE.md §4 "yalnızca kazanç": geri alma/değiştirme yolu yok).
    """

    __tablename__ = "odul_secimi"
    __table_args__ = (UniqueConstraint("kullanici_id", "kademe_id", name="uq_odul_kullanici_kademe"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    # Örn. "kupon_market", "kupon_spor" — `OdulViewModel.KADEMELER`'deki id.
    kademe_id: Mapped[str] = mapped_column(String(40))
    # Örn. "market_kuponu", "fidan_bagisi".
    secenek_id: Mapped[str] = mapped_column(String(40))
    tarih: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi)


class Heartbeat(Taban):
    """
    Cihaz canlı sinyali.

    `docs/spec-veri.md`: çocuk uygulamayı silerse ebeveyne bildirim, **sunucu
    heartbeat** ile tetikleniyor — doğrudan silme tespiti iki platformda da
    güvenilir değil.
    """

    __tablename__ = "heartbeat"

    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), primary_key=True)
    son_sinyal: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi)
