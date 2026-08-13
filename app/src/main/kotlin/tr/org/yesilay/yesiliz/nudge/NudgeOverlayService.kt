package tr.org.yesilay.yesiliz.nudge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tr.org.yesilay.yesiliz.MainActivity
import tr.org.yesilay.yesiliz.R
import tr.org.yesilay.yesiliz.data.depo.BildirimDeposu
import tr.org.yesilay.yesiliz.data.depo.KategoriDeposu
import tr.org.yesilay.yesiliz.data.kullanim.KullanimOkuyucu
import tr.org.yesilay.yesiliz.data.kullanim.OturumCikar
import tr.org.yesilay.yesiliz.domain.kullanim.KullanimOlay
import tr.org.yesilay.yesiliz.domain.kullanim.OruntuCikar
import tr.org.yesilay.yesiliz.risk.Sabitler
import tr.org.yesilay.yesiliz.ui.gezinme.Rota
import tr.org.yesilay.yesiliz.ui.tema.YesilizTemasi
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Nudge'ın canlı tetikleyicisi + overlay pencere sahibi.
 *
 * `MainActivity.onResume()` bu servisi izin + kullanıcı tercihine göre
 * başlatıp durduruyor (CLAUDE.md §5 "Foreground Service" — WorkManager'ın
 * ~15dk asgari periyodu canlı izleme için uygun değil).
 *
 * Tek Activity + NavHost mimarisinde (CLAUDE.md §6) bu servis kendi
 * `WindowManager` penceresine bağlı bir `ComposeView` tutan **yeni** bir
 * kalıp — başka hiçbir ekran bunu yapmıyor, overlay'in doğası gereği.
 */
@AndroidEntryPoint
class NudgeOverlayService : Service() {

    @Inject lateinit var kullanimOkuyucu: KullanimOkuyucu
    @Inject lateinit var kategoriDeposu: KategoriDeposu
    @Inject lateinit var bildirimDeposu: BildirimDeposu
    @Inject lateinit var saat: Clock

    private val servisIsi = SupervisorJob()
    private val kapsam = CoroutineScope(Dispatchers.Default + servisIsi)

    private var sonEylem: SonEylem? = null
    private var overlayGorunumu: ComposeView? = null
    private var overlaySahibi: OverlayYasamDongusu? = null
    private var otoKapanmaIsi: Job? = null

    override fun onCreate() {
        super.onCreate()
        // Ayar ekranında izin servis başlarken/yaşarken geri alınabilir.
        // İzinsiz addView bir SecurityException ile tüm süreci düşürür.
        if (!NudgeIzni.verildiMi(this)) {
            stopSelf()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                BILDIRIM_ID,
                bildirimOlustur(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(BILDIRIM_ID, bildirimOlustur())
        }
        kapsam.launch { izlemeDongusu() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        overlayiKapat()
        kapsam.cancel()
        super.onDestroy()
    }

    private suspend fun izlemeDongusu() {
        val esleyici = kategoriDeposu.esleyiciAl()

        while (kapsam.isActive) {
            if (overlayGorunumu == null) {
                val simdi = Instant.now(saat)
                // Pencereyi en az 10 dakika tut ki kısa eşiklerde bile yeterli
                // geçmiş olsun ve UsageStatsManager'ın gecikmeyle yazdığı
                // olaylar da yakalanabilsin.
                val enAzPencere = OruntuCikar.BLOK_TOLERANSI.plusMinutes(10)
                val pencere = maxOf(
                    NudgeSabitleri.ESIK.plus(OruntuCikar.BLOK_TOLERANSI).plusMinutes(5),
                    enAzPencere,
                )
                val hamOlaylar = kullanimOkuyucu.sonPencereOlaylari(pencere, simdi)
                val oturumlar = OturumCikar.cikar(hamOlaylar, simdi.minus(pencere), simdi)

                val sonOturum = oturumlar.maxByOrNull { it.bitis }
                val suAnTuketimdeMi = sonOturum != null && esleyici.bul(
                    sonOturum.paketAdi,
                    kullanimOkuyucu.androidKategorisi(sonOturum.paketAdi),
                ).anahtar in Sabitler.TUKETIM

                val tuketimOlaylari = oturumlar.mapNotNull { oturum ->
                    val kategori = esleyici.bul(
                        oturum.paketAdi,
                        kullanimOkuyucu.androidKategorisi(oturum.paketAdi),
                    )
                    if (kategori.anahtar !in Sabitler.TUKETIM) return@mapNotNull null
                    KullanimOlay(oturum.paketAdi, kategori, oturum.baslangic, oturum.bitis)
                }

                // Ön plandaki uygulamayı kontrol et: UsageStatsManager olayları
                // gecikebilir; hâlâ açık olan tüketim uygulaması için son
                // oturumun bitişini şimdiye uzat.
                val guncelTuketim = if (suAnTuketimdeMi) onPlanTuketimUzat(tuketimOlaylari, simdi) else tuketimOlaylari

                val sonuc = Tetikleyici.degerlendir(guncelTuketim, simdi, sonEylem, suAnTuketimdeMi)
                Log.d(TAG, "poll: oturum=${oturumlar.size} tuketim=${guncelTuketim.size} sonuc=$sonuc")

                if (sonuc == TetikSonucu.GOSTER) {
                    overlayiGoster()
                    // Overlay 3-5 saniye görünüp kayboluyor; kullanıcı ekrana
                    // bakmıyorsa kaçırıyor. Bildirim defterine düşmesi,
                    // "neden mola önerdi" sorusunun cevabını saklıyor.
                    bildirimDeposu.molaHatirlatildi(
                        NudgeSabitleri.ESIK.toMinutes().toInt(),
                    )
                }
            }
            delay(NudgeSabitleri.POLL_ARALIGI.toMillis())
        }
    }

    /**
     * `UsageStatsManager.queryEvents()` hâlâ ön planda olan uygulamanın
     * `ACTIVITY_PAUSED` olayını henüz yazmamış olabilir. Bu durumda
     * `OturumCikar` oturumu `pencereBit = simdi` ile kapatır — bu doğru.
     *
     * Ama gerçek cihazlarda bazen `ACTIVITY_RESUMED` olayının kendisi de
     * birkaç saniye gecikmeli yazılabiliyor. Bu durumda son tüketim
     * oturumunun `bitis`'i `simdi`'den birkaç saniye önce kalır ve
     * `acikBlokSuresi`'nin `simdiyeBosluk` kontrolü 2dk tolerans içinde
     * olsa bile blok süresini olduğundan kısa hesaplar.
     *
     * Bu düzeltme: eğer son tüketim oturumunun bitişi `simdi`'ye
     * tolerans içinde yakınsa, bitişi `simdi`'ye uzatır — blok hâlâ
     * devam ediyor demektir.
     */
    private fun onPlanTuketimUzat(
        olaylar: List<KullanimOlay>,
        simdi: Instant,
    ): List<KullanimOlay> {
        if (olaylar.isEmpty()) return olaylar
        val sonOlay = olaylar.maxByOrNull { it.bitis } ?: return olaylar
        val bosluk = java.time.Duration.between(sonOlay.bitis, simdi)
        // Tolerans içindeyse ve bitis simdi'den önce ise, simdi'ye uzat.
        if (bosluk > java.time.Duration.ZERO && bosluk <= OruntuCikar.BLOK_TOLERANSI) {
            return olaylar.map { olay ->
                if (olay === sonOlay) olay.copy(bitis = simdi) else olay
            }
        }
        return olaylar
    }

    private suspend fun overlayiGoster() = withContext(Dispatchers.Main) {
        if (overlayGorunumu != null) return@withContext
        val yasamDongusu = OverlayYasamDongusu().apply { baslat() }
        val viewModelSahibi = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }

        val composeView = ComposeView(this@NudgeOverlayService).apply {
            setViewTreeLifecycleOwner(yasamDongusu)
            setViewTreeViewModelStoreOwner(viewModelSahibi)
            setViewTreeSavedStateRegistryOwner(yasamDongusu)
            setContent {
                YesilizTemasi {
                    NudgeOverlay(
                        esikSaniye = NudgeSabitleri.ESIK.seconds,
                        onDevamEt = { eylemVeKapat(EylemTuru.DEVAM_EDILDI) },
                        onNefesEvet = {
                            nefesEkraninaGit()
                            eylemVeKapat(EylemTuru.MOLA_VERILDI)
                        },
                        onNefesHayir = { eylemVeKapat(EylemTuru.MOLA_VERILDI) },
                    )
                }
            }
        }

        val katmanTuru = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        var bayraklar = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bayraklar = bayraklar or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        }

        val katmanParametreleri = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            katmanTuru,
            bayraklar,
            PixelFormat.TRANSLUCENT,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            katmanParametreleri.blurBehindRadius = BULANIKLIK_YARICAPI_PX
        }

        val pencereYoneticisi = getSystemService(WINDOW_SERVICE) as WindowManager
        try {
            pencereYoneticisi.addView(composeView, katmanParametreleri)
        } catch (_: SecurityException) {
            // İzin, yukarıdaki kontrolden addView'e kadar olan dar aralıkta
            // da geri alınabilir. Bu normal bir izin yarışıdır, fatal değil.
            yasamDongusu.durdur()
            stopSelf()
            return@withContext
        }

        overlayGorunumu = composeView
        overlaySahibi = yasamDongusu

        otoKapanmaIsi = kapsam.launch(Dispatchers.Main) {
            delay(NudgeSabitleri.OTO_KAPANMA.toMillis())
            eylemVeKapat(EylemTuru.DEVAM_EDILDI)
        }
    }

    private fun eylemVeKapat(tur: EylemTuru) {
        sonEylem = SonEylem(tur, Instant.now(saat))
        overlayiKapat()
    }

    private fun overlayiKapat() {
        otoKapanmaIsi?.cancel()
        otoKapanmaIsi = null

        val gorunum = overlayGorunumu ?: return
        overlayGorunumu = null
        val pencereYoneticisi = getSystemService(WINDOW_SERVICE) as WindowManager
        runCatching { pencereYoneticisi.removeView(gorunum) }
        overlaySahibi?.durdur()
        overlaySahibi = null
    }

    private fun nefesEkraninaGit() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_HEDEF_ROTA, Rota.NEFES)
        }
        startActivity(intent)
    }

    private fun bildirimOlustur(): android.app.Notification {
        val yonetici = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kanal = NotificationChannel(
                KANAL_ID,
                "Mola hatırlatıcı",
                NotificationManager.IMPORTANCE_MIN,
            )
            yonetici.createNotificationChannel(kanal)
        }

        val acilisIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, KANAL_ID)
            .setContentTitle("Yeşilİz kullanım süreni takip ediyor")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(acilisIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private companion object {
        const val TAG = "NudgeOverlay"
        const val KANAL_ID = "nudge_servis"
        const val BILDIRIM_ID = 1
        const val BULANIKLIK_YARICAPI_PX = 120
    }
}

/**
 * Servisin kendi `WindowManager` penceresine bağladığı `ComposeView`'a
 * gereken minimal `LifecycleOwner` + `SavedStateRegistryOwner` — tek
 * Activity mimarisinde bunun bir emsali yok, overlay'in kendine özgü
 * gereksinimi.
 */
private class OverlayYasamDongusu : SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun baslat() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun durdur() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}
