package tr.org.yesilay.yesiliz.data.uzak

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * 401 alındığında erişim jetonunu bir kez yeniler ve isteği tekrarlar.
 *
 * Erişim jetonu **30 dakikada** doluyor (`backend/app/ayarlar.py`). Bu sınıf
 * olmadan 30 dakikadan uzun her oturumda kimlikli çağrılar sessizce 401
 * vermeye başlıyor: lig `YedekliLigDeposu` üzerinden sahte veriye düşüyor,
 * `SenkronDeposu` sessizce `false` dönüyor, kullanıcı hâlâ giriş yapmış
 * görünüyor. Sessiz olduğu için demo sırasında "çalışıyor" gibi de görünür.
 *
 * ## Üç koruma
 *
 * - **Bir kez dene.** `priorResponse` doluysa bu istek zaten bir kez
 *   yenilenip tekrarlanmış demektir; ikinci 401'de pes ediliyor.
 * - **`auth/` yolları hariç.** `backend/app/uclar/hesap.py` yanlış şifrede de
 *   401 veriyor. Bu hariç tutma olmasa yanlış şifre bir yenileme denemesi
 *   tetiklerdi ve §12 K18'in "iş hatası olduğu gibi geçer" sözleşmesi
 *   bozulurdu — kullanıcıya yanlış şifreyle "giriş yaptın" demiş olurduk.
 * - **Tek yenileme.** Eşzamanlı iki istek birlikte 401 alırsa yalnızca biri
 *   sunucuya gidiyor; ikincisi ilkinin aldığı jetonu kullanıyor.
 */
class JetonAuthenticator(
    private val yenileyici: JetonYenileyici,
    private val jetonlar: JetonKaynak,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        if (AUTH_YOLU in response.request.url.encodedPath) return null

        val yeni = tazele(response.request.header(BASLIK)) ?: return null
        return response.request.newBuilder()
            .header(BASLIK, "$SEMA$yeni")
            .build()
    }

    /**
     * @param kullanilanBaslik 401 alan isteğin taşıdığı `Authorization` başlığı
     * @return kullanılacak yeni erişim jetonu; oturum sürdürülemiyorsa `null`
     */
    @Synchronized
    private fun tazele(kullanilanBaslik: String?): String? {
        // Bu istek beklerken başka bir iş parçacığı yenilemiş olabilir.
        // Depodaki jeton, 401 alan istekte kullanılandan farklıysa yenileme
        // zaten olmuş demektir — ikinci kez sunucuya gitmeye gerek yok.
        val depodaki = jetonlar.erisim()
        if (depodaki != null && kullanilanBaslik != "$SEMA$depodaki") return depodaki

        val yenileme = jetonlar.yenileme() ?: return null

        return when (val sonuc = yenileyici.yenile(yenileme)) {
            is YenilemeSonuc.Basarili -> {
                jetonlar.kaydet(sonuc.jetonlar.erisim, sonuc.jetonlar.yenileme)
                sonuc.jetonlar.erisim
            }

            // Sunucu yenileme jetonunu da reddetti — sürdürülebilecek bir
            // oturum kalmadı. Sessizce kapanıyor: kullanıcı bir ekranın
            // ortasında olabilir, giriş ekranına fırlatmak akışı keserdi.
            // Profil'de "Giriş yap" satırı geri geliyor, lig K18
            // sarmalayıcısıyla yerel yedeğe düşüyor.
            YenilemeSonuc.Reddedildi -> {
                jetonlar.oturumuKapat()
                null
            }

            // Ağ gitti. Oturum **korunuyor** — bağlantı gelince aynı yenileme
            // jetonu yine denenecek.
            YenilemeSonuc.AgHatasi -> null
        }
    }

    private companion object {
        const val BASLIK = "Authorization"
        const val SEMA = "Bearer "
        const val AUTH_YOLU = "auth/"
    }
}
