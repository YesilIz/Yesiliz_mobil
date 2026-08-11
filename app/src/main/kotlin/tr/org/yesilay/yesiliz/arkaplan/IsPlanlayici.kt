package tr.org.yesilay.yesiliz.arkaplan

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ExperimentalWorkRequestBuilderApi
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import tr.org.yesilay.yesiliz.data.tercih.BildirimSaatleri
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
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
    private val saat: Clock,
) {

    private val isYoneticisi get() = WorkManager.getInstance(baglam)

    /**
     * Gecelik işi bir sonraki gerçek yerel 03:00 anına bağlar. `UPDATE`
     * çalışan işi iptal etmeden takvimi yeniler; sabit 24 saat eklemez.
     */
    @OptIn(ExperimentalWorkRequestBuilderApi::class)
    fun gecelikPlanla() {
        val istek = PeriodicWorkRequestBuilder<GunlukOzetWorker>(
            Duration.ofDays(1),
        )
            // Periyodik aralık geçen süreye göredir; DST/seyahat sonrası
            // duvar saatinde kayar. Bir sonraki takvim anını açıkça verip
            // worker bitince yeniden hesaplıyoruz.
            .setNextScheduleTimeOverride(sonrakiHedefAni(saat, CALISMA_SAATI).toEpochMilli())
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
            ExistingPeriodicWorkPolicy.UPDATE,
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
     * Günlük hatırlatmaları planlar.
     *
     * Ayar kapalıysa iki iş de **iptal ediliyor** — kapalı ayarla her gün
     * uyanıp hiçbir şey yapmayan bir iş, pil bütçesini boşa harcar.
     * `HatirlatmaWorker` yine de kendi içinde kontrol ediyor: kullanıcı
     * ayarı değiştirip uygulamayı kapatırsa kuyruktaki iş yanlış davranmasın.
     */
    fun hatirlatmayiPlanla(saatler: BildirimSaatleri) {
        if (!saatler.acik) {
            isYoneticisi.cancelUniqueWork(HatirlatmaWorker.SABAH_IS_ADI)
            isYoneticisi.cancelUniqueWork(HatirlatmaWorker.AKSAM_IS_ADI)
            return
        }

        planla(HatirlatmaWorker.SABAH_IS_ADI, saatler.sabahSaati, aksamMi = false)
        planla(HatirlatmaWorker.AKSAM_IS_ADI, saatler.aksamSaati, aksamMi = true)
    }

    @OptIn(ExperimentalWorkRequestBuilderApi::class)
    private fun planla(isAdi: String, saatDegeri: Int, aksamMi: Boolean) {
        val hedefSaat = LocalTime.of(saatDegeri, 0)
        val istek = PeriodicWorkRequestBuilder<HatirlatmaWorker>(Duration.ofDays(1))
            .setNextScheduleTimeOverride(sonrakiHedefAni(saat, hedefSaat).toEpochMilli())
            .setInputData(workDataOf(HatirlatmaWorker.AKSAM_MI to aksamMi))
            .build()

        // UPDATE: kullanıcı saati değiştirdiğinde plan gerçekten değişsin.
        // KEEP olsaydı ilk seçilen saat kalıcı olurdu ve ayar çalışmazdı.
        isYoneticisi.enqueueUniquePeriodicWork(
            isAdi,
            ExistingPeriodicWorkPolicy.UPDATE,
            istek,
        )
    }

    private companion object {
        val CALISMA_SAATI: LocalTime = LocalTime.of(3, 0)
        const val ACILIS_IS_ADI = "gunluk_ozet_acilis"
    }
}

/** DST boşluk/tekrarlarını saat bölgesinin kurallarına bırakarak sıradaki hedefi bulur. */
internal fun sonrakiHedefAni(saat: Clock, hedefSaat: LocalTime): Instant {
    val simdi = Instant.now(saat)
    val zon = saat.zone
    val yerelSimdi = simdi.atZone(zon)
    var hedef = yerelSimdi.toLocalDate().atTime(hedefSaat).atZone(zon)
    if (!hedef.toInstant().isAfter(simdi)) {
        hedef = yerelSimdi.toLocalDate().plusDays(1).atTime(hedefSaat).atZone(zon)
    }
    return hedef.toInstant()
}
