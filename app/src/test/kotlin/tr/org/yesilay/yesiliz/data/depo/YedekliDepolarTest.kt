package tr.org.yesilay.yesiliz.data.depo

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

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

    @Test
    fun demoKodundaYetkisizKullaniciYerelYedegeDoner() = runBlocking {
        val govde = "{}".toResponseBody("application/json".toMediaType())
        val yetkisiz = HttpException(Response.error<Any>(401, govde))

        assertEquals("yerel-kod", demoYedegi({ throw yetkisiz }, { "yerel-kod" }))
    }

    @Test
    fun demoKodundaIsHatasiYerelBasariyaDonusmez() {
        val govde = "{}".toResponseBody("application/json".toMediaType())
        val cakisma = HttpException(Response.error<Any>(409, govde))
        var yerelCalisti = false

        try {
            runBlocking {
                demoYedegi(
                    uzak = { throw cakisma },
                    yerel = { yerelCalisti = true; "yanlis-basari" },
                )
            }
        } catch (hata: HttpException) {
            assertEquals(409, hata.code())
        }

        assertFalse(yerelCalisti)
    }

    @Test(expected = CancellationException::class)
    fun demoYedegiIptaliYutmaz() = run {
        runBlocking {
            demoYedegi<String>(
                uzak = { throw CancellationException("ekran kapandı") },
                yerel = { "yanlış başarı" },
            )
        }
        Unit
    }
}
