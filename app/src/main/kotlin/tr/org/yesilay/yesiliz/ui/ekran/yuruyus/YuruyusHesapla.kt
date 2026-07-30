package tr.org.yesilay.yesiliz.ui.ekran.yuruyus

/**
 * Aktif yürüyüşün ilerleme hesabı — **saf fonksiyon**.
 *
 * `NefesFazHesapla` ile aynı gerekçe (CLAUDE.md §6): sayaç mantığı
 * ViewModel'den ayrılınca emülatörsüz test edilebiliyor.
 *
 * Puan `docs/spec-puan.md §7.1`: doğrulanmış yürüyüş dakika başına 3 puan,
 * günlük tavan 30 dakika (90 puan). Bu ekranda hedef 15 dakika — görev
 * kataloğundaki [tr.org.yesilay.yesiliz.domain.gorev.Gorev.YURUYUS] 45 puan
 * veriyor, yani 15 × 3.
 */
data class YuruyusIlerleme(
    val gecenSaniye: Int,
    val kalanSaniye: Int,
    val oran: Float,
    val tamamlandiMi: Boolean,
    /** O ana kadar hak edilen puan — dakika başına, kesirli dakika saymaz. */
    val kazanilanPuan: Int,
)

object YuruyusHesapla {

    /** Görev hedefi: 15 dakika. */
    const val HEDEF_DAKIKA = 15

    /** `spec-puan.md §7.1` — dakika başına 3 puan. */
    const val DAKIKA_PUANI = 3

    private const val SANIYE_DAKIKA = 60

    fun ilerleme(gecenSaniye: Int, hedefDakika: Int = HEDEF_DAKIKA): YuruyusIlerleme {
        val hedefSaniye = hedefDakika * SANIYE_DAKIKA
        val gecen = gecenSaniye.coerceAtLeast(0)
        val kalan = (hedefSaniye - gecen).coerceAtLeast(0)

        return YuruyusIlerleme(
            gecenSaniye = gecen,
            kalanSaniye = kalan,
            oran = if (hedefSaniye == 0) 1f else (gecen.toFloat() / hedefSaniye).coerceIn(0f, 1f),
            tamamlandiMi = gecen >= hedefSaniye,
            // Tam dakikalar sayılıyor: 59 saniye yürüyüp puan almak, "doğrulanmış
            // yürüyüş" tanımını anlamsızlaştırırdı.
            kazanilanPuan = (gecen / SANIYE_DAKIKA).coerceAtMost(hedefDakika) * DAKIKA_PUANI,
        )
    }

    /** `12:34` biçimi — sayaç metni. */
    fun sayacMetni(saniye: Int): String =
        "%d:%02d".format(saniye / SANIYE_DAKIKA, saniye % SANIYE_DAKIKA)
}
