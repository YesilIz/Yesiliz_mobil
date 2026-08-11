package tr.org.yesilay.yesiliz.arkaplan

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.junit.Assert.assertEquals
import org.junit.Test

class TekSeferlikIslemTest {

    @Test
    fun `es zamanli bitirme girisimlerinden yalnizca biri sahiplenir`() {
        val kilit = TekSeferlikIslem()
        val havuz = Executors.newFixedThreadPool(8)
        try {
            val sonuclar = havuz.invokeAll(List(32) { Callable { kilit.sahiplen() } })
                .map { it.get() }

            assertEquals(1, sonuclar.count { it })
        } finally {
            havuz.shutdownNow()
        }
    }
}
