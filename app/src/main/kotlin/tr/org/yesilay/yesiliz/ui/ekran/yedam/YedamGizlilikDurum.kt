package tr.org.yesilay.yesiliz.ui.ekran.yedam

/**
 * "Verilerine kim baktı" ve rıza geri çekme ekranının durumu (M5, M6).
 *
 * `docs/yedam-riza-metni-v1.md`'nin iki sözünün karşılığı:
 * *"Danışman bilgilerine her baktığında uygulamada bir kayıt görürsün"*
 * ve *"İstediğin an geri alabilirsin."*
 */
data class YedamGizlilikDurum(
    val yukleniyor: Boolean = true,
    /** Sunucuya ulaşılamadı — boş liste ile karıştırılmamalı. */
    val cevrimdisi: Boolean = false,
    val kayitlar: List<ErisimGorunumu> = emptyList(),

    /** Geçerli bir rıza var mı? Yoksa geri çekilecek bir şey de yok. */
    val rizaVar: Boolean = false,
    val geriCekiliyor: Boolean = false,
    /** Geri çekme başarısız oldu — kullanıcı yeniden deneyebilir. */
    val geriCekilemedi: Boolean = false,
    val geriCekildi: Boolean = false,
)

/**
 * Tek bir erişim kaydı.
 *
 * **Danışmanın adı yok** — sunucu da göndermiyor. Kullanıcıya kimin
 * baktığını değil, bakıldığını bildiriyoruz: ad, danışmanı korumasız
 * bırakırdı ve şeffaflık yükümlülüğü için gerekli değil.
 */
data class ErisimGorunumu(val zaman: String, val kapsam: String) {
    /** `risk,surukleyici` → "risk, sürükleyici". Boşsa veri görülmemiş. */
    val kapsamMetni: String
        get() = if (kapsam.isBlank()) {
            "Görüntülenebilir veri yoktu"
        } else {
            kapsam.split(",").joinToString(", ") { ETIKETLER[it] ?: it }
        }

    val tarih: String get() = zaman.take(10)

    private companion object {
        val ETIKETLER = mapOf(
            "kimlik" to "kimlik",
            "risk" to "risk bandı",
            "surukleyici" to "kategoriler",
            "anket" to "anket",
            "davranis" to "davranış özeti",
            "not" to "notun",
        )
    }
}
