package tr.org.yesilay.yesiliz.data.uzak

import org.junit.Assert.assertEquals
import org.junit.Test

class JetonYenilemeSiniflandirmaTest {

    @Test
    fun gecersizJetonYanitlariOturumuKapattirir() {
        listOf(400, 401, 403, 422).forEach { kod ->
            assertEquals("HTTP $kod", YenilemeSonuc.Reddedildi, yenilemeHttpSonucu(kod))
        }
    }

    @Test
    fun geciciSunucuYanitlariOturumuKorur() {
        listOf(408, 429, 500, 502, 503, 504).forEach { kod ->
            assertEquals("HTTP $kod", YenilemeSonuc.AgHatasi, yenilemeHttpSonucu(kod))
        }
    }
}
