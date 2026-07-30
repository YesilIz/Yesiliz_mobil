package tr.org.yesilay.yesiliz.domain.kategori

import android.content.pm.ApplicationInfo

/**
 * Uygulama kategorisi.
 *
 * [anahtar] değerleri **core-risk'in beklediği** dizelerle birebir aynı
 * (spec-risk.md §4.1). Motor bu dizeleri görür, `Kategori` enum'unu değil —
 * core-risk saf JVM modülü olduğu için app tiplerini tanımaz.
 *
 * [SISTEM] motora HİÇ gitmez: launcher, SystemUI, üretici bloatware.
 * Bunlar "ekran süresi" değil; telefonun kendi arayüzü kullanıcıya risk
 * olarak yazılamaz. Günlük özet oluşturulurken düşülür.
 */
enum class Kategori(val anahtar: String, val hesabaGirer: Boolean = true) {
    SOCIAL("social"),
    ENTERTAINMENT("entertainment"),
    GAMES("games"),
    COMMUNICATION("communication"),
    PRODUCTIVITY("productivity"),
    EDUCATION("education"),
    HEALTH("health"),
    OTHER("other"),
    SISTEM("sistem", hesabaGirer = false),
    ;

    companion object {
        /** Tabloda bulunamayan paketin düştüğü kategori. */
        val VARSAYILAN = OTHER

        fun anahtardan(anahtar: String): Kategori =
            entries.firstOrNull { it.anahtar == anahtar }
                ?: error("Bilinmeyen kategori anahtarı: $anahtar")

        /**
         * Android'in kendi beyan ettiği kategoriden ([ApplicationInfo.category])
         * kaba bir karşılık üretir.
         *
         * Tabloda olmayan uygulamalar için **yedek katman**. Geliştirici
         * `android:appCategory` beyan etmişse işe yarar, etmemişse
         * [ApplicationInfo.CATEGORY_UNDEFINED] döner ve yedek devreye girmez.
         * Zayıf bir sinyal ama "other" varsayımından iyi: 3.106 satırlık
         * tablo bile Türkiye pazarını tam kapsamıyor.
         *
         * @return karşılığı yoksa null — çağıran [VARSAYILAN]'a düşer
         */
        fun androidKategorisinden(kategori: Int): Kategori? = when (kategori) {
            ApplicationInfo.CATEGORY_SOCIAL -> SOCIAL
            ApplicationInfo.CATEGORY_VIDEO -> ENTERTAINMENT
            ApplicationInfo.CATEGORY_AUDIO -> ENTERTAINMENT
            ApplicationInfo.CATEGORY_GAME -> GAMES
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> PRODUCTIVITY
            ApplicationInfo.CATEGORY_IMAGE -> PRODUCTIVITY
            ApplicationInfo.CATEGORY_MAPS -> PRODUCTIVITY
            ApplicationInfo.CATEGORY_NEWS -> OTHER
            else -> null
        }
    }
}
