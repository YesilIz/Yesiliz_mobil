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

/**
 * Bakım gövdesinin çözümlenmesi (M9, `docs/panel-plani.md §5.7`).
 *
 * Bakım **askıdan da çevrimdışından da farklı**: askı hesaba özel ve
 * kalıcı, çevrimdışı sunucunun sessiz kalması, bakım ise sunucunun
 * "geçici olarak kapalıyım" demesi. Üçünü ayırmak, kullanıcıya doğru
 * şeyi söylemenin tek yolu.
 */
class BakimTespitTest {

    @Test
    fun `bakım gövdesi mesajı veriyor`() {
        val govde = """{"hata":"bakim","mesaj":"Kısa bir bakım var."}"""
        assertEquals("Kısa bir bakım var.", bakimMesajiCoz(govde))
    }

    @Test
    fun `askı gövdesi bakım sayılmıyor`() {
        // Askı `detail` sarmalayıcısıyla geliyor (HTTPException), bakım
        // düz gövdeyle (middleware). İkisi karışmamalı.
        val govde = """{"detail":{"hata":"hesap_askida","sebep":"x"}}"""
        assertNull(bakimMesajiCoz(govde))
    }

    @Test
    fun `başka bir 503 bakım sayılmıyor`() {
        assertNull(bakimMesajiCoz("""{"detail":"Maddi ödül teslimatı etkin değil"}"""))
    }

    @Test
    fun `mesaj yoksa boş dize dönüyor, null değil`() {
        // Ayrım önemli: null "bakım değil", boş dize "bakım ama mesaj yok".
        assertEquals("", bakimMesajiCoz("""{"hata":"bakim"}"""))
    }

    @Test
    fun `bozuk gövde çökmüyor`() {
        assertNull(bakimMesajiCoz("<html>502</html>"))
    }
}
