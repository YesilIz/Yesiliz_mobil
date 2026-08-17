package tr.org.yesilay.yesiliz.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import java.time.LocalDate

/**
 * Widget'ın "veri var mı" kararı — T4-002.
 *
 * Kritik senaryo: **satır var, `olayVarMi = false`.** Gecelik worker ölçüm
 * bulamadığı güne de sıfır dolu bir satır yazıyor (CLAUDE.md §9); widget
 * `ozet != null` sorduğu için bu satırı veri sayıp `%0 · 0dk` gösteriyordu.
 */
class EkranSuresiVerisiTest {

    private val bugun: LocalDate = LocalDate.of(2026, 8, 17)

    private fun ozet(toplamDk: Double, olayVarMi: Boolean) = GunlukOzetEntity(
        tarih = bugun,
        socialDk = toplamDk,
        olayVarMi = olayVarMi,
    )

    @Test
    fun `satir var ama olcum yoksa veri yok sayilir`() {
        val veri = EkranSuresiVerisi.olustur(ozet(toplamDk = 0.0, olayVarMi = false), hedefDk = 285)

        assertFalse("ölçülmemiş gün veri sayılmamalı", veri.veriVarMi)
        assertEquals(0, veri.dakika)
    }

    @Test
    fun `olcum varsa dakika ve oran gosterilir`() {
        val veri = EkranSuresiVerisi.olustur(ozet(toplamDk = 142.0, olayVarMi = true), hedefDk = 285)

        assertTrue(veri.veriVarMi)
        assertEquals(142, veri.dakika)
        assertEquals("2s 22dk", veri.metin())
    }

    @Test
    fun `satir hic yoksa veri yok sayilir`() {
        val veri = EkranSuresiVerisi.olustur(ozet = null, hedefDk = 285)

        assertFalse(veri.veriVarMi)
        assertEquals(0, veri.dakika)
    }

    @Test
    fun `gercekten sifir dakika olculdugu gun veri sayilir`() {
        // "Ölçtük, sıfır çıktı" ile "ölçemedik" farklı şeyler: ilkinde
        // %0 göstermek doğru.
        val veri = EkranSuresiVerisi.olustur(ozet(toplamDk = 0.0, olayVarMi = true), hedefDk = 285)

        assertTrue(veri.veriVarMi)
        assertEquals(0, veri.dakika)
        assertEquals(0f, veri.oran, 0.0001f)
    }

    @Test
    fun `kategori dakikalari toplanir`() {
        val veri = EkranSuresiVerisi.olustur(
            GunlukOzetEntity(
                tarih = bugun,
                socialDk = 30.0,
                entertainmentDk = 20.4,
                gamesDk = 10.0,
                otherDk = 5.0,
                olayVarMi = true,
            ),
            hedefDk = 120,
        )

        assertEquals(65, veri.dakika)
    }

    @Test
    fun `oran hedefin ustunde birde durur`() {
        val veri = EkranSuresiVerisi.olustur(ozet(toplamDk = 400.0, olayVarMi = true), hedefDk = 285)

        assertEquals(1f, veri.oran, 0.0001f)
    }
}
