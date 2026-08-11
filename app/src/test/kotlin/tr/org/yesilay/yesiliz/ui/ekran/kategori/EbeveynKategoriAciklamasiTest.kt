package tr.org.yesilay.yesiliz.ui.ekran.kategori

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EbeveynKategoriAciklamasiTest {

    @Test
    fun `kategori dagilimi gonderildi iddiasi yerine gercek siniri soyler`() {
        assertTrue(EBEVEYN_KATEGORI_ACIKLAMASI.contains("gönderilmez"))
        assertTrue(EBEVEYN_KATEGORI_ACIKLAMASI.contains("günlük toplamları"))
        assertTrue(EBEVEYN_KATEGORI_ACIKLAMASI.contains("yeterli veriye dayanan"))
        assertFalse(EBEVEYN_KATEGORI_ACIKLAMASI.contains("gönderildi"))
    }
}
