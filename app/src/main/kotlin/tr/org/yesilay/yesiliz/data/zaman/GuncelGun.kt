package tr.org.yesilay.yesiliz.data.zaman

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uygulama açık kalırken takvim gününü ortak ve canlı tutar.
 *
 * ViewModel'lerin kurulurken `LocalDate.now()` değerini saklaması, gece
 * yarısından sonra eski günü okumalarına ve yeni kayıtları düne yazmalarına
 * yol açıyordu. [akis] hem ilk abonelikte hem [yenile] çağrısında saati
 * yeniden okur. Aynı tarih de yeniden yayılır; böylece uygulamaya geri
 * dönüldüğünde saate bağlı etiketler de tazelenir.
 */
@Singleton
class GuncelGun @Inject constructor(
    private val saat: Clock,
) {
    private val yenilemeIsareti = MutableStateFlow(0L)

    val akis: Flow<LocalDate> = yenilemeIsareti.map { bugun() }

    /** Yazma anında daima gerçek günü döndürür. */
    fun bugun(): LocalDate = LocalDate.now(saat)

    /** Activity yeniden öne geldiğinde çağrılır. */
    fun yenile() {
        yenilemeIsareti.update { it + 1L }
    }

    /** Activity öndeyken bir sonraki yerel gece yarısında akışı yeniler. */
    suspend fun geceYarilariniIzle() {
        while (currentCoroutineContext().isActive) {
            delay(sonrakiGuneKalanMillis(saat))
            yenile()
        }
    }
}

/** DST'nin 23/25 saatlik günlerini de doğru hesaplar. */
internal fun sonrakiGuneKalanMillis(saat: Clock): Long {
    val simdi = ZonedDateTime.now(saat)
    val sonrakiGun = simdi.toLocalDate().plusDays(1).atStartOfDay(saat.zone)
    return Duration.between(simdi, sonrakiGun).toMillis().coerceAtLeast(1L)
}
