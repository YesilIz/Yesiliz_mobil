package tr.org.yesilay.yesiliz.ui.ekran.lig

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Son **0** kişi bir alt lige iniyor" bir kural değil, kuralın yokluğu —
 * üstelik tek kişilik bir ligde olmayan bir cezayı gündeme getiriyor
 * (CLAUDE.md §4, 5. tarama G5-030).
 */
class LigTerfiMetniTest {

    @Test
    fun `iki kol da varsa ikisi de soylenir`() {
        val metin = ligTerfiMetni(yukselen = 3, dusen = 3)

        assertTrue(metin.contains("ilk 3 kişi bir üst lige"))
        assertTrue(metin.contains("son 3 kişi bir alt lige"))
    }

    @Test
    fun `dusen sifirken alt lige inmekten hic soz edilmez`() {
        val metin = ligTerfiMetni(yukselen = 3, dusen = 0)

        assertTrue(metin.contains("ilk 3 kişi bir üst lige"))
        assertFalse("olmayan bir tenzil duyurulmamalı", metin.contains("alt lige"))
        assertFalse(metin.contains("0"))
    }

    @Test
    fun `yukselen sifirken terfiden hic soz edilmez`() {
        val metin = ligTerfiMetni(yukselen = 0, dusen = 3)

        assertTrue(metin.contains("son 3 kişi bir alt lige"))
        assertFalse(metin.contains("üst lige"))
        assertFalse(metin.contains("0"))
    }

    @Test
    fun `ikisi de sifirken degisim olmadigi soylenir`() {
        val metin = ligTerfiMetni(yukselen = 0, dusen = 0)

        assertFalse(metin.contains("üst lige"))
        assertFalse(metin.contains("alt lige"))
        assertTrue(metin.contains("lig değişimi yok"))
    }
}
