package tr.org.yesilay.yesiliz.ui.ekran.odak

/**
 * Odak seansının saf hesabı.
 *
 * `NefesFazHesapla` ve `YuruyusHesapla` ile aynı desen (CLAUDE.md §6
 * istisnası): ekran mantığının gerçekten saf bir alt-hesaba ayrılabilen
 * kısmı ViewModel'den çıkarılıp emülatörsüz test edilebilir kılınıyor.
 *
 * Seansın **doğrulaması** burada değil: ekranın kapalı kalıp kalmadığını
 * `OdakViewModel` yaşam döngüsünden okuyor. Burada yalnızca "kaç saniye
 * geçti, ne kadar kaldı, bitti mi" var.
 */
object OdakHesapla {

    /** Hızlı seçim çipleri — en sık kullanılan üç süre. */
    val SURELER = listOf(15, 25, 45)

    /** Tekerlekle seçilebilecek en kısa seans. */
    const val EN_KISA_DK = 5

    /**
     * Tekerlekle seçilebilecek en uzun seans.
     *
     * Puan süreye bağlı DEĞİL (sabit [tr.org.yesilay.yesiliz.domain.gorev.Gorev.ODAK]
     * puanı), o yüzden uzun süre seçmek bir kazanç yolu açmıyor; sınır
     * yalnızca makullük için.
     */
    const val EN_UZUN_DK = 120

    /** Tekerlek adımı — dakika. */
    const val ADIM_DK = 5

    /**
     * Tekerlekteki tüm seçenekler.
     *
     * Eskiden yalnızca [SURELER] vardı ve "Süreyi değiştir" butonu listede
     * döngüsel geziyordu: aradaki bir değer (30 dk) seçilemiyor, istenen
     * süreye kaç dokunuşta ulaşılacağı belirsiz kalıyordu.
     */
    val TEKERLEK_SURELERI: List<Int> = (EN_KISA_DK..EN_UZUN_DK step ADIM_DK).toList()

    /**
     * Seansın anlık durumu.
     *
     * @param gecenSaniye seans başladığından beri geçen süre
     * @param toplamSaniye seçilen seans uzunluğu
     */
    fun ilerleme(gecenSaniye: Int, toplamSaniye: Int): OdakIlerleme {
        val kalan = (toplamSaniye - gecenSaniye).coerceAtLeast(0)
        val oran = if (toplamSaniye <= 0) {
            0f
        } else {
            (gecenSaniye.toFloat() / toplamSaniye).coerceIn(0f, 1f)
        }
        return OdakIlerleme(
            kalanSaniye = kalan,
            oran = oran,
            tamamlandiMi = toplamSaniye > 0 && gecenSaniye >= toplamSaniye,
        )
    }

    /**
     * Seans başarılı sayılır mı.
     *
     * Ekran seans boyunca kapalı kalmalı. Tek bir uyandırma bile seansı
     * bozuyor — "telefonu bırak" görevinin anlamı bu. Ama **ceza yok**:
     * bozulan seans yalnızca puan vermiyor, var olan puanı silmiyor
     * (CLAUDE.md §4).
     */
    fun basariliMi(tamamlandiMi: Boolean, ekranAcildiMi: Boolean): Boolean =
        tamamlandiMi && !ekranAcildiMi

    /** Kalan süreyi `12:05` biçiminde verir. */
    fun kalanMetni(kalanSaniye: Int): String =
        "%d:%02d".format(kalanSaniye / 60, kalanSaniye % 60)
}

data class OdakIlerleme(
    val kalanSaniye: Int,
    /** 0..1 — halka doluluğu. */
    val oran: Float,
    val tamamlandiMi: Boolean,
)
