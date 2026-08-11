package tr.org.yesilay.yesiliz.ui.ekran.lig

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LigBosDurumTest {

    @Test
    fun cevrimdisiBosDurumSunucuHatasiniSoylerVeEylemleriGizler() {
        val icerik = bosLigIcerigi(cevrimdisi = true)

        assertTrue(icerik.baslik.contains("kullanılamıyor"))
        assertTrue(icerik.aciklama.contains("Sunucuya ulaşılamıyor"))
        assertFalse(icerik.eylemlerGorunur)
    }

    @Test
    fun gercekBosDurumKatilmaEylemleriniGosterir() {
        val icerik = bosLigIcerigi(cevrimdisi = false)

        assertTrue(icerik.baslik.contains("Henüz"))
        assertTrue(icerik.eylemlerGorunur)
    }
}
