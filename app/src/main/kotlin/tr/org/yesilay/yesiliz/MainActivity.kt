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
import androidx.lifecycle.lifecycleScope
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
        lifecycleScope.launch {
            gecmisVeriDeposu.hesaplaGerekirse()

            val servisIntent = Intent(this@MainActivity, NudgeOverlayService::class.java)
            val kullaniciIstedi = tercihler.molaHatirlaticiAcikMi.first()
            if (kullaniciIstedi && NudgeIzni.verildiMi(this@MainActivity)) {
                ContextCompat.startForegroundService(this@MainActivity, servisIntent)
            } else {
                stopService(servisIntent)
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
