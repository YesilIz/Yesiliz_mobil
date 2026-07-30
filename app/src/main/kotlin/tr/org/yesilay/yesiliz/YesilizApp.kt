package tr.org.yesilay.yesiliz

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * DI kökü (CLAUDE.md §6).
 *
 * Ayrıca WorkManager'ın **isteğe bağlı** başlatma noktası: `@HiltWorker`
 * ile işaretli worker'lara bağımlılık enjekte edilebilmesi için WorkManager'ın
 * varsayılan fabrikası yerine [HiltWorkerFactory] kullanılıyor.
 *
 * Bu, manifest'teki `WorkManagerInitializer` sağlayıcısının kaldırılmasını
 * gerektiriyor — yoksa WorkManager kendini varsayılan yapılandırmayla
 * başlatır ve buradaki fabrika hiç devreye girmez.
 */
@HiltAndroidApp
class YesilizApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFabrikasi: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFabrikasi)
            .build()
}
