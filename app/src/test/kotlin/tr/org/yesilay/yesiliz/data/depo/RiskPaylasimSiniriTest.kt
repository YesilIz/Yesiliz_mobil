package tr.org.yesilay.yesiliz.data.depo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import tr.org.yesilay.yesiliz.data.yerel.entity.RiskSonucEntity
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.Trend
import tr.org.yesilay.yesiliz.risk.VeriDurumu

class RiskPaylasimSiniriTest {

    @Test
    fun `yalnizca yeterli olcumun risk bandi sunucuya paylasilir`() {
        val hesaplandi = risk(VeriDurumu.HESAPLANDI)
        val riskler = listOf(
            risk(VeriDurumu.YETERSIZ),
            hesaplandi,
            risk(VeriDurumu.ESKI),
        )

        assertEquals(listOf(hesaplandi), riskler.paylasilabilirRiskler())
    }

    private fun risk(durum: VeriDurumu) = RiskSonucEntity(
        tarih = LocalDate.of(2026, 8, 11),
        S = 1.0,
        rB = 1.0,
        rS = null,
        alpha = 1.0,
        R = 1.0,
        adayBant = Bant.AZ.name,
        gosterilenBant = Bant.AZ.name,
        guvenlikKurali = null,
        trend = Trend.BELIRSIZ.name,
        surukleyiciler = emptyList(),
        veriDurumu = durum.name,
    )
}
