package tr.org.yesilay.yesiliz.ui.ekran.nefes

/**
 * Kutu nefesi (box breathing) — 4 eşit fazlı döngü: Nefes al → Tut →
 * Nefes ver → Tut. `docs/tasarim-referans.md §2` ekran 16'nın tek somut
 * rakamı "4 saniye" — varsayılan tempo bu yüzden 4 sn.
 */
enum class NefesFazi(val etiket: String, val yonerge: String) {
    NEFES_AL("Nefes al", "burnundan yavaşça nefes al"),
    TUT_1("Tut", "nefesini tut"),
    NEFES_VER("Nefes ver", "ağzından yavaşça nefes ver"),
    TUT_2("Tut", "nefesini tut"),
}

data class NefesIlerleme(
    val fazIndeksi: Int,
    val fazIciKalanSaniye: Int,
    val toplamKalanSaniye: Int,
    val tamamlandiMi: Boolean,
)

/**
 * Faz ilerlemesini hesaplayan **saf fonksiyon** — ViewModel'in saniye
 * sayacından ayrı tutuluyor ki emülatörsüz test edilebilsin
 * (`OturumCikar`/`Tetikleyici` ile aynı desen).
 *
 * Tempo (faz başına saniye) değişince özel bir "geçiş" mantığı gerekmiyor:
 * her çağrı geçen toplam saniyeyi güncel tempoyla yeniden yorumluyor, bu
 * yüzden [NefesViewModel] tempo değiştiğinde sadece bu fonksiyonu tekrar
 * çağırıyor.
 */
object NefesFazHesapla {

    /** Kutu nefesinin faz sayısı — [NefesFazi.entries] ile birebir. */
    private const val FAZ_SAYISI = 4

    fun ilerlemeyiHesapla(toplamSaniye: Int, gecenSaniye: Int, fazSaniye: Int): NefesIlerleme {
        val kalan = (toplamSaniye - gecenSaniye).coerceAtLeast(0)
        if (kalan == 0) {
            return NefesIlerleme(
                fazIndeksi = FAZ_SAYISI - 1,
                fazIciKalanSaniye = 0,
                toplamKalanSaniye = 0,
                tamamlandiMi = true,
            )
        }

        val fazIndeksi = (gecenSaniye / fazSaniye) % FAZ_SAYISI
        val fazIciGecen = gecenSaniye % fazSaniye
        return NefesIlerleme(
            fazIndeksi = fazIndeksi,
            fazIciKalanSaniye = fazSaniye - fazIciGecen,
            toplamKalanSaniye = kalan,
            tamamlandiMi = false,
        )
    }
}
