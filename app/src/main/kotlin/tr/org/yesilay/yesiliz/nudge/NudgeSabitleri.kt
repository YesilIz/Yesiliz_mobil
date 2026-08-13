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
     * bilerek aynı değil — nudge kendi eşiğini taşıyor.
     *
     * **Şu an 15 saniye: demo/geliştirme değeri.** Popup'ı cihazda 45 dakika
     * beklemeden görebilmek için bilinçli olarak kısaltıldı (13 Ağu 2026).
     * [POLL_ARALIGI] ve sönüm süreleri otomatik uyum sağlıyor; `TetikleyiciTest`
     * de artık dakika sabitleri yerine bu değere **göreli** yazıldığı için
     * eşik ne olursa olsun yeşil kalıyor.
     *
     * Gerçek kullanıcı davranışı 45 dakikaydı ve mockup metni ("45 dakikadır
     * buradasın", `DESIGN.md §5`) hâlâ ona göre. **Yayın öncesi geri alınmalı:**
     * 15 saniyede bir nudge, uygulamanın "zorlama yok, hafif dokunuş" ilkesinin
     * (CLAUDE.md §3) tam tersi olur.
     */
    val ESIK: Duration = Duration.ofSeconds(15)

    /**
     * Canlı izleme aralığı — WorkManager'ın ~15dk asgari periyodu
     * bunun için uygun değil.
     *
     * [ESIK] kısa tutulduğunda polling de hızlanır: eşiğin yarısını
     * veya 20 saniyeyi aşmaz. Böylece kısa test eşiklerinde bile
     * en az bir poll eşiğin altında gerçekleşir.
     */
    val POLL_ARALIGI: Duration = minOf(
        Duration.ofSeconds(20),
        ESIK.dividedBy(2).coerceAtLeast(Duration.ofSeconds(3)),
    )

    /** Dokunulmazsa overlay kendiliğinden kapanır. */
    val OTO_KAPANMA: Duration = Duration.ofSeconds(60)

    /**
     * "5 dk mola ver" sonrası sönüm — kullanıcı uygulamadan gerçekten
     * ayrıldığı için kısa bir yeniden-tetiklenme penceresi yeterli.
     *
     * [ESIK] kısa tutulduğunda sönüm eşiği aşmamalı.
     */
    val SONUM_MOLA: Duration = minOf(Duration.ofMinutes(5), ESIK)

    /**
     * "Devam et" (veya zaman aşımı / dışarı dokunma) sonrası sönüm —
     * kullanıcı **aynı** tüketim bloğunda kalmaya devam ediyor; kısa bir
     * sönüm birkaç dakika içinde yeniden rahatsız ederdi, bu yüzden
     * [SONUM_MOLA]'dan daha uzun.
     *
     * [ESIK] kısa tutulduğunda sönüm eşiği aşmamalı.
     */
    val SONUM_DEVAM: Duration = minOf(Duration.ofMinutes(10), ESIK)
}
