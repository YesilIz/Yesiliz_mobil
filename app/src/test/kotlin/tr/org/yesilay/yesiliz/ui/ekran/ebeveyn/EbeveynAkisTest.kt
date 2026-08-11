package tr.org.yesilay.yesiliz.ui.ekran.ebeveyn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.org.yesilay.yesiliz.data.depo.EbeveynOzetSonucu
import tr.org.yesilay.yesiliz.data.uzak.EbeveynOzetDto

class EbeveynAkisTest {

    @Test
    fun basariliVeyaHataliOzetYuklemedeKalmaz() {
        val veri = EbeveynOzetDto(
            cocukAdi = "Elif",
            gunlukDakikalar = listOf(60),
            ortalamaDk = 60,
        )

        assertFalse(ozetTamamlandi(EbeveynOzetSonucu(veri = veri)).yukleniyor)
        assertFalse(ozetTamamlandi(EbeveynOzetSonucu(hata = "Ağ hatası")).yukleniyor)
    }

    @Test
    fun eslesmeBasarisiEslestiktenSonraDaGosterilir() {
        assertTrue(eslestirmeAltSayfasiGoster(true, EslestirmeAdimi.BASARILI))
        assertFalse(eslestirmeAltSayfasiGoster(true, EslestirmeAdimi.ROL_SECIMI))
        assertTrue(eslestirmeAltSayfasiGoster(false, EslestirmeAdimi.ROL_SECIMI))
    }

    @Test
    fun riskBandiSunucuEnumuYerineAnlasilirMetinGosterir() {
        assertEquals("Düşük risk", "AZ".riskBandiMetni())
        assertEquals("Orta risk", "ORTA".riskBandiMetni())
        assertEquals("Yüksek risk", "COK".riskBandiMetni())
        assertEquals("Henüz hesaplanmadı", "BILINMEYEN".riskBandiMetni())
        assertEquals("Henüz hesaplanmadı", null.riskBandiMetni())
    }

    @Test
    fun belirsizVeyaBilinmeyenTrendSabitDiyeSunulmaz() {
        assertEquals(null, "BELIRSIZ".trendMesajiVeyaNull())
        assertEquals(null, "YENI".trendMesajiVeyaNull())
        assertEquals("Geçen haftaya göre benzer", "SABIT".trendMesajiVeyaNull())
    }

    @Test
    fun sessizlikUygulamaAcilmadiDiyeYorumlanmaz() {
        val mesaj = sessizlikMesaji(72)

        assertTrue(mesaj.contains("yeni paylaşım gelmedi"))
        assertFalse(mesaj.contains("açılmadı"))
        assertFalse(mesaj.contains("silindi"))
    }
}
