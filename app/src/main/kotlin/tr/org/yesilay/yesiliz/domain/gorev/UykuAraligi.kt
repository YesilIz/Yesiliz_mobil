package tr.org.yesilay.yesiliz.domain.gorev

/**
 * Uyku beyanı seçenekleri — "Bugünün sorusu" widget'ı (tasarım 39) ve
 * görev listesindeki [Gorev.UYKU_BEYANI] aynı dört seçeneği kullanıyor.
 *
 * **Neden aralık, neden saat değil:** kullanıcı zaten tahmin ediyor.
 * "6 saat 40 dakika" gibi bir sayı sahte hassasiyet üretir; risk motoru da
 * bu değeri kullanmıyor (uyku çıkarımı `OruntuCikar` içinde davranıştan
 * yapılıyor, beyandan değil). Beyan yalnızca kullanıcının kendi farkındalığı
 * ve görevin dayanağı.
 */
enum class UykuAraligi(val anahtar: String, val etiket: String) {
    KISA("1-3", "1-3 saat"),
    ORTA("3-5", "3-5 saat"),
    IYI("5-7", "5-7 saat"),
    UZUN("7+", "7+ saat"),
    ;

    companion object {
        fun anahtardan(anahtar: String): UykuAraligi? =
            entries.firstOrNull { it.anahtar == anahtar }
    }
}
