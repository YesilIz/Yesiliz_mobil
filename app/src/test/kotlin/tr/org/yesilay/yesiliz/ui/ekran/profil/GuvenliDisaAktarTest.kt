package tr.org.yesilay.yesiliz.ui.ekran.profil

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuvenliDisaAktarTest {

    @Test
    fun `dosya hatasi kullaniciya yeniden deneme durumu dondurur`() = runTest {
        var paylasilan = false

        val basarili = guvenliDisaAktar<String>(
            uret = { throw IllegalStateException("disk dolu") },
            onHazir = { paylasilan = true },
        )

        assertFalse(basarili)
        assertFalse(paylasilan)
    }

    @Test
    fun `basarili dosya paylasim callbackine aynen verilir`() = runTest {
        var paylasilan = ""

        val basarili = guvenliDisaAktar(
            uret = { "dosya" },
            onHazir = { paylasilan = it },
        )

        assertTrue(basarili)
        assertEquals("dosya", paylasilan)
    }

    @Test(expected = CancellationException::class)
    fun `yasam dongusu iptali hata diye yutulmaz`() = runTest {
        guvenliDisaAktar<String>(
            uret = { throw CancellationException("ekran kapandi") },
            onHazir = {},
        )
    }
}
