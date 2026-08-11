package tr.org.yesilay.yesiliz.data.depo

import org.junit.Assert.assertEquals
import org.junit.Test

class OdulUzlastirmaTest {

    @Test
    fun `yeni telefonda sunucu secimleri yerele geri yuklenir`() {
        val plan = odulUzlastirmaPlani(
            yerel = emptyMap(),
            uzak = mapOf("kupon_market" to "fidan_bagisi"),
        )

        assertEquals(mapOf("kupon_market" to "fidan_bagisi"), plan.yereleYaz)
        assertEquals(emptyMap<String, String>(), plan.sunucuyaYaz)
    }

    @Test
    fun `agda dusen yerel secim yeniden gider ve uzak secim otoritedir`() {
        val plan = odulUzlastirmaPlani(
            yerel = mapOf(
                "kupon_market" to "market_kuponu",
                "kupon_spor" to "kupon_spor_kitap",
            ),
            uzak = mapOf("kupon_market" to "fidan_bagisi"),
        )

        assertEquals(mapOf("kupon_market" to "fidan_bagisi"), plan.yereleYaz)
        assertEquals(mapOf("kupon_spor" to "kupon_spor_kitap"), plan.sunucuyaYaz)
    }
}
