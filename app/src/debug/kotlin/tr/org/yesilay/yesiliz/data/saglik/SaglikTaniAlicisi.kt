package tr.org.yesilay.yesiliz.data.saglik

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Yalnızca debug APK'da bulunan, gerçek uygulama UID'siyle Health Connect tanısı. */
class SaglikTaniAlicisi : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bekleyen = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val istemci = HealthConnectClient.getOrCreate(context)
                val gerekli = setOf(
                    HealthPermission.getReadPermission(StepsRecord::class),
                    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
                    HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
                )
                val verilen = istemci.permissionController.getGrantedPermissions()
                val aralik = TimeRangeFilter.between(
                    LocalDate.now().minusDays(7).atStartOfDay(),
                    LocalDateTime.now(),
                )
                val kayitSonucu = runCatching {
                    istemci.readRecords(
                        ReadRecordsRequest(
                            recordType = StepsRecord::class,
                            timeRangeFilter = aralik,
                        ),
                    )
                }
                val toplamSonucu = runCatching {
                    istemci.aggregate(
                        AggregateRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter = aralik,
                        ),
                    )[StepsRecord.COUNT_TOTAL]
                }
                val kayitlar = kayitSonucu.getOrNull()?.records.orEmpty()
                val kaynaklar = kayitlar.map { it.metadata.dataOrigin.packageName }.distinct()
                Log.i(
                    ETIKET,
                    "uid=${Process.myUid()} gerekli=$gerekli verilen=$verilen " +
                        "son7GunKayit=${kayitlar.size} kaynaklar=$kaynaklar " +
                        "toplam=${toplamSonucu.getOrNull()} " +
                        "kayitHata=${kayitSonucu.exceptionOrNull()} " +
                        "toplamHata=${toplamSonucu.exceptionOrNull()}",
                )
            } catch (hata: Throwable) {
                Log.e(ETIKET, "Tanı çalışmadı", hata)
            } finally {
                bekleyen.finish()
            }
        }
    }

    private companion object {
        const val ETIKET = "YesilizSaglikTani"
    }
}
