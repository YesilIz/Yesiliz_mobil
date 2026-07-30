package tr.org.yesilay.yesiliz.arkaplan

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arka plan işlerinin planlaması.
 *
 * İki tetik var ve ikisi de gerekli:
 *  - **gecelik periyodik**: gün kapandıktan sonra dünü kesinleştirir
 *  - **açılışta tek seferlik**: kullanıcı uygulamayı açtığında bugünü
 *    tazeler ve kaçırılan günleri telafi eder
 *
 * Yalnızca periyodik iş olsaydı uygulamayı ilk kez açan kullanıcı ekranları
 * bir gece boyunca boş görürdü.
 */
@Singleton
class IsPlanlayici @Inject constructor(
    @ApplicationContext private val baglam: Context,
    private val zon: ZoneId,
    private val saat: Clock,
) {

    private val isYoneticisi get() = WorkManager.getInstance(baglam)

    /**
     * Gecelik işi planlar. `KEEP` ile: uygulama her açılışta yeniden
     * planlanmıyor, yoksa periyot sürekli baştan başlar ve iş hiç koşmaz.
     */
    fun gecelikPlanla() {
        val istek = PeriodicWorkRequestBuilder<GunlukOzetWorker>(
            Duration.ofDays(1),
        )
            .setInitialDelay(geceyeKalanSure())
            .setConstraints(
                Constraints.Builder()
                    // Ağ yok, pil kısıtı yok: iş tamamen cihaz içi ve kısa.
                    // Yalnızca cihaz meşgulken beklesin.
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .build()

        isYoneticisi.enqueueUniquePeriodicWork(
            GunlukOzetWorker.IS_ADI,
            ExistingPeriodicWorkPolicy.KEEP,
            istek,
        )
    }

    /**
     * Açılışta hemen bir tazeleme.
     *
     * `REPLACE` değil `KEEP`: kullanıcı uygulamayı hızlıca açıp kapatırsa
     * aynı iş üst üste kuyruğa girmesin.
     */
    fun simdiTazele() {
        isYoneticisi.enqueueUniqueWork(
            ACILIS_IS_ADI,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<GunlukOzetWorker>().build(),
        )
    }

    /**
     * Bir sonraki [CALISMA_SAATI]'ne kalan süre.
     *
     * Gece 03:00 seçildi: gün dönmüş oluyor ve kullanıcı büyük olasılıkla
     * uyuyor, yani işin telefonu yavaşlatması kimseyi rahatsız etmiyor.
     */
    private fun geceyeKalanSure(): Duration {
        val simdi = LocalDate.now(saat).atTime(LocalTime.now(saat)).atZone(zon)
        var hedef = simdi.toLocalDate().atTime(CALISMA_SAATI).atZone(zon)
        if (!hedef.isAfter(simdi)) hedef = hedef.plusDays(1)
        return Duration.between(simdi, hedef)
    }

    private companion object {
        val CALISMA_SAATI: LocalTime = LocalTime.of(3, 0)
        const val ACILIS_IS_ADI = "gunluk_ozet_acilis"
    }
}
