package tr.org.yesilay.yesiliz.ui.ekran.hesap

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HesapIslemTekilligiTest {

    @Test
    fun `hizli iki gonderimden yalnizca ilki ag islemini sahiplenir`() {
        val durum = MutableStateFlow(HesapDurum())

        assertTrue(durum.islemiSahiplen())
        assertFalse(durum.islemiSahiplen())
        assertTrue(durum.value.islemde)
    }

    @Test
    fun `tamamlanan islemden sonra yeniden deneme sahiplenilebilir`() {
        val durum = MutableStateFlow(HesapDurum(islemde = true))
        durum.value = durum.value.copy(islemde = false)

        assertTrue(durum.islemiSahiplen())
    }
}
