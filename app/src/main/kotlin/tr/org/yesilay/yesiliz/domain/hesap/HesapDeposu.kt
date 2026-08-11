package tr.org.yesilay.yesiliz.domain.hesap

import kotlinx.coroutines.flow.Flow

/**
 * Hesap işlemleri — **arayüz**.
 *
 * `LigDeposu` ile aynı desen (CLAUDE.md §12 K7): sunucusuz çalışırken
 * [tr.org.yesilay.yesiliz.data.depo.YerelSahteHesapDeposu], sunucu ayaktayken
 * `RetrofitHesapDeposu` uyguluyor; ikisini `YedekliHesapDeposu` sarıyor.
 * Ekranlar hiçbirini bilmiyor.
 *
 * **Şifre hiçbir yere yazılmıyor** — ne DataStore'a ne Room'a. Doğrulama
 * sunucuda (`backend/app/guvenlik.py`, argon2id); cihazda yalnızca JWT var
 * ([tr.org.yesilay.yesiliz.data.uzak.JetonDeposu]). Yerel sahte depo
 * sunucuya ulaşılamadığında devreye giriyor (CLAUDE.md §12 K16, K18).
 */
interface HesapDeposu {

    val oturum: Flow<Oturum>

    suspend fun girisYap(eposta: String, sifre: String): HesapSonuc

    suspend fun kayitOl(ad: String, eposta: String, sifre: String): HesapSonuc

    suspend fun sifreSifirla(eposta: String): HesapSonuc

    suspend fun misafirDevam()

    suspend fun cikisYap()

    /**
     * Hesabı sunucudan da kalıcı siler ve oturumu kapatır.
     *
     * @return sunucudaki kaydın gerçekten silinip silinmediği. `false`
     *   dönerse hem oturum hem cihaz verisi korunur; ekran yeniden deneme
     *   sunar. Kimliği silip uzak hesabı bırakırsak kullanıcı aynı silme
     *   isteğini doğrulanmış olarak yeniden gönderemez. Misafir kullanıcıda
     *   sunucuda kayıt hiç yok, `true` dönüyor.
     */
    suspend fun hesabiSil(): Boolean
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
