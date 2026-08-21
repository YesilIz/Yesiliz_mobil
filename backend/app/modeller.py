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

**Panel tabloları dosyanın sonunda** (`personel`, `personel_oturum`,
`denetim_kaydi`, …). Sözleşmeyi değiştirmiyorlar: personel kimliği ve
denetim izi taşıyorlar, davranış verisi değil. Ayrı bir modül dosyasına
konmadılar çünkü `alembic/env.py` yalnızca `app.modeller`'i import ediyor —
ikinci bir dosya orada da bildirilmezse autogenerate o tabloları görmez ve
**silmeye kalkar**.
"""

from __future__ import annotations

import datetime as dt
import enum

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
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.veritabani import Taban


def _simdi() -> dt.datetime:
    return dt.datetime.now(dt.timezone.utc)


class KullaniciDurumu(str, enum.Enum):
    """
    Son kullanıcı hesabının durumu.

    `PersonelDurumu` ile aynı değerleri taşıyor ama **ayrı enum**: iki
    kimlik alanı birbirini tanımıyor (`docs/panel-plani.md §2`, K57) ve
    ortak bir enum ilk paylaşılan alan olurdu.
    """

    AKTIF = "aktif"
    ASKIDA = "askida"


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

    # `aktif` | `askida` — panelden, gerekçeyle ve denetim kaydıyla
    # değiştiriliyor (`docs/panel-plani.md §5.2`). Askıya alma **veri
    # silmiyor**: hesap yerinde duruyor, yalnızca API erişimi kapanıyor.
    # Yumuşak silmeyle karıştırılmamalı; silme hâlâ sert (`kullanici_silme`).
    durum: Mapped[str] = mapped_column(
        String(20), default="aktif", server_default=text("'aktif'")
    )
    # Kullanıcıya cihazda GÖSTERİLİYOR — sebebini öğrenemeyen kullanıcı
    # uygulamayı bozuk sanır (CLAUDE.md §4 "yapılmayan işi yapıldı
    # gösterme" ailesi). Bu yüzden serbest metin ve zorunlu.
    askiya_alma_sebebi: Mapped[str] = mapped_column(String(500), default="")
    askiya_alan_id: Mapped[int | None] = mapped_column(
        ForeignKey("personel.id"), nullable=True
    )
    askiya_alma_zamani: Mapped[dt.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    ozetler: Mapped[list[GunlukOzet]] = relationship(back_populates="kullanici")

    @property
    def askida_mi(self) -> bool:
        return self.durum == KullaniciDurumu.ASKIDA.value


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


# ---------------------------------------------------------------------------
# PANEL — personel kimliği ve denetim
#
# `docs/panel-plani.md §2` (CLAUDE.md §12 K57): personel hesapları son
# kullanıcıdan **ayrı tabloda**. `Kullanici.rol` kolonu bu iş için
# KULLANILMIYOR ve kullanılmayacak. Dört gerekçe:
#
#   1. KVKK'da son kullanıcı *veri sahibi*, personel *veri işleyen* — iki
#      ayrı rejim, iki ayrı aydınlatma metni, iki ayrı saklama süresi.
#   2. Personelin ekran süresi, lig üyeliği, ödül kademesi, serisi
#      OLMAMALI. Aynı tabloda olsalardı her uçta "bu kullanıcı personel mi"
#      diye sormak gerekirdi ve bir uçta unutmak danışmanı ligde gösterirdi.
#   3. `rol` kolonuyla ayrım her uç için ayrı ayrı doğru yazılmak zorunda;
#      ayrı tabloyla ayrım yabancı anahtar seviyesinde fiziksel.
#   4. Aynı kişi hem danışman hem kullanıcı olabilir — iki ayrı hesap açar.
#
# Bu tabloların hiçbiri `kullanici.id`'ye yabancı anahtar VERMİYOR;
# `denetim_kaydi.hedef_id` bilerek düz `Integer` (aşağıda gerekçesi).
# ---------------------------------------------------------------------------


class PersonelRolu(str, enum.Enum):
    """
    En az yetki ilkesi (`docs/panel-plani.md §2`).

    Bir rolün göremediği veri ekranda gri de görünmüyor: uç 403 dönüyor ve
    arayüzde o bölüm hiç çizilmiyor.
    """

    ADMIN = "admin"
    OPERASYON = "operasyon"
    YEDAM_YONETICI = "yedam_yonetici"
    YEDAM_DANISMAN = "yedam_danisman"
    GOZLEMCI = "gozlemci"


class PersonelDurumu(str, enum.Enum):
    AKTIF = "aktif"
    ASKIDA = "askida"


class Personel(Taban):
    """
    Panel kullanıcısı. Son kullanıcı DEĞİL — yukarıdaki blok neden.

    Parola `argon2id` ile özetleniyor (`kullanici` ile aynı), TOTP sırrı ise
    **şifreleniyor** (`guvenlik.sir_sifrele`): sır her doğrulamada okunmak
    zorunda olduğu için özet işe yaramıyor.
    """

    __tablename__ = "personel"

    id: Mapped[int] = mapped_column(primary_key=True)
    eposta: Mapped[str] = mapped_column(String(320), unique=True, index=True)
    ad: Mapped[str] = mapped_column(String(120))
    sifre_ozet: Mapped[str] = mapped_column(String(255))
    rol: Mapped[str] = mapped_column(String(20))
    durum: Mapped[str] = mapped_column(
        String(20), default=PersonelDurumu.AKTIF.value, server_default=text("'aktif'")
    )

    # Fernet ile şifrelenmiş base32 sır. Boş = 2FA henüz kurulmadı; ilk
    # girişte kurulum ekranına yönlendiriliyor.
    totp_sir: Mapped[str] = mapped_column(String(255), default="", server_default=text("''"))

    # Panelden açılan hesap geçici parolayla geliyor ve o parola bir
    # insan kanalından (kurumsal e-posta, elden) geçiyor. İlk girişte
    # değiştirilmesi zorunlu: aksi hâlde geçici parola kalıcı olur ve
    # onu ileten kanalda süresiz durur.
    parola_degistirmeli: Mapped[bool] = mapped_column(
        Boolean, default=False, server_default=text("false")
    )

    # Doğrulanan son TOTP sayacı. Aynı kodun ikinci kez kullanılması
    # reddediliyor — omuz üstünden okunan bir kod 30 sn geçerli kalmamalı.
    son_totp_sayaci: Mapped[int] = mapped_column(Integer, default=0, server_default=text("0"))

    # Kaba kuvvete karşı hesap kilidi (`docs/panel-plani.md §1.1 U5`).
    # Redis yerine bu seçildi: paylaşımlı kurum NAT'ının arkasında IP
    # tabanlı bir sınır zaten tek bir adrese bakıyor olurdu.
    basarisiz_deneme: Mapped[int] = mapped_column(Integer, default=0, server_default=text("0"))
    kilit_bitis: Mapped[dt.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    son_giris: Mapped[dt.datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    olusturma: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi)


class PersonelOturum(Taban):
    """
    Sunucu tarafı oturum. JWT **değil**, çünkü JWT tarayıcıda iptal edilemez.

    Personel hesabı askıya alındığında oturumun o anda düşmesi gerekiyor;
    bu tablo bunu tek `UPDATE` ile yapıyor.

    Jeton çereze **ham** yazılıyor, buraya SHA-256 özeti — `YenilemeJetonu`
    ile aynı sözleşme (CLAUDE.md §12 K36): veritabanı sızıntısı oturum ele
    geçirmiyor.

    İki adımlı giriş **tek satırda**: parola doğrulanınca satır
    `totp_dogrulandi = False` ve 5 dakikalık `mutlak_bitis` ile açılıyor,
    TOTP doğrulanınca bayrak dönüyor ve süre vardiya uzunluğuna çekiliyor.
    İkinci bir tablo ve ikinci bir çerez yok; yarım kalmış giriş de
    listelenebilir ve düşürülebilir bir oturum.
    """

    __tablename__ = "personel_oturum"

    id: Mapped[int] = mapped_column(primary_key=True)
    personel_id: Mapped[int] = mapped_column(ForeignKey("personel.id"), index=True)
    jeton_ozet: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    totp_dogrulandi: Mapped[bool] = mapped_column(Boolean, default=False)

    ip: Mapped[str] = mapped_column(String(45), default="", server_default=text("''"))
    tarayici: Mapped[str] = mapped_column(String(255), default="", server_default=text("''"))

    olusturma: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi)
    son_erisim: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi)

    # Kolon, ayardan türetilen anlık değer DEĞİL: `panel_oturum_mutlak_saat`
    # sonradan büyütülürse yaşayan oturumlar geriye dönük uzamasın.
    mutlak_bitis: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True))

    iptal: Mapped[bool] = mapped_column(Boolean, default=False)


class PersonelKurtarmaKodu(Taban):
    """
    Telefonunu kaybeden personelin tek kullanımlık giriş kodu.

    Kodun kendisi değil SHA-256 özeti saklanıyor (`PersonelOturum` ile aynı
    gerekçe). Kurulumda 8 tane üretiliyor ve **bir kez** gösteriliyor.
    """

    __tablename__ = "personel_kurtarma_kodu"

    id: Mapped[int] = mapped_column(primary_key=True)
    personel_id: Mapped[int] = mapped_column(ForeignKey("personel.id"), index=True)
    kod_ozet: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    kullanildi: Mapped[bool] = mapped_column(Boolean, default=False)
    olusturma: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi)


class DenetimKaydi(Taban):
    """
    Her personel yazma eyleminin silinemez izi (`docs/panel-plani.md §7`).

    **`hedef_id` kasten yabancı anahtar DEĞİL.** Kullanıcı silinince
    (`DELETE /auth/hesap`) cascade baskısı doğmasın ve denetim satırı hayatta
    kalsın diye. Silmeden sonra `hedef_id` takma-adlaşmış bir sayıya
    dönüşüyor: hangi kaydın kastedildiği artık çözülemiyor ama eylemin
    yapıldığı ve kim tarafından yapıldığı duruyor.

    **Satır yazma `db.commit()`'ten ÖNCE çağrılıyor** (`panel/denetim.py`):
    denetim kaydı ve etkisi aynı transaction'da, yani "gerçekleşmiş ama kaydı
    olmayan eylem" fiziksel olarak imkânsız.

    Silinemezlik üç katmanlı: kod disiplini (hiçbir `delete`/`update` yok,
    test tarıyor) · veritabanı tetikleyicisi (migration) · F6'da ayrı DB
    rolü.
    """

    __tablename__ = "denetim_kaydi"

    id: Mapped[int] = mapped_column(primary_key=True)
    personel_id: Mapped[int] = mapped_column(ForeignKey("personel.id"), index=True)

    # Örn. "kullanici.askiya_al", "puan.duzelt", "yedam.basvuru.ata".
    eylem: Mapped[str] = mapped_column(String(60), index=True)
    # Örn. "kullanici", "personel", "yedam_basvuru".
    hedef_tur: Mapped[str] = mapped_column(String(40))
    hedef_id: Mapped[int | None] = mapped_column(Integer, nullable=True, index=True)

    # Zorunlu ve serbest metin. Uçlar en az 20 karakter arıyor: "düzeltme"
    # diyen bir eylemin niçini kaydın kendisinde durmalı.
    gerekce: Mapped[str] = mapped_column(String(500), default="", server_default=text("''"))

    ip: Mapped[str] = mapped_column(String(45), default="", server_default=text("''"))
    detay: Mapped[str] = mapped_column(String(2000), default="", server_default=text("''"))
    zaman: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi, index=True)


class OzellikBayragi(Taban):
    """
    Panelden yönetilen sunucu tarafı anahtarlar.

    **Risk motoru sabitleri buraya GİRMİYOR** (CLAUDE.md §12 K17): skorlama
    cihazda ve `core-risk` referans Python ile birebir doğrulanmış durumda.
    Sunucudan sabit dağıtmak o doğrulamayı bozardı. Panel onları yalnızca
    gösteriyor.

    Buraya giren şeyler sunucu davranışını kontrol ediyor: maddi ödül
    kapısı (K28), YEDAM köprüsünün açık olup olmadığı, 18 altı ebeveyn
    onayı kuralı, bakım modu.
    """

    __tablename__ = "ozellik_bayragi"

    anahtar: Mapped[str] = mapped_column(String(60), primary_key=True)
    deger: Mapped[str] = mapped_column(String(200))
    aciklama: Mapped[str] = mapped_column(String(300), default="", server_default=text("''"))
    guncelleyen_id: Mapped[int | None] = mapped_column(
        ForeignKey("personel.id"), nullable=True
    )
    guncelleme: Mapped[dt.datetime] = mapped_column(
        DateTime(timezone=True), default=_simdi, onupdate=_simdi
    )


# ---------------------------------------------------------------------------
# YEDAM — rıza, başvuru, temas, erişim izi
#
# `docs/panel-plani.md §6, §7` · rıza metni `docs/yedam-riza-metni-v1.md`.
#
# **İki rıza bağımsız.** "Yeşilay ile paylaşım" (K19) günlük özetin sunucuya
# gitmesini yönetiyor; YEDAM rızası danışmanın görmesini. Paylaşımı kapalı
# bir kullanıcı YEDAM desteği isteyebiliyor ve o durumda danışman davranış
# bölümünü BOŞ görüyor — rıza metni bunu açıkça söylüyor.
#
# **Silme asimetrik ve bilinçli** (rıza metni §3): rıza bitince
# `risk_surukleyici` ve `anket_yanit` SİLİNİYOR; `yedam_basvuru`,
# `yedam_temas`, `danisman_notu` ve `erisim_kaydi` KALIYOR. Sonuncular bir
# danışmanlık ilişkisinin kaydı ve erişim izi — silinmeleri, geçmişte ne
# olduğunu kullanıcının da görememesi demek olurdu.
# ---------------------------------------------------------------------------


class BasvuruDurumu(str, enum.Enum):
    """`docs/panel-plani.md §6.3`."""

    YENI = "yeni"
    ATANDI = "atandi"
    TEMAS_KURULDU = "temas_kuruldu"
    KAPANDI = "kapandi"
    ULASILAMADI = "ulasilamadi"
    RIZA_GERI_CEKILDI = "riza_geri_cekildi"
    SURESI_DOLDU = "suresi_doldu"
    #: Kapıda bekleyen 18 altı başvuru. **Hiçbir danışman görmüyor.**
    EBEVEYN_ONAYI_BEKLIYOR = "ebeveyn_onayi_bekliyor"


#: Danışmanın havuzda görebileceği durumlar. Bekleyen ve kapanmış
#: başvurular dışarıda: onay beklerken veri görünmemeli.
HAVUZDA_GORUNEN = (
    BasvuruDurumu.YENI.value,
    BasvuruDurumu.ATANDI.value,
    BasvuruDurumu.TEMAS_KURULDU.value,
    BasvuruDurumu.ULASILAMADI.value,
)

#: Başvurunun kapanmış sayıldığı durumlar — yeni başvuru açılabilir.
KAPANMIS = (
    BasvuruDurumu.KAPANDI.value,
    BasvuruDurumu.RIZA_GERI_CEKILDI.value,
    BasvuruDurumu.SURESI_DOLDU.value,
)


class TemasTuru(str, enum.Enum):
    ARANDI = "arandi"
    ULASILAMADI = "ulasilamadi"
    GORUSULDU = "gorusuldu"


class YedamRiza(Taban):
    """
    Açık rıza kaydı (`docs/yedam-riza-metni-v1.md`).

    `metin_surumu` **birebir** o belgedeki sürüm. Metnin kapsamı değişirse
    sürüm `v2` olur ve daha önce rıza vermiş herkesten yeniden rıza
    isteniyor — eski rıza yeni kapsamı karşılamaz. Bu yüzden sürüm bir
    kolon: "hangi metne rıza verildi" sorusunun cevabı kayıtta durmalı.

    `kapsam` virgülle ayrılmış anahtar listesi (`kimlik,risk,surukleyici,
    anket,not`). Liste olarak saklanıyor çünkü ileride bir madde
    çıkarılabilir ve o zaman eski rızaların neyi kapsadığı hâlâ okunabilir
    olmalı.
    """

    __tablename__ = "yedam_riza"

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    metin_surumu: Mapped[str] = mapped_column(String(20))
    kapsam: Mapped[str] = mapped_column(String(300))

    verilme: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi)
    son_gecerlilik: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), index=True)
    geri_cekme: Mapped[dt.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    #: 18 altı kullanıcıda onayı beklenen ebeveyn; yoksa `None`.
    ebeveyn_id: Mapped[int | None] = mapped_column(
        ForeignKey("kullanici.id"), nullable=True
    )
    ebeveyn_onay_tarihi: Mapped[dt.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    #: Beyan edilen yaş — kararın hangi bilgiyle verildiği kayıtta dursun.
    #: Doğrulanabilir DEĞİL (CLAUDE.md §12 K26).
    beyan_yas: Mapped[int | None] = mapped_column(Integer, nullable=True)


class YedamBasvuru(Taban):
    """Destek talebi. Kullanıcı başına aynı anda **en fazla bir açık** başvuru."""

    __tablename__ = "yedam_basvuru"

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    riza_id: Mapped[int] = mapped_column(ForeignKey("yedam_riza.id"))

    durum: Mapped[str] = mapped_column(
        String(30), default=BasvuruDurumu.YENI.value, index=True
    )
    #: `telefon` | `eposta`
    iletisim_turu: Mapped[str] = mapped_column(String(20))
    iletisim_degeri: Mapped[str] = mapped_column(String(320))
    kullanici_notu: Mapped[str] = mapped_column(String(1000), default="", server_default=text("''"))

    atanan_personel_id: Mapped[int | None] = mapped_column(
        ForeignKey("personel.id"), nullable=True, index=True
    )
    olusturma: Mapped[dt.datetime] = mapped_column(
        DateTime(timezone=True), default=_simdi, index=True
    )
    kapanis: Mapped[dt.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    kapanis_nedeni: Mapped[str] = mapped_column(String(300), default="", server_default=text("''"))

    #: Ulaşılamadı sayacı — 3'ten sonra durum `ulasilamadi`'ya çekiliyor.
    ulasilamadi_sayisi: Mapped[int] = mapped_column(Integer, default=0, server_default=text("0"))


class YedamTemas(Taban):
    """
    Görüşme kaydı. Kullanıcı **görüyor** (şeffaflık), içeriği yok.

    Serbest metin taşımıyor: o `danisman_notu`'nun işi ve ayrı bir rejime
    bağlı (S4). Buradaki kayıt "arandı / ulaşılamadı / görüşüldü"den ibaret.
    """

    __tablename__ = "yedam_temas"

    id: Mapped[int] = mapped_column(primary_key=True)
    basvuru_id: Mapped[int] = mapped_column(ForeignKey("yedam_basvuru.id"), index=True)
    personel_id: Mapped[int] = mapped_column(ForeignKey("personel.id"))
    tur: Mapped[str] = mapped_column(String(20))
    zaman: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi)


class DanismanNotu(Taban):
    """
    Klinik not. Kullanıcıya **gösterilmiyor**.

    Saklama süresi ve erişim rejimi açık (`docs/panel-plani.md §14 S4`);
    o karar gelene kadar özellik bayrağı (`yedam.danisman_notu`) **kapalı**
    ve bu tabloya satır yazılmıyor. Tablo şimdiden var çünkü bayrak
    açıldığında migration beklemek istemiyoruz.
    """

    __tablename__ = "danisman_notu"

    id: Mapped[int] = mapped_column(primary_key=True)
    basvuru_id: Mapped[int] = mapped_column(ForeignKey("yedam_basvuru.id"), index=True)
    personel_id: Mapped[int] = mapped_column(ForeignKey("personel.id"))
    metin: Mapped[str] = mapped_column(Text)
    zaman: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=_simdi)


class ErisimKaydi(Taban):
    """
    "Danışman X, 14 Ağustos 10:32'de risk detayına baktı."

    Kullanıcıya gösterilen şeffaflık kaydı ve rıza metninin verdiği sözün
    ta kendisi: *"Danışman bilgilerine her baktığında uygulamada bir kayıt
    görürsün."* Rıza geri çekilse de **silinmiyor** — silinmesi, geçmişte
    kimin baktığını kullanıcının da görememesi demek olurdu.

    `denetim_kaydi`'ndan ayrı: o personeli denetliyor ve personel görüyor;
    bu kullanıcıya gösteriliyor. Aynı olayın iki farklı izleyicisi var ve
    tek tabloda tutmak, birini diğerinin erişim kurallarına tabi kılardı.
    """

    __tablename__ = "erisim_kaydi"

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    personel_id: Mapped[int] = mapped_column(ForeignKey("personel.id"))
    basvuru_id: Mapped[int | None] = mapped_column(
        ForeignKey("yedam_basvuru.id"), nullable=True
    )
    #: Hangi bölümler görüntülendi (`risk,surukleyici,anket,davranis`).
    kapsam: Mapped[str] = mapped_column(String(300), default="", server_default=text("''"))
    zaman: Mapped[dt.datetime] = mapped_column(
        DateTime(timezone=True), default=_simdi, index=True
    )


class RiskSurukleyici(Taban):
    """
    Skoru en çok yükselten faktörler — **cihazda hesaplanmış** çıktı.

    Sunucu bunu ÜRETMİYOR, saklıyor (CLAUDE.md §12 K17). `core-risk`
    referans Python ile birebir doğrulanmış motorun `surukleyiciler`
    çıktısı burada duruyor.

    Yalnızca geçerli YEDAM rızası varken dolu; rıza bitince **satırlar
    siliniyor** (rıza metni §3). Cihazda zaten duruyorlar; sunucuda
    tutmanın gerekçesi rızaydı, gerekçe bitince veri de bitiyor.
    """

    __tablename__ = "risk_surukleyici"
    __table_args__ = (
        UniqueConstraint(
            "kullanici_id", "tarih", "faktor", name="uq_surukleyici_kullanici_tarih_faktor"
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    tarih: Mapped[dt.date] = mapped_column(Date, index=True)
    #: Örn. "social", "gece", "oturum" — cihazdaki faktör anahtarı.
    faktor: Mapped[str] = mapped_column(String(40))
    #: 1 = en çok katkı yapan. **Katkı DEĞERİ değil sırası** saklanıyor:
    #: `core-risk` sürükleyicileri sıralı bir isim listesi olarak veriyor
    #: (`RiskHesapla.surukleyiciler`), değeri hesaplayıp atıyor. Sıradan
    #: bir "katkı yüzdesi" uydurmak, ölçülmemiş bir sayıyı ölçülmüş gibi
    #: göstermek olurdu (CLAUDE.md §9). Motoru değiştirip değeri de
    #: döndürmek mümkün ama o, referans Python ile birebir doğrulanmış
    #: çıktıyı değiştirmek demek (§7) — rıza metni de yalnızca
    #: "kategoriler" vaat ediyor.
    sira: Mapped[int] = mapped_column(Integer)


class AnketYanit(Taban):
    """
    SAS-SV yanıt geçmişi. Sunucuda **yeni** — YEDAM paneli için.

    Madde puanları tek tek taşınıyor: danışman "hangi maddede yüksek"
    sorusunu soruyor ve toplam puan onu cevaplamıyor. Ölçek birebir
    korunmak zorunda (`docs/spec-anket.md §6.1`, K12).

    Rıza kapılı: rıza bitince siliniyor.
    """

    __tablename__ = "anket_yanit"
    __table_args__ = (
        UniqueConstraint("kullanici_id", "tarih", name="uq_anket_kullanici_tarih"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    kullanici_id: Mapped[int] = mapped_column(ForeignKey("kullanici.id"), index=True)
    tarih: Mapped[dt.date] = mapped_column(Date, index=True)
    #: 10 maddenin Likert puanları, virgülle ayrılmış (`1,3,2,...`).
    madde_puanlari: Mapped[str] = mapped_column(String(60))
    ham_puan: Mapped[float] = mapped_column(Float)
    r_s: Mapped[float] = mapped_column(Float)
