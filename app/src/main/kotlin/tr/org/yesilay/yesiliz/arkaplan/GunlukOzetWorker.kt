package tr.org.yesilay.yesiliz.arkaplan

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import tr.org.yesilay.yesiliz.data.depo.BildirimDeposu
import tr.org.yesilay.yesiliz.data.depo.KullanimDeposu
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.data.depo.RiskDeposu
import tr.org.yesilay.yesiliz.data.kullanim.KullanimIzni
import tr.org.yesilay.yesiliz.data.saglik.SaglikOkuyucu
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.KullanimOlayDao
import tr.org.yesilay.yesiliz.widget.widgetlariTazele
import java.time.Clock
import java.time.LocalDate
import kotlin.math.roundToInt

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
    private val bildirimDeposu: BildirimDeposu,
    private val tercihler: tr.org.yesilay.yesiliz.data.tercih.Tercihler,
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

            // Bildirim defterine bugünün kazanımları. `birKezGunde` koruması
            // sayesinde worker gün içinde tekrar koşsa da satır katlanmıyor.
            bugunuBildir(bugun)

            // Ham olaylar sonsuza kadar tutulmuyor (veri minimizasyonu,
            // CLAUDE.md §4). Özet zaten çıkarıldı.
            olayDao.eskileriSil(bugun.minusDays(HAM_OLAY_SAKLAMA_GUN))

            // Bildirim defteri de sınırsız büyümüyor.
            bildirimDeposu.eskileriTemizle()

            // Widget'lar özet yazıldıktan sonra tazeleniyor. Kendi periyodik
            // güncellemelerini beklemek, ana ekranda saatlerce eski sayı
            // göstermek demek (updatePeriodMillis zaten 0).
            widgetlariTazele(applicationContext)

            Result.success()
        }.getOrElse { Result.retry() }
    }

    /**
     * Bugünün olumlu sonuçlarını bildirim defterine yazar.
     *
     * **Yalnızca kazanımlar.** "Hedefini aştın" diye bir satır bilinçli
     * olarak yok — CLAUDE.md §4: ceza suçluluk üretir. Kötü giden bir günü
     * kullanıcıya bildirimle hatırlatmak, uygulamayı sildiren davranış.
     */
    private suspend fun bugunuBildir(bugun: LocalDate) {
        val ozet = ozetDao.getir(bugun) ?: return
        val hedef = tercihler.anlik().gunlukHedefDk
        val ekranDk = ozet.let {
            it.socialDk + it.entertainmentDk + it.gamesDk + it.communicationDk +
                it.productivityDk + it.educationDk + it.healthDk + it.otherDk
        }.roundToInt()

        if (ekranDk <= hedef) {
            bildirimDeposu.hedefTutturuldu(ekranDk, hedef)
        }

        val seri = puanDeposu.seriGunu(bugun)
        if (seri in SERI_ESIKLERI) {
            bildirimDeposu.seriIlerledi(seri)
        }
    }

    companion object {
        /** Serinin kutlandığı gün eşikleri — her gün bildirim göndermiyoruz. */
        val SERI_ESIKLERI = setOf(3, 7, 14, 30, 60, 100)

        /** Kaçırılan günler için geriye dönük tarama penceresi. */
        const val TELAFI_GUN = 7L

        /** Ham olay saklama süresi; özet kalıcı, ayrıntı değil. */
        const val HAM_OLAY_SAKLAMA_GUN = 30L

        const val IS_ADI = "gunluk_ozet"
    }
}
