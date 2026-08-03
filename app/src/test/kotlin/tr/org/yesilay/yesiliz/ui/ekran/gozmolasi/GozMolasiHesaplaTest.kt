package tr.org.yesilay.yesiliz.ui.ekran.gozmolasi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GozMolasiHesaplaTest {

    @Test
    fun `basta yirmi saniye kalir`() {
        val i = GozMolasiHesapla.ilerleme(0)
        assertEquals(GozMolasiHesapla.BAKIS_SANIYE, i.kalanSaniye)
        assertEquals(0f, i.oran, 1e-6f)
        assertFalse(i.tamamlandiMi)
    }

    @Test
    fun `geri sayiyor`() {
        assertEquals(15, GozMolasiHesapla.ilerleme(5).kalanSaniye)
        assertEquals(1, GozMolasiHesapla.ilerleme(19).kalanSaniye)
    }

    @Test
    fun `yirminci saniyede tamamlanir`() {
        val i = GozMolasiHesapla.ilerleme(GozMolasiHesapla.BAKIS_SANIYE)
        assertTrue(i.tamamlandiMi)
        assertEquals(0, i.kalanSaniye)
        assertEquals(1f, i.oran, 1e-6f)
    }

    @Test
    fun `sure asilirsa deger sinirlarda kalir`() {
        val i = GozMolasiHesapla.ilerleme(100)
        assertEquals("kalan negatif olmamalı", 0, i.kalanSaniye)
        assertEquals("oran 1'i geçmemeli", 1f, i.oran, 1e-6f)
    }
}
