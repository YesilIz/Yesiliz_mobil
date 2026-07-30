package tr.org.yesilay.yesiliz.ui.ekran.lig

/**
 * Lig ekranı durumu — `docs/tasarim-referans.md §2 / 10`.
 *
 * Lig seviyeleri Fidan → Filiz → Ağaç → Koru → Orman (CLAUDE.md §12 K1).
 */
data class LigDurum(
    val ligAdi: String = "",
    val kalanGun: Int = 0,
    val yukselen: Int = 7,
    val dusen: Int = 7,
    val siralama: List<LigSatiriModel> = emptyList(),
)

data class LigSatiriModel(
    val sira: Int,
    val isim: String,
    val puan: Int,
    val benMiyim: Boolean = false,
)
