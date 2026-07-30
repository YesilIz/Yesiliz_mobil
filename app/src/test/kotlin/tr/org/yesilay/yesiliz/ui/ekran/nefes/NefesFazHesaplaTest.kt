package tr.org.yesilay.yesiliz.ui.ekran.nefes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NefesFazHesaplaTest {

    @Test
    fun `basta ilk faz ve tam faz suresi kalir`() {
        val ilerleme = NefesFazHesapla.ilerlemeyiHesapla(toplamSaniye = 120, gecenSaniye = 0, fazSaniye = 4)
        assertEquals(0, ilerleme.fazIndeksi)
        assertEquals(4, ilerleme.fazIciKalanSaniye)
        assertEquals(120, ilerleme.toplamKalanSaniye)
        assertFalse(ilerleme.tamamlandiMi)
    }

    @Test
    fun `faz siniri tam saniyede bir sonrakine gecer`() {
        // fazSaniye=4: 0-3 NEFES_AL, 4-7 TUT_1, ...
        assertEquals(0, NefesFazHesapla.ilerlemeyiHesapla(120, 3, 4).fazIndeksi)
        assertEquals(1, NefesFazHesapla.ilerlemeyiHesapla(120, 4, 4).fazIndeksi)
        assertEquals(2, NefesFazHesapla.ilerlemeyiHesapla(120, 8, 4).fazIndeksi)
        assertEquals(3, NefesFazHesapla.ilerlemeyiHesapla(120, 12, 4).fazIndeksi)
    }

    @Test
    fun `dorduncu fazdan sonra basa doner`() {
        assertEquals(0, NefesFazHesapla.ilerlemeyiHesapla(120, 16, 4).fazIndeksi)
    }

    @Test
    fun `farkli tempo faz sinirlarini degistirir`() {
        // fazSaniye=6: 0-5 NEFES_AL, 6-11 TUT_1
        assertEquals(0, NefesFazHesapla.ilerlemeyiHesapla(120, 5, 6).fazIndeksi)
        assertEquals(1, NefesFazHesapla.ilerlemeyiHesapla(120, 6, 6).fazIndeksi)
    }

    @Test
    fun `tempo degisimi ayni gecen saniyeyi yeniden yorumlar`() {
        // 5 saniye geçmişken tempo 4'ten 6'ya değişirse, aynı "gecenSaniye" ile
        // yeniden hesaplanan faz farklı olabilir — özel geçiş kodu gerekmez.
        val eskiTempo = NefesFazHesapla.ilerlemeyiHesapla(120, 5, 4)
        val yeniTempo = NefesFazHesapla.ilerlemeyiHesapla(120, 5, 6)
        assertEquals(1, eskiTempo.fazIndeksi)
        assertEquals(0, yeniTempo.fazIndeksi)
    }

    @Test
    fun `toplam sureye ulasinca tamamlandi doner`() {
        val ilerleme = NefesFazHesapla.ilerlemeyiHesapla(toplamSaniye = 120, gecenSaniye = 120, fazSaniye = 4)
        assertTrue(ilerleme.tamamlandiMi)
        assertEquals(0, ilerleme.toplamKalanSaniye)
        assertEquals(0, ilerleme.fazIciKalanSaniye)
    }

    @Test
    fun `gecen sure toplami asarsa negatif kalan olusmaz`() {
        val ilerleme = NefesFazHesapla.ilerlemeyiHesapla(toplamSaniye = 120, gecenSaniye = 200, fazSaniye = 4)
        assertTrue(ilerleme.tamamlandiMi)
        assertEquals(0, ilerleme.toplamKalanSaniye)
    }

    @Test
    fun `toplam kalan saniye dogru azalir`() {
        val ilerleme = NefesFazHesapla.ilerlemeyiHesapla(toplamSaniye = 120, gecenSaniye = 45, fazSaniye = 4)
        assertEquals(75, ilerleme.toplamKalanSaniye)
    }
}
