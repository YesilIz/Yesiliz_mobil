package tr.org.yesilay.yesiliz.data.zaman

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GuncelGunTest {

    @Test
    fun `yenile gece yarisindan sonra yeni gunu yayar`() = runBlocking {
        val saat = AyarlanabilirSaat(
            Instant.parse("2026-08-10T20:59:59Z"),
            ZoneId.of("Europe/Istanbul"),
        )
        val guncelGun = GuncelGun(saat)
        val tarihler = mutableListOf<LocalDate>()
        val dinleyici = launch { guncelGun.akis.take(2).toList(tarihler) }
        yield()

        saat.an = Instant.parse("2026-08-10T21:00:01Z")
        guncelGun.yenile()
        dinleyici.join()

        assertEquals(
            listOf(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11)),
            tarihler,
        )
        assertEquals(LocalDate.of(2026, 8, 11), guncelGun.bugun())
    }

    @Test
    fun `gece yarisi beklemesi dst gununde sabit 24 saat varsaymaz`() {
        val zon = ZoneId.of("Europe/Berlin")
        val saat = Clock.fixed(Instant.parse("2026-03-28T23:30:00Z"), zon)

        assertEquals(Duration.ofMinutes(22 * 60L + 30L).toMillis(), sonrakiGuneKalanMillis(saat))
    }
}

private class AyarlanabilirSaat(
    var an: Instant,
    private val bolge: ZoneId,
) : Clock() {
    override fun getZone(): ZoneId = bolge

    override fun withZone(zone: ZoneId): Clock = AyarlanabilirSaat(an, zone)

    override fun instant(): Instant = an
}
