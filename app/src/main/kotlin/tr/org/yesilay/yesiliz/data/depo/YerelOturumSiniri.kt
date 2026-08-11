package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.JetonDeposu
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb
import java.time.Clock
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aynı telefonda iki hesabın yerel davranış verisini birbirinden ayırır.
 *
 * Room tek cihazlı bir şemadır; tablolarda `kullanici_id` yoktur. Bu
 * nedenle kullanıcı A çıkış yaptıktan sonra B giriş yaparsa veriyi
 * satır bazında ayırmak mümkün değildir: B'nin A'ya ait ekran süresi,
 * risk ve ödülleri görmemesi için yerel alan temizlenir.
 *
 * Aynı hesaba geri dönüşte veri korunur. Misafirden hesap oluşturma ise
 * dönüşümdür; kullanıcının hesap açmadan önce topladığı veri yeni
 * hesabına taşınır.
 */
@Singleton
class YerelOturumSiniri @Inject constructor(
    private val db: YesilizDb,
    private val tercihler: Tercihler,
    private val jetonDeposu: JetonDeposu,
    private val kategoriDeposu: KategoriDeposu,
    private val saat: Clock,
) {
    private val gecisKilidi = Mutex()

    /**
     * Bu özellik gelmeden önce kurulmuş cihazlarda sahip anahtarı yoktur.
     * Var olan oturumdan sahibini bir kez geri doldurur; oturum da yoksa
     * yerel veriyi "bilinmeyen eski sahip" sayar ve sonraki girişte ayırır.
     */
    suspend fun baslat() = withContext(Dispatchers.IO) {
        gecisKilidi.withLock { mevcutSahibiCoz() }
    }

    suspend fun hesabaGec(eposta: String) {
        val yeni = eposta.trim().lowercase(Locale.ROOT)
        gecisYap(yeni)
    }

    suspend fun misafireGec() {
        gecisYap(MISAFIR_SAHIBI)
    }

    private suspend fun gecisYap(yeni: String) = withContext(Dispatchers.IO) {
        gecisKilidi.withLock {
            val mevcut = mevcutSahibiCoz()
            val oncekiKurulum = tercihler.ilkKurulumTarihi.first()
            val temizlendi = yerelVeriTemizlenmeli(mevcut, yeni)
            if (temizlendi) yereliTemizle()

            if (tercihler.veriBaslangicAni.first() == null) {
                // İlk sahip/misafir mevcut kurulum verisini devralır. Gerçek
                // hesap değişiminde ise milisaniye kesinliğinde "şimdi"den
                // önceki UsageStats olayları yeni hesaba giremez.
                val baslangic = if (temizlendi) {
                    saat.instant()
                } else {
                    oncekiKurulum
                        ?.atStartOfDay(saat.zone)
                        ?.toInstant()
                        ?: saat.instant()
                }
                tercihler.veriBaslangiciniKaydet(baslangic)
            }
            if (temizlendi) {
                val bugun = LocalDate.now(saat)
                tercihler.kurulumuIsaretle(bugun, gecmisGun = 0)
                // Sistem izinleri cihaz düzeyindedir. Yeni hesap, önceki
                // sahibin kurulum öncesi hacmini alpha girdisi olarak da almasın.
                tercihler.gecmisGunuKaydet(0)
            }
            tercihler.veriSahibiniKaydet(yeni)
        }
    }

    private suspend fun mevcutSahibiCoz(): String? {
        tercihler.veriSahibi.first()?.let { return it }
        val oturum = tercihler.oturum.first()
        val sahip = when {
            oturum.girisYapildiMi && oturum.eposta.isNotBlank() ->
                oturum.eposta.trim().lowercase(Locale.ROOT)
            oturum.misafirMi -> MISAFIR_SAHIBI
            // Yeni kurulumda tarih henüz yazılmamıştır. Tarih var ama
            // oturum yoksa bu, eski sürümden kalan sahipsiz yerel veridir.
            tercihler.ilkKurulumTarihi.first() != null -> BILINMEYEN_SAHIP
            else -> null
        }
        if (sahip != null) tercihler.veriSahibiniKaydet(sahip)
        return sahip
    }

    private suspend fun yereliTemizle() {
        // Sunucu adresi cihaz/geliştirici ayarıdır; hesap verisi değil.
        // Gerçek cihazda LAN tüneli kullanılıyorsa hesap geçişi sonrası
        // bağlantının kendiliğinden bozulmaması için korunur.
        val sunucuTabani = tercihler.sunucuTabani.first()
        db.clearAllTables()
        tercihler.sifirla()
        tercihler.sunucuTabaniniKaydet(sunucuTabani)
        jetonDeposu.temizle()
        kategoriDeposu.seedEt(zorla = true)
    }
}

internal const val MISAFIR_SAHIBI = "__misafir__"
internal const val BILINMEYEN_SAHIP = "__bilinmeyen_eski_sahip__"

/** Saf karar tablosu; hesap/misafir geçişlerinin regresyon testini kolaylaştırır. */
internal fun yerelVeriTemizlenmeli(mevcut: String?, yeni: String): Boolean = when {
    mevcut == null -> false
    mevcut == yeni -> false
    // Misafir verisi hesap açılınca kaybolmaz; hesaba dönüşür.
    mevcut == MISAFIR_SAHIBI && yeni != MISAFIR_SAHIBI -> false
    else -> true
}
