package tr.org.yesilay.yesiliz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.depo.GecmisVeriDeposu
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.nudge.NudgeIzni
import tr.org.yesilay.yesiliz.nudge.NudgeOverlayService
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
 * `onResume` her seferinde iki izin-bağımlı işi tazeler: geçmiş veri
 * hesaplaması ([GecmisVeriDeposu]) ve nudge servisinin açık/kapalı durumu.
 * İkisi de izin sistem Ayarları'ndan verildiği için buraya geri dönüşün
 * kendisi tek geri bildirim kaynağı (`manifest`'te `singleTop` bu yüzden).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var gecmisVeriDeposu: GecmisVeriDeposu

    @Inject lateinit var tercihler: Tercihler

    private var hedefRota by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hedefRota = intent?.let { hedefRotaOku(it) }
        nudgeServisiniIzle()
        setContent {
            YesilizTemasi {
                YesilizGezinme(
                    hedefRota = hedefRota,
                    onHedefRotaTuketildi = { hedefRota = null },
                )
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
        lifecycleScope.launch { gecmisVeriDeposu.hesaplaGerekirse() }
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
                        ContextCompat.startForegroundService(this@MainActivity, servisIntent)
                    } else {
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
    private fun hedefRotaOku(intent: Intent): String? = when (intent.action) {
        GEREKCE_EYLEMI, Intent.ACTION_VIEW_PERMISSION_USAGE -> Rota.SAGLIK_IZIN
        else -> intent.getStringExtra(EXTRA_HEDEF_ROTA)
    }

    companion object {
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
