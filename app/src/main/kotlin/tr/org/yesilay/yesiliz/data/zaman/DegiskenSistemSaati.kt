package tr.org.yesilay.yesiliz.data.zaman

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Anı sistemden, zaman dilimini ise her okumada yeniden alan üretim saati.
 *
 * `Clock.system(ZoneId.systemDefault())` oluşturulduğu andaki [ZoneId]'yi
 * saklar. Uygulama süreci seyahat/sistem ayarı boyunca açık kalırsa yeni
 * gün sınırlarını ancak süreç yeniden başlayınca görür. Bu saat aynı test
 * edilebilir [Clock] sözleşmesini korurken bölgeyi canlı tutar.
 */
internal class DegiskenSistemSaati(
    private val anSaglayici: () -> Instant = { Instant.now() },
    private val zonSaglayici: () -> ZoneId = { ZoneId.systemDefault() },
) : Clock() {
    override fun instant(): Instant = anSaglayici()

    override fun getZone(): ZoneId = zonSaglayici()

    override fun withZone(zone: ZoneId): Clock = DegiskenSistemSaati(
        anSaglayici = anSaglayici,
        zonSaglayici = { zone },
    )
}
