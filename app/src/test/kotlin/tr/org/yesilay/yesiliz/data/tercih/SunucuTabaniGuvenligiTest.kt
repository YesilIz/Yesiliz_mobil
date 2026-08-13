package tr.org.yesilay.yesiliz.data.tercih

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    /**
     * Varsayılan adres emülatörünkiyken kaydedilmiş ezmeler cihazlarda kaldı;
     * işaretçisi olmayan her kurulumda bir kez siliniyor.
     */
    @Test
    fun isaretcisiOlmayanKurulumdaEskiAdresTemizlenir() {
        assertTrue(sunucuAdresiTemizlenmeli(null))
        assertTrue(sunucuAdresiTemizlenmeli(0))
    }

    @Test
    fun temizlikBirKezKosar() {
        assertFalse(sunucuAdresiTemizlenmeli(Tercihler.SUNUCU_TABANI_GECERLI_SURUM))
    }
}
