package tr.org.yesilay.yesiliz.zaman

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GecenSureHesaplaTest {

    @Test
    fun `tam saniyeler monoton saat farkindan hesaplanir`() {
        assertEquals(0, GecenSureHesapla.saniye(10_000, 10_999))
        assertEquals(1, GecenSureHesapla.saniye(10_000, 11_000))
        assertEquals(90, GecenSureHesapla.saniye(10_000, 100_999))
    }

    @Test
    fun `derin uyku kadar ileri sicrayan saat tum sureyi sayar`() {
        val baslangic = 50_000L
        val uykuSonrasi = baslangic + 25 * 60 * 1_000L

        assertTrue(GecenSureHesapla.dolduMu(baslangic, uykuSonrasi, 25 * 60))
        assertFalse(GecenSureHesapla.dolduMu(baslangic, uykuSonrasi - 1, 25 * 60))
    }

    @Test
    fun `beklenmedik geri giden girdi negatif sure uretmez`() {
        assertEquals(0, GecenSureHesapla.saniye(20_000, 10_000))
    }
}
