package tr.org.yesilay.yesiliz.ui.ekran.lig

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DavetKoduGirdisiTest {

    @Test
    fun baskaKullanicininGecerliKoduKabulEdilir() {
        assertNull(davetKodunuDogrula("ysz-7k2qabcd", "YSZ-ABCDEFGH"))
    }

    @Test
    fun bozukKodVeKendiKoduReddedilir() {
        assertNotNull(davetKodunuDogrula("7K2QABCD", "YSZ-ABCDEFGH"))
        assertNotNull(davetKodunuDogrula(" ysz-abcdefgh ", "YSZ-ABCDEFGH"))
    }
}
