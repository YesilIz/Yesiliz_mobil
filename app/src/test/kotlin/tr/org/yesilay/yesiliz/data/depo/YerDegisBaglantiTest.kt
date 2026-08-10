package tr.org.yesilay.yesiliz.data.depo

import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity

class YerDegisBaglantiTest {

    @Test
    fun gecmisOzetlerVeBugunYerDegisGirdisineBaglanir() {
        val bas = LocalDate.of(2026, 8, 5)
        val gecmis = (0 until 5).map { indeks ->
            GunlukOzetEntity(
                tarih = bas.plusDays(indeks.toLong()),
                socialDk = if (indeks < 3) 200.0 else 300.0,
                adim = if (indeks < 3) 8_000.0 else 5_000.0,
                olayVarMi = true,
            )
        }

        val sonuc = yerDegisDegeri(gecmis, bugunEkranDk = 300.0, bugunAdim = 5_000.0)

        assertTrue("ekran artıp adım düşünce sinyal bekleniyordu: $sonuc", sonuc > 0.5)
    }
}
