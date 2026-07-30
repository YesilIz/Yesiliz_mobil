package tr.org.yesilay.yesiliz.nudge

import java.time.Duration

/**
 * Nudge'a özgü zamanlama sabitleri — **risk motoru sabiti değil**.
 *
 * `core-risk/Sabitler.kt`'e girmiyor bilerek: buradakiler UI/bildirim
 * zamanlaması, kalibrasyon değil (CLAUDE.md §7 "Sabit, eşik, katsayı →
 * core-risk/Sabitler.kt" kuralı yalnızca risk hesabına giren sabitler için).
 */
object NudgeSabitleri {

    /**
     * Kesintisiz tüketim bloğu bu süreyi aşınca nudge tetiklenir.
     *
     * `OruntuCikar.BLOK_ESIGI` (30dk, risk skorunun `oturum` örüntüsü) ile
     * bilerek aynı değil — nudge kendi eşiğini taşıyor, mockup'taki
     * "45 dakikadır buradasın" metniyle uyumlu.
     */
    val ESIK: Duration = Duration.ofMinutes(45)

    /** Canlı izleme aralığı — WorkManager'ın ~15dk asgari periyodu bunun için uygun değil. */
    val POLL_ARALIGI: Duration = Duration.ofSeconds(20)

    /** Dokunulmazsa overlay kendiliğinden kapanır. */
    val OTO_KAPANMA: Duration = Duration.ofSeconds(6)

    /**
     * "5 dk mola ver" sonrası sönüm — kullanıcı uygulamadan gerçekten
     * ayrıldığı için kısa bir yeniden-tetiklenme penceresi yeterli.
     */
    val SONUM_MOLA: Duration = Duration.ofMinutes(5)

    /**
     * "Devam et" (veya zaman aşımı / dışarı dokunma) sonrası sönüm —
     * kullanıcı **aynı** tüketim bloğunda kalmaya devam ediyor; kısa bir
     * sönüm birkaç dakika içinde yeniden rahatsız ederdi, bu yüzden
     * [SONUM_MOLA]'dan daha uzun.
     */
    val SONUM_DEVAM: Duration = Duration.ofMinutes(10)
}
