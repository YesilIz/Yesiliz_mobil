package tr.org.yesilay.yesiliz.domain.yedam

/**
 * YEDAM (Yeşilay Danışmanlık Merkezi) hattının tek doğruluk kaynağı.
 *
 * Numara ve çalışma saatleri **üç ekranda** birden görünüyor (Profil › YEDAM
 * hakkında, YEDAM onayı, Ebeveyn paneli). Metin dört ayrı yere kopyalanmıştı;
 * Yeşilay hat saatlerini değiştirdiği gün üçünü düzeltip birini unutmak
 * sessizce yanlış bilgi bırakırdı — bir destek hattı için bunun bedeli
 * yüksek (5. tarama G5-009).
 *
 * **Buradaki değer değişirse `assets/bilgi_bankasi.json`'daki YEDAM yazısı
 * da güncellenmeli** — o bir içerik metni, sabit olarak buradan beslenmiyor.
 */
object Yedam {

    /** Ücretsiz ve gizli YEDAM Hattı. */
    const val NUMARA = "115"

    /** `tel:` şeması — [android.content.Intent.ACTION_DIAL] için. */
    const val TEL_URI = "tel:$NUMARA"

    const val CALISMA_SAATLERI =
        "Çalışma saatleri: hafta içi 08:30–00:00, cumartesi 08:30–17:30."

    const val ARA_BUTONU = "$NUMARA'i ara"
}
