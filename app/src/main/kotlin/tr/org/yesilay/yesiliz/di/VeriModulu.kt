package tr.org.yesilay.yesiliz.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tr.org.yesilay.yesiliz.data.depo.KategoriDeposu
import tr.org.yesilay.yesiliz.data.kullanim.KullanimOkuyucu
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb
import tr.org.yesilay.yesiliz.data.yerel.dao.AnketYanitDao
import tr.org.yesilay.yesiliz.data.yerel.dao.GorevTamamlamaDao
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.KategoriEslemeDao
import tr.org.yesilay.yesiliz.data.yerel.dao.KullanimOlayDao
import tr.org.yesilay.yesiliz.data.yerel.dao.PuanGunlukDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton

/**
 * Veri katmanının Hilt bağlantıları (CLAUDE.md §6).
 *
 * Depo sınıflarının kendisi `@Inject constructor` ile işaretli; burada
 * yalnızca **Hilt'in kendi başına üretemeyeceği** tipler sağlanıyor:
 * Room, DAO'lar, sistem servisleri ve `ZoneId`.
 */
@Module
@InstallIn(SingletonComponent::class)
object VeriModulu {

    /**
     * Kullanıcının zaman dilimi.
     *
     * Sabit değil, çağrı anında okunmuyor — enjekte ediliyor ki testler
     * belirlenimci olsun (`KullanimOkuyucu` ve `OruntuCikar` gün sınırlarını
     * buna göre kuruyor).
     */
    @Provides
    @Singleton
    fun zon(): ZoneId = ZoneId.systemDefault()

    /**
     * Saat de enjekte ediliyor — `LocalDate.now()` çıplak çağrısı testte
     * bugünü sabitlemeyi imkânsız kılıyor. Worker ve depo katmanı gün
     * sınırlarını buradan alıyor.
     */
    @Provides
    @Singleton
    fun saat(zon: ZoneId): Clock = Clock.system(zon)

    @Provides
    @Singleton
    fun veritabani(@ApplicationContext baglam: Context): YesilizDb = YesilizDb.al(baglam)

    @Provides
    fun kullanimOlayDao(db: YesilizDb): KullanimOlayDao = db.kullanimOlayDao()

    @Provides
    fun gunlukOzetDao(db: YesilizDb): GunlukOzetDao = db.gunlukOzetDao()

    @Provides
    fun riskSonucDao(db: YesilizDb): RiskSonucDao = db.riskSonucDao()

    @Provides
    fun anketYanitDao(db: YesilizDb): AnketYanitDao = db.anketYanitDao()

    @Provides
    fun kategoriEslemeDao(db: YesilizDb): KategoriEslemeDao = db.kategoriEslemeDao()

    @Provides
    fun gorevTamamlamaDao(db: YesilizDb): GorevTamamlamaDao = db.gorevTamamlamaDao()

    @Provides
    fun puanGunlukDao(db: YesilizDb): PuanGunlukDao = db.puanGunlukDao()

    @Provides
    @Singleton
    fun kullanimOkuyucu(
        @ApplicationContext baglam: Context,
        zon: ZoneId,
    ): KullanimOkuyucu = KullanimOkuyucu.olustur(baglam, zon)

    /**
     * Kategori tablosu `assets/kategori_tablosu.json`'dan okunuyor.
     *
     * 3.106 satırlık dosya; okuma IO dispatcher'a alınıyor. `KategoriDeposu`
     * bunu yalnızca ilk seed'de çağırıyor, sonraki açılışlarda Room dolu
     * olduğu için hiç okunmuyor.
     */
    @Provides
    @Singleton
    fun kategoriDeposu(
        @ApplicationContext baglam: Context,
        dao: KategoriEslemeDao,
    ): KategoriDeposu = KategoriDeposu(
        dao = dao,
        tabloOkuyucu = {
            withContext(Dispatchers.IO) {
                baglam.assets.open(KATEGORI_TABLOSU).bufferedReader().use { it.readText() }
            }
        },
    )

    private const val KATEGORI_TABLOSU = "kategori_tablosu.json"
}
