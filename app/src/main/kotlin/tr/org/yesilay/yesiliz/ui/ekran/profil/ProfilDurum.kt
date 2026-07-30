package tr.org.yesilay.yesiliz.ui.ekran.profil

/**
 * Profil ekranı durumu — `docs/tasarim-referans.md §2 / 12`.
 *
 * [yesilayPaylasimi], [ligdeGorun] ve [gunlukHedefDk] DataStore'da tutuluyor
 * (CLAUDE.md §6): tekil tercih değerleri Room tablosu istemiyor.
 */
data class ProfilDurum(
    val isim: String = "",
    val ligAdi: String = "",
    val yesilayPaylasimi: Boolean = false,
    val ligdeGorun: Boolean = true,
    val molaHatirlaticiAcik: Boolean = false,
    val gunlukHedefDk: Int = 0,
    /** Debug derlemesinde demo tohumu satırı gösterilsin mi. */
    val demoKullanilabilir: Boolean = false,
)
