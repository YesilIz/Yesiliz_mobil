package tr.org.yesilay.yesiliz.ui

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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

/**
 * Tek vurgu biçimi: `**kalın**`.
 *
 * Markdown kütüphanesi eklemek yerine tek kural elle işleniyor — içerik
 * bizim yazdığımız asset dosyalarından ibaret, keyfi markdown gelmiyor.
 * Bilgi Bankası yazıları ve sözleşme metinleri ortak kullanıyor.
 */
fun kalinIsle(metin: String) = buildAnnotatedString {
    var kalan = metin
    while (true) {
        val bas = kalan.indexOf(KALIN_AYRACI)
        if (bas < 0) break
        val son = kalan.indexOf(KALIN_AYRACI, bas + KALIN_AYRACI.length)
        if (son < 0) break

        append(kalan.substring(0, bas))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(kalan.substring(bas + KALIN_AYRACI.length, son))
        }
        kalan = kalan.substring(son + KALIN_AYRACI.length)
    }
    append(kalan)
}

private const val KALIN_AYRACI = "**"
