package tr.org.yesilay.yesiliz.data.kullanim

/**
 * `KullanimOkuyucu.gunlukToplamSureler`'in ham milisaniyelerini
 * `Tercihler.gecmisGunSayisi`'ne çevirir — **SAF FONKSİYON**.
 *
 * `UsageStatsManager` bazı OEM'lerde sistem/launcher gürültüsü için sıfır
 * olmayan ama anlamsız küçük süreler döndürebiliyor; [GURULTU_ESIGI_MS]
 * bunları elemek için var.
 */
object GecmisGunSayaci {

    /** Bu eşiğin altındaki günlük süre "gerçek kullanım" sayılmaz. */
    private const val GURULTU_ESIGI_MS = 60_000L

    /** @param gunlukSureler her günün toplam ön plan süresi (ms) */
    fun say(gunlukSureler: List<Long>): Int = gunlukSureler.count { it >= GURULTU_ESIGI_MS }
}
