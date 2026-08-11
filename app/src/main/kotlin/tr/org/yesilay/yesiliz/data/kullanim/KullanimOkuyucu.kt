package tr.org.yesilay.yesiliz.data.kullanim

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import tr.org.yesilay.yesiliz.data.zaman.DegiskenSistemSaati
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * `UsageStatsManager` sarmalayıcısı — sistemden ham olay okur.
 *
 * Bu sınıf **yalnızca çeviri yapar**: sistem olaylarını [HamOlay]'a çevirir,
 * oturum kurma işini [OturumCikar]'a bırakır. Böylece asıl mantık emülatörsüz
 * test edilebiliyor.
 *
 * ## Mahremiyet (CLAUDE.md §4)
 *
 * Okunanlar: paket adı, olay türü, zaman damgası. **Başka hiçbir şey.**
 * `UsageEvents.Event` içinde `className` ve `shortcutId` gibi alanlar da var —
 * bunlar hangi ekranda ne yapıldığını sızdırabilir, **okunmuyor**.
 */
class KullanimOkuyucu(
    private val usageStatsManager: UsageStatsManager,
    private val packageManager: PackageManager,
    private val saat: Clock,
    private val kendiPaketAdi: String,
) {

    /**
     * Bir günün ön plan oturumlarını döndürür.
     *
     * Pencere yerel gün sınırlarına göre kuruluyor; kullanıcının zaman dilimi
     * dışarıdan veriliyor ki test belirlenimci olsun.
     *
     * @param simdi bugünün verisi isteniyorsa pencere burada kesilir
     */
    fun gununOturumlari(
        tarih: LocalDate,
        simdi: Instant = Instant.now(),
        enErken: Instant? = null,
    ): List<HamOturum> {
        val zon = saat.zone
        val gunBaslangici = tarih.atStartOfDay(zon).toInstant()
        val pencereBas = enErken?.let { maxOf(gunBaslangici, it) } ?: gunBaslangici
        val pencereBit = minOf(tarih.plusDays(1).atStartOfDay(zon).toInstant(), simdi)
        if (!pencereBit.isAfter(pencereBas)) return emptyList()

        val olaylar = olaylariOku(pencereBas, pencereBit)
        return OturumCikar.cikar(olaylar, pencereBas, pencereBit)
    }

    /**
     * Kurulumdan önceki günlerde telefonda ne kadar gerçek kullanım geçmişi
     * var — `Tercihler.gecmisGunSayisi`'nin tek seferlik girdisi.
     *
     * `queryEvents` (yukarıdaki [gununOturumlari]) yalnızca birkaç haftalık
     * geçmişi tutuyor ve oturum/örüntü detayına iniyor; burada yalnızca
     * "o gün hiç kullanım var mıydı" sorusunun cevabı yeterli, bu yüzden
     * `queryAndAggregateUsageStats` ile hacim bazlı sorgulanıyor.
     *
     * Tek bir `INTERVAL_DAILY` çağrısı yerine gün gün dönülüyor:
     * `INTERVAL_DAILY` kovaları yerel gün sınırıyla hizalı değil
     * (`docs/spike-usagestats-sonuc.md`), bu yardımcı zaten yerel gün
     * sınırlarını kullanıyor. Bu, ömür boyu bir kez çalışacağı için pahalı
     * değil.
     *
     * @param dahilDegilTarih bu tarih ve sonrası **sorgulanmaz** — kurulum
     *   günü zaten `canliGun` tarafından sayılıyor, burada tekrar sayılırsa
     *   çift sayım olurdu
     * @param gunLimiti kaç gün geriye gidileceği
     * @return en yeniden en eskiye, her günün toplam ön plan süresi (ms)
     */
    fun gunlukToplamSureler(dahilDegilTarih: LocalDate, gunLimiti: Int): List<Long> {
        val zon = saat.zone
        return (1..gunLimiti).map { geriGun ->
            val gun = dahilDegilTarih.minusDays(geriGun.toLong())
            val pencereBas = gun.atStartOfDay(zon).toInstant().toEpochMilli()
            val pencereBit = gun.plusDays(1).atStartOfDay(zon).toInstant().toEpochMilli()
            usageStatsManager.queryAndAggregateUsageStats(pencereBas, pencereBit)
                .values
                .filter { it.packageName != kendiPaketAdi }
                .sumOf { it.totalTimeInForeground }
        }
    }

    /**
     * Son [pencere] içindeki ham olaylar — nudge'ın canlı tetikleyicisi için.
     *
     * [gununOturumlari]'nin aksine gün sınırına bağlı değil; kısa, kayan bir
     * pencere ister (`NudgeSabitleri.POLL_ARALIGI`'nın birkaç katı yeter).
     */
    fun sonPencereOlaylari(pencere: Duration, simdi: Instant = Instant.now()): List<HamOlay> =
        olaylariOku(simdi.minus(pencere), simdi)

    /** Sistem olay akışını [HamOlay] listesine çevirir. */
    private fun olaylariOku(bas: Instant, bit: Instant): List<HamOlay> {
        val akis: UsageEvents = usageStatsManager.queryEvents(bas.toEpochMilli(), bit.toEpochMilli())
        val sonuc = mutableListOf<HamOlay>()
        val olay = UsageEvents.Event()

        while (akis.getNextEvent(olay)) {
            val tur = turuCevir(olay.eventType) ?: continue
            val paket = olay.packageName ?: continue

            // Kendi uygulamamız ekran süresi sayılmaz — kullanıcı Yeşilİz'e
            // baktığı için cezalandırılmamalı.
            if (paket == kendiPaketAdi && tur != OlayTuru.EKRAN_KAPANDI) continue

            sonuc += HamOlay(paket, tur, Instant.ofEpochMilli(olay.timeStamp))
        }
        return sonuc
    }

    /**
     * Sistem olay sabitlerini [OlayTuru]'ne çevirir; ilgilenmediklerimiz null.
     *
     * `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` API 29'da geldi ama değerleri
     * eskiden beri kullanılan `MOVE_TO_FOREGROUND`/`MOVE_TO_BACKGROUND` ile
     * aynı (1 ve 2) — minSdk 26'da da doğru çalışır.
     */
    private fun turuCevir(sistemTuru: Int): OlayTuru? = when (sistemTuru) {
        UsageEvents.Event.ACTIVITY_RESUMED -> OlayTuru.ON_PLANA_GELDI
        UsageEvents.Event.ACTIVITY_PAUSED -> OlayTuru.ARKA_PLANA_GITTI
        UsageEvents.Event.SCREEN_INTERACTIVE -> OlayTuru.EKRAN_ACILDI
        UsageEvents.Event.SCREEN_NON_INTERACTIVE -> OlayTuru.EKRAN_KAPANDI
        UsageEvents.Event.KEYGUARD_SHOWN -> OlayTuru.EKRAN_KAPANDI
        else -> null
    }

    /**
     * Paketin Android'e beyan ettiği kategori
     * ([android.content.pm.ApplicationInfo.category]).
     *
     * Kategori tablosunda olmayan uygulamalar için yedek sinyal.
     * Paket görünür değilse veya beyan yoksa null döner — çağıran
     * `Kategori.VARSAYILAN`'a düşer.
     */
    fun androidKategorisi(paketAdi: String): Int? = try {
        packageManager.getApplicationInfo(paketAdi, 0).category
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    companion object {
        fun olustur(baglam: Context, saat: Clock = DegiskenSistemSaati()): KullanimOkuyucu =
            KullanimOkuyucu(
                usageStatsManager = baglam.getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager,
                packageManager = baglam.packageManager,
                saat = saat,
                kendiPaketAdi = baglam.packageName,
            )
    }
}
