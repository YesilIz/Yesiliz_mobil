package tr.org.yesilay.yesiliz.ui.ekran.lig

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DavetKoduGirdisiTest {

    @Test
    fun baskaKullanicininGecerliKoduKabulEdilir() {
        assertNull(davetKodunuDogrula("ysz-7k2q", "YSZ-ABCD"))
    }

    @Test
    fun bozukKodVeKendiKoduReddedilir() {
        assertNotNull(davetKodunuDogrula("7K2Q", "YSZ-ABCD"))
        assertNotNull(davetKodunuDogrula(" ysz-abcd ", "YSZ-ABCD"))
    }
}
