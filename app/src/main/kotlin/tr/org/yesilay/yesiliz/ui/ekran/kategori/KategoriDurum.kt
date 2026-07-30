package tr.org.yesilay.yesiliz.ui.ekran.kategori

import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import tr.org.yesilay.yesiliz.risk.Bant

/**
 * Uygulama kategorileri — `docs/tasarim-referans.md §2 / 31, 32, 33`.
 *
 * Üç ekran tek ViewModel paylaşıyor (liste · kategorize et · kategori seç):
 * üçü de aynı eşleme tablosunu okuyup yazıyor, ayrı ViewModel'ler aynı
 * veriyi iki kez yükleyip birbirinden habersiz kalırdı.
 */
data class KategoriDurum(
    val gruplar: List<KategoriGrubu> = emptyList(),
    /** Yığın bar dilimleri — grup toplamlarından. */
    val toplamDk: Int = 0,
    val riskBandi: Bant? = null,
    val ebeveynAktifMi: Boolean = false,
    /** Kategorisi belirlenmemiş uygulamalar (tasarım 32). */
    val kategorisizler: List<UygulamaSatiri> = emptyList(),
    /** Alt sayfada açık olan uygulama (tasarım 33); `null` = kapalı. */
    val secilenUygulama: UygulamaSatiri? = null,
    val yukleniyor: Boolean = true,
    val ebeveyneGonderildi: Boolean = false,
) {
    /**
     * "Bugünkü ekran süresinin %65'i sosyal medya ve videoda geçti" —
     * tasarımdaki özet cümlenin oranı.
     */
    val eglenceOrani: Int
        get() {
            if (toplamDk == 0) return 0
            val eglence = gruplar
                .filter { it.kategori == Kategori.SOCIAL || it.kategori == Kategori.ENTERTAINMENT }
                .sumOf { it.toplamDk }
            return eglence * 100 / toplamDk
        }
}

data class KategoriGrubu(
    val kategori: Kategori,
    val toplamDk: Int,
    val uygulamalar: List<UygulamaSatiri>,
    val acik: Boolean = false,
)

data class UygulamaSatiri(
    val paketAdi: String,
    val ad: String,
    val dakika: Int,
    val kategori: Kategori,
    /** Tablodan gelen tahmin — alt sayfada "Önerilen" etiketiyle işaretli. */
    val onerilen: Kategori? = null,
)
