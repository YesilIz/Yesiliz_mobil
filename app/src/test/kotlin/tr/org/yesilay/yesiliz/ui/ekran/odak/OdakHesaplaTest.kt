package tr.org.yesilay.yesiliz.ui.ekran.odak

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** `NefesFazHesaplaTest` deseni — saf hesap, emülatörsüz. */
class OdakHesaplaTest {

    @Test
    fun `basta tam sure kalir`() {
        val i = OdakHesapla.ilerleme(gecenSaniye = 0, toplamSaniye = 900)
        assertEquals(900, i.kalanSaniye)
        assertEquals(0f, i.oran, 1e-6f)
        assertFalse(i.tamamlandiMi)
    }

    @Test
    fun `yarida oran yarim`() {
        val i = OdakHesapla.ilerleme(gecenSaniye = 450, toplamSaniye = 900)
        assertEquals(450, i.kalanSaniye)
        assertEquals(0.5f, i.oran, 1e-6f)
    }

    @Test
    fun `sure dolunca tamamlanir ve kalan sifirin altina inmez`() {
        val i = OdakHesapla.ilerleme(gecenSaniye = 1_000, toplamSaniye = 900)
        assertTrue(i.tamamlandiMi)
        assertEquals("kalan negatif olmamalı", 0, i.kalanSaniye)
        assertEquals("oran 1'i geçmemeli", 1f, i.oran, 1e-6f)
    }

    @Test
    fun `sifir sureli seans tamamlanmis sayilmaz`() {
        // Sıfıra bölme yok ve "hiç başlamadan bitti" durumu üretilmiyor.
        val i = OdakHesapla.ilerleme(gecenSaniye = 0, toplamSaniye = 0)
        assertEquals(0f, i.oran, 1e-6f)
        assertFalse(i.tamamlandiMi)
    }

    @Test
    fun `ekran acilirsa seans basarisiz`() {
        assertTrue(OdakHesapla.basariliMi(tamamlandiMi = true, ekranAcildiMi = false))
        assertFalse(
            "telefon açıldıysa puan yok",
            OdakHesapla.basariliMi(tamamlandiMi = true, ekranAcildiMi = true),
        )
        assertFalse(
            "süre dolmadıysa puan yok",
            OdakHesapla.basariliMi(tamamlandiMi = false, ekranAcildiMi = false),
        )
    }

    @Test
    fun `kalan metni dakika saniye`() {
        assertEquals("0:00", OdakHesapla.kalanMetni(0))
        assertEquals("0:09", OdakHesapla.kalanMetni(9))
        assertEquals("1:00", OdakHesapla.kalanMetni(60))
        assertEquals("14:59", OdakHesapla.kalanMetni(899))
        assertEquals("45:00", OdakHesapla.kalanMetni(2_700))
    }

    @Test
    fun `secilebilir sureler artan ve tekrarsiz`() {
        assertEquals(OdakHesapla.SURELER.sorted(), OdakHesapla.SURELER)
        assertEquals(OdakHesapla.SURELER.distinct(), OdakHesapla.SURELER)
        assertTrue("tüm süreler pozitif", OdakHesapla.SURELER.all { it > 0 })
    }
}
