package tr.org.yesilay.yesiliz.ui.ekran.profil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HesapSilmeOnayiTest {

    @Test
    fun turkceKucukVeBuyukHarfKabulEdilir() {
        assertTrue(hesapSilmeOnayiGecerli("SİL"))
        assertTrue(hesapSilmeOnayiGecerli("sil"))
        assertTrue(hesapSilmeOnayiGecerli("  Sil  "))
    }

    @Test
    fun yanlisOnayReddedilir() {
        assertFalse(hesapSilmeOnayiGecerli("silme"))
        assertFalse(hesapSilmeOnayiGecerli(""))
    }
}
