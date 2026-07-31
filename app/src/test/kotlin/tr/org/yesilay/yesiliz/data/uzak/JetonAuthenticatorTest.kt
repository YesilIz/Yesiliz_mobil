package tr.org.yesilay.yesiliz.data.uzak

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `JetonAuthenticator` — 401 sonrası jeton yenileme kararları.
 *
 * Sunucu ayağa kaldırılmıyor: `Authenticator` arayüzü düz `Response` alıyor,
 * onu OkHttp'nin kendi kurucularıyla üretmek yeterli. `JetonYenileyici` ve
 * `JetonKaynak` arayüz olduğu için sahteleri burada.
 */
class JetonAuthenticatorTest {

    // --- sahteler ---

    private class SahteYenileyici(
        var sonuc: YenilemeSonuc,
    ) : JetonYenileyici {
        var cagriSayisi = 0
        override fun yenile(yenilemeJetonu: String): YenilemeSonuc {
            cagriSayisi++
            return sonuc
        }
    }

    private class SahteKaynak(
        var erisim: String? = ESKI,
        var yenileme: String? = "yenileme-jetonu",
    ) : JetonKaynak {
        var kapatildiMi = false
        override fun erisim() = erisim
        override fun yenileme() = yenileme
        override fun kaydet(erisim: String, yenileme: String) {
            this.erisim = erisim
            this.yenileme = yenileme
        }

        override fun oturumuKapat() {
            kapatildiMi = true
            erisim = null
            yenileme = null
        }
    }

    // --- yardımcılar ---

    private fun yanit(
        yol: String = "ozet",
        baslik: String? = "Bearer $ESKI",
        oncekiVarMi: Boolean = false,
    ): Response {
        val istek = Request.Builder()
            .url("https://ornek.gecersiz/$yol")
            .apply { baslik?.let { header("Authorization", it) } }
            .build()

        fun kurulum(r: Request) = Response.Builder()
            .request(r)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody(null))

        return kurulum(istek)
            .apply { if (oncekiVarMi) priorResponse(kurulum(istek).build()) }
            .build()
    }

    private fun authenticator(
        yenileyici: SahteYenileyici,
        kaynak: SahteKaynak,
    ) = JetonAuthenticator(yenileyici, kaynak)

    // --- testler ---

    @Test
    fun basariliYenilemeIstegiYeniJetonlaTekrarlar() {
        val yenileyici = SahteYenileyici(
            YenilemeSonuc.Basarili(
                JetonlarDto(erisim = YENI, yenileme = "y2", ad = "Ada", eposta = "a@b.c"),
            ),
        )
        val kaynak = SahteKaynak()

        val istek = authenticator(yenileyici, kaynak).authenticate(null, yanit())

        assertEquals("istek yeni jetonla tekrarlanmalı", "Bearer $YENI", istek?.header("Authorization"))
        assertEquals("yeni jeton saklanmalı", YENI, kaynak.erisim)
        assertFalse("oturum kapatılmamalı", kaynak.kapatildiMi)
    }

    @Test
    fun sunucuYenilemeyiReddedinceOturumKapanir() {
        val yenileyici = SahteYenileyici(YenilemeSonuc.Reddedildi)
        val kaynak = SahteKaynak()

        val istek = authenticator(yenileyici, kaynak).authenticate(null, yanit())

        assertNull("tekrarlanacak istek olmamalı", istek)
        assertTrue("oturum kapatılmalı", kaynak.kapatildiMi)
    }

    /** §12 K18: ağ hatası iş hatası değil — oturum korunur. */
    @Test
    fun agHatasindaOturumKORUNUR() {
        val yenileyici = SahteYenileyici(YenilemeSonuc.AgHatasi)
        val kaynak = SahteKaynak()

        val istek = authenticator(yenileyici, kaynak).authenticate(null, yanit())

        assertNull("tekrarlanacak istek olmamalı", istek)
        assertFalse("ağ hatası oturumu KAPATMAMALI", kaynak.kapatildiMi)
        assertEquals("jeton silinmemeli", ESKI, kaynak.erisim)
    }

    /**
     * `auth/giris` yanlış şifrede de 401 veriyor (`backend/app/uclar/hesap.py`).
     * Yenileme denenirse §12 K18'in "iş hatası olduğu gibi geçer" sözleşmesi
     * bozulur — kullanıcıya yanlış şifreyle "giriş yaptın" demiş olurduk.
     */
    @Test
    fun TUZAK_authYollarindaYenilemeDENENMEZ() {
        val yenileyici = SahteYenileyici(
            YenilemeSonuc.Basarili(
                JetonlarDto(erisim = YENI, yenileme = "y2", ad = "Ada", eposta = "a@b.c"),
            ),
        )
        val kaynak = SahteKaynak()

        listOf("auth/giris", "auth/kayit", "auth/yenile").forEach { yol ->
            val istek = authenticator(yenileyici, kaynak).authenticate(null, yanit(yol = yol))
            assertNull("$yol için yenileme denenmemeli", istek)
        }

        assertEquals("sunucuya hiç yenileme gitmemeli", 0, yenileyici.cagriSayisi)
        assertFalse("oturum kapatılmamalı", kaynak.kapatildiMi)
    }

    /** İkinci 401'de pes edilmeli, yoksa istek sonsuza dek döner. */
    @Test
    fun TUZAK_ikinciDenemedeVazgecer() {
        val yenileyici = SahteYenileyici(
            YenilemeSonuc.Basarili(
                JetonlarDto(erisim = YENI, yenileme = "y2", ad = "Ada", eposta = "a@b.c"),
            ),
        )
        val kaynak = SahteKaynak()

        val istek = authenticator(yenileyici, kaynak)
            .authenticate(null, yanit(oncekiVarMi = true))

        assertNull("zaten bir kez yenilenmiş istek tekrar denenmemeli", istek)
        assertEquals("ikinci yenileme yapılmamalı", 0, yenileyici.cagriSayisi)
    }

    /**
     * Eşzamanlı iki istek birlikte 401 alırsa yalnızca biri sunucuya gitmeli;
     * ikincisi depodaki taze jetonu kullanmalı.
     */
    @Test
    fun baskaBirIsParcacigiYenilediyseTekrarYenilenmez() {
        val yenileyici = SahteYenileyici(
            YenilemeSonuc.Basarili(
                JetonlarDto(erisim = "cok-yeni", yenileme = "y3", ad = "Ada", eposta = "a@b.c"),
            ),
        )
        // Depoda ZATEN yeni jeton var; 401 alan istek eskisini taşıyor.
        val kaynak = SahteKaynak(erisim = YENI)

        val istek = authenticator(yenileyici, kaynak)
            .authenticate(null, yanit(baslik = "Bearer $ESKI"))

        assertEquals("depodaki taze jeton kullanılmalı", "Bearer $YENI", istek?.header("Authorization"))
        assertEquals("sunucuya ikinci kez gidilmemeli", 0, yenileyici.cagriSayisi)
    }

    @Test
    fun yenilemeJetonuYoksaSessizceVazgecer() {
        val yenileyici = SahteYenileyici(YenilemeSonuc.Reddedildi)
        val kaynak = SahteKaynak(yenileme = null)

        val istek = authenticator(yenileyici, kaynak).authenticate(null, yanit())

        assertNull(istek)
        assertEquals("yenileme jetonu yokken sunucuya gidilmemeli", 0, yenileyici.cagriSayisi)
    }

    private companion object {
        const val ESKI = "eski-erisim"
        const val YENI = "yeni-erisim"
    }
}
