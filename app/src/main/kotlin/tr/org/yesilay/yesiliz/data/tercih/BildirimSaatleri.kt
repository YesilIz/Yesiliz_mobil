package tr.org.yesilay.yesiliz.data.tercih

/**
 * Günlük hatırlatma ayarı — Profil › Bildirim saatleri.
 *
 * DataStore'da tek satır olarak saklanıyor (`"true|9|21"`). Üç ayrı anahtar
 * açmanın kazancı yok: üçü birlikte okunup birlikte yazılıyor.
 *
 * **Varsayılan kapalı.** Kullanıcının istemediği bildirimi göndermek,
 * uygulamayı sildiren davranışların başında geliyor; hatırlatma opt-in.
 */
data class BildirimSaatleri(
    val acik: Boolean = false,
    val sabahSaati: Int = VARSAYILAN_SABAH,
    val aksamSaati: Int = VARSAYILAN_AKSAM,
) {
    fun metin(): String = "$acik|$sabahSaati|$aksamSaati"

    companion object {
        const val VARSAYILAN_SABAH = 9
        const val VARSAYILAN_AKSAM = 21

        /**
         * Sabah hatırlatmasının izin verilen aralığı.
         *
         * Sabit çip listesi (`7, 8, 9, 10, 11`) yerine aralık: beş çip
         * satıra sığmıyor, "11:00" tek karakter genişliğinde dikey bir
         * sütuna sıkışıyordu. Kullanıcı ayrıca istediği saati
         * giremiyordu (`debug.md` O-5, İ-5).
         *
         * Aralık hâlâ sınırlı: gece yarısı hatırlatması göndermek,
         * uygulamayı sildiren davranışların başında.
         */
        val SABAH_ARALIGI = 6..11

        /** Akşam hatırlatması — gece yarısına taşmıyor. */
        val AKSAM_ARALIGI = 18..23

        /**
         * Bozuk ya da eski biçimli satır **varsayılana** düşüyor; hata
         * fırlatmak, tek bir tercih uğruna uygulamayı çökertmek olurdu.
         */
        fun coz(metin: String): BildirimSaatleri {
            val parcalar = metin.split("|")
            if (parcalar.size != 3) return BildirimSaatleri()
            return BildirimSaatleri(
                acik = parcalar[0].toBooleanStrictOrNull() ?: false,
                sabahSaati = parcalar[1].toIntOrNull()?.takeIf { it in SABAH_ARALIGI }
                    ?: VARSAYILAN_SABAH,
                aksamSaati = parcalar[2].toIntOrNull()?.takeIf { it in AKSAM_ARALIGI }
                    ?: VARSAYILAN_AKSAM,
            )
        }
    }
}
