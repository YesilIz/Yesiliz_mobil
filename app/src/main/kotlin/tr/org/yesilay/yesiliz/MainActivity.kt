package tr.org.yesilay.yesiliz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tr.org.yesilay.yesiliz.ui.ekran.hesap.BakimSeridi
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.arkaplan.IsPlanlayici
import tr.org.yesilay.yesiliz.data.depo.AdimDeposu
import tr.org.yesilay.yesiliz.data.depo.GecmisVeriDeposu
import tr.org.yesilay.yesiliz.data.depo.YerelOturumSiniri
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.zaman.GuncelGun
import tr.org.yesilay.yesiliz.nudge.NudgeIzni
import tr.org.yesilay.yesiliz.nudge.NudgeOverlayService
import tr.org.yesilay.yesiliz.ui.ekran.hesap.AskidaEkran
import tr.org.yesilay.yesiliz.ui.ekran.hesap.AskidaViewModel
import tr.org.yesilay.yesiliz.ui.gezinme.Rota
import tr.org.yesilay.yesiliz.ui.gezinme.YesilizGezinme
import tr.org.yesilay.yesiliz.ui.tema.YesilizTemasi
import javax.inject.Inject

/**
 * Tek Activity — gezinme Compose NavHost ile (CLAUDE.md §6).
 *
 * `enableEdgeToEdge()`: yeşil başlık status bar'ın altına kadar uzanıyor
 * (`docs/tasarim-referans.md §1`), bu yüzden sistem çubukları saydam.
 *
 * `onResume` izin-bağımlı işleri tazeler: geçmiş veri hesaplaması
 * ([GecmisVeriDeposu]) ve donanım adım sayacının örneklenmesi ([AdimDeposu]).
 * İzinler sistem Ayarları'ndan verildiği için buraya geri dönüşün kendisi
 * tek geri bildirim kaynağı (`manifest`'te `singleTop` bu yüzden).
 *
 * **Burada izin İSTENMİYOR.** Nudge'ın iki sistem izni yalnızca kullanıcı
 * mola hatırlatıcıyı açtığında sorulur (`HedefEkran` → [NudgeIzni]);
 * gerekçesi orada yazılı. Servisin açık/kapalı olması `onResume`'a değil
 * tercih akışına bağlı ([nudgeServisiniIzle]).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var gecmisVeriDeposu: GecmisVeriDeposu

    @Inject lateinit var adimDeposu: AdimDeposu

    @Inject lateinit var tercihler: Tercihler

    @Inject lateinit var guncelGun: GuncelGun

    @Inject lateinit var yerelOturumSiniri: YerelOturumSiniri

    @Inject lateinit var isPlanlayici: IsPlanlayici

    private var hedefRota by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hedefRota = intent?.let { hedefRotaOku(it) }
        nudgeServisiniIzle()
        geceYarisiniIzle()
        setContent {
            YesilizTemasi {
                YesilizGezinme(
                    hedefRota = hedefRota,
                    onHedefRotaTuketildi = { hedefRota = null },
                )
                // Gezinmenin ÜSTÜNDE: askı bir ekran değil bir durum, ve
                // etrafından dolaşılamamalı. Rota olarak eklenseydi geri
                // tuşu ya da bir derin bağlantı onu atlayabilirdi.
                AskiKapisi()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        hedefRotaOku(intent)?.let { hedefRota = it }
    }

    override fun onResume() {
        super.onResume()
        // Uzun süre arka planda kalan ViewModel'ler eski tarihi saklamasın.
        // Aynı günde dönülse bile selamlama ve zaman etiketleri tazelenir.
        guncelGun.yenile()
        lifecycleScope.launch {
            // Varsayılan sunucu adresi emülatörünkiyken cihazlara elle
            // yazılmış ezmeler DataStore'da kaldı ve canlı adresi ezmeye
            // devam ediyordu. Tek seferlik; ikinci açılıştan sonra no-op.
            tercihler.eskiSunucuAdresiniTemizle()
            // Eski sürümden gelen cihazlarda yerel davranış verisinin
            // sahibini UsageStats yeniden okunmadan önce geri doldur.
            yerelOturumSiniri.baslat()
            gecmisVeriDeposu.hesaplaGerekirse()
            // Donanım adım sayacı yalnızca açılıştan beri biriken kümülatif
            // sayıyı veriyor; güne düşen payı çıkarmak için örneklemek şart
            // ve iki örnek arasında kalan adımlar kayboluyor. Uygulamanın
            // her açılışı bedava bir örnekleme noktası.
            adimDeposu.ornekle()
            isPlanlayici.simdiTazele()
        }
    }

    /** Uygulama ekrandayken gece yarısı geçilirse tüm tarihli akışları yeniler. */
    private fun geceYarisiniIzle() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                guncelGun.geceYarilariniIzle()
            }
        }
    }

    /**
     * Nudge servisini tercih akışını **dinleyerek** yönetir.
     *
     * Eskiden karar yalnızca `onResume` içinde bir kez veriliyordu.
     * Kullanıcı "Mola hatırlatıcı" anahtarını uygulama önplandayken açtığı
     * için `onResume` tekrar çalışmıyordu: anahtar açık ve overlay izni
     * verilmiş olmasına rağmen çalışan servis **sıfırdı**. Ana ekrana
     * çıkıp dönmeden nudge (§3 çekirdek döngünün "YÖNLENDİR" adımı) o
     * oturum boyunca hiç çalışmıyordu (`debug.md` Y-9).
     *
     * `repeatOnLifecycle(STARTED)`: arka planda dinlemeye gerek yok, üstelik
     * Android 12+ arka plandan foreground service başlatmayı zaten
     * reddediyor.
     */
    private fun nudgeServisiniIzle() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tercihler.molaHatirlaticiAcikMi.collect { kullaniciIstedi ->
                    val servisIntent =
                        Intent(this@MainActivity, NudgeOverlayService::class.java)
                    // İzin her toplamada yeniden okunuyor: kullanıcı izni
                    // sistem ekranından verip dönebiliyor ve tercih o sırada
                    // değişmediği için akış tek başına haber vermezdi.
                    if (kullaniciIstedi && NudgeIzni.verildiMi(this@MainActivity)) {
                        Log.d(TAG, "Nudge servisi baslatiliyor")
                        ContextCompat.startForegroundService(this@MainActivity, servisIntent)
                    } else {
                        Log.d(TAG, "Nudge servisi durduruluyor (istendi=$kullaniciIstedi, izin=${NudgeIzni.verildiMi(this@MainActivity)})")
                        stopService(servisIntent)
                    }
                }
            }
        }
    }

    /**
     * Hangi ekranla açılacağız?
     *
     * İki kaynak var:
     *  - [EXTRA_HEDEF_ROTA] — nudge overlay'in derin bağlantısı
     *  - Health Connect'in **gerekçe** çağrısı: kullanıcı HC uygulamasından
     *    "bu uygulama verimi niçin istiyor?" dediğinde sistem bizi bu
     *    eylemlerle başlatıyor. Cevabı zaten [Rota.SAGLIK_IZIN] veriyor
     *    ("Yalnızca adım sayısı ve egzersiz süresi okunur. Kalp atışı, uyku,
     *    kilo ve konum hiç istenmez."), o yüzden ayrı bir ekran yok.
     */
    private fun hedefRotaOku(intent: Intent): String? = disHedefRotasiniDogrula(
        eylem = intent.action,
        hamRota = intent.getStringExtra(EXTRA_HEDEF_ROTA),
    )

    companion object {
        private const val TAG = "MainActivity"

        /** Nudge overlay'in "Nefes egzersizi yapmak ister misin? → Evet" derin bağlantısı. */
        const val EXTRA_HEDEF_ROTA = "hedef_rota"

        /**
         * Android 13 ve altında Health Connect'in gerekçe eylemi.
         *
         * Sabit `androidx.health.connect.client` içinde **açığa çıkarılmamış**;
         * dize olarak yazmak zorunlu. Android 14+ tarafı
         * `Intent.ACTION_VIEW_PERMISSION_USAGE` ile geliyor ve manifest'teki
         * `activity-alias` üzerinden bize ulaşıyor.
         */
        const val GEREKCE_EYLEMI = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
    }
}

/**
 * [MainActivity] başlatıcı olduğu için dışa açıktır; başka bir uygulama
 * ona keyfî bir `hedef_rota` extra'sı gönderebilir. Ham değeri NavController'a
 * vermek, grafikte olmayan her dizede ana iş parçacığını çökertir. Yalnızca
 * kendi immutable PendingIntent/servis akışlarımızın ürettiği iki hedef kabul
 * edilir; Health Connect gerekçe eylemi kendi sabit hedefini seçer.
 */
internal fun disHedefRotasiniDogrula(eylem: String?, hamRota: String?): String? = when (eylem) {
    MainActivity.GEREKCE_EYLEMI, Intent.ACTION_VIEW_PERMISSION_USAGE -> Rota.SAGLIK_IZIN
    else -> hamRota?.takeIf { it == Rota.NEFES || it == Rota.YURUYUS }
}


/**
 * Hesap askıya alınmışsa her şeyin üstüne engelleyici ekranı çizer.
 *
 * Durum ağ katmanından geliyor (`AskiDurumu`): hangi uç 403 + `hesap_askida`
 * dönerse dönsün buraya düşüyor. Askı yokken hiçbir şey çizilmiyor.
 */
@Composable
private fun AskiKapisi(gorunum: AskidaViewModel = hiltViewModel()) {
    val hal by gorunum.hal.collectAsState()
    val deneniyor by gorunum.deneniyor.collectAsState()
    val bakim by gorunum.bakim.collectAsState()
    var bakimKapatildi by remember { mutableStateOf(false) }

    hal?.let {
        AskidaEkran(
            sebep = it.sebep,
            onTekrarDene = gorunum::tekrarDene,
            deneniyor = deneniyor,
        )
        return
    }

    // Bakım **engelleyici değil** (M9): ölçüm, görevler ve aktiviteler
    // cihazda çalışmaya devam ediyor. Kullanıcıyı yapabileceği şeylerden
    // alıkoymanın gerekçesi yok — bilgilendirme yeter ve kapatılabiliyor.
    bakim?.takeIf { !bakimKapatildi }?.let { mesaj ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(Olcu.EkranKenari),
            contentAlignment = Alignment.TopCenter,
        ) {
            BakimSeridi(mesaj = mesaj, onKapat = { bakimKapatildi = true })
        }
    }
}
