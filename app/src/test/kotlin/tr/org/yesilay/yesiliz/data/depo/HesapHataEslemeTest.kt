package tr.org.yesilay.yesiliz.data.depo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.org.yesilay.yesiliz.domain.hesap.HesapAlani

class HesapHataEslemeTest {

    @Test
    fun `eposta servisi yoksa alan yerine genel hata gosterilir`() {
        assertEquals(HesapAlani.GENEL, hesapHataAlani(503))
        assertTrue(hesapHataMesaji(503).contains("henüz kullanılamıyor"))
    }
}
