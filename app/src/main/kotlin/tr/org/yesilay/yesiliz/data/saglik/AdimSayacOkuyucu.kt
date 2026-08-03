package tr.org.yesilay.yesiliz.data.saglik

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Donanım adım sayacı (`Sensor.TYPE_STEP_COUNTER`).
 *
 * ## Niçin var
 *
 * Health Connect adım **saymıyor**, saklıyor. Yazan bir uygulama (Samsung
 * Health, Google Fit) yoksa okuma sonsuza kadar sıfır dönüyor ve test
 * telefonunda tam olarak bu oldu — Health Connect "hiç veriniz yok" diyordu
 * (`debug.md` K-0). Donanım sayacı üçüncü parti hiçbir şeye bağlı değil;
 * telefonun kendi çipi sayıyor.
 *
 * Health Connect'in **yerine** geçmiyor, **yedeği**: veri oradan geliyorsa
 * o kazanıyor (bkz. [AktiviteSaglayici]). Health Connect'te başka
 * uygulamaların yazdığı, saatten/bileklikten gelen daha iyi veri olabilir.
 *
 * ## Ne verir, ne vermez
 *
 * Verdiği tek şey **açılıştan beri biriken** kümülatif sayı. "Bugün kaç
 * adım" sorusunun cevabı sensörde yok, geçmiş de yok — kurulumdan önceki
 * günler için hiçbir şey söyleyemiyor. Güne düşen payı [adimBirikimGuncelle]
 * hesaplıyor.
 *
 * ## Mahremiyet
 *
 * Okunan tek şey bir tam sayı. Konum, hız, hareket izi yok (CLAUDE.md §4).
 */
@Singleton
class AdimSayacOkuyucu @Inject constructor(
    @ApplicationContext private val baglam: Context,
) {

    private val yonetici: SensorManager? =
        ContextCompat.getSystemService(baglam, SensorManager::class.java)

    private val sensor: Sensor?
        get() = yonetici?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    /** Cihazda donanım sayacı var mı. Emülatörde ve eski cihazlarda yok. */
    fun cihazDestekliyor(): Boolean = sensor != null

    /**
     * Çalışma zamanı izni.
     *
     * `ACTIVITY_RECOGNITION` Android 10'dan (API 29) beri **tehlikeli** izin;
     * manifest'te bildirmek yetmiyor, kullanıcının vermesi gerekiyor. Altında
     * izin diye bir şey yok, o yüzden verilmiş sayılıyor.
     */
    fun izinVarMi(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(baglam, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun kullanilabilir(): Boolean = cihazDestekliyor() && izinVarMi()

    /**
     * Sayacın o anki ham değeri; okunamazsa `null`.
     *
     * `TYPE_STEP_COUNTER` bir **on-change** sensörü: kaydolur kaydolmaz son
     * bilinen değeri gönderiyor, yeni adım beklemiyor. Yine de zaman aşımı
     * var — bazı cihazlarda ilk olay gecikebiliyor ve süresiz askıda kalan
     * bir çağrı gecelik işi bloke ederdi.
     */
    suspend fun anlikSayac(): Long? {
        val yonetici = yonetici ?: return null
        val sensor = sensor ?: return null
        if (!izinVarMi()) return null

        return try {
            withTimeout(ZAMAN_ASIMI_MS) {
                suspendCancellableCoroutine { devam ->
                    val dinleyici = object : SensorEventListener {
                        override fun onSensorChanged(olay: SensorEvent) {
                            val deger = olay.values.firstOrNull()?.toLong()
                            yonetici.unregisterListener(this)
                            if (devam.isActive) devam.resume(deger)
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, dogruluk: Int) = Unit
                    }
                    yonetici.registerListener(dinleyici, sensor, SensorManager.SENSOR_DELAY_UI)
                    devam.invokeOnCancellation { yonetici.unregisterListener(dinleyici) }
                }
            }
        } catch (_: TimeoutCancellationException) {
            null
        }
    }

    private companion object {
        const val ZAMAN_ASIMI_MS = 3_000L
    }
}
