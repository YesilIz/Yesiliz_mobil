package tr.org.yesilay.yesiliz.arkaplan

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import tr.org.yesilay.yesiliz.R
import tr.org.yesilay.yesiliz.data.depo.BildirimDeposu
import tr.org.yesilay.yesiliz.data.tercih.Tercihler

/**
 * Günlük hatırlatma bildirimi — Profil › Bildirim saatleri.
 *
 * **Ceza ya da baskı yok.** Metin "bugün X dakika fazla kullandın" demiyor;
 * kullanıcıyı uygulamaya çağıran nötr bir cümle (CLAUDE.md §4). Bildirim
 * dili de risk dili gibi betimleyici.
 *
 * Ayar kapalıysa iş **hiçbir şey yapmadan** başarıyla dönüyor: planı iptal
 * etmek yerine burada kontrol etmek, kullanıcı ayarı kapatıp açtığında
 * planın yeniden kurulmasını beklemeyi gereksiz kılıyor.
 */
@HiltWorker
class HatirlatmaWorker @AssistedInject constructor(
    @Assisted private val baglam: Context,
    @Assisted parametreler: WorkerParameters,
    private val tercihler: Tercihler,
    private val bildirimDeposu: BildirimDeposu,
) : CoroutineWorker(baglam, parametreler) {

    override suspend fun doWork(): Result {
        val saatler = tercihler.bildirimSaatleri.first()
        if (!saatler.acik) return Result.success()

        val aksamMi = inputData.getBoolean(AKSAM_MI, false)
        val baslik = if (aksamMi) "Günü kapatmadan" else "Günaydın"
        val govde = if (aksamMi) {
            "Bugünün özetine ve görevlerine göz atabilirsin."
        } else {
            "Bugün için küçük bir hedef seçmek ister misin?"
        }

        // Defter her hâlükârda yazılıyor — sistem bildirimi izni kapalı
        // olsa bile kullanıcı Bildirimler ekranında görebilsin.
        bildirimDeposu.hatirlatma(baslik, govde)
        sistemBildirimiGonder(baslik, govde, aksamMi)

        return Result.success()
    }

    private fun sistemBildirimiGonder(baslik: String, govde: String, aksamMi: Boolean) {
        if (!izinVarMi()) return

        kanalKur()
        val bildirim = NotificationCompat.Builder(baglam, KANAL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(baslik)
            .setContentText(govde)
            .setStyle(NotificationCompat.BigTextStyle().bigText(govde))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(baglam)
                .notify(if (aksamMi) AKSAM_ID else SABAH_ID, bildirim)
        }
    }

    /** API 33+ çalışma zamanı izni; verilmemişse sessizce vazgeçiliyor. */
    private fun izinVarMi(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(baglam, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun kanalKur() {
        val yonetici = baglam.getSystemService(NotificationManager::class.java) ?: return
        if (yonetici.getNotificationChannel(KANAL_ID) != null) return

        yonetici.createNotificationChannel(
            NotificationChannel(KANAL_ID, "Günlük hatırlatma", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Sabah ve akşam kısa hatırlatmalar" },
        )
    }

    companion object {
        const val SABAH_IS_ADI = "hatirlatma_sabah"
        const val AKSAM_IS_ADI = "hatirlatma_aksam"
        const val AKSAM_MI = "aksam_mi"
        const val KANAL_ID = "hatirlatma"

        private const val SABAH_ID = 4001
        private const val AKSAM_ID = 4002
    }
}
