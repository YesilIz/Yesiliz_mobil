package tr.org.yesilay.yesiliz.ui.gezinme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Spa
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
    const val NASIL_CALISIR = "onboarding/nasilcalisir"
    const val IZIN = "onboarding/izin"
    const val HAREKET_IZNI = "onboarding/hareketizni"
    const val PROFIL_KURULUM = "onboarding/profil"
    const val EBEVEYN_ONAYI = "onboarding/ebeveynonayi"
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

    /**
     * Profil açılırken doğrudan bir alt sayfayı açmak için.
     *
     * Risk detayındaki "Hatırlatma saatlerini ayarla → Aç" eylemi Profil
     * ekranının tepesine bırakıyor, bildirim saatleri alt sayfasını
     * açmıyordu (`debug.md` O-8). Diğer iki eylem ("Ayarla" → Günlük
     * hedef, "Başlat" → Nefes) doğrudan hedefe gidiyordu.
     */
    const val PROFIL_SAYFA_ARG = "acilacakSayfa"
    const val PROFIL_SAYFALI = "profil?$PROFIL_SAYFA_ARG={$PROFIL_SAYFA_ARG}"

    /** [PROFIL_SAYFALI] için tanınan alt sayfa adları. */
    const val SAYFA_BILDIRIM_SAATLERI = "bildirim_saatleri"

    fun profilSayfasi(sayfa: String): String = "profil?$PROFIL_SAYFA_ARG=$sayfa"

    // --- sözleşmeler (Kayıt onayı ve Profil › Hakkında) ---
    const val SOZLESME = "sozlesme/{sozlesmeId}"
    const val SOZLESME_ARG = "sozlesmeId"

    fun sozlesme(id: String): String = "sozlesme/$id"

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

    // --- aktiviteler (dördüncü sekme, `debug.md` İ-1) ---
    const val AKTIVITE = "aktivite"
    const val YURUYUS = "yuruyus"
    const val ODAK = "aktivite/odak"
    const val GOZ_MOLASI = "aktivite/gozmolasi"
    const val GECE_KAPANISI = "aktivite/gecekapanisi"
    const val EKRANSIZ = "aktivite/ekransiz"

    // --- sağlık ve destek ---
    const val SAGLIK_IZIN = "saglik/izin"
    const val YEDAM_ONAY = "yedam/onay"

    /** "Verilerine kim baktı" + rıza geri çekme (M5, M6). */
    const val YEDAM_GIZLILIK = "yedam/gizlilik"

    // --- hesap (akış dışı) ---
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

    /**
     * Dördüncü sekme **Profil değil Aktiviteler** (`debug.md` İ-1).
     *
     * Profil bir ayar ekranı: kullanıcı oraya nadiren gidiyor ve gittiğinde
     * belirli bir şey aramaya gidiyor. Kalıcı bir sekmeyi işgal etmesi,
     * uygulamanın en sık kullanılacak dördüncü alanını harcıyordu.
     * Aktiviteler ise çekirdek döngünün "ÖDÜLLENDİR" adımının girişi
     * (CLAUDE.md §3) ve tekrar tekrar açılıyor.
     *
     * Profil artık Ana Sayfa başlığındaki avatardan açılıyor -- O-17 zaten
     * o avatarın tıklanabilir olmasını istiyordu, İ-1 ile birlikte bu bir
     * eksiklik değil zorunluluk oldu.
     */
    AKTIVITE(Rota.AKTIVITE, "Aktiviteler", Icons.Filled.Spa, Icons.Outlined.Spa),
}
