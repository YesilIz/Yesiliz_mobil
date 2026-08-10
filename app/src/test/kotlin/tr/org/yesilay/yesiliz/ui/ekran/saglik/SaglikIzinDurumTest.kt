package tr.org.yesilay.yesiliz.ui.ekran.saglik

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.org.yesilay.yesiliz.data.saglik.SaglikDurumu

class SaglikIzinDurumTest {

    @Test
    fun `tum kullanilabilir izinler verilmisse eksiklik notu gizlenir`() {
        val durum = SaglikIzinDurum(
            saglikDurumu = SaglikDurumu.HAZIR,
            izinVarMi = true,
            arkaPlanIzniVarMi = true,
            sayacCihazdaVarMi = true,
            sayacIzniVarMi = true,
        )

        assertFalse(durum.izinEksikligiVarMi)
    }

    @Test
    fun `arka plan veya sayac izni eksikse not gorunur`() {
        assertTrue(
            SaglikIzinDurum(
                saglikDurumu = SaglikDurumu.HAZIR,
                izinVarMi = true,
                arkaPlanIzniVarMi = false,
            ).izinEksikligiVarMi,
        )
        assertTrue(
            SaglikIzinDurum(
                saglikDurumu = SaglikDurumu.HAZIR,
                izinVarMi = true,
                arkaPlanIzniVarMi = true,
                sayacCihazdaVarMi = true,
                sayacIzniVarMi = false,
            ).izinEksikligiVarMi,
        )
    }
}
