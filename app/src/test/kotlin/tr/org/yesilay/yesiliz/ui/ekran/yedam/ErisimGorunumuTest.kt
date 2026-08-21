package tr.org.yesilay.yesiliz.ui.ekran.yedam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Erişim kaydının kullanıcıya gösterilen hâli (M5).
 *
 * Rıza metni: *"Danışman bilgilerine her baktığında uygulamada bir kayıt
 * görürsün."* Kayıt danışmanın **adını taşımıyor** — sunucu da
 * göndermiyor; burada yalnızca kapsam ve tarih biçimleniyor.
 */
class ErisimGorunumuTest {

    @Test
    fun `kapsam anahtarlari turkceye ceviriliyor`() {
        val gorunum = ErisimGorunumu("2026-08-21T10:32:00Z", "risk,surukleyici")
        assertEquals("risk bandı, kategoriler", gorunum.kapsamMetni)
    }

    @Test
    fun `bos kapsam veri gorulmedigini soyluyor`() {
        // Rıza geçersizken danışman ekranı açabiliyor ama veri göremiyor;
        // kayıt yine yazılıyor ve kullanıcı bunu doğru okumalı.
        val gorunum = ErisimGorunumu("2026-08-21T10:32:00Z", "")
        assertEquals("Görüntülenebilir veri yoktu", gorunum.kapsamMetni)
    }

    @Test
    fun `bilinmeyen kapsam anahtari oldugu gibi gosteriliyor`() {
        val gorunum = ErisimGorunumu("2026-08-21T10:32:00Z", "yepyeni")
        assertEquals("yepyeni", gorunum.kapsamMetni)
    }

    @Test
    fun `tarih ISO damgasindan kesiliyor`() {
        assertEquals("2026-08-21", ErisimGorunumu("2026-08-21T10:32:00Z", "risk").tarih)
    }

    @Test
    fun `kapsam metni danisman adi tasimiyor`() {
        // Sunucu ad göndermiyor; DTO'da da alan yok. Bu test, ileride
        // eklenmesi hâlinde ekrana sızmasını yakalar.
        val alanlar = ErisimGorunumu("2026-08-21T10:32:00Z", "risk")
        assertTrue(alanlar.toString().let { !it.contains("ad=") })
    }
}
