package tr.org.yesilay.yesiliz.ui.ekran.hedef

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HedefBilgiMetniTest {

    @Test
    fun `eslesmis ebeveyne gonderilmeyen bildirim vaat edilmez`() {
        val metin = hedefBilgiMetni(ebeveynAktifMi = true)

        assertTrue(metin.contains("Paylaşım açıksa"))
        assertTrue(metin.contains("haftalık özetini panelden görebilir"))
        assertFalse(metin.contains("bildirim"))
        assertFalse(metin.contains("gönderilir"))
    }

    @Test
    fun `eslesme yoksa ebeveyn iddiasi gosterilmez`() {
        val metin = hedefBilgiMetni(ebeveynAktifMi = false)

        assertEquals("Değişiklikler yarından itibaren geçerli olur.", metin)
    }
}
