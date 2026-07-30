package tr.org.yesilay.yesiliz.ui

import java.text.NumberFormat
import java.util.Locale

private val TR_BICIM: Locale = Locale.forLanguageTag("tr")

/**
 * Binlik ayraçlı sayı — `3240` → `3.240`.
 *
 * Türkçede binlik ayracı **nokta**, ondalık ayracı virgül. Cihazın yerel
 * ayarına bırakılırsa İngilizce telefonda `3,240` çıkar; tasarımda hep
 * `3.240` yazıyor. Bu yüzden yerel ayar sabitleniyor.
 */
fun Int.binlik(): String = NumberFormat.getIntegerInstance(TR_BICIM).format(this)

/**
 * Dakika → okunur süre. `260` → `4s 20dk`, `45` → `45 dk`, `120` → `2s`.
 *
 * Tasarımda saat/dakika arasında boşluk yok (`4s 20dk`), tek birim varken var
 * (`285 dk`) — ekran 08 ve 09'da böyle.
 */
fun Int.sureMetni(): String {
    val saat = this / 60
    val dakika = this % 60
    return when {
        saat == 0 -> "$dakika dk"
        dakika == 0 -> "${saat}s"
        else -> "${saat}s ${dakika}dk"
    }
}

/** Grafik ekseni için kısa süre: `480` → `8s`. */
fun Int.kisaSaat(): String = "${this / 60}s"

/** Grafik ekseni için kısa adım: `8000` → `8b`. */
fun Int.kisaBin(): String = "${this / 1000}b"
