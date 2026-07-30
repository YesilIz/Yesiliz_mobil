package tr.org.yesilay.yesiliz.data.yerel

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import tr.org.yesilay.yesiliz.data.yerel.dao.AnketYanitDao
import tr.org.yesilay.yesiliz.data.yerel.dao.GorevTamamlamaDao
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.KategoriEslemeDao
import tr.org.yesilay.yesiliz.data.yerel.dao.KullanimOlayDao
import tr.org.yesilay.yesiliz.data.yerel.dao.PuanGunlukDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.yerel.entity.AnketYanitEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.GorevTamamlamaEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.KategoriEslemeEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.KullanimOlayEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.PuanGunlukEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.RiskSonucEntity

/**
 * Cihaz üzerindeki tek veritabanı. **Ham veri buradan çıkmaz** (CLAUDE.md §4).
 *
 * Lig ve ebeveyn tabloları (CLAUDE.md §6) Faz 2'de eklenecek — MVP'de
 * `YerelSahteLigDeposu` sabit veriyle çalışıyor, tabloya ihtiyaç yok.
 */
@Database(
    entities = [
        KullanimOlayEntity::class,
        GunlukOzetEntity::class,
        RiskSonucEntity::class,
        AnketYanitEntity::class,
        KategoriEslemeEntity::class,
        GorevTamamlamaEntity::class,
        PuanGunlukEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Donusturucu::class)
abstract class YesilizDb : RoomDatabase() {

    abstract fun kullanimOlayDao(): KullanimOlayDao
    abstract fun gunlukOzetDao(): GunlukOzetDao
    abstract fun riskSonucDao(): RiskSonucDao
    abstract fun anketYanitDao(): AnketYanitDao
    abstract fun kategoriEslemeDao(): KategoriEslemeDao
    abstract fun gorevTamamlamaDao(): GorevTamamlamaDao
    abstract fun puanGunlukDao(): PuanGunlukDao

    companion object {
        private const val AD = "yesiliz.db"

        @Volatile
        private var ornek: YesilizDb? = null

        /**
         * Prototip boyunca `fallbackToDestructiveMigration` (CLAUDE.md §6):
         * şema sık değişecek, gerçek migration stratejisi yarışma sonrasına
         * bırakıldı. **Yayına çıkmadan önce mutlaka değiştirilmeli** —
         * yoksa her güncellemede kullanıcının geçmişi silinir.
         */
        fun al(baglam: Context): YesilizDb = ornek ?: synchronized(this) {
            ornek ?: Room.databaseBuilder(
                baglam.applicationContext,
                YesilizDb::class.java,
                AD,
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { ornek = it }
        }
    }
}
