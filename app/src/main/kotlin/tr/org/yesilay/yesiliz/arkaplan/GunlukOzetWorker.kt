package tr.org.yesilay.yesiliz.arkaplan

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import tr.org.yesilay.yesiliz.data.depo.KullanimDeposu
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.data.depo.RiskDeposu
import tr.org.yesilay.yesiliz.data.kullanim.KullanimIzni
import tr.org.yesilay.yesiliz.data.saglik.SaglikOkuyucu
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.KullanimOlayDao
import java.time.Clock
import java.time.LocalDate

/**
 * Günlük özet + risk hesabı — CLAUDE.md §11 md.1.
 *
 * Zincirin tamamı burada koşuyor:
 * ```
 * Health Connect → adım/spor
 * UsageStatsManager → oturumlar → kategori → Room → günlük özet
 * günlük özet → core-risk → risk_sonuc (skor + bant + trend)
 * eski ham olayları sil
 * ```
 *
 * ## Telafi
 *
 * Yalnızca dünü işlemek yetmiyor: kullanıcı telefonu kapatmış, uygulama
 * kısıtlanmış ya da worker ertelenmiş olabilir. Her koşuda son
 * [TELAFI_GUN] günden **işlenmemiş** olanlar da işleniyor. Zaten işlenmiş
 * günler atlanıyor, yani ek maliyet yok.
 *
 * Bugün her koşuda yeniden işleniyor: gün henüz bitmedi, veri artmaya devam
 * ediyor. `gunuIsle` günü silip yeniden yazdığı için tekrar güvenli.
 */
@HiltWorker
class GunlukOzetWorker @AssistedInject constructor(
    @Assisted baglam: Context,
    @Assisted parametreler: WorkerParameters,
    private val kullanimDeposu: KullanimDeposu,
    private val riskDeposu: RiskDeposu,
    private val puanDeposu: PuanDeposu,
    private val saglikOkuyucu: SaglikOkuyucu,
    private val ozetDao: GunlukOzetDao,
    private val olayDao: KullanimOlayDao,
    private val saat: Clock,
) : CoroutineWorker(baglam, parametreler) {

    override suspend fun doWork(): Result {
        // İzin yoksa yapacak bir şey yok. `retry` DEĞİL: izin Ayarlar'dan
        // elle veriliyor, tekrar denemek pili boşuna yakar. İzin verildiğinde
        // uygulama açılışı zaten telafiyi tetikliyor.
        if (!KullanimIzni.verildiMi(applicationContext)) return Result.success()

        val bugun = LocalDate.now(saat)
        val islenmis = ozetDao
            .islenmisGunler(bugun.minusDays(TELAFI_GUN), bugun)
            .toSet()

        return runCatching {
            (TELAFI_GUN downTo 0).forEach { geriGun ->
                val gun = bugun.minusDays(geriGun)
                // Bugün hep yeniden; geçmiş günler yalnızca eksikse.
                if (gun != bugun && gun in islenmis) return@forEach

                kullanimDeposu.gunuIsle(gun, saglikOkuyucu.gununAktivitesi(gun))
                riskDeposu.gunuHesapla(gun)
                // Adım/spor/gece değerleri değişti; günün puanı da değişir.
                puanDeposu.gunuYenidenHesapla(gun)
            }

            // Ham olaylar sonsuza kadar tutulmuyor (veri minimizasyonu,
            // CLAUDE.md §4). Özet zaten çıkarıldı.
            olayDao.eskileriSil(bugun.minusDays(HAM_OLAY_SAKLAMA_GUN))
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        /** Kaçırılan günler için geriye dönük tarama penceresi. */
        const val TELAFI_GUN = 7L

        /** Ham olay saklama süresi; özet kalıcı, ayrıntı değil. */
        const val HAM_OLAY_SAKLAMA_GUN = 30L

        const val IS_ADI = "gunluk_ozet"
    }
}
