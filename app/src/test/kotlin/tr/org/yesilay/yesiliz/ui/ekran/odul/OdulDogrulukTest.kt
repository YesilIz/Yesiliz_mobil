package tr.org.yesilay.yesiliz.ui.ekran.odul

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OdulDogrulukTest {

    @Test
    fun `teslimat altyapisi olmayan maddi odul kazanildi sayilmaz`() {
        val hal = odulHaliniBelirle(
            hakEdildi = true,
            cokluSecenek = true,
            secimVarMi = false,
            teslimatEtkinMi = false,
        )

        assertEquals(OdulHali.PLANLANAN, hal)
    }

    @Test
    fun `etkin coklu odul secim bekler`() {
        val hal = odulHaliniBelirle(
            hakEdildi = true,
            cokluSecenek = true,
            secimVarMi = false,
            teslimatEtkinMi = true,
        )

        assertEquals(OdulHali.SECIM_BEKLIYOR, hal)
    }

    @Test
    fun `maddi odul metni teslimat sinirini aciklar`() {
        assertTrue(MADDI_ODUL_ACIKLAMASI.contains("henüz etkin değil"))
        assertTrue(MADDI_ODUL_ACIKLAMASI.contains("kupon ya da bağış oluşturmaz"))
        assertFalse(MADDI_ODUL_ACIKLAMASI.contains("kazanırsın"))
    }

    @Test
    fun `basari karti resmi belge gibi sunulmaz`() {
        assertTrue(BASARI_KARTI_ACIKLAMASI.contains("uygulama içi"))
        assertTrue(BASARI_KARTI_ACIKLAMASI.contains("resmî belge değildir"))
    }
}
