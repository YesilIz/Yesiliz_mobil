package tr.org.yesilay.yesiliz.data.uzak

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Askı gövdesinin çözümlenmesi.
 *
 * Bu fonksiyon yanlış çalışırsa hata **sessiz** olur: askıdaki kullanıcı
 * "sunucuya ulaşılamıyor" görür ve hesabının kapalı olduğunu hiç
 * öğrenemez (CLAUDE.md §4, §12 K37). Gövdenin şekli sunucudaki
 * `guvenlik.askiya_alma_govdesi` ile eşleşmek zorunda.
 */
class AskiTespitTest {

    @Test
    fun `askı gövdesi sebebi veriyor`() {
        val govde = """{"detail":{"hata":"hesap_askida","sebep":"Hesap paylaşımı"}}"""
        assertEquals("Hesap paylaşımı", askiSebebiCoz(govde))
    }

    @Test
    fun `sebep boşsa boş dize dönüyor, null değil`() {
        // Ayrım önemli: null "askı değil", boş dize "askı ama sebep yok".
        val govde = """{"detail":{"hata":"hesap_askida","sebep":""}}"""
        assertEquals("", askiSebebiCoz(govde))
    }

    @Test
    fun `sebep alanı hiç yoksa boş dize`() {
        val govde = """{"detail":{"hata":"hesap_askida"}}"""
        assertEquals("", askiSebebiCoz(govde))
    }

    @Test
    fun `başka bir 403 askı sayılmıyor`() {
        val govde = """{"detail":"Bu bölüme yetkiniz yok"}"""
        assertNull(askiSebebiCoz(govde))
    }

    @Test
    fun `farklı hata kodu askı sayılmıyor`() {
        val govde = """{"detail":{"hata":"baska_sebep","sebep":"x"}}"""
        assertNull(askiSebebiCoz(govde))
    }

    @Test
    fun `bozuk JSON çökmüyor`() {
        assertNull(askiSebebiCoz("<html>502 Bad Gateway</html>"))
    }

    @Test
    fun `boş gövde çökmüyor`() {
        assertNull(askiSebebiCoz(""))
    }

    @Test
    fun `bilinmeyen ek alanlar sorun çıkarmıyor`() {
        // Sunucu gövdeye alan eklerse eski istemci çalışmaya devam etmeli.
        val govde =
            """{"detail":{"hata":"hesap_askida","sebep":"S","zaman":"2026-08-21","yeni":1}}"""
        assertEquals("S", askiSebebiCoz(govde))
    }
}
