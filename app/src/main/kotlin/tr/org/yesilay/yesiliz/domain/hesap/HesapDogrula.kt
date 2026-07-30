package tr.org.yesilay.yesiliz.domain.hesap

/**
 * Hesap formlarının alan doğrulaması — **saf fonksiyon**.
 *
 * `android.util.Patterns.EMAIL_ADDRESS` kullanılmıyor: Android sınıfı,
 * emülatörsüz test edilemezdi. Basit ama yeterli bir kalıp yazıldı; asıl
 * doğrulama zaten sunucuda (Faz 2) yapılacak.
 *
 * Hata metinleri **yargısız**: "Geçersiz e-posta!" değil, ne beklendiğini
 * söyleyen bir cümle. Hata rengi de yeşil (`Renk.Hata`) — CLAUDE.md §4,
 * kırmızı yalnızca iki istisnada.
 */
object HesapDogrula {

    /** En az 8 karakter — `docs/spec-veri.md`'de bir kural yok, yaygın taban. */
    const val EN_AZ_SIFRE = 8

    private val EPOSTA_KALIBI = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

    fun eposta(deger: String): String? = when {
        deger.isBlank() -> "E-posta adresini yazman gerekiyor."
        !EPOSTA_KALIBI.matches(deger.trim()) -> "Bu bir e-posta adresine benzemiyor."
        else -> null
    }

    fun ad(deger: String): String? = when {
        deger.isBlank() -> "Adını yazman gerekiyor."
        deger.trim().length < 2 -> "Adın en az iki harf olmalı."
        else -> null
    }

    fun sifre(deger: String): String? = when {
        deger.isEmpty() -> "Bir şifre belirlemen gerekiyor."
        deger.length < EN_AZ_SIFRE -> "Şifre en az $EN_AZ_SIFRE karakter olmalı."
        else -> null
    }

    fun sifreTekrar(sifre: String, tekrar: String): String? =
        if (sifre != tekrar) "İki şifre birbirini tutmuyor." else null
}
