package tr.org.yesilay.yesiliz.data.depo

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HesapSilmeGuvenligiTest {

    @Test
    fun uzakSilmeBasarisizsaYerelVeriKorunur() = runBlocking {
        var yerelSilme = 0

        val sonuc = uzakSilinirseYereliTemizle(
            uzagiSil = { false },
            yereliSil = { yerelSilme++ },
        )

        assertFalse(sonuc)
        assertEquals(0, yerelSilme)
    }

    @Test
    fun uzakSilmeBasariliysaYerelVeriBirKezSilinir() = runBlocking {
        var yerelSilme = 0

        val sonuc = uzakSilinirseYereliTemizle(
            uzagiSil = { true },
            yereliSil = { yerelSilme++ },
        )

        assertTrue(sonuc)
        assertEquals(1, yerelSilme)
    }

    @Test
    fun uzakHataBasarisizSonucaDoner() = runBlocking {
        assertFalse(hesapSilmeIstegi { throw IOException("baglanti yok") })
    }

    @Test(expected = CancellationException::class)
    fun hesapSilmeIptaliYutulmaz() {
        runBlocking {
            hesapSilmeIstegi { throw CancellationException("ekran kapandi") }
        }
    }
}
