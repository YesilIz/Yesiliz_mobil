package tr.org.yesilay.yesiliz.data.yerel

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import tr.org.yesilay.yesiliz.data.yerel.dao.AdimSayacDao
import tr.org.yesilay.yesiliz.data.yerel.dao.AktiviteOturumuDao
import tr.org.yesilay.yesiliz.data.yerel.dao.AnketYanitDao
import tr.org.yesilay.yesiliz.data.yerel.dao.ArkadasDao
import tr.org.yesilay.yesiliz.data.yerel.dao.BildirimDao
import tr.org.yesilay.yesiliz.data.yerel.dao.FarkindalikTestiDao
import tr.org.yesilay.yesiliz.data.yerel.dao.GorevTamamlamaDao
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.KategoriEslemeDao
import tr.org.yesilay.yesiliz.data.yerel.dao.KullanimOlayDao
import tr.org.yesilay.yesiliz.data.yerel.dao.OdulSecimiDao
import tr.org.yesilay.yesiliz.data.yerel.dao.PuanGunlukDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RuhHaliDao
import tr.org.yesilay.yesiliz.data.yerel.dao.UykuBeyaniDao
import tr.org.yesilay.yesiliz.data.yerel.entity.AdimSayacEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.AktiviteOturumuEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.AnketYanitEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.ArkadasEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.BildirimEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.FarkindalikTestiEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.GorevTamamlamaEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.KategoriEslemeEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.KullanimOlayEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.OdulSecimiEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.PuanGunlukEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.RiskSonucEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.RuhHaliEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.UykuBeyaniEntity

/**
 * Cihaz üzerindeki tek veritabanı. **Ham veri buradan çıkmaz** (CLAUDE.md §4).
 *
 * Lig üyeliği ve ebeveyn bağlantısı (CLAUDE.md §6) hâlâ Faz 2'de — sıralama
 * sunucudan gelecek. [ArkadasEntity] onun yerine geçmiyor: yalnızca lig
 * sekmesinin boş/dolu durumunu ve davet defterini yerelde tutuyor.
 *
 * **Sürüm 2** (30 Tem 2026): ruh_hali · uyku_beyani · farkindalik_testi ·
 * bildirim · arkadas tabloları eklendi.
 *
 * **Sürüm 4** (3 Ağu 2026): adim_sayac — donanım adım sayacının günlük
 * birikimi (`debug.md` K-0). Sürüm 3 `gunluk_ozet.olayVarMi` kolonuydu.
 *
 * **Sürüm 5** (4 Ağu 2026): odul_secimi — bazı ödül kademelerinde market
 * kuponu yerine fidan bağışı gibi alternatif seçenekler.
 *
 * **Sürüm 6** (5 Ağu 2026): risk_sonuc.veriDurumu — yetersiz ölçümlü bir
 * günün "AZ RİSKLİ" olarak gösterilmesini engelliyor (`gorev2.md` §8,
 * `Gecisler.kt` GECIS_5_6).
 *
 * **Sürüm 7** (6 Ağu 2026): aktivite_oturumu — uygulamanın kendi yürüyüş
 * oturumları artık kalıcı; Health Connect'in egzersiz kayıtlarıyla birlikte
 * [tr.org.yesilay.yesiliz.domain.kullanim.AktiviteTekillestir]'e giriyor
 * (`gorev2.md` §4-§6, `Gecisler.kt` GECIS_6_7).
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
        RuhHaliEntity::class,
        UykuBeyaniEntity::class,
        FarkindalikTestiEntity::class,
        BildirimEntity::class,
        ArkadasEntity::class,
        AdimSayacEntity::class,
        OdulSecimiEntity::class,
        AktiviteOturumuEntity::class,
    ],
    version = 7,
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
    abstract fun ruhHaliDao(): RuhHaliDao
    abstract fun uykuBeyaniDao(): UykuBeyaniDao
    abstract fun farkindalikTestiDao(): FarkindalikTestiDao
    abstract fun bildirimDao(): BildirimDao
    abstract fun arkadasDao(): ArkadasDao
    abstract fun adimSayacDao(): AdimSayacDao
    abstract fun odulSecimiDao(): OdulSecimiDao
    abstract fun aktiviteOturumuDao(): AktiviteOturumuDao

    companion object {
        private const val AD = "yesiliz.db"

        @Volatile
        private var ornek: YesilizDb? = null

        /**
         * **Yıkıcı geri düşüş KALDIRILDI** (31 Tem 2026).
         *
         * Artık gerçek `Migration` nesneleri kullanılıyor (`Gecisler.kt`).
         * Bir şema değişikliği için geçiş yazılmazsa uygulama açılışta
         * çöküyor — sessizce kullanıcının geçmişini silmektense gürültülü
         * başarısız olmak yeğ. Ekran süresi geçmişi bu uygulamanın
         * biriktirdiği tek değerli şey.
         *
         * Entity değiştiren her PR bir geçiş de yazmak zorunda (CLAUDE.md §7).
         */
        fun al(baglam: Context): YesilizDb = ornek ?: synchronized(this) {
            ornek ?: Room.databaseBuilder(
                baglam.applicationContext,
                YesilizDb::class.java,
                AD,
            )
                .addMigrations(*TUM_GECISLER)
                .build()
                .also { ornek = it }
        }
    }
}
