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

    // --- keşfet (ana ekrandaki iki kart) ---
    const val BILGI_BANKASI = "bilgi"
    const val BILGI_YAZI = "bilgi/{yaziId}"
    const val KENDINI_TEST = "kendinitest"

    /** Argümanlı rotayı kurar — çağıranın string birleştirmesi gerekmesin. */
    fun bilgiYazisi(id: String): String = "bilgi/$id"

    // --- bildirim ---
    const val BILDIRIM = "bildirim"

    // --- hedef ve kategori (Profil › Hedeflerim) ---
    const val GUNLUK_HEDEF = "hedef/gunluk"
    const val KATEGORI = "kategori"
    const val KATEGORIZE_ET = "kategori/duzenle"

    /**
     * SAS-SV'nin yeniden çözülmesi. Onboarding'deki [ANKET] ile **aynı
     * ekranı** kullanıyor ama farklı rota: o onboarding alt grafiğinin
     * çocuğu, bu ana grafiğin.
     */
    const val ANKET_YENILE = "anket/yenile"

    // --- lig ---
    const val ARKADAS_EKLE = "lig/arkadas"

    // --- görev ekranları ---
    const val YURUYUS = "yuruyus"

    // --- sağlık ve destek ---
    const val SAGLIK_IZIN = "saglik/izin"
    const val YEDAM_ONAY = "yedam/onay"

    // --- hesap (akış dışı) ---
    const val HESAP = "hesap"
    const val GIRIS = "hesap/giris"
    const val KAYIT = "hesap/kayit"
    const val SIFRE = "hesap/sifre"
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
