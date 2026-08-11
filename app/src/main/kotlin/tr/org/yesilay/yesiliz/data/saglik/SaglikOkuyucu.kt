package tr.org.yesilay.yesiliz.data.saglik

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import tr.org.yesilay.yesiliz.data.depo.GunlukAktivite
import tr.org.yesilay.yesiliz.domain.kullanim.AktiviteKaynagi
import tr.org.yesilay.yesiliz.domain.kullanim.AktiviteOturumu
import java.time.Clock
import java.time.LocalDate
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
    private val saat: Clock,
) {

    /**
     * Okuma izinleri — bunlar olmadan hiçbir şey okunamıyor.
     * [izinVarMi] yalnızca bunlara bakıyor.
     */
    val gerekliIzinler: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    /**
     * İzin diyaloğunda istenecek küme: okuma izinleri **+** arka planda
     * okuma.
     *
     * Arka plan izni ayrı tutuluyor çünkü kullanıcı okumaya izin verip
     * buna vermeyebilir; [gerekliIzinler]'e konsaydı o durumda ön plandaki
     * okuma da kapanırdı. Cihazın Health Connect sürümü bu yeteneği
     * tanımıyorsa istenmiyor — tanımadığı bir izin isteği diyaloğu
     * komple başarısız kılıyor.
     */
    fun istenenIzinler(): Set<String> =
        if (arkaPlanDestekli()) gerekliIzinler + ARKA_PLAN_IZNI else gerekliIzinler

    /**
     * Arka planda okuma izni verildi mi.
     *
     * Verilmediğinde `GunlukOzetWorker` ve `YuruyusServisi` okuyamıyor;
     * izin ekranı bunu kullanıcıya söylüyor. Cihaz yeteneği hiç yoksa
     * "verilmiş" sayılıyor: istenemeyen bir izni eksik göstermenin
     * kullanıcıya söyleyeceği bir şey yok.
     */
    suspend fun arkaPlanIzniVarMi(): Boolean {
        if (!arkaPlanDestekli()) return true
        val istemci = istemci() ?: return false
        return runCatching {
            istemci.permissionController.getGrantedPermissions().contains(ARKA_PLAN_IZNI)
        }.getOrElse { hata ->
            if (hata is CancellationException) throw hata
            false
        }
    }

    private fun arkaPlanDestekli(): Boolean {
        val istemci = istemci() ?: return false
        return runCatching {
            istemci.features.getFeatureStatus(
                HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND,
            ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        }.getOrDefault(false)
    }

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
        }.getOrElse { hata ->
            if (hata is CancellationException) throw hata
            false
        }
    }

    /**
     * Bir günün adım sayısı. Veri yoksa [GunlukAktivite] sıfırlarla döner —
     * çağıranın hata yolu yazması gerekmiyor.
     *
     * Egzersiz süresi burada YOK: tek bir toplam sayı, hangi oturumun
     * uygulamanın kendi yürüyüşüyle çakıştığını ayırt edemez. Onun için
     * [gununEgzersizOturumlari] — [tr.org.yesilay.yesiliz.domain.kullanim.AktiviteTekillestir]
     * çakışmayı oturum bazında hesaplıyor (`gorev2.md` §4-§6).
     */
    suspend fun gununAktivitesi(tarih: LocalDate): GunlukAktivite {
        val istemci = istemci() ?: return GunlukAktivite()
        if (!izinVarMi()) return GunlukAktivite()

        val aralik = gunAraligi(tarih)

        return runCatching {
            val sonuc: AggregationResult = istemci.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = aralik,
                ),
            )
            GunlukAktivite(adim = (sonuc[StepsRecord.COUNT_TOTAL] ?: 0L).toDouble())
        }.getOrElse { hata ->
            if (hata is CancellationException) throw hata
            // Sessiz `getOrDefault` bu kusuru aylarca sakladı: arka plan
            // okuması reddediliyordu, çağıran sıfır alıyordu ve hiçbir yerde
            // sebebi yazmıyordu. Sıfır dönmeye devam ediyor (güvenli taraf),
            // ama artık niçin sıfır olduğu logcat'te duruyor.
            Log.w(ETIKET, "Health Connect okunamadı ($tarih) — aktivite sıfır sayılıyor", hata)
            GunlukAktivite()
        }
    }

    /**
     * O güne ait Health Connect egzersiz oturumları — ortak aktivite
     * modeline çevrilmiş (`gorev2.md` §4). Her kaydın [AktiviteOturumu.disKimlik]'i
     * Health Connect'in kendi `Metadata.id`'si: aynı kaydın iki kez
     * senkronize edilmesi [tr.org.yesilay.yesiliz.domain.kullanim.AktiviteTekillestir]'de
     * bununla yakalanıyor.
     *
     * Veri yoksa boş liste — [gununAktivitesi] ile aynı "sessiz yok" ilkesi.
     */
    suspend fun gununEgzersizOturumlari(tarih: LocalDate): List<AktiviteOturumu> {
        val istemci = istemci() ?: return emptyList()
        if (!izinVarMi()) return emptyList()

        return runCatching {
            istemci.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = gunAraligi(tarih),
                ),
            ).records.map {
                AktiviteOturumu(
                    baslangic = it.startTime,
                    bitis = it.endTime,
                    kaynak = AktiviteKaynagi.HEALTH_CONNECT,
                    disKimlik = it.metadata.id,
                )
            }
        }.getOrElse { hata ->
            if (hata is CancellationException) throw hata
            Log.w(ETIKET, "Health Connect egzersiz oturumları okunamadı ($tarih)", hata)
            emptyList()
        }
    }

    private fun gunAraligi(tarih: LocalDate): TimeRangeFilter {
        val zon = saat.zone
        return TimeRangeFilter.between(
            tarih.atStartOfDay(zon).toLocalDateTime(),
            tarih.plusDays(1).atStartOfDay(zon).toLocalDateTime(),
        )
    }

    private fun istemci(): HealthConnectClient? =
        if (kullanilabilir()) {
            runCatching { HealthConnectClient.getOrCreate(baglam) }.getOrNull()
        } else {
            null
        }

    private companion object {
        const val ETIKET = "YesilizSaglik"
        val ARKA_PLAN_IZNI: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
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
