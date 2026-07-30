package tr.org.yesilay.yesiliz.ui.ekran.yuruyus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YuruyusHesaplaTest {

    @Test
    fun `baslangicta oran sifir puan sifir`() {
        val i = YuruyusHesapla.ilerleme(0)

        assertEquals(0f, i.oran, 0.001f)
        assertEquals(0, i.kazanilanPuan)
        assertFalse(i.tamamlandiMi)
    }

    @Test
    fun `puan tam dakikalarda veriliyor`() {
        // 59 saniye yürüyüp puan almak "doğrulanmış yürüyüş" tanımını bozar.
        assertEquals(0, YuruyusHesapla.ilerleme(59).kazanilanPuan)
        assertEquals(YuruyusHesapla.DAKIKA_PUANI, YuruyusHesapla.ilerleme(60).kazanilanPuan)
        assertEquals(YuruyusHesapla.DAKIKA_PUANI, YuruyusHesapla.ilerleme(119).kazanilanPuan)
        assertEquals(2 * YuruyusHesapla.DAKIKA_PUANI, YuruyusHesapla.ilerleme(120).kazanilanPuan)
    }

    @Test
    fun `hedefte tamamlaniyor ve gorev puanina esit`() {
        val i = YuruyusHesapla.ilerleme(YuruyusHesapla.HEDEF_DAKIKA * 60)

        assertTrue(i.tamamlandiMi)
        assertEquals(1f, i.oran, 0.001f)
        assertEquals(0, i.kalanSaniye)
        // Gorev.YURUYUS 45 puan = 15 dk × 3 (spec-puan.md §7.1)
        assertEquals(45, i.kazanilanPuan)
    }

    @Test
    fun `hedefi asmak puani buyutmuyor`() {
        val i = YuruyusHesapla.ilerleme(YuruyusHesapla.HEDEF_DAKIKA * 60 * 3)

        assertEquals(45, i.kazanilanPuan)
        assertEquals(1f, i.oran, 0.001f)
        assertEquals(0, i.kalanSaniye)
    }

    @Test
    fun `negatif sure sifira kirpiliyor`() {
        val i = YuruyusHesapla.ilerleme(-30)

        assertEquals(0, i.gecenSaniye)
        assertEquals(0, i.kazanilanPuan)
    }

    @Test
    fun `sayac metni iki haneli dakika ile bicimleniyor`() {
        assertEquals("0:00", YuruyusHesapla.sayacMetni(0))
        assertEquals("0:07", YuruyusHesapla.sayacMetni(7))
        assertEquals("1:05", YuruyusHesapla.sayacMetni(65))
        assertEquals("15:00", YuruyusHesapla.sayacMetni(900))
    }

    @Test
    fun `kalan sure hedefe gore azaliyor`() {
        assertEquals(900, YuruyusHesapla.ilerleme(0).kalanSaniye)
        assertEquals(600, YuruyusHesapla.ilerleme(300).kalanSaniye)
    }
}
