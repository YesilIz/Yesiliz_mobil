package tr.org.yesilay.yesiliz.domain.hesap

import kotlinx.coroutines.flow.Flow

/**
 * Hesap işlemleri — **arayüz**.
 *
 * `LigDeposu` ile aynı desen (CLAUDE.md §12 K7): MVP'de
 * [tr.org.yesilay.yesiliz.data.depo.YerelSahteHesapDeposu] uyguluyor,
 * Faz 2'de `RetrofitHesapDeposu` bağlanıyor. Ekranlar değişmiyor.
 *
 * **MVP'de gerçek kimlik doğrulama yok.** Şifre hiçbir yere yazılmıyor —
 * ne DataStore'a ne Room'a. Yerel uygulama yalnızca kullanıcının **adını**
 * saklıyor; parola sunucu işi ve sunucu Faz 2'de. Yarım bir kimlik
 * doğrulama yazmak, güvenli olduğu izlenimi verirdi.
 */
interface HesapDeposu {

    val oturum: Flow<Oturum>

    suspend fun girisYap(eposta: String, sifre: String): HesapSonuc

    suspend fun kayitOl(ad: String, eposta: String, sifre: String): HesapSonuc

    suspend fun sifreSifirla(eposta: String): HesapSonuc

    suspend fun misafirDevam()

    suspend fun cikisYap()
}

/**
 * Oturum durumu.
 *
 * [misafirMi] true iken hesap ekranı bir daha sorulmuyor ama Profil'den
 * "hesap oluştur" hâlâ mümkün.
 */
data class Oturum(
    val girisYapildiMi: Boolean = false,
    val misafirMi: Boolean = false,
    val ad: String = "",
    val eposta: String = "",
) {
    /** Hesap ekranı gösterilsin mi — ne giriş yapılmış ne misafir seçilmiş. */
    val karsilamaGerekliMi: Boolean get() = !girisYapildiMi && !misafirMi
}

sealed interface HesapSonuc {
    data object Basarili : HesapSonuc

    /** Alan bazlı hata; ekran ilgili alanın altında gösteriyor. */
    data class Hata(val alan: HesapAlani, val mesaj: String) : HesapSonuc
}

enum class HesapAlani { AD, EPOSTA, SIFRE, SIFRE_TEKRAR, KOSULLAR, GENEL }
