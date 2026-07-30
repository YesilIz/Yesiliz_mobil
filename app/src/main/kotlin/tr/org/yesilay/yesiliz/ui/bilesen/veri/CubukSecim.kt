package tr.org.yesilay.yesiliz.ui.bilesen.veri

/**
 * Dokunulan x koordinatının hangi çubuğa denk geldiğini bulur.
 *
 * **Saf fonksiyon** — `Canvas`'ın çizim geometrisiyle birebir aynı hesabı
 * kullanıyor ve emülatörsüz test edilebiliyor. Çizimle seçim ayrı ayrı
 * hesaplanırsa, biri değişip diğeri unutulduğunda dokunma yanlış çubuğu
 * seçer ve bu ancak elde fark edilir.
 *
 * Vuruş alanı **yuva kadar geniş**, çubuk kadar dar değil: çubuklar yuvanın
 * yalnızca %46'sını kaplıyor, aradaki boşluğa dokunanın hiçbir şey seçememesi
 * kırık gibi hissettirirdi.
 */
object CubukSecim {

    /** Çubuk genişliğinin yuvaya oranı — `CubukGrafik` çizimiyle aynı. */
    const val CUBUK_ORANI = 0.46f

    /**
     * @return dokunulan çubuğun indeksi, alan dışındaysa -1
     */
    fun indeks(x: Float, toplamGenislik: Float, cubukSayisi: Int): Int {
        if (cubukSayisi <= 0 || toplamGenislik <= 0f) return -1
        if (x < 0f || x > toplamGenislik) return -1

        val yuva = toplamGenislik / cubukSayisi
        return (x / yuva).toInt().coerceIn(0, cubukSayisi - 1)
    }
}
