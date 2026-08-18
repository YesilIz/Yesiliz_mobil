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
import tr.org.yesilay.yesiliz.data.saglik.AdimKaynagi
import tr.org.yesilay.yesiliz.data.saglik.AdimSayacOkuyucu
import tr.org.yesilay.yesiliz.data.zaman.DegiskenSistemSaati
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb
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
import java.time.Clock
import javax.inject.Singleton

/**
 * Veri katmanının Hilt bağlantıları (CLAUDE.md §6).
 *
 * Depo sınıflarının kendisi `@Inject constructor` ile işaretli; burada
 * yalnızca **Hilt'in kendi başına üretemeyeceği** tipler sağlanıyor:
 * Room, DAO'lar ve sistem servisleri.
 */
@Module
@InstallIn(SingletonComponent::class)
object VeriModulu {

    /**
     * Saat de enjekte ediliyor — `LocalDate.now()` çıplak çağrısı testte
     * bugünü sabitlemeyi imkânsız kılıyor. Worker ve depo katmanı gün
     * sınırlarını buradan alıyor.
     */
    @Provides
    @Singleton
    fun saat(): Clock = DegiskenSistemSaati()

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
    fun ruhHaliDao(db: YesilizDb): RuhHaliDao = db.ruhHaliDao()

    @Provides
    fun uykuBeyaniDao(db: YesilizDb): UykuBeyaniDao = db.uykuBeyaniDao()

    @Provides
    fun farkindalikTestiDao(db: YesilizDb): FarkindalikTestiDao = db.farkindalikTestiDao()

    @Provides
    fun adimSayacDao(db: YesilizDb): AdimSayacDao = db.adimSayacDao()

    @Provides
    fun bildirimDao(db: YesilizDb): BildirimDao = db.bildirimDao()

    @Provides
    fun arkadasDao(db: YesilizDb): ArkadasDao = db.arkadasDao()

    @Provides
    fun odulSecimiDao(db: YesilizDb): OdulSecimiDao = db.odulSecimiDao()

    @Provides
    fun aktiviteOturumuDao(db: YesilizDb): AktiviteOturumuDao = db.aktiviteOturumuDao()

    /**
     * `AdimDeposu` sahte bir kaynakla test edilebilsin diye somut sınıf
     * yerine arayüz istiyor (`AdimDeposuTest`, CLAUDE.md §12 K33).
     */
    @Provides
    fun adimKaynagi(okuyucu: AdimSayacOkuyucu): AdimKaynagi = okuyucu

    @Provides
    @Singleton
    fun kullanimOkuyucu(
        @ApplicationContext baglam: Context,
        saat: Clock,
    ): KullanimOkuyucu = KullanimOkuyucu.olustur(baglam, saat)

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
