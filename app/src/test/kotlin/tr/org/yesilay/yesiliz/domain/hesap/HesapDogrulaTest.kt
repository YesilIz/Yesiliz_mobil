package tr.org.yesilay.yesiliz.domain.hesap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HesapDogrulaTest {

    @Test
    fun `gecerli eposta kabul ediliyor`() {
        assertNull(HesapDogrula.eposta("turan@example.com"))
        assertNull(HesapDogrula.eposta("a.b+etiket@alt.alan.com.tr"))
    }

    @Test
    fun `bozuk eposta reddediliyor`() {
        assertNotNull(HesapDogrula.eposta(""))
        assertNotNull(HesapDogrula.eposta("turan"))
        assertNotNull(HesapDogrula.eposta("turan@"))
        assertNotNull(HesapDogrula.eposta("@example.com"))
        assertNotNull(HesapDogrula.eposta("turan@example"))
        assertNotNull(HesapDogrula.eposta("turan @example.com"))
    }

    @Test
    fun `eposta bastaki sondaki bosluga tolerans gosteriyor`() {
        assertNull(HesapDogrula.eposta("  turan@example.com  "))
    }

    @Test
    fun `sifre uzunlugu esigi uygulaniyor`() {
        assertNotNull(HesapDogrula.sifre(""))
        assertNotNull(HesapDogrula.sifre("a".repeat(HesapDogrula.EN_AZ_SIFRE - 1)))
        assertNull(HesapDogrula.sifre("a".repeat(HesapDogrula.EN_AZ_SIFRE)))
    }

    @Test
    fun `sifre tekrari eslesmeli`() {
        assertNull(HesapDogrula.sifreTekrar("parola123", "parola123"))
        assertNotNull(HesapDogrula.sifreTekrar("parola123", "parola124"))
    }

    @Test
    fun `ad en az iki harf`() {
        assertNotNull(HesapDogrula.ad(""))
        assertNotNull(HesapDogrula.ad("  "))
        assertNotNull(HesapDogrula.ad("A"))
        assertNull(HesapDogrula.ad("Ay"))
        assertNull(HesapDogrula.ad("Turan Kechagia"))
    }

    @Test
    fun `hata metinleri unlem ve suclama icermiyor`() {
        // CLAUDE.md §4: emir kipi ve korkutma yok. Form hataları da aynı dilde.
        val mesajlar = listOfNotNull(
            HesapDogrula.eposta(""),
            HesapDogrula.eposta("bozuk"),
            HesapDogrula.ad(""),
            HesapDogrula.sifre(""),
            HesapDogrula.sifre("kisa"),
            HesapDogrula.sifreTekrar("a", "b"),
        )
        assertEquals(6, mesajlar.size)
        mesajlar.forEach { mesaj ->
            assertTrue("ünlem var: $mesaj", !mesaj.contains("!"))
            assertTrue("büyük harf bağırıyor: $mesaj", mesaj != mesaj.uppercase())
        }
    }
}
