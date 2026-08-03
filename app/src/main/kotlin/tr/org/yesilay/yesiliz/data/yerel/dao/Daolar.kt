package tr.org.yesilay.yesiliz.data.yerel.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import tr.org.yesilay.yesiliz.data.yerel.entity.AdimSayacEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.AnketYanitEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.ArkadasEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.BildirimEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.FarkindalikTestiEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.GorevTamamlamaEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.KategoriEslemeEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.KullanimOlayEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.PuanGunlukEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.RiskSonucEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.RuhHaliEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.UykuBeyaniEntity
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

    /**
     * Alpha'nın `canliGun` girdisi — kaç gün **gerçek veri** birikmiş.
     *
     * `olayVarMi` filtresi şart. Satır saymak, worker'ın telafi ettiği boş
     * günleri de geçmiş sayıp uygulamanın ilk gününde `d = 8` sanmasına yol
     * açıyordu: `alpha` 1,0 yerine 0,49 çıkıyor, anketin ağırlığı yarıya
     * iniyor ve gösterilen risk olduğundan düşük görünüyordu
     * (`debug.md` K-5). `docs/spec-risk.md §4.5`'e göre `d` "kaç gün gerçek
     * veri biriktiği", satır sayısı değil.
     */
    @Query("SELECT COUNT(*) FROM gunluk_ozet WHERE tarih <= :tarih AND olayVarMi = 1")
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

    /**
     * En son doldurulan anket.
     *
     * **`id` eşitlik bozucusu şart.** `tarih` bir `LocalDate`, yani gün
     * çözünürlüğünde: kullanıcı anketi aynı gün ikinci kez doldurduğunda —
     * test sırasında tam olarak bu yapılıyor — iki satırın `tarih`'i eşit
     * oluyor. Yalnızca `ORDER BY tarih DESC` ile SQLite eşitlikte hangi
     * satırın önce geleceğini garanti etmiyor ve pratikte `rowid` sırasını,
     * yani **eski** anketi veriyor. `RiskDeposu.anketGirdisi()` buna
     * dayandığı için sonuç "anketi yeniledim, risk değişmedi" oluyordu.
     */
    @Query("SELECT * FROM anket_yanit ORDER BY tarih DESC, id DESC LIMIT 1")
    suspend fun sonuncu(): AnketYanitEntity?

    /** Aynı belirsizlik geçmiş listesinde de var — orada da `id` ile kırılıyor. */
    @Query("SELECT * FROM anket_yanit ORDER BY tarih, id")
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

    /** Dışa aktarım — tüm görev geçmişi (DisaAktarDeposu). */
    @Query("SELECT * FROM gorev_tamamlama ORDER BY tarih")
    suspend fun hepsi(): List<GorevTamamlamaEntity>
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

    /** Lig sıralamasının girdisi — haftalık toplam (SenkronDeposu). */
    @Query("SELECT COALESCE(SUM(puanNet), 0) FROM puan_gunluk WHERE tarih BETWEEN :bas AND :bit")
    suspend fun aralikToplami(bas: LocalDate, bit: LocalDate): Int
}

@Dao
interface RuhHaliDao {
    @Upsert
    suspend fun kaydet(kayit: RuhHaliEntity)

    @Query("SELECT * FROM ruh_hali WHERE tarih = :tarih")
    fun gununAkisi(tarih: LocalDate): Flow<RuhHaliEntity?>

    @Query("SELECT * FROM ruh_hali WHERE tarih BETWEEN :bas AND :bit ORDER BY tarih")
    suspend fun aralik(bas: LocalDate, bit: LocalDate): List<RuhHaliEntity>
}

@Dao
interface UykuBeyaniDao {
    @Upsert
    suspend fun kaydet(kayit: UykuBeyaniEntity)

    @Query("SELECT * FROM uyku_beyani WHERE tarih = :tarih")
    suspend fun getir(tarih: LocalDate): UykuBeyaniEntity?

    @Query("SELECT * FROM uyku_beyani WHERE tarih = :tarih")
    fun gununAkisi(tarih: LocalDate): Flow<UykuBeyaniEntity?>
}

@Dao
interface FarkindalikTestiDao {
    @Insert
    suspend fun ekle(sonuc: FarkindalikTestiEntity)

    @Query("SELECT * FROM farkindalik_testi ORDER BY tarih DESC, id DESC LIMIT 1")
    suspend fun sonuncu(): FarkindalikTestiEntity?

    @Query("SELECT * FROM farkindalik_testi ORDER BY tarih DESC, id DESC LIMIT 1")
    fun sonuncuAkisi(): Flow<FarkindalikTestiEntity?>

    @Query("SELECT * FROM farkindalik_testi ORDER BY tarih")
    fun hepsiAkisi(): Flow<List<FarkindalikTestiEntity>>
}

@Dao
interface BildirimDao {
    @Insert
    suspend fun ekle(bildirim: BildirimEntity)

    @Query("SELECT * FROM bildirim ORDER BY zaman DESC LIMIT :adet")
    fun sonlarAkisi(adet: Int): Flow<List<BildirimEntity>>

    @Query("SELECT COUNT(*) FROM bildirim WHERE okundu = 0")
    fun okunmamisSayisiAkisi(): Flow<Int>

    @Query("UPDATE bildirim SET okundu = 1 WHERE okundu = 0")
    suspend fun tumunuOku(): Int

    /**
     * Aynı olayın iki kez yazılmasını engeller — worker gün içinde birden çok
     * kez koşabiliyor (bkz. [KullanimOlayDao.gununuSil] gerekçesi).
     */
    @Query("SELECT COUNT(*) FROM bildirim WHERE tur = :tur AND zaman >= :zamandanSonra")
    suspend fun sayi(tur: String, zamandanSonra: java.time.Instant): Int

    /** Defter sonsuza kadar büyümesin. */
    @Query("DELETE FROM bildirim WHERE zaman < :zaman")
    suspend fun eskileriSil(zaman: java.time.Instant): Int
}

@Dao
interface ArkadasDao {
    @Upsert
    suspend fun kaydet(arkadas: ArkadasEntity)

    @Query("SELECT * FROM arkadas ORDER BY eklenmeTarihi DESC")
    fun hepsiAkisi(): Flow<List<ArkadasEntity>>

    @Query("SELECT COUNT(*) FROM arkadas")
    fun sayiAkisi(): Flow<Int>

    @Query("DELETE FROM arkadas WHERE isim = :isim")
    suspend fun sil(isim: String)
}

@Dao
interface AdimSayacDao {
    @Upsert
    suspend fun kaydet(kayit: AdimSayacEntity)

    @Query("SELECT * FROM adim_sayac WHERE tarih = :tarih")
    suspend fun gunun(tarih: LocalDate): AdimSayacEntity?

    /** Sayaç birikimi geçmişe dönük anlam taşımıyor; eskiler temizlenir. */
    @Query("DELETE FROM adim_sayac WHERE tarih < :tarih")
    suspend fun eskileriSil(tarih: LocalDate): Int
}
