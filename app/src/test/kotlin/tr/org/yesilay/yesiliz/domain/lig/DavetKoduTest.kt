package tr.org.yesilay.yesiliz.domain.lig

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DavetKoduTest {

    @Test
    fun `kod tasarimdaki bicimde uretiliyor`() {
        val kod = DavetKodu.uret(Random(42))

        // YSZ-XXXXXXXX
        assertTrue(kod, Regex("^YSZ-[A-HJ-NP-Z2-9]{8}$").matches(kod))
        assertEquals(DavetKodu.UZUNLUK, kod.removePrefix("YSZ-").length)
    }

    @Test
    fun `ayni tohum ayni kodu veriyor`() {
        assertEquals(DavetKodu.uret(Random(7)), DavetKodu.uret(Random(7)))
    }

    @Test
    fun `karistirilabilir karakterler kullanilmiyor`() {
        // I/1 ve O/0 telefonda sesli okunurken ayırt edilemiyor.
        val yasakli = setOf('I', 'O', '0', '1')
        repeat(500) { tohum ->
            val govde = DavetKodu.uret(Random(tohum)).removePrefix("YSZ-")
            govde.forEach { karakter ->
                assertFalse("'$karakter' alfabede olmamalı", karakter in yasakli)
            }
        }
    }

    @Test
    fun `uretilen her kod gecerli sayiliyor`() {
        repeat(200) { tohum ->
            assertTrue(DavetKodu.gecerliMi(DavetKodu.uret(Random(tohum))))
        }
    }

    @Test
    fun `gecerlilik kucuk harf ve bosluga tolerans gosteriyor`() {
        assertTrue(DavetKodu.gecerliMi("ysz-7k2qabcd"))
        assertTrue(DavetKodu.gecerliMi("  YSZ-7K2QABCD  "))
    }

    @Test
    fun `bozuk kodlar reddediliyor`() {
        assertFalse(DavetKodu.gecerliMi(""))
        assertFalse(DavetKodu.gecerliMi("YSZ-"))
        assertFalse(DavetKodu.gecerliMi("YSZ-7K2Q"))
        assertFalse(DavetKodu.gecerliMi("YSZ-7K2QABC"))
        assertFalse(DavetKodu.gecerliMi("YSZ-7K2QABCDF"))
        assertFalse(DavetKodu.gecerliMi("ABC-7K2QABCD"))
        // Yasaklı karakter içeren kod da geçersiz.
        assertFalse(DavetKodu.gecerliMi("YSZ-7K2OABCD"))
    }
}
