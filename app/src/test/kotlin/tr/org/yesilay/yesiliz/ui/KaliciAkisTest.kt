package tr.org.yesilay.yesiliz.ui

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KaliciAkisTest {

    @Test
    fun `ilerleme yalnizca kalici yazma bittikten sonra olur`() = runBlocking {
        val sira = mutableListOf<String>()

        kalicilastirVeIlerle(
            kalicilastir = { sira += "yazildi" },
            ilerle = { sira += "ilerledi" },
        )

        assertEquals(listOf("yazildi", "ilerledi"), sira)
    }

    @Test
    fun `kalici yazma basarisizsa basari veya gezinme tetiklenmez`() = runBlocking {
        var ilerledi = false

        try {
            kalicilastirVeIlerle(
                kalicilastir = { error("disk yazma hatasi") },
                ilerle = { ilerledi = true },
            )
        } catch (_: IllegalStateException) {
            // Beklenen: hata başarı durumuna dönüştürülmez.
        }

        assertFalse(ilerledi)
    }
}
