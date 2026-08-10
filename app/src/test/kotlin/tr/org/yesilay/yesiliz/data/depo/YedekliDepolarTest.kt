package tr.org.yesilay.yesiliz.data.depo

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class YedekliDepolarTest {

    @Test
    fun baglantiHatasindaYerelYedekCalisir() = runBlocking {
        assertEquals("yerel", agdanAl({ throw IOException("çevrimdışı") }, { "yerel" }))
    }

    @Test
    fun isHatasiYerelBasariyaDonusturulmez() {
        var yerelCalisti = false

        try {
            runBlocking {
                agdanAl(
                    uzak = { throw IllegalStateException("sunucu reddetti") },
                    yerel = { yerelCalisti = true; "yerel" },
                )
            }
        } catch (_: IllegalStateException) {
            // Beklenen: iş hatası çağırana ulaşır.
        }

        assertFalse(yerelCalisti)
    }

    @Test(expected = CancellationException::class)
    fun iptalYerelYanEtkiUretmez() = run {
        runBlocking {
            agdanAl<String>(
                uzak = { throw CancellationException("ekran kapandı") },
                yerel = { "yanlış başarı" },
            )
        }
        Unit
    }
}
