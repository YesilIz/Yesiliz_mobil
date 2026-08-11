package tr.org.yesilay.yesiliz.data.depo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.org.yesilay.yesiliz.ui.ekran.lig.LigDurum

class YerelLigEsikleriTest {
    @Test
    fun `on kisilik ornek grupta yukselen ve dusen kumeleri cakisma zorlamaz`() {
        assertEquals(3, ORNEK_LIG_YUKSELEN)
        assertEquals(3, ORNEK_LIG_DUSEN)
        assertTrue(ORNEK_LIG_YUKSELEN + ORNEK_LIG_DUSEN <= 10)
    }

    @Test
    fun `sunucu yaniti gelmeden hareket esigi uydurulmaz`() {
        assertEquals(0, LigDurum().yukselen)
        assertEquals(0, LigDurum().dusen)
    }
}
