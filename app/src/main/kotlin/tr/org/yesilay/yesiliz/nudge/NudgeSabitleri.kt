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
     * eşikten türüyor (§12 K53). Buradaki sabit yalnızca **hiç seçim
     * yapılmamış** durumda geçerli.
     *
     * **45 dakika — gerçek kullanıcı değeri.** Mockup metni de buna göre
     * ("45 dakikadır buradasın", `DESIGN.md §5`); `NudgeOverlay.AnaKart`
     * eşik 60 sn'yi aşınca dakikaya çeviriyor ve `BildirimDeposu`'na giden
     * `ESIK.toMinutes()` de ancak burada anlamlı bir sayı üretiyor.
     *
     * **Bu satır `dev` ile bilinçli olarak ayrışıyor** (§12 K50): geliştirme
     * dalında değer 15 saniye, çünkü popup'ı cihazda doğrulamak için 45 dakika
     * beklemek pratik değil. Yayınlanan dal olan `main` gerçek değeri taşır —
     * 15 saniyede bir nudge, uygulamanın "zorlama yok, hafif dokunuş" ilkesinin
     * (CLAUDE.md §3) tam tersi olurdu.
     *
     * Ayrışmanın maliyeti: `main` `dev`'e fast-forward **edilemez**, gerçek
     * merge gerekiyor ve `dev` bu dosyaya dokunduğu için merge **çakışıyor** —
     * istenen davranış bu, karar insana kalıyor. 25 Ağu 2026 birleştirmesinde
     * çakışma böyle çözüldü: yapı `dev`'den (K53, [NudgeAyari]), değer
     * `main`'den (45 dk). `git reset --hard dev` değeri sessizce yutar.
     *
     * `TetikleyiciTest` dakika sabitleri yerine bu değere **göreli** yazıldığı
     * için iki dalda da yeşil kalıyor — yani testler ayrışmayı yakalamaz,
     * KDoc'un kendisi tek uyarı noktası.
     */
    val ESIK: Duration = Duration.ofMinutes(45)

    /** Dokunulmazsa overlay kendiliğinden kapanır. */
    val OTO_KAPANMA: Duration = Duration.ofSeconds(60)
}
