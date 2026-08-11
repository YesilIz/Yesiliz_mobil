package tr.org.yesilay.yesiliz.arkaplan

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class IsPlanlayiciZamanTest {

    @Test
    fun `ilkbahar dst gununde hedef 24 saatlik sureye sabitlenmez`() {
        val saat = Clock.fixed(
            Instant.parse("2026-03-28T23:30:00Z"),
            ZoneId.of("Europe/Berlin"),
        )

        assertEquals(
            Instant.parse("2026-03-29T01:00:00Z"),
            sonrakiHedefAni(saat, LocalTime.of(3, 0)),
        )
    }

    @Test
    fun `hedef gectiyse bir sonraki yerel gun secilir`() {
        val saat = Clock.fixed(
            Instant.parse("2026-08-10T08:00:00Z"),
            ZoneId.of("Europe/Istanbul"),
        )

        assertEquals(
            Instant.parse("2026-08-11T06:00:00Z"),
            sonrakiHedefAni(saat, LocalTime.of(9, 0)),
        )
    }
}
