package tr.org.yesilay.yesiliz.data.depo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartbeatRizaTest {

    @Test
    fun `yalnizca riza veren eslesmis cocuk heartbeat yollar`() {
        assertTrue(heartbeatGonderilebilir(true, false, true, true, "cocuk"))
    }

    @Test
    fun `paylasim kapaliysa heartbeat yollanmaz`() {
        assertFalse(heartbeatGonderilebilir(true, false, false, true, "cocuk"))
    }

    @Test
    fun `eslesmemis veya ebeveyn cihaz heartbeat yollamaz`() {
        assertFalse(heartbeatGonderilebilir(true, false, true, false, "cocuk"))
        assertFalse(heartbeatGonderilebilir(true, false, true, true, "ebeveyn"))
    }
}
