package tr.org.yesilay.yesiliz.ui.ekran.sozlesme

import tr.org.yesilay.yesiliz.domain.sozlesme.Sozlesme

/**
 * Sözleşme metni ekranı — Kullanım Koşulları, Gizlilik Politikası ve açık
 * kaynak lisansları aynı ekranı paylaşıyor.
 */
data class SozlesmeDurum(
    val sozlesme: Sozlesme? = null,
    val yukleniyor: Boolean = true,
)
