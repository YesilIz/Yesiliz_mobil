package tr.org.yesilay.yesiliz.domain.kullanim

import java.time.Duration
import java.time.Instant

/**
 * Ortak aktivite oturumu modeli — uygulamanın kendi yürüyüşü ile Health
 * Connect egzersiz kaydı aynı tipten geçiyor (`gorev2.md` §4).
 *
 * [AktiviteTekillestir] bunları zaman örtüşmesine göre birleştiriyor;
 * aynı yürüyüş iki kaynakta da varsa yalnızca bir kez sayılsın diye.
 *
 * @param disKimlik Health Connect kaydının kendi ürettiği kimlik
 *   (`Metadata.id`) — aynı kaydın tekrar senkronize edilmesini yakalamak
 *   için birincil eşleştirme anahtarı (§6 "dış kimlik varsa önce bununla
 *   eşleştir"). Uygulama yürüyüşünde `null`.
 */
data class AktiviteOturumu(
    val baslangic: Instant,
    val bitis: Instant,
    val kaynak: AktiviteKaynagi,
    val disKimlik: String? = null,
) {
    init {
        require(!bitis.isBefore(baslangic)) {
            "bitiş ($bitis) başlangıçtan ($baslangic) önce olamaz"
        }
    }

    /** Süre — negatif ya da gerçek dışı olamaz, [init] bunu garanti ediyor. */
    val sureDk: Double
        get() = Duration.between(baslangic, bitis).toMillis() / 60_000.0
}

/** Aktivite oturumunun kaynağı — `gorev2.md` §4 örneğindeki adlarla aynı fikir. */
enum class AktiviteKaynagi {
    /** `YuruyusServisi`'nde tamamlanan aktif yürüyüş. */
    UYGULAMA_YURUYUSU,

    /** Health Connect'teki egzersiz kaydı (saat, bileklik, üçüncü parti uygulama). */
    HEALTH_CONNECT,
}
