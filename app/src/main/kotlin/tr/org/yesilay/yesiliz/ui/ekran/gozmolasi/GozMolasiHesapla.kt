package tr.org.yesilay.yesiliz.ui.ekran.gozmolasi

/**
 * 20-20-20 göz molası — saf faz hesabı.
 *
 * Kural: her 20 dakikada bir, 20 saniye boyunca ~6 metre uzağa bak. Bu
 * ekran yalnızca **20 saniyelik bakış** kısmını yürütüyor; 20 dakikalık
 * aralık hatırlatıcının işi.
 *
 * `NefesFazHesapla` ile aynı desen: ViewModel'den ayrılmış, emülatörsüz
 * test edilebilir (CLAUDE.md §6).
 */
object GozMolasiHesapla {

    /** Bakış süresi, saniye — 20-20-20 kuralının ikinci 20'si. */
    const val BAKIS_SANIYE = 20

    /**
     * Molanın anlık durumu.
     *
     * Sayı **geri** sayıyor: kullanıcı ekrana bakmıyor olmalı, o yüzden
     * gördüğü tek şey ne kadar kaldığı olmalı.
     */
    fun ilerleme(gecenSaniye: Int): GozMolasiIlerleme {
        val kalan = (BAKIS_SANIYE - gecenSaniye).coerceAtLeast(0)
        return GozMolasiIlerleme(
            kalanSaniye = kalan,
            oran = (gecenSaniye.toFloat() / BAKIS_SANIYE).coerceIn(0f, 1f),
            tamamlandiMi = gecenSaniye >= BAKIS_SANIYE,
        )
    }
}

data class GozMolasiIlerleme(
    val kalanSaniye: Int,
    val oran: Float,
    val tamamlandiMi: Boolean,
)
