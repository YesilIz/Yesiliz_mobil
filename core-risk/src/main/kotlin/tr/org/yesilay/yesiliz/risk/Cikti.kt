package tr.org.yesilay.yesiliz.risk

/**
 * Risk bandı.
 *
 * **Kırmızı yok** — UI'da koyulaşan yeşille gösterilir (DESIGN.md §1).
 * [sira] histerezis ve güvenlik tabanı karşılaştırmalarında kullanılır;
 * enum sırası bilinçli: AZ < ORTA < ÇOK.
 */
enum class Bant(val etiket: String) {
    AZ("AZ RİSKLİ"),
    ORTA("ORTA RİSKLİ"),
    COK("ÇOK RİSKLİ"),
    ;

    val sira: Int get() = ordinal

    companion object {
        /** Referans Python'la ortak test vektörlerini okumak için. */
        fun etiketten(etiket: String): Bant =
            entries.firstOrNull { it.etiket == etiket }
                ?: error("Bilinmeyen bant etiketi: $etiket")
    }
}

/** Trend — bandı DEĞİŞTİRMEZ, gösterilen MESAJI değiştirir (§5.5). */
enum class Trend(val etiket: String) {
    BELIRSIZ("belirsiz"),
    IYILESIYOR("iyileşiyor"),
    SABIT("sabit"),
    KOTULESIYOR("kötüleşiyor"),
    ;

    companion object {
        fun etiketten(etiket: String): Trend =
            entries.firstOrNull { it.etiket == etiket }
                ?: error("Bilinmeyen trend etiketi: $etiket")
    }
}

/**
 * Tek günlük risk hesabının tam çıktısı.
 *
 * §4.6 md.5: **her hesap `surukleyiciler` döndürmeli.** Nudge metni ve
 * risk detayı ekranı (DESIGN.md §5.12) bunu kullanıyor — "neden bu skor?"
 * sorusuna cevap veremeyen bir skor kullanıcı için kara kutudur.
 *
 * @param S ham yük skoru (sınırsız)
 * @param rB davranıştan gelen skor, 0-100
 * @param rS ankettan gelen skor 0-100; anket yoksa null
 * @param alpha ankete verilen ağırlık, 0-1
 * @param R birleşik risk skoru 0-100
 * @param bant eşikten gelen ham bant — histerezis/güvenlik UYGULANMAMIŞ.
 *   Gösterilecek bant için [Bantlama.gunuIsle] kullan.
 * @param surukleyiciler en çok katkı yapan ilk 3 kategori, azalan sırada
 */
data class RiskSonuc(
    val S: Double,
    val P: Double,
    val D: Double,
    val bTuketim: Double,
    val bIslevsel: Double,
    val rB: Double,
    val rS: Double?,
    val alpha: Double,
    val R: Double,
    val bant: Bant,
    val surukleyiciler: List<String>,
)

/**
 * Hesabın tek güne bağlı ara ürünleri (§4.4 adım 1–4).
 *
 * `gunluk_ozet` satırı bunları saklıyor: risk detayı ekranı "neden bu skor?"
 * sorusunu tek sorguyla cevaplayabilsin. Anket ve alpha burada YOK — onlar
 * çok günlü, [RiskHesapla.hesapla] ekliyor.
 *
 * @param A aktivite payı 0-1; [D] bundan türüyor (tavan %15)
 */
data class AraUrun(
    val bTuketim: Double,
    val bIslevsel: Double,
    val P: Double,
    val A: Double,
    val D: Double,
    val S: Double,
)

/** Güvenlik tabanı sonucu (§5.3). Taban yoksa ikisi de null. */
data class GuvenlikSonuc(
    val taban: Bant?,
    val kural: String?,
)

/**
 * Çok günlü takibin bir günlük çıktısı.
 *
 * @param adayBant eşik + güvenlik tabanı uygulanmış aday
 * @param gosterilenBant histerezisten geçmiş, kullanıcıya GÖSTERİLECEK bant
 * @param guvenlikKurali taban devreye girdiyse hangi kuralla
 * @param bekleyen histerezis sayacının durumu ("AZ RİSKLİ adayı: 3/7 gün")
 */
data class GunSonuc(
    val skor: Double,
    val adayBant: Bant,
    val gosterilenBant: Bant,
    val guvenlikKurali: String?,
    val bekleyen: String?,
    val trend: Trend,
)
