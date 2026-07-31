package tr.org.yesilay.yesiliz.data.uzak

import kotlinx.coroutines.runBlocking
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [JetonAuthenticator]'ın jeton deposuna baktığı dar pencere.
 *
 * **Niçin arayüz:** `JetonDeposu` ve `Tercihler` ikisi de DataStore, ikisi de
 * `Context` istiyor — yani emülatörsüz kurulamıyorlar. Authenticator'ın asıl
 * değerli kısmı ise saf karar mantığı (bir kez dene · `auth/` yollarını atla ·
 * eşzamanlı 401'lerde tek yenileme) ve bunun emülatörsüz test edilebilmesi
 * gerekiyor. `OturumCikar` / `NefesFazHesapla` / `Tetikleyici` ile aynı desen:
 * Android'e bağlı kısım arayüzün arkasına çekiliyor.
 *
 * Metotlar **askıya alınabilir değil**: OkHttp `Authenticator`'ı senkron bir
 * arayüz, coroutine bekleyemiyor.
 */
interface JetonKaynak {

    /** Şu an saklanan erişim jetonu. */
    fun erisim(): String?

    /** Şu an saklanan yenileme jetonu. */
    fun yenileme(): String?

    fun kaydet(erisim: String, yenileme: String)

    /**
     * Yenileme de reddedildi — oturumu kapat.
     *
     * Hem jetonları hem `Tercihler`'deki oturum bayrağını siliyor; yalnızca
     * jetonları silmek Profil'de hâlâ "giriş yapılmış" göstermek olurdu.
     */
    fun oturumuKapat()
}

/**
 * Gerçek uygulama — `JetonDeposu` (jetonlar) + `Tercihler` (oturum bayrağı).
 *
 * `RetrofitHesapDeposu.cikisYap()` ile **aynı ikiliyi** temizliyor; oradaki
 * çıkış ile buradaki zorunlu çıkış arasında davranış farkı olmasın diye.
 */
@Singleton
class VarsayilanJetonKaynak @Inject constructor(
    private val jetonDeposu: JetonDeposu,
    private val tercihler: Tercihler,
) : JetonKaynak {

    override fun erisim(): String? = runBlocking { jetonDeposu.erisim() }

    override fun yenileme(): String? = runBlocking { jetonDeposu.yenileme() }

    override fun kaydet(erisim: String, yenileme: String) = runBlocking {
        jetonDeposu.kaydet(erisim, yenileme)
    }

    override fun oturumuKapat() = runBlocking {
        jetonDeposu.temizle()
        tercihler.oturumuSil()
    }
}
