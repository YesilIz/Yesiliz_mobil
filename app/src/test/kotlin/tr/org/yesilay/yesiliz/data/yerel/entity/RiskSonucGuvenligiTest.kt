package tr.org.yesilay.yesiliz.data.yerel.entity

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.Trend
import tr.org.yesilay.yesiliz.risk.VeriDurumu

class RiskSonucGuvenligiTest {

    @Test
    fun `yetersiz olcum hesaplanmis olsa bile bandi gizler`() {
        assertNull(risk(veriDurumu = VeriDurumu.YETERSIZ.name).gosterilebilirBant())
    }

    @Test
    fun `yeterli olcumde bilinen bandi verir`() {
        assertEquals(Bant.ORTA, risk().gosterilebilirBant())
    }

    @Test
    fun `bilinmeyen kayit degerleri risk veya trend iddiasi uretmez`() {
        assertNull(risk(bant = "GELECEK_BANT").gosterilebilirBant())
        assertNull(risk(bant = "GELECEK_BANT").adayBantVeyaNull())
        assertEquals(VeriDurumu.YETERSIZ, "GELECEK_DURUM".veriDurumuVeyaYetersiz())
        assertNull("GELECEK_TREND".trendVeyaNull())
        assertEquals(Trend.SABIT, Trend.SABIT.name.trendVeyaNull())
    }

    private fun risk(
        bant: String = Bant.ORTA.name,
        veriDurumu: String = VeriDurumu.HESAPLANDI.name,
    ) = RiskSonucEntity(
        tarih = LocalDate.of(2026, 8, 11),
        S = 50.0,
        rB = 50.0,
        rS = null,
        alpha = 1.0,
        R = 50.0,
        adayBant = bant,
        gosterilenBant = bant,
        guvenlikKurali = null,
        trend = Trend.SABIT.name,
        surukleyiciler = emptyList(),
        veriDurumu = veriDurumu,
    )
}
