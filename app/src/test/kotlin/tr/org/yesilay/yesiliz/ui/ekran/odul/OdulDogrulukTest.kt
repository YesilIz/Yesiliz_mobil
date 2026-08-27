package tr.org.yesilay.yesiliz.ui.ekran.odul

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.org.yesilay.yesiliz.data.depo.MADDI_ODUL_TESLIMATI_ETKIN

class OdulDogrulukTest {

    @Test
    fun `maddi odul teslimat kapisi fail closed`() {
        assertFalse(MADDI_ODUL_TESLIMATI_ETKIN)
    }

    @Test
    fun `teslimat altyapisi olmayan maddi odul kazanildi sayilmaz`() {
        val hal = odulHaliniBelirle(
            hakEdildi = true,
            cokluSecenek = true,
            secimVarMi = false,
            teslimatEtkinMi = false,
        )

        assertEquals(OdulHali.PLANLANAN, hal)
    }

    @Test
    fun `etkin coklu odul secim bekler`() {
        val hal = odulHaliniBelirle(
            hakEdildi = true,
            cokluSecenek = true,
            secimVarMi = false,
            teslimatEtkinMi = true,
        )

        assertEquals(OdulHali.SECIM_BEKLIYOR, hal)
    }

    @Test
    fun `maddi odul metni teslimat sinirini aciklar`() {
        assertTrue(MADDI_ODUL_ACIKLAMASI.contains("henüz etkin değil"))
        assertTrue(MADDI_ODUL_ACIKLAMASI.contains("kupon ya da bağış oluşturmaz"))
        assertFalse(MADDI_ODUL_ACIKLAMASI.contains("kazanırsın"))
    }

    @Test
    fun `basari karti resmi belge gibi sunulmaz`() {
        assertTrue(BASARI_KARTI_ACIKLAMASI.contains("uygulama içi"))
        assertTrue(BASARI_KARTI_ACIKLAMASI.contains("resmî belge değildir"))
    }

    // --- stok kapısının hangi kademelere uygulandığı (§12 K64) ---

    private fun kademe(id: String) =
        OdulViewModel.KADEMELER.first { it.id == id }

    /**
     * Dijital rozet stok kapısına **takılmıyor**.
     *
     * Kapı bir kez koşulsuz uygulanıyordu: `teslimatEtkinMi`, kademenin
     * sunucudan gelen açık seçenek listesinde bulunmasına bağlıydı. Sunucu
     * yalnızca kuponları tanıdığı için (`SECILEBILIR_ODULLER`) rozet hiçbir
     * zaman listede olmuyor ve `false`'a düşüyordu: 500 puana ulaşan HER
     * kullanıcı, tedarik zinciri gerektirmeyen bir uygulama içi başarı kartı
     * için "Puan hedefi tamamlandı · teslimat etkin değil" görüyordu.
     */
    @Test
    fun `dijital rozet stok kapisina takilmiyor`() {
        val rozet = kademe("rozet")

        assertFalse(rozet.stogaBagli)
        assertTrue(rozet.teslimatAcikMi(emptySet()))
        assertEquals(
            OdulHali.KAZANILDI,
            rozet.copy(teslimatEtkinMi = rozet.teslimatAcikMi(emptySet()))
                .duruma(puan = 500, aktifGun = 0, secilenId = null)
                .hal,
        )
    }

    @Test
    fun `maddi kademeler stok gelene kadar kapali`() {
        for (id in listOf("fidan", "kupon_market", "kupon_spor")) {
            val k = kademe(id)
            assertTrue("$id stoğa bağlı olmalı", k.stogaBagli)
            assertFalse("$id stoksuz açık olmamalı", k.teslimatAcikMi(emptySet()))
        }
    }

    @Test
    fun `stok gelince kademe aciliyor`() {
        val market = kademe("kupon_market")

        assertTrue(market.teslimatAcikMi(setOf("kupon_market" to "market_kuponu")))
        // Kademede EN AZ BİR seçenek açıksa kademe açık — sunucudaki
        // `odul_stogu.kademe_acik_mi` ile aynı kural.
        assertTrue(market.teslimatAcikMi(setOf("kupon_market" to "fidan_bagisi")))
        // Başka kademenin stoğu bunu açmıyor.
        assertFalse(market.teslimatAcikMi(setOf("kupon_spor" to "fidan_bagisi")))
    }

    /**
     * Metin kapıyı takip ediyor.
     *
     * Kapı sunucudaki stoğa taşındığında (K62) ad ve açıklama derlemeye
     * gömülü "planlanan" dilinde kalmıştı: panelden parti yüklenince kademe
     * seçilebilir oluyor ama kullanıcı "Planlanan market kuponu —
     * teslimat altyapısı henüz etkin değil" yazan bir seçeneği seçiyordu.
     */
    @Test
    fun `odul metni teslimat durumunu takip ediyor`() {
        val market = kademe("kupon_market")

        val kapali = market.copy(teslimatEtkinMi = false)
            .duruma(puan = 5_000, aktifGun = 30, secilenId = "market_kuponu")
        assertEquals("Planlanan market kuponu", kapali.ad)
        assertEquals(MADDI_ODUL_ACIKLAMASI, kapali.aciklama)

        val acik = market.copy(teslimatEtkinMi = true)
            .duruma(puan = 5_000, aktifGun = 30, secilenId = "market_kuponu")
        assertEquals("Market kuponu", acik.ad)
        assertFalse(acik.aciklama.contains("henüz etkin değil"))
        // Seçim sayfasındaki alternatifler de aynı dili konuşuyor.
        assertTrue(acik.secenekler.none { it.ad.startsWith("Planlanan") })
    }
}
