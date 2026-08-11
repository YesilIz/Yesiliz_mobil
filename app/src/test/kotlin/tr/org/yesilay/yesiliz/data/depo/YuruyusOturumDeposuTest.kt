package tr.org.yesilay.yesiliz.data.depo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YuruyusOturumDeposuTest {

    @Test
    fun `erken bitis hedef tamamlandi veya puan verildi demez`() {
        val depo = YuruyusOturumDeposu()
        depo.surdur()

        depo.tamamlandi(hedefTamamlandi = false, puanVerildi = false)

        assertTrue(depo.oturum.value.tamamlandi)
        assertFalse(depo.oturum.value.hedefTamamlandi)
        assertFalse(depo.oturum.value.puanVerildi)
    }

    @Test
    fun `hedef tamamlanmasi ile gunluk puan sonucu ayri tutulur`() {
        val depo = YuruyusOturumDeposu()

        depo.tamamlandi(hedefTamamlandi = true, puanVerildi = false)

        assertTrue(depo.oturum.value.hedefTamamlandi)
        assertFalse(depo.oturum.value.puanVerildi)
    }
}
