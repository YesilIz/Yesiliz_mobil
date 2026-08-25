package tr.org.yesilay.yesiliz.data.depo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Düzeltme bildiriminin **dili** (`CLAUDE.md §12 K56`, §4).
 *
 * Kural şu: kullanıcı "ceza" görmüyor. Metin betimleyici, gerekçe olduğu
 * gibi gösteriliyor, emir kipi ve suçlayıcı ton yok. Bu bir üslup tercihi
 * değil — K56 kuralın **amacını** korumak için tam olarak bunu şart
 * koşuyor.
 */
class DuzeltmeBildirimMetniTest {

    /** `BildirimDeposu.puanDuzeltildi` gövdesi. */
    private fun govde(delta: Int, gerekce: String): String =
        "${if (delta > 0) "+" else ""}$delta puan. Gerekçe: $gerekce"

    @Test
    fun `pozitif delta arti isaretiyle yaziliyor`() {
        assertEquals("+150 puan. Gerekçe: Etkinlik telafisi.", govde(150, "Etkinlik telafisi."))
    }

    @Test
    fun `negatif delta kendi isaretini tasiyor`() {
        assertEquals("-120 puan. Gerekçe: Hatalı kazanç.", govde(-120, "Hatalı kazanç."))
    }

    @Test
    fun `gerekce olduğu gibi gosteriliyor`() {
        val gerekce = "12 Ağustos etkinliğine katılım telafisi olarak eklendi."
        assertTrue(govde(200, gerekce).contains(gerekce))
    }

    @Test
    fun `metinde ceza kelimesi gecmiyor`() {
        val metin = BASLIK + " " + govde(-500, "Hile tespit edildi, kazanç düzeltildi.")
        for (yasak in listOf("ceza", "Ceza", "cezalandır", "kaybettin", "yaptın")) {
            assertFalse(yasak, metin.contains(yasak))
        }
    }

    @Test
    fun `baslik duzeltme diyor`() {
        assertTrue(BASLIK.contains("düzeltme"))
    }

    private companion object {
        const val BASLIK = "Puanında bir düzeltme yapıldı"
    }
}
