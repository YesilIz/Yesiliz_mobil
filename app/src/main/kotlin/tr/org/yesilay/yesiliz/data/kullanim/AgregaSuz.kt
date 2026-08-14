package tr.org.yesilay.yesiliz.data.kullanim

import java.time.Duration

/**
 * Android'in `UsageStats` kovasının saf karşılığı.
 *
 * `UsageStats` bir Android sınıfı ve elle kurulamıyor; süzme mantığı bu
 * modele çevrilip [AgregaSuz]'e veriliyor ki emülatörsüz test edilebilsin
 * ([OturumCikar] ile aynı desen).
 */
data class AgregaKova(
    val paketAdi: String,
    /** Kovanın kapsadığı aralığın başı (epoch ms). */
    val basMs: Long,
    /** Kovanın kapsadığı aralığın sonu (epoch ms). */
    val bitMs: Long,
    /** Kova boyunca ön planda geçen süre (ms). */
    val onPlanMs: Long,
)

/**
 * Agrega kovalarından bir günün paket sürelerini süzer.
 *
 * ## Niçin süzmek gerekiyor
 *
 * `queryAndAggregateUsageStats(bas, bit)` istenen aralığa **kırpma yapmıyor**:
 * aralıkla kesişen kovaları olduğu gibi birleştiriyor. Android günlük kovaları
 * yalnızca ~7-10 gün saklıyor; daha eski bir gün sorulduğunda sistem elindeki
 * **haftalık ya da aylık** kovayı döndürüyor. Yani "1 Ağustos'ta ne kadar
 * kullandım" sorusuna Ağustos'un ilk haftasının toplamı geliyor ve tek güne
 * yazılıyor — istatistik ekranındaki "19 saat" tam olarak buydu.
 *
 * ## Kural
 *
 * 1. Süresi [EN_UZUN_KOVA_SAAT]'ten uzun kova **reddedilir** (haftalık/aylık).
 * 2. Kabul edilen kova, **orta noktasının** düştüğü güne yazılır. Böylece bir
 *    kova iki komşu güne birden sayılmaz (çift sayma yok).
 * 3. Paket başına ve toplamda [GUN_DK_TAVANI] uygulanır — bir gün 24 saatten
 *    uzun olamaz. Savunma derinliği: sistem beklenmedik bir değer dönerse
 *    grafiğe absürt bir çubuk girmesin.
 * 4. Hiç kabul edilebilir kova yoksa **`null`** döner: "bu gün için güvenilir
 *    veri yok". Sıfır dönmek yanlış olurdu — sıfır "hiç kullanmadın" demek,
 *    oysa bilmiyoruz (CLAUDE.md §9 "satır var ≠ veri var").
 *
 * ## Kalan sınır (bilinçli)
 *
 * Günlük kovalar yerel gün sınırıyla hizalı değil
 * (`docs/spike-usagestats-sonuc.md`); Türkiye'de kova yerel 03:00→03:00
 * aralığını kapsıyor. Yani değer gerçek bir 24 saatlik ölçüm ama penceresi
 * yerel günden kaymış. Bunu düzeltmenin tek yolu olay bazlı okuma
 * ([KullanimOkuyucu.gununOturumlari]) ve o yol zaten **birincil** — bu
 * yardımcı yalnızca olay geçmişi tükendiğinde devreye giriyor.
 */
object AgregaSuz {

    /** Bundan uzun kova günlük değildir — haftalık/aylık kovadır, reddedilir. */
    val EN_UZUN_KOVA_SAAT: Long = 26

    /** Bir gün 24 saatten uzun olamaz. */
    const val GUN_DK_TAVANI: Double = 1440.0

    /**
     * @return paket → dakika; güvenilir kova bulunamadıysa `null`
     */
    fun gunlukDakikalar(
        kovalar: List<AgregaKova>,
        gunBasMs: Long,
        gunBitMs: Long,
    ): Map<String, Double>? {
        val enUzunMs = Duration.ofHours(EN_UZUN_KOVA_SAAT).toMillis()

        val kabul = kovalar.filter { kova ->
            val sure = kova.bitMs - kova.basMs
            if (sure <= 0L || sure > enUzunMs) return@filter false
            // Orta nokta kuralı: kova tam olarak bir güne ait sayılır.
            val orta = kova.basMs + sure / 2
            orta >= gunBasMs && orta < gunBitMs
        }
        if (kabul.isEmpty()) return null

        val dakikalar = mutableMapOf<String, Double>()
        kabul.forEach { kova ->
            if (kova.onPlanMs <= 0L) return@forEach
            val dk = kova.onPlanMs / 60_000.0
            dakikalar[kova.paketAdi] = (dakikalar[kova.paketAdi] ?: 0.0) + dk
        }
        if (dakikalar.isEmpty()) return null

        // Paket başına tavan, sonra toplamda tavan. İkisi ayrı: tek paket
        // makul görünürken toplam 24 saati aşabilir (kovalar örtüşürse).
        val tavanli = dakikalar.mapValues { (_, dk) -> dk.coerceAtMost(GUN_DK_TAVANI) }
        val toplam = tavanli.values.sum()
        if (toplam <= GUN_DK_TAVANI) return tavanli

        val oran = GUN_DK_TAVANI / toplam
        return tavanli.mapValues { (_, dk) -> dk * oran }
    }
}
