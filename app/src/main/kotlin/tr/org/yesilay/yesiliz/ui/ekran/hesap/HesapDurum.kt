package tr.org.yesilay.yesiliz.ui.ekran.hesap

import tr.org.yesilay.yesiliz.domain.hesap.HesapAlani

/**
 * Giriş · Kayıt · Şifremi unuttum ekranlarının ortak durumu —
 * `docs/tasarim-referans.md §2 / 14, 15, 38`.
 *
 * Üç ekran tek durum sınıfı paylaşıyor: alanların çoğu ortak ve kullanıcı
 * aralarında gidip gelirken yazdığı e-postayı kaybetmiyor.
 */
data class HesapDurum(
    val ad: String = "",
    val eposta: String = "",
    val sifre: String = "",
    val sifreTekrar: String = "",
    val kosullarOnaylandi: Boolean = false,
    val sifreGorunur: Boolean = false,
    /** Alan → hata metni. Boşsa hata yok. */
    val hatalar: Map<HesapAlani, String> = emptyMap(),
    val islemde: Boolean = false,
    /** Şifre sıfırlama isteği gönderildi. */
    val sifirlamaGonderildi: Boolean = false,
) {
    fun hata(alan: HesapAlani): String? = hatalar[alan]
}
