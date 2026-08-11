package tr.org.yesilay.yesiliz.data.tercih

import org.junit.Assert.assertEquals
import org.junit.Test

class SunucuTabaniGuvenligiTest {

    @Test
    fun debugKaliciGelistiriciAdresiniKullanabilir() {
        assertEquals(
            "http://192.0.2.1:8000/",
            etkinSunucuTabani(
                debugMi = true,
                saklanan = "http://192.0.2.1:8000/",
                varsayilan = "http://10.0.2.2:8000/",
            ),
        )
    }

    @Test
    fun releaseKaliciAdresiYokSayipDerlemeAdresineKilitlenir() {
        assertEquals(
            "https://api.yesiliz.example/",
            etkinSunucuTabani(
                debugMi = false,
                saklanan = "https://baska.example/",
                varsayilan = "https://api.yesiliz.example/",
            ),
        )
    }
}
