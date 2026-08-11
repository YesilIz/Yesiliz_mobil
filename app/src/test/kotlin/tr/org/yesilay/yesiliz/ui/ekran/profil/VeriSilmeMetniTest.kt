package tr.org.yesilay.yesiliz.ui.ekran.profil

import org.junit.Assert.assertTrue
import org.junit.Test

class VeriSilmeMetniTest {

    @Test
    fun `yerel silme sunucuda kalanlari islemden once soyler`() {
        assertTrue(YEREL_VERI_SILME_ACIKLAMASI.contains("yalnızca bu cihazı"))
        assertTrue(YEREL_VERI_SILME_ACIKLAMASI.contains("Sunucudaki hesabın"))
        assertTrue(YEREL_VERI_SILME_ACIKLAMASI.contains("ebeveyn bağlantın kalır"))
        assertTrue(YEREL_VERI_SILME_ACIKLAMASI.contains("Hesabımı sil"))
    }
}
