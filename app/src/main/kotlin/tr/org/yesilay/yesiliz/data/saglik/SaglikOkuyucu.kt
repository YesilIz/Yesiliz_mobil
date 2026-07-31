package tr.org.yesilay.yesiliz.data.saglik

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import tr.org.yesilay.yesiliz.data.depo.GunlukAktivite
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health Connect'ten günlük adım ve egzersiz süresi.
 *
 * ## Neden hepsi "yoksa sıfır"
 *
 * Aktivite sönümlemesi risk skorunu **düşürüyor** (tavan %15). Veri
 * alınamadığında uydurma bir aktivite varsaymak skoru haksız yere düşürür;
 * sıfır dönmek `A = 0` → `D = 1.0` demek, yani sönümleme uygulanmaz. Bu
 * güvenli tarafta kalan varsayılan.
 *
 * Üç ayrı yokluk hali var ve üçü de aynı sonuca çıkıyor:
 *  - Health Connect cihazda kurulu değil (emülatörün varsayılan hali)
 *  - kullanıcı izin vermedi
 *  - okuma sırasında hata
 *
 * ## Mahremiyet
 *
 * Yalnızca **adım sayısı** ve **egzersiz süresi** okunuyor. Kalp atışı, uyku,
 * kilo, konum içeren egzersiz rotası HİÇ istenmiyor (CLAUDE.md §4).
 */
@Singleton
class SaglikOkuyucu @Inject constructor(
    @ApplicationContext private val baglam: Context,
    private val zon: ZoneId,
) {

    /** Uygulamanın istediği izinler — Profil ekranındaki istek bunu kullanıyor. */
    val gerekliIzinler: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    /**
     * Health Connect'in bu cihazdaki durumu.
     *
     * Düz `Boolean` yetmiyordu: "kurulu değil" ile "güncellemen gerek"
     * ayırt edilemediği için ekran ikisinde de Play Store'a yönlendirip
     * "kurulu değil" diyordu (`debug.md` K-6). İki durumun çözümü farklı.
     */
    fun durum(): SaglikDurumu = when (HealthConnectClient.getSdkStatus(baglam)) {
        HealthConnectClient.SDK_AVAILABLE -> SaglikDurumu.HAZIR
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
            SaglikDurumu.GUNCELLEME_GEREKLI

        else -> SaglikDurumu.YOK
    }

    fun kullanilabilir(): Boolean = durum() == SaglikDurumu.HAZIR

    suspend fun izinVarMi(): Boolean {
        val istemci = istemci() ?: return false
        return runCatching {
            istemci.permissionController.getGrantedPermissions().containsAll(gerekliIzinler)
        }.getOrDefault(false)
    }

    /**
     * Bir günün aktivitesi. Veri yoksa [GunlukAktivite] sıfırlarla döner —
     * çağıranın hata yolu yazması gerekmiyor.
     */
    suspend fun gununAktivitesi(tarih: LocalDate): GunlukAktivite {
        val istemci = istemci() ?: return GunlukAktivite()
        if (!izinVarMi()) return GunlukAktivite()

        val aralik = TimeRangeFilter.between(
            tarih.atStartOfDay(zon).toLocalDateTime(),
            tarih.plusDays(1).atStartOfDay(zon).toLocalDateTime(),
        )

        return runCatching {
            val sonuc: AggregationResult = istemci.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                    ),
                    timeRangeFilter = aralik,
                ),
            )
            GunlukAktivite(
                adim = (sonuc[StepsRecord.COUNT_TOTAL] ?: 0L).toDouble(),
                sporDk = (sonuc[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes() ?: 0L)
                    .toDouble(),
            )
        }.getOrDefault(GunlukAktivite())
    }

    private fun istemci(): HealthConnectClient? =
        if (kullanilabilir()) {
            runCatching { HealthConnectClient.getOrCreate(baglam) }.getOrNull()
        } else {
            null
        }
}

/**
 * Health Connect'in cihazdaki durumu.
 *
 * Üç durumun üç ayrı çözümü var; tek bir `Boolean` ikisini birbirine
 * karıştırıyordu (`debug.md` K-6).
 */
enum class SaglikDurumu {
    /** Kurulu ve kullanılabilir — izin istenebilir. */
    HAZIR,

    /** Kurulu ama sürümü eski; kullanıcının güncellemesi gerekiyor. */
    GUNCELLEME_GEREKLI,

    /** Kurulu değil ya da bu cihazda desteklenmiyor. */
    YOK,
}
