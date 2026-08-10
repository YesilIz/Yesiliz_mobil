package tr.org.yesilay.yesiliz.arkaplan

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class GunlukOzetTamamlamaTest {

    @Test
    fun yalnizcaUcCiktisiDaOlanGunTamamlanmisSayilir() {
        val tam = LocalDate.of(2026, 8, 8)
        val riskEksik = LocalDate.of(2026, 8, 9)
        val puanEksik = LocalDate.of(2026, 8, 10)

        val sonuc = tamamlanmisGunler(
            ozetGunleri = listOf(tam, riskEksik, puanEksik),
            riskGunleri = listOf(tam, puanEksik),
            puanGunleri = listOf(tam, riskEksik),
        )

        assertEquals(setOf(tam), sonuc)
    }
}
