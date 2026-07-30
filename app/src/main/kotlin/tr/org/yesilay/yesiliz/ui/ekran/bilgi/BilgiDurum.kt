package tr.org.yesilay.yesiliz.ui.ekran.bilgi

import tr.org.yesilay.yesiliz.domain.bilgi.BilgiYazisi

/**
 * Bilgi Bankası liste ekranının durumu.
 *
 * Arama kutusu **bilinçli olarak yok**: on üç yazı için arama, ekrana
 * kalabalık ekleyip hiçbir şey çözmüyor. Kategori çipleri yeterli.
 */
data class BilgiDurum(
    val yazilar: List<BilgiYazisi> = emptyList(),
    val kategoriler: List<String> = emptyList(),
    /** `null` = tümü */
    val seciliKategori: String? = null,
    val yukleniyor: Boolean = true,
) {
    val gorunenYazilar: List<BilgiYazisi>
        get() = seciliKategori?.let { kat -> yazilar.filter { it.kategori == kat } } ?: yazilar
}

/** Yazı detayının durumu — tek yazı, ayrı ViewModel'e değmez. */
data class BilgiYaziDurum(
    val yazi: BilgiYazisi? = null,
    val yukleniyor: Boolean = true,
)
