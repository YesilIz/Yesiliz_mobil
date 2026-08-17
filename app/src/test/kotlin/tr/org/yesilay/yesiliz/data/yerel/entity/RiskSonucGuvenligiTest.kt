package tr.org.yesilay.yesiliz.data.yerel.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.VeriDurumu
import java.time.LocalDate

/**
 * `risk_sonuc` satırının hangi alanının nerede kullanılabileceği.
 *
 * `histerezisAdayi` ile `gosterilebilirBant` **farklı** sorular soruyor ama
 * ikisi de aynı ön koşula bağlı: ölçülmemiş bir gün ne bant gösterebilir ne
 * de histerezis sayacına gözlem verebilir.
 */
class RiskSonucGuvenligiTest {

    private fun satir(
        adayBant: String = Bant.AZ.name,
        gosterilenBant: String = Bant.AZ.name,
        veriDurumu: String = VeriDurumu.HESAPLANDI.name,
    ) = RiskSonucEntity(
        tarih = LocalDate.of(2026, 8, 17),
        S = 0.0,
        rB = 0.0,
        rS = null,
        alpha = 0.0,
        R = 0.0,
        adayBant = adayBant,
        gosterilenBant = gosterilenBant,
        guvenlikKurali = null,
        trend = "BELIRSIZ",
        surukleyiciler = emptyList(),
        veriDurumu = veriDurumu,
    )

    @Test
    fun `olculmus gunun adayi histerezise girer`() {
        assertEquals(Bant.AZ, satir().histerezisAdayi())
    }

    @Test
    fun `yetersiz gunun adayi histerezise girmez`() {
        assertNull(satir(veriDurumu = VeriDurumu.YETERSIZ.name).histerezisAdayi())
    }

    @Test
    fun `bilinmeyen veri durumu histerezise girmez`() {
        assertNull(satir(veriDurumu = "ESKI_SURUMDEN_KALAN").histerezisAdayi())
    }

    @Test
    fun `bilinmeyen aday bandi histerezise girmez`() {
        assertNull(satir(adayBant = "MAVI").histerezisAdayi())
    }

    @Test
    fun `yetersiz gunun bandi gosterilmez`() {
        assertNull(satir(veriDurumu = VeriDurumu.YETERSIZ.name).gosterilebilirBant())
        assertEquals(Bant.AZ, satir().gosterilebilirBant())
    }
}
