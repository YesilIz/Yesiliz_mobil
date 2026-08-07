package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tr.org.yesilay.yesiliz.data.yerel.dao.AktiviteOturumuDao
import tr.org.yesilay.yesiliz.data.yerel.entity.AktiviteOturumuEntity
import tr.org.yesilay.yesiliz.domain.kullanim.AktiviteKaynagi
import tr.org.yesilay.yesiliz.domain.kullanim.AktiviteOturumu
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uygulamanın kendi kaydettiği aktivite oturumları (`aktivite_oturumu`).
 *
 * `YuruyusServisi` bir yürüyüş tamamlandığında buraya yazıyor,
 * `AktiviteSaglayici` günün toplam spor dakikasını hesaplarken Health
 * Connect kayıtlarıyla birlikte buradan okuyor (`gorev2.md` §4).
 */
@Singleton
class AktiviteOturumuDeposu @Inject constructor(
    private val dao: AktiviteOturumuDao,
    private val saat: Clock,
) {

    /**
     * Tamamlanan bir yürüyüşü kaydeder. `bitis` başlangıçtan önce olamaz —
     * [AktiviteOturumu.init] bunu doğruluyor.
     */
    suspend fun kaydet(baslangic: Instant, bitis: Instant) = withContext(Dispatchers.IO) {
        // Doğrulama için gerçek modeli kur: negatif/gerçek dışı süreler
        // burada da reddedilsin, yalnızca [AktiviteTekillestir] çağrılırken değil.
        val oturum = AktiviteOturumu(baslangic, bitis, AktiviteKaynagi.UYGULAMA_YURUYUSU)
        dao.ekle(
            AktiviteOturumuEntity(
                baslangic = oturum.baslangic,
                bitis = oturum.bitis,
                tarih = LocalDate.ofInstant(bitis, saat.zone),
            ),
        )
    }

    /** O güne ait uygulama yürüyüşleri, ortak aktivite modeline çevrilmiş. */
    suspend fun gununOturumlari(tarih: LocalDate): List<AktiviteOturumu> = withContext(Dispatchers.IO) {
        dao.gununOturumlari(tarih).map {
            AktiviteOturumu(it.baslangic, it.bitis, AktiviteKaynagi.UYGULAMA_YURUYUSU)
        }
    }

    suspend fun eskileriSil(sinir: LocalDate) = withContext(Dispatchers.IO) {
        dao.eskileriSil(sinir)
    }
}
