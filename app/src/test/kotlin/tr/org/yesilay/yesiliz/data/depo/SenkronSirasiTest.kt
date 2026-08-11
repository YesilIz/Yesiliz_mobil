package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SenkronSirasiTest {

    @Test
    fun `eski gizlilik istegi bitmeden yenisi sunucuya baslamaz`() = runTest {
        val sira = SenkronSirasi()
        val ilkBitebilir = CompletableDeferred<Unit>()
        val olaylar = mutableListOf<String>()

        val ilk = async {
            sira.sirala {
                olaylar += "acik-basladi"
                ilkBitebilir.await()
                olaylar += "acik-bitti"
            }
        }
        runCurrent()
        val son = async {
            sira.sirala {
                olaylar += "kapali-basladi"
                olaylar += "kapali-bitti"
            }
        }
        runCurrent()

        assertFalse(son.isCompleted)
        assertEquals(listOf("acik-basladi"), olaylar)

        ilkBitebilir.complete(Unit)
        ilk.await()
        son.await()
        assertEquals(
            listOf("acik-basladi", "acik-bitti", "kapali-basladi", "kapali-bitti"),
            olaylar,
        )
    }
}
