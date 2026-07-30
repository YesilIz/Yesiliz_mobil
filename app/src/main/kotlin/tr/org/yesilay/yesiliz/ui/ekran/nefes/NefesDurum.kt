package tr.org.yesilay.yesiliz.ui.ekran.nefes

/**
 * Nefes ekranının durumu — `docs/tasarim-referans.md §2 / 16`.
 *
 * [fazSaniye] varsayılan 4 — tasarımın tek somut rakamı ("4 saniye
 * boyunca..."). Tempo seçici [fazSaniye]'yi 4/5/6 arasında döngüler.
 */
data class NefesDurum(
    val ilerleme: NefesIlerleme = NefesIlerleme(
        fazIndeksi = 0,
        fazIciKalanSaniye = 4,
        toplamKalanSaniye = 120,
        tamamlandiMi = false,
    ),
    val fazSaniye: Int = 4,
    val oynatiliyor: Boolean = true,
)
