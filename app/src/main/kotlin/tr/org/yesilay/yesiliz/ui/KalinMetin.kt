package tr.org.yesilay.yesiliz.ui

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

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
