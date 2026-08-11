package tr.org.yesilay.yesiliz.arkaplan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.MainActivity
import tr.org.yesilay.yesiliz.R
import tr.org.yesilay.yesiliz.data.depo.AktiviteOturumuDeposu
import tr.org.yesilay.yesiliz.data.depo.BildirimDeposu
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.data.depo.YuruyusOturumDeposu
import tr.org.yesilay.yesiliz.data.depo.AdimDeposu
import tr.org.yesilay.yesiliz.data.saglik.AktiviteSaglayici
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.ui.ekran.yuruyus.YuruyusHesapla
import tr.org.yesilay.yesiliz.ui.gezinme.Rota
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * Aktif yürüyüş sayacı — foreground service.
 *
 * ## §12 K15 tersine çevrildi
 *
 * Eski karar: "Aktif yürüyüş foreground service'siz; ekran açık kaldığı
 * sürece sayıyor." Gerekçesi arka planda adım takibinin ayrı izin, ayrı
 * bildirim kanalı ve ayrı pil bütçesi istemesiydi ve bunun MVP'nin kritik
 * yolunda olmamasıydı.
 *
 * Gerçek cihazda bu karar özelliği kullanılamaz kılıyor: yürüyüşe çıkan
 * kullanıcı telefonu cebine koyuyor, ekran kapanıyor ve sayaç duruyor
 * (`debug.md` İ-3). Ekranı 15 dakika açık tutarak yürümek ne makul bir
 * istek ne de sağlıklı bir öneri -- üstelik uygulamanın kendi mesajıyla
 * ("ekrandan uzaklaş") doğrudan çelişiyor.
 *
 * ## Pil ve izin yükü
 *
 * Servis **yalnızca oturum sürerken** yaşıyor; kullanıcı başlatıyor,
 * tamamlanınca kendini durduruyor. Yaptığı iş saniyede bir sayaç artırmak
 * ve 30 saniyede bir Health Connect'e adım sormak.
 *
 * Bildirim kanalı ayrı ve **düşük öncelikli**: yürüyüş sırasında titreşim
 * ya da ses yok, yalnızca kalıcı bir satır. Bu satır aynı zamanda
 * Android'in şart koştuğu şey -- kullanıcı arka planda ne çalıştığını
 * görebiliyor.
 */
@AndroidEntryPoint
class YuruyusServisi : Service() {

    @Inject lateinit var oturumDeposu: YuruyusOturumDeposu

    @Inject lateinit var puanDeposu: PuanDeposu

    @Inject lateinit var bildirimDeposu: BildirimDeposu

    @Inject lateinit var aktiviteSaglayici: AktiviteSaglayici

    @Inject lateinit var adimDeposu: AdimDeposu

    @Inject lateinit var aktiviteOturumuDeposu: AktiviteOturumuDeposu

    @Inject lateinit var saat: Clock

    private val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sayacIsi: Job? = null
    private val bitirmeKilidi = TekSeferlikIslem()

    /** Yürüyüşe başlarkenki adım — fark, yürüyüşün kendi adımı. */
    private var baslangicAdimi: Int? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            EYLEM_DURAKLAT -> {
                sayacIsi?.cancel()
                oturumDeposu.duraklat()
                bildirimiTazele()
                return START_NOT_STICKY
            }

            EYLEM_BITIR -> {
                bitir(puanVer = false)
                return START_NOT_STICKY
            }
        }

        kanaliKur()
        startForeground(BILDIRIM_ID, bildirim())
        if (sayacIsi?.isActive != true) baslat()
        // Devam eden sayaç henüz diske yazılmıyor. START_STICKY ile
        // süreç ölümünden sonra boş Intent alan yeni servis, bellekteki
        // sıfırlanmış depoyu "sürdürüp" kullanıcı istemeden yeni bir
        // yürüyüş başlatıyordu. Geri yüklenebilir oturum yoksa doğru
        // davranış yeniden doğmamaktır.
        return START_NOT_STICKY
    }

    private fun baslat() {
        if (!oturumDeposu.oturum.value.calisiyor) oturumDeposu.surdur()

        sayacIsi = kapsam.launch {
            if (baslangicAdimi == null) baslangicAdimi = adimOku()
            var gecen = oturumDeposu.oturum.value.gecenSaniye

            while (isActive) {
                delay(SANIYE_MS)
                gecen++
                val adim = if (gecen % ADIM_ARALIK_SN == 0) adimFarki() else null
                oturumDeposu.tikla(gecen, adim)

                if (gecen % BILDIRIM_ARALIK_SN == 0) bildirimiTazele()
                if (YuruyusHesapla.ilerleme(gecen).tamamlandiMi) {
                    bitir(puanVer = true)
                    break
                }
            }
        }
    }

    private fun bitir(puanVer: Boolean) {
        // Sayaç hedefe ulaştığı anda bildirimdeki "Bitir" de gelebilir.
        // İki iş aynı oturumu iki kez aktivite tablosuna yazmamalı ve
        // `puanVerildi` sonucunu son tamamlanan iş rastgele ezmemeli.
        if (!bitirmeKilidi.sahiplen()) return
        sayacIsi?.cancel()
        kapsam.launch {
            // Gerçek yürünen süre — duraklatılan aralıkları saymaz (sayaç
            // yalnızca çalışırken artıyor). Hedefe ulaşılmasa bile (`puanVer
            // = false`, kullanıcı elle bitirdi) yürünen süre gerçek: spor
            // dakikasına yansımalı (`gorev2.md` §5). Aralık "şimdi"den geriye
            // doğru kuruluyor — gerçek başlangıç zamanını tutmak, duraklatma
            // sırasında geçen zamanı da (yanlışlıkla) süreye katardı.
            val gecenSaniye = oturumDeposu.oturum.value.gecenSaniye
            if (gecenSaniye > 0) {
                val bitis = Instant.now(saat)
                aktiviteOturumuDeposu.kaydet(bitis.minusSeconds(gecenSaniye.toLong()), bitis)
            }

            var yazildi = false
            if (puanVer) {
                val bugun = LocalDate.now(saat)
                yazildi = puanDeposu.gorevTamamla(Gorev.YURUYUS, bugun)
                if (yazildi) bildirimDeposu.gorevTamamlandi(Gorev.YURUYUS)
            }
            oturumDeposu.tamamlandi(yazildi)
            stopSelf()
        }
    }

    override fun onDestroy() {
        sayacIsi?.cancel()
        kapsam.cancel()
        super.onDestroy()
    }

    private suspend fun adimFarki(): Int? {
        val simdiki = adimOku() ?: return null
        val bas = baslangicAdimi ?: simdiki.also { baslangicAdimi = it }
        return (simdiki - bas).coerceAtLeast(0)
    }

    /**
     * Günün toplam adımı; hiçbir kaynak veremiyorsa `null` — ekran adımı gizler.
     *
     * Her tikte önce donanım sayacı örnekleniyor: [AktiviteSaglayici] Room'da
     * biriktirilmiş değeri okuyor, örneklenmezse yürüyüş boyunca aynı sayı
     * kalırdı. Health Connect verisi varsa zaten o kazanıyor ve örnekleme
     * boşa gitmiyor — sonraki günlerin birikimi yine de ilerliyor.
     */
    private suspend fun adimOku(): Int? {
        adimDeposu.ornekle()
        val adim = aktiviteSaglayici.gununAktivitesi(LocalDate.now(saat)).adim
        return if (adim > 0.0) adim.roundToInt() else null
    }

    private fun bildirimiTazele() {
        val yonetici = getSystemService(NotificationManager::class.java)
        yonetici?.notify(BILDIRIM_ID, bildirim())
    }

    private fun bildirim(): android.app.Notification {
        val oturum = oturumDeposu.oturum.value
        val ilerleme = YuruyusHesapla.ilerleme(oturum.gecenSaniye)

        val ac = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_HEDEF_ROTA, Rota.YURUYUS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(
                if (oturum.calisiyor) "Yürüyüş sürüyor" else "Yürüyüş duraklatıldı",
            )
            .setContentText(
                "${YuruyusHesapla.sayacMetni(ilerleme.kalanSaniye)} kaldı" +
                    (oturum.adim?.let { " · $it adım" } ?: ""),
            )
            .setContentIntent(ac)
            .setOngoing(true)
            // Sessiz: yürüyüş sırasında titreşim ya da ses yok.
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Bitir", eylem(EYLEM_BITIR))
            .build()
    }

    private fun eylem(ad: String): PendingIntent = PendingIntent.getService(
        this,
        ad.hashCode(),
        Intent(this, YuruyusServisi::class.java).setAction(ad),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun kanaliKur() {
        val yonetici = getSystemService(NotificationManager::class.java) ?: return
        if (yonetici.getNotificationChannel(KANAL) != null) return
        yonetici.createNotificationChannel(
            NotificationChannel(KANAL, "Yürüyüş", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Aktif yürüyüş sayacı çalışırken görünür."
                setShowBadge(false)
            },
        )
    }

    companion object {
        private const val KANAL = "yuruyus"
        private const val BILDIRIM_ID = 4201
        private const val SANIYE_MS = 1_000L

        /** Adım her saniye sorulmuyor: Health Connect sorgusu ucuz değil. */
        private const val ADIM_ARALIK_SN = 30

        /** Bildirim metni her saniye yenilenmiyor — pil. */
        private const val BILDIRIM_ARALIK_SN = 10

        const val EYLEM_DURAKLAT = "yuruyus.duraklat"
        const val EYLEM_BITIR = "yuruyus.bitir"

        fun baslat(baglam: Context) {
            androidx.core.content.ContextCompat.startForegroundService(
                baglam,
                Intent(baglam, YuruyusServisi::class.java),
            )
        }

        fun komut(baglam: Context, eylem: String) {
            baglam.startService(Intent(baglam, YuruyusServisi::class.java).setAction(eylem))
        }

        fun durdur(baglam: Context) {
            baglam.stopService(Intent(baglam, YuruyusServisi::class.java))
        }
    }
}

/** Servis komutları farklı Binder/iş parçacığı yollarından yarışabilir. */
internal class TekSeferlikIslem {
    private val sahiplenildi = AtomicBoolean(false)

    fun sahiplen(): Boolean = sahiplenildi.compareAndSet(false, true)
}
