package tr.org.yesilay.yesiliz.data.yerel.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import tr.org.yesilay.yesiliz.data.yerel.entity.AnketYanitEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.GorevTamamlamaEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.KategoriEslemeEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.KullanimOlayEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.PuanGunlukEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.RiskSonucEntity
import java.time.LocalDate

@Dao
interface KullanimOlayDao {
    @Insert
    suspend fun ekle(olaylar: List<KullanimOlayEntity>)

    @Query("SELECT * FROM kullanim_olay WHERE tarih = :tarih ORDER BY baslangic")
    suspend fun gunun(tarih: LocalDate): List<KullanimOlayEntity>

    @Query("SELECT COUNT(*) FROM kullanim_olay WHERE tarih = :tarih")
    suspend fun gununSayisi(tarih: LocalDate): Int

    /**
     * Ham olaylar sonsuza kadar tutulmaz — özet çıkarıldıktan sonra
     * eskiler silinir. Mahremiyet açısından da doğru: cihazda gereğinden
     * fazla ayrıntı birikmesin.
     */
    @Query("DELETE FROM kullanim_olay WHERE tarih < :tarih")
    suspend fun eskileriSil(tarih: LocalDate): Int

    /**
     * Bir günü yeniden işlemeden önce temizler.
     *
     * Günlük worker aynı gün için birden çok kez koşabilir (kullanıcı
     * uygulamayı açtı, cihaz yeniden başladı). Silmeden eklemek olayları
     * ikiye katlar ve süreleri şişirir.
     */
    @Query("DELETE FROM kullanim_olay WHERE tarih = :tarih")
    suspend fun gununuSil(tarih: LocalDate): Int
}

@Dao
interface GunlukOzetDao {
    @Upsert
    suspend fun kaydet(ozet: GunlukOzetEntity)

    @Query("SELECT * FROM gunluk_ozet WHERE tarih = :tarih")
    suspend fun getir(tarih: LocalDate): GunlukOzetEntity?

    @Query("SELECT * FROM gunluk_ozet ORDER BY tarih DESC LIMIT :gun")
    suspend fun sonGunler(gun: Int): List<GunlukOzetEntity>

    @Query("SELECT * FROM gunluk_ozet ORDER BY tarih DESC LIMIT :gun")
    fun sonGunlerAkisi(gun: Int): Flow<List<GunlukOzetEntity>>

    /** Alpha eğrisinin `d` girdisi — kaç gün veri birikmiş (spec-risk.md §4.5). */
    @Query("SELECT COUNT(*) FROM gunluk_ozet")
    suspend fun gunSayisi(): Int

    /**
     * [tarih] ve öncesinin son [gun] kaydı, **eskiden yeniye**.
     *
     * `Bantlama.guvenlikTabani` `takeLast()` kullanıyor; artan kronolojik sıra
     * ŞART. Ayrıca hesaplanan günün kendisi de dahil (senaryo testleri böyle
     * çağırıyor).
     */
    @Query(
        """
        SELECT * FROM (
            SELECT * FROM gunluk_ozet WHERE tarih <= :tarih ORDER BY tarih DESC LIMIT :gun
        ) ORDER BY tarih ASC
        """,
    )
    suspend fun buGuneKadar(tarih: LocalDate, gun: Int): List<GunlukOzetEntity>

    /** Alpha'nın `canliGun` girdisi — geleceğe ait satır sayılmasın. */
    @Query("SELECT COUNT(*) FROM gunluk_ozet WHERE tarih <= :tarih")
    suspend fun gunSayisiEKadar(tarih: LocalDate): Int

    /** İstatistik ekranının takvim hizalı penceresi — eskiden yeniye. */
    @Query("SELECT * FROM gunluk_ozet WHERE tarih BETWEEN :bas AND :bit ORDER BY tarih ASC")
    fun araliktakilerAkisi(bas: LocalDate, bit: LocalDate): Flow<List<GunlukOzetEntity>>

    /** Bir günün özeti — Ana Sayfa canlı okuma. */
    @Query("SELECT * FROM gunluk_ozet WHERE tarih = :tarih")
    fun gununAkisi(tarih: LocalDate): Flow<GunlukOzetEntity?>

    /** Worker telafisi: hangi günler zaten işlenmiş? */
    @Query("SELECT tarih FROM gunluk_ozet WHERE tarih BETWEEN :bas AND :bit")
    suspend fun islenmisGunler(bas: LocalDate, bit: LocalDate): List<LocalDate>
}

@Dao
interface RiskSonucDao {
    @Upsert
    suspend fun kaydet(sonuc: RiskSonucEntity)

    @Query("SELECT * FROM risk_sonuc WHERE tarih = :tarih")
    suspend fun getir(tarih: LocalDate): RiskSonucEntity?

    @Query("SELECT * FROM risk_sonuc ORDER BY tarih DESC LIMIT 1")
    fun sonuncuAkisi(): Flow<RiskSonucEntity?>

    /** Trend hesabı 14 gün ister (§5.5); histerezis 7 (§5.4). */
    @Query("SELECT * FROM risk_sonuc ORDER BY tarih DESC LIMIT :gun")
    suspend fun sonGunler(gun: Int): List<RiskSonucEntity>

    /**
     * [tarih]'ten **önceki** son [gun] kayıt, eskiden yeniye.
     *
     * `tarih` hariç: o günün skoru henüz hesaplanıyor. Gün yeniden işlenirse
     * (worker iki kez koştu) eski satırın geçmişe iki kez girmesini de bu
     * engelliyor.
     */
    @Query(
        """
        SELECT * FROM (
            SELECT * FROM risk_sonuc WHERE tarih < :tarih ORDER BY tarih DESC LIMIT :gun
        ) ORDER BY tarih ASC
        """,
    )
    suspend fun oncekiler(tarih: LocalDate, gun: Int): List<RiskSonucEntity>
}

@Dao
interface AnketYanitDao {
    @Insert
    suspend fun ekle(yanit: AnketYanitEntity): Long

    @Query("SELECT * FROM anket_yanit ORDER BY tarih DESC LIMIT 1")
    suspend fun sonuncu(): AnketYanitEntity?

    @Query("SELECT * FROM anket_yanit ORDER BY tarih")
    suspend fun hepsi(): List<AnketYanitEntity>
}

@Dao
interface KategoriEslemeDao {
    @Query("SELECT * FROM kategori_esleme")
    suspend fun hepsi(): List<KategoriEslemeEntity>

    @Query("SELECT * FROM kategori_esleme WHERE paketAdi = :paketAdi")
    suspend fun getir(paketAdi: String): KategoriEslemeEntity?

    /**
     * Seed sırasında kullanılır: kullanıcının değiştirdiği satırlar
     * EZİLMEZ ([OnConflictStrategy.IGNORE]).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedEt(esleme: List<KategoriEslemeEntity>)

    @Upsert
    suspend fun kullaniciSecimi(esleme: KategoriEslemeEntity)

    @Query("SELECT COUNT(*) FROM kategori_esleme")
    suspend fun sayi(): Int
}

@Dao
interface GorevTamamlamaDao {
    @Insert
    suspend fun ekle(kayit: GorevTamamlamaEntity)

    @Query("SELECT * FROM gorev_tamamlama WHERE tarih = :tarih")
    suspend fun gunun(tarih: LocalDate): List<GorevTamamlamaEntity>

    @Query("SELECT COALESCE(SUM(puan), 0) FROM gorev_tamamlama WHERE tarih = :tarih")
    suspend fun gununPuani(tarih: LocalDate): Int

    /** Günlük limit denetimi — aynı görev bugün kaç kez tamamlandı. */
    @Query("SELECT COUNT(*) FROM gorev_tamamlama WHERE tarih = :tarih AND gorevId = :gorevId")
    suspend fun gununSayisi(tarih: LocalDate, gorevId: String): Int

    @Query("SELECT * FROM gorev_tamamlama WHERE tarih = :tarih")
    fun gununAkisi(tarih: LocalDate): Flow<List<GorevTamamlamaEntity>>

    /** Puan geçmişi alt sayfası — son kayıtlar, yeniden eskiye. */
    @Query("SELECT * FROM gorev_tamamlama ORDER BY tarih DESC, id DESC LIMIT :adet")
    fun sonKayitlarAkisi(adet: Int): Flow<List<GorevTamamlamaEntity>>
}

@Dao
interface PuanGunlukDao {
    @Upsert
    suspend fun kaydet(puan: PuanGunlukEntity)

    @Query("SELECT * FROM puan_gunluk WHERE tarih = :tarih")
    suspend fun getir(tarih: LocalDate): PuanGunlukEntity?

    @Query("SELECT COALESCE(SUM(puanNet), 0) FROM puan_gunluk")
    fun toplamPuanAkisi(): Flow<Int>

    @Query("SELECT * FROM puan_gunluk ORDER BY tarih DESC LIMIT 1")
    suspend fun sonuncu(): PuanGunlukEntity?

    /**
     * Seri hesabının dayanağı: [tarih]'ten **önceki** en son puan kazanılan gün.
     *
     * `puanNet > 0` şartı önemli — puan kazanılmayan gün seriyi ilerletmiyor
     * ("aktif gün" tanımı, `docs/spec-puan.md §9.1`).
     */
    @Query(
        "SELECT * FROM puan_gunluk WHERE tarih < :tarih AND puanNet > 0 " +
            "ORDER BY tarih DESC LIMIT 1",
    )
    suspend fun oncekiAktifGun(tarih: LocalDate): PuanGunlukEntity?

    /** Ödüller ekranının "aktif gün" sayacı — ödül kademelerinin ek koşulu. */
    @Query("SELECT COUNT(*) FROM puan_gunluk WHERE puanNet > 0")
    fun aktifGunAkisi(): Flow<Int>

    /** Ana Sayfa'daki seri çipi. */
    @Query("SELECT * FROM puan_gunluk WHERE tarih = :tarih")
    fun gununAkisi(tarih: LocalDate): Flow<PuanGunlukEntity?>
}
