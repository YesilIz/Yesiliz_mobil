package tr.org.yesilay.yesiliz.domain.bildirim

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Bildirim listesinin gün gruplarına ayrılması (tasarım 35: BUGÜN · DÜN ·
 * BU HAFTA · DAHA ÖNCE).
 *
 * **Saf fonksiyon** — `Instant` ve `ZoneId` girer, grup çıkar. Emülatörsüz
 * test edilebilsin diye ViewModel'den ayrıldı; `OturumCikar` ve
 * `NefesFazHesapla` ile aynı desen (CLAUDE.md §6).
 */
enum class BildirimGrubu(val baslik: String) {
    BUGUN("Bugün"),
    DUN("Dün"),
    BU_HAFTA("Bu hafta"),
    DAHA_ONCE("Daha önce"),
}

object BildirimGrupla {

    /**
     * "Bu hafta", takvim haftasıdır (Pazartesi başlangıçlı, `WeekFields.ISO`)
     * — `Aralik.HAFTA` ile aynı tanım. Kayan 7 gün kullanılsaydı pazartesi
     * sabahı "geçen çarşamba" da bu haftaya düşerdi.
     */
    fun grubu(zaman: Instant, bugun: LocalDate, zon: ZoneId): BildirimGrubu {
        val gun = zaman.atZone(zon).toLocalDate()
        return when {
            gun == bugun -> BildirimGrubu.BUGUN
            gun == bugun.minusDays(1) -> BildirimGrubu.DUN
            gun >= bugun.with(WeekFields.of(Locale.ROOT).dayOfWeek(), 1) ->
                BildirimGrubu.BU_HAFTA
            else -> BildirimGrubu.DAHA_ONCE
        }
    }

    /**
     * Göreli zaman etiketi — tasarımda "2 saat önce", "Dün, 21:14",
     * "Pazartesi" biçimleri var.
     */
    fun zamanEtiketi(zaman: Instant, simdi: Instant, zon: ZoneId): String {
        val gecenDk = (simdi.epochSecond - zaman.epochSecond) / SANIYE_DK
        val yerel = zaman.atZone(zon)
        val bugun = simdi.atZone(zon).toLocalDate()
        val gun = yerel.toLocalDate()

        return when {
            gecenDk < 1 -> "Az önce"
            gecenDk < DAKIKA_SAAT -> "$gecenDk dakika önce"
            gun == bugun -> "${gecenDk / DAKIKA_SAAT} saat önce"
            gun == bugun.minusDays(1) -> "Dün, ${saatMetni(yerel.hour, yerel.minute)}"
            gun >= bugun.minusDays(HAFTA_GUN) -> gunAdi(gun)
            else -> "${gun.dayOfMonth} ${ayAdi(gun.monthValue)}"
        }
    }

    private fun saatMetni(saat: Int, dakika: Int): String =
        "%02d:%02d".format(saat, dakika)

    private fun gunAdi(gun: LocalDate): String = when (gun.dayOfWeek.value) {
        1 -> "Pazartesi"
        2 -> "Salı"
        3 -> "Çarşamba"
        4 -> "Perşembe"
        5 -> "Cuma"
        6 -> "Cumartesi"
        else -> "Pazar"
    }

    private fun ayAdi(ay: Int): String = AYLAR[ay - 1]

    private val AYLAR = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık",
    )

    private const val SANIYE_DK = 60L
    private const val DAKIKA_SAAT = 60L
    private const val HAFTA_GUN = 7L
}
