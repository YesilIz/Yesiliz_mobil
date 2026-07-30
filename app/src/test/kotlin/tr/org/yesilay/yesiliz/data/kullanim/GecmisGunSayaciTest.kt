package tr.org.yesilay.yesiliz.data.kullanim

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Geçmiş gün sayacı — `queryAndAggregateUsageStats` çıktısını
 * `Tercihler.gecmisGunSayisi`'ne çeviren saf fonksiyon.
 */
class GecmisGunSayaciTest {

    @Test
    fun `bos liste sifir gun verir`() {
        assertEquals(0, GecmisGunSayaci.say(emptyList()))
    }

    @Test
    fun `esigin altindaki gurultu sayilmaz`() {
        assertEquals(0, GecmisGunSayaci.say(listOf(0L, 1_000L, 59_999L)))
    }

    @Test
    fun `esik ve ustu gunler sayilir`() {
        assertEquals(2, GecmisGunSayaci.say(listOf(60_000L, 0L, 3_600_000L, 30_000L)))
    }

    @Test
    fun `hepsi gercek kullanimsa hepsi sayilir`() {
        val otuzGun = List(30) { 600_000L }
        assertEquals(30, GecmisGunSayaci.say(otuzGun))
    }
}
