package tr.org.yesilay.yesiliz.ui.gezinme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Gezinme rotaları — `docs/tasarim-referans.md §3`.
 *
 * Tip güvenli navigation (`@Serializable` rotalar) yerine düz `String`:
 * ekranların çoğu argümansız, argümanlılar da tek `Long`/`String` alıyor.
 * Serialization eklentisini yalnızca gezinme için açmak gereksiz ağırlık.
 */
object Rota {
    // --- onboarding ---
    /** Onboarding alt grafiğinin rotası — NavHost'un başlangıç hedefi.
     *  İç ekranlar bu grafiğin çocukları; `startDestination` olarak
     *  doğrudan `KARSILAMA` verilemez (grafiğin doğrudan çocuğu değil). */
    const val ONBOARDING = "onboarding"
    const val KARSILAMA = "onboarding/karsilama"
    const val IZIN = "onboarding/izin"
    const val PROFIL_KURULUM = "onboarding/profil"
    const val ANKET = "onboarding/anket"
    const val ILK_SONUC = "onboarding/ilksonuc"
    const val HEDEF = "onboarding/hedef"
    const val HEDEF_OZEL = "onboarding/hedefozel"

    // --- alt sekmeler ---
    const val ANA = "ana"
    const val ISTATISTIK = "istatistik"
    const val LIG = "lig"
    const val PROFIL = "profil"

    // --- sekme dışı ---
    const val ODUL = "odul"
    const val NEFES = "nefes"
    const val EBEVEYN = "ebeveyn"
    const val RISK_DETAY = "riskdetay"
}

/**
 * Alt gezinme sekmesi.
 *
 * Seçiliyken dolu, seçili değilken çizgi ikon — tasarımda tam olarak böyle.
 */
enum class Sekme(
    val rota: String,
    val etiket: String,
    val ikonDolu: ImageVector,
    val ikonCizgi: ImageVector,
) {
    ANA(Rota.ANA, "Ana Sayfa", Icons.Filled.Home, Icons.Outlined.Home),
    ISTATISTIK(Rota.ISTATISTIK, "İstatistik", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    LIG(Rota.LIG, "Lig", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    PROFIL(Rota.PROFIL, "Profil", Icons.Filled.Person, Icons.Outlined.Person),
}
