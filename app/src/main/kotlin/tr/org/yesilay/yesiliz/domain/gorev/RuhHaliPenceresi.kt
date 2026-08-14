package tr.org.yesilay.yesiliz.domain.gorev

import java.time.LocalTime

/**
 * Günün ruh hali pencereleri — sabah / öğlen / akşam.
 *
 * Ruh hali gün içinde değişen bir şey; günde tek kayıt "bugün nasıldın"
 * sorusunu sabahki tek bir ana indirgiyordu. Üç pencere, pencere başına bir
 * kayıt ve bir puan.
 *
 * ## Sınırlar
 *
 * | Pencere | Aralık |
 * |---|---|
 * | Sabah | 05:00 – 11:59 |
 * | Öğlen | 12:00 – 17:59 |
 * | Akşam | 18:00 – 04:59 (ertesi sabaha sarkar) |
 *
 * Akşam penceresi gece yarısını **aşıyor** ama takvim günü değişmiyor:
 * 00:30'da yapılan kayıt hâlâ *dünün* akşam penceresine yazılır. Yoksa gece
 * yarısından sonra uygulamayı açan kullanıcı yeni günün akşam kaydını
 * saatler önce yapmış olurdu. Bu yüzden [gunuVeDilimi] takvim gününü de
 * döndürüyor — çağıran "bugün" değerini kendi hesaplamamalı.
 *
 * Saf fonksiyon: [java.time.Clock] dışarıdan geliyor, test belirlenimci.
 */
enum class RuhHaliPenceresi(val anahtar: String, val etiket: String) {
    SABAH("sabah", "Sabah"),
    OGLEN("oglen", "Öğlen"),
    AKSAM("aksam", "Akşam"),
    ;

    companion object {

        /** Günde kaç kez ruh hali kaydedilebilir — [Gorev.RUH_HALI] limiti. */
        const val GUNLUK_PENCERE_SAYISI = 3

        private val SABAH_BAS = LocalTime.of(5, 0)
        private val OGLEN_BAS = LocalTime.of(12, 0)
        private val AKSAM_BAS = LocalTime.of(18, 0)

        /** Verilen yerel saatin düştüğü pencere. */
        fun bul(saat: LocalTime): RuhHaliPenceresi = when {
            saat >= AKSAM_BAS -> AKSAM
            saat >= OGLEN_BAS -> OGLEN
            saat >= SABAH_BAS -> SABAH
            // 00:00 – 04:59: hâlâ dünün akşamı.
            else -> AKSAM
        }

        /**
         * Kaydın yazılacağı takvim günü ve pencere.
         *
         * Gece yarısı ile 05:00 arası **bir önceki günün** akşam penceresine
         * yazılır; bkz. sınıf açıklaması.
         */
        fun gunuVeDilimi(
            tarih: java.time.LocalDate,
            saat: LocalTime,
        ): Pair<java.time.LocalDate, RuhHaliPenceresi> {
            val pencere = bul(saat)
            val gun = if (pencere == AKSAM && saat < SABAH_BAS) tarih.minusDays(1) else tarih
            return gun to pencere
        }

        fun anahtardan(anahtar: String): RuhHaliPenceresi? =
            entries.firstOrNull { it.anahtar == anahtar }
    }
}
