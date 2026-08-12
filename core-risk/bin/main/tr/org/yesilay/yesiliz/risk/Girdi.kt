package tr.org.yesilay.yesiliz.risk

/**
 * Risk motorunun girdi modelleri.
 *
 * Hepsi **veri sınıfı** — Android tipi, Context, tarih/saat API'si YOK.
 * Motor saf fonksiyon; girdi burada, çıktı `Cikti.kt`'de.
 */

/**
 * Bir günün davranış özeti.
 *
 * @param kategoriDakika kategori → o gün geçirilen dakika.
 *   Anahtarlar [Sabitler.KATEGORI_AGIRLIK] ile aynı; tanınmayan anahtar
 *   [Sabitler.VARSAYILAN_AGIRLIK] ile ağırlıklandırılır ve *işlevsel*
 *   tarafta sayılır (tüketim değil).
 *   **Ekleme sırası önemli:** `drivers` sıralamasında eşitlik olursa
 *   bu sıra belirleyici (referans Python'un dict davranışıyla birebir).
 * @param oruntu örüntü adı → 0..1 normalize değer.
 *   Eksik anahtar 0 kabul edilir.
 * @param adim o günkü adım sayısı
 * @param sporDk o günkü spor dakikası
 */
data class GunlukGirdi(
    val kategoriDakika: Map<String, Double>,
    val oruntu: Map<String, Double> = emptyMap(),
    val adim: Double = 0.0,
    val sporDk: Double = 0.0,
) {
    /** Tüketim kategorilerinde geçen ham dakika — güvenlik tabanı bunu kullanır. */
    val tuketimDk: Double
        get() = kategoriDakika.entries
            .filter { it.key in Sabitler.TUKETIM }
            .sumOf { it.value }

    /** Toplam ekran dakikası. */
    val toplamDk: Double
        get() = kategoriDakika.values.sum()

    /** Gece kullanım dakikası — oran × toplam. Güvenlik tabanı bunu kullanır. */
    val geceDk: Double
        get() = (oruntu["gece"] ?: 0.0) * toplamDk
}

/**
 * SAS-SV anket girdisi.
 *
 * @param hamPuan 10 madde toplamı, 10..60 arası (spec-anket.md §6).
 *   Ham puan saklanır, bant değil — kesme puanı değişirse yeniden hesaplanabilsin.
 * @param kacGunOnce anketin doldurulmasından bu yana geçen gün (`g`).
 *   60 günde ağırlığı tamamen söner.
 */
data class AnketGirdi(
    val hamPuan: Double,
    val kacGunOnce: Int = 0,
)

/**
 * Davranış verisinin birikmişliği — alpha eğrisinin `d` girdisi (§4.5).
 *
 * @param canliGun uygulama kuruluyken tam veri toplanan gün sayısı
 * @param gecmisGun kurulumda `queryUsageStats` ile geriye dönük alınan gün.
 *   Yarım veridir (hacim var, örüntü yok) — 0.6 ağırlıkla sayılır.
 *   iOS'te bu daima 0 (§10).
 */
data class GecmisPencere(
    val canliGun: Int = 0,
    val gecmisGun: Int = 0,
)

/**
 * Güvenlik tabanı (§5.3) için tek günlük özet.
 * Risk skorundan bağımsız, ham dakikalara bakar.
 */
data class GunlukOzet(
    val tuketimDk: Double,
    val geceDk: Double,
)
