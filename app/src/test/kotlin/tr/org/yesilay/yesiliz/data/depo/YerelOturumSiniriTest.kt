package tr.org.yesilay.yesiliz.data.depo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YerelOturumSiniriTest {

    @Test
    fun `ilk sahip ve ayni hesap veriyi korur`() {
        assertFalse(yerelVeriTemizlenmeli(null, "a@example.com"))
        assertFalse(yerelVeriTemizlenmeli("a@example.com", "a@example.com"))
    }

    @Test
    fun `farkli hesap veya hesaptan misafire gecis veriyi temizler`() {
        assertTrue(yerelVeriTemizlenmeli("a@example.com", "b@example.com"))
        assertTrue(yerelVeriTemizlenmeli("a@example.com", MISAFIR_SAHIBI))
        assertTrue(yerelVeriTemizlenmeli(BILINMEYEN_SAHIP, "b@example.com"))
    }

    @Test
    fun `misafirden hesaba donusum veriyi korur`() {
        assertFalse(yerelVeriTemizlenmeli(MISAFIR_SAHIBI, "a@example.com"))
    }
}
