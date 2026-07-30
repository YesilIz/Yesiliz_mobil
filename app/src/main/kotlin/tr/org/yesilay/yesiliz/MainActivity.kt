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

    private fun hedefRotaOku(intent: Intent): String? = intent.getStringExtra(EXTRA_HEDEF_ROTA)

    companion object {
        /** Nudge overlay'in "Nefes egzersizi yapmak ister misin? → Evet" derin bağlantısı. */
        const val EXTRA_HEDEF_ROTA = "hedef_rota"
    }
}
