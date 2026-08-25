package tr.org.yesilay.yesiliz.ui.ekran.yedam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * YEDAM ekran durumunun saf kuralları.
 *
 * İki şey korunuyor: rıza onayı **zorunlu** (rıza metni onay kutusunu
 * şart koşuyor) ve kullanıcıya gösterilen durum metni **uydurulmuyor** —
 * her metin sunucudan gelen bir durum kodunun karşılığı (§4, K37).
 */
class YedamDurumTest {

    @Test
    fun `onay verilmeden gonderilemez`() {
        val durum = YedamDurum(onayVerildi = false, iletisimDegeri = "05551112233")
        assertFalse(durum.gonderilebilir)
    }

    @Test
    fun `iletisim bilgisi bos ise gonderilemez`() {
        val durum = YedamDurum(onayVerildi = true, iletisimDegeri = "   ")
        assertFalse(durum.gonderilebilir)
    }

    @Test
    fun `onay ve iletisim varsa gonderilebilir`() {
        val durum = YedamDurum(onayVerildi = true, iletisimDegeri = "05551112233")
        assertTrue(durum.gonderilebilir)
    }

    @Test
    fun `gonderim surerken tekrar gonderilemez`() {
        val durum = YedamDurum(
            onayVerildi = true,
            iletisimDegeri = "05551112233",
            gonderiliyor = true,
        )
        assertFalse(durum.gonderilebilir)
    }

    @Test
    fun `bilinmeyen durum kodu bos metin veriyor`() {
        // Uydurma açıklama üretmek yerine sessiz kalmak doğru: sunucu yeni
        // bir durum eklerse eski istemci yanlış bir cümle kurmamalı.
        val gorunum = basvuru(durum = "yepyeni_durum")
        assertEquals("", gorunum.aciklama)
    }

    @Test
    fun `ebeveyn onayi bekleyen basvuru danismanin gormedigini soyluyor`() {
        val metin = basvuru(durum = "ebeveyn_onayi_bekliyor").aciklama
        assertTrue(metin.contains("hiçbir danışman"))
    }

    @Test
    fun `suresi dolan basvuru silindigini soyluyor`() {
        val metin = basvuru(durum = "suresi_doldu").aciklama
        assertTrue(metin.contains("silindi"))
    }

    @Test
    fun `kapanmis durumlar acik sayilmiyor`() {
        for (kod in listOf("kapandi", "riza_geri_cekildi", "suresi_doldu")) {
            assertFalse(kod, basvuru(durum = kod).acikMi)
        }
    }

    @Test
    fun `acik durumlar acik sayiliyor`() {
        for (kod in listOf("yeni", "atandi", "temas_kuruldu", "ulasilamadi")) {
            assertTrue(kod, basvuru(durum = kod).acikMi)
        }
    }

    @Test
    fun `temas turleri turkce etikete cevriliyor`() {
        assertEquals("Ulaşılamadı", TemasGorunumu("ulasilamadi", "2026-08-21").etiket)
        assertEquals("Görüşüldü", TemasGorunumu("gorusuldu", "2026-08-21").etiket)
        // Bilinmeyen tür olduğu gibi geçiyor — uydurma etiket yok.
        assertEquals("yeni_tur", TemasGorunumu("yeni_tur", "2026-08-21").etiket)
    }

    @Test
    fun `riza metninin maddeleri kisaltilmamis`() {
        // Rıza metni §1: "Kısaltılamaz, özetlenemez; madde atlanırsa rıza
        // o maddeyi kapsamaz." Sayılar belgedeki listelerle aynı olmalı.
        assertEquals(5, PAYLASILACAKLAR.size)
        assertEquals(4, PAYLASILMAYACAKLAR.size)
        assertTrue(PAYLASILMAYACAKLAR.any { it.contains("Konumun") })
        assertTrue(PAYLASILMAYACAKLAR.any { it.contains("Şifren") })
    }

    private fun basvuru(durum: String) = BasvuruGorunumu(
        id = 1,
        durum = durum,
        ebeveynOnayiBekleniyor = false,
        rizaBitisi = null,
        temaslar = emptyList(),
    )
}
