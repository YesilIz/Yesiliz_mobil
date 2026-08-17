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
     * Kesintisiz tüketim bloğu bu süreyi aşınca nudge tetiklenir —
     * **kullanıcı kendi eşiğini seçmediyse.**
     *
     * `OruntuCikar.BLOK_ESIGI` (30dk, risk skorunun `oturum` örüntüsü) ile
     * bilerek aynı değil — nudge kendi eşiğini taşıyor.
     *
     * Etkin değer [NudgeAyari]'den geliyor: kullanıcı Profil › Hedeflerim'den
     * 15–90 dakika arasında bir eşik seçebiliyor ve sönüm/polling süreleri o
     * eşikten türüyor. Buradaki sabit yalnızca **hiç seçim yapılmamış**
     * durumda geçerli.
     *
     * **Şu an 15 saniye: demo/geliştirme değeri.** Popup'ı cihazda 45 dakika
     * beklemeden görebilmek için bilinçli olarak kısaltıldı (13 Ağu 2026) ve
     * dala göre ayrışıyor (§12 K50: `main` 45 dk, `dev` 15 sn). `TetikleyiciTest`
     * dakika sabitleri yerine bu değere **göreli** yazıldığı için eşik ne
     * olursa olsun yeşil kalıyor.
     *
     * **Yayın öncesi geri alınmalı:** 15 saniyede bir nudge, uygulamanın
     * "zorlama yok, hafif dokunuş" ilkesinin (CLAUDE.md §3) tam tersi olur.
     */
    val ESIK: Duration = Duration.ofSeconds(15)

    /** Dokunulmazsa overlay kendiliğinden kapanır. */
    val OTO_KAPANMA: Duration = Duration.ofSeconds(60)
}
