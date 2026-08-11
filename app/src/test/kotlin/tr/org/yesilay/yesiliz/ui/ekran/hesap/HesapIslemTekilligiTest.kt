package tr.org.yesilay.yesiliz.ui.ekran.hesap

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.org.yesilay.yesiliz.domain.hesap.HesapAlani
import tr.org.yesilay.yesiliz.domain.hesap.HesapSonuc

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

    @Test
    fun `beklenmeyen depo hatasi genel hataya cevrilir ve kaydedilir`() = runTest {
        val hata = IllegalStateException("disk yazilamadi")
        var kaydedilen: Throwable? = null

        val sonuc = hesapIsteginiKoru(beklenmeyenHata = { kaydedilen = it }) {
            throw hata
        }

        assertSame(hata, kaydedilen)
        assertTrue(sonuc is HesapSonuc.Hata)
        sonuc as HesapSonuc.Hata
        assertEquals(HesapAlani.GENEL, sonuc.alan)
    }

    @Test(expected = CancellationException::class)
    fun `yasam dongusu iptali kullanici hatasina cevrilmez`() = runTest {
        hesapIsteginiKoru { throw CancellationException("ekran kapandi") }
    }
}
