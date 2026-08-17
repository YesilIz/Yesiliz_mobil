package tr.org.yesilay.yesiliz.ui.ekran.riskdetay

import org.junit.Assert.assertEquals
import org.junit.Test
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.Sabitler

/**
 * Halka ile bant etiketi çelişmemeli.
 *
 * Gerçek cihazda görülen durum: anket yenilendi, `R = 66,3` (`adayBant = COK`)
 * ama histerezis bandı `AZ`'da tuttu; halka %66 dolu, ortasında "Düşük"
 * yazıyordu.
 */
class RiskHalkaOraniTest {

    private val tolerans = 0.0001f

    @Test
    fun `bant ile skor uyusuyorsa halka gercek skoru gosterir`() {
        assertEquals(0.25f, RiskHalkaOrani.hesapla(25.0, Bant.AZ), tolerans)
        assertEquals(0.50f, RiskHalkaOrani.hesapla(50.0, Bant.ORTA), tolerans)
        assertEquals(0.80f, RiskHalkaOrani.hesapla(80.0, Bant.COK), tolerans)
    }

    @Test
    fun `histerezis bandi geride tutuyorsa halka bandin sinirinda durur`() {
        // Cihazdaki durum: R = 66,3 ama gösterilen bant hâlâ AZ.
        val oran = RiskHalkaOrani.hesapla(66.3, Bant.AZ)

        assertEquals((Sabitler.BANT_AZ_UST / 100.0).toFloat(), oran, tolerans)
    }

    @Test
    fun `bant skordan yuksekse halka bandin altina dusmez`() {
        // Güvenlik tabanı bandı yukarı çekmiş olabilir (§5.3): skor düşük
        // ama bant COK. Halka "düşük risk" görüntüsü vermemeli.
        val oran = RiskHalkaOrani.hesapla(10.0, Bant.COK)

        assertEquals((Sabitler.BANT_ORTA_UST / 100.0).toFloat(), oran, tolerans)
    }

    @Test
    fun `oran her zaman sifir bir arasinda`() {
        for (bant in Bant.entries) {
            for (r in listOf(-50.0, 0.0, 39.9, 40.0, 64.9, 65.0, 100.0, 250.0)) {
                val oran = RiskHalkaOrani.hesapla(r, bant)
                assert(oran in 0f..1f) { "bant=$bant R=$r oran=$oran" }
            }
        }
    }

    @Test
    fun `bant sinirlari esiktenBant ile ayni sabitleri okur`() {
        // AZ'ın tavanı ORTA'nın tabanı; aralıklar arasında boşluk yok.
        assertEquals(
            RiskHalkaOrani.hesapla(Sabitler.BANT_AZ_UST, Bant.AZ),
            RiskHalkaOrani.hesapla(Sabitler.BANT_AZ_UST, Bant.ORTA),
            tolerans,
        )
        assertEquals(
            RiskHalkaOrani.hesapla(Sabitler.BANT_ORTA_UST, Bant.ORTA),
            RiskHalkaOrani.hesapla(Sabitler.BANT_ORTA_UST, Bant.COK),
            tolerans,
        )
    }
}
