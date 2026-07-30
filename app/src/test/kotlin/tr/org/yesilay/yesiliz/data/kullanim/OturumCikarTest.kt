package tr.org.yesilay.yesiliz.data.kullanim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Oturum çıkarımı — sistem olay akışından ön plan oturumları kurma.
 *
 * Bu testler `UsageStatsManager` olmadan koşuyor: [OturumCikar] saf fonksiyon,
 * girdisi [HamOlay] listesi. Gerçek sistem olaylarının bu biçime çevrilmesi
 * [KullanimOkuyucu]'nun işi ve orada mantık yok.
 */
class OturumCikarTest {

    private val zon: ZoneId = ZoneId.of("Europe/Istanbul")
    private val tarih: LocalDate = LocalDate.of(2026, 7, 28)
    private val pencereBas: Instant = tarih.atStartOfDay(zon).toInstant()
    private val pencereBit: Instant = tarih.plusDays(1).atStartOfDay(zon).toInstant()

    private fun an(saat: Int, dakika: Int, saniye: Int = 0): Instant =
        tarih.atTime(LocalTime.of(saat, dakika, saniye)).atZone(zon).toInstant()

    private fun on(paket: String, saat: Int, dk: Int, sn: Int = 0) =
        HamOlay(paket, OlayTuru.ON_PLANA_GELDI, an(saat, dk, sn))

    private fun arka(paket: String, saat: Int, dk: Int, sn: Int = 0) =
        HamOlay(paket, OlayTuru.ARKA_PLANA_GITTI, an(saat, dk, sn))

    private fun ekranKapandi(saat: Int, dk: Int) =
        HamOlay("android", OlayTuru.EKRAN_KAPANDI, an(saat, dk))

    private fun cikar(vararg olaylar: HamOlay) =
        OturumCikar.cikar(olaylar.toList(), pencereBas, pencereBit)

    private val IG = "com.instagram.android"
    private val WA = "com.whatsapp"

    // -----------------------------------------------------------------
    @Test
    fun `bos olay listesi bos oturum verir`() {
        assertTrue(cikar().isEmpty())
    }

    @Test
    fun `basit oturum kurulur`() {
        val o = cikar(on(IG, 10, 0), arka(IG, 10, 30))
        assertEquals(1, o.size)
        assertEquals(IG, o[0].paketAdi)
        assertEquals(Duration.ofMinutes(30), o[0].sure)
    }

    @Test
    fun `uygulama degisimi onceki oturumu kapatir`() {
        // Instagram açık, WhatsApp öne geliyor — PAUSED olayı gelmese bile
        // Instagram oturumu orada bitmeli
        val o = cikar(on(IG, 10, 0), on(WA, 10, 20), arka(WA, 10, 35))
        assertEquals(2, o.size)
        assertEquals(IG, o[0].paketAdi)
        assertEquals(Duration.ofMinutes(20), o[0].sure)
        assertEquals(WA, o[1].paketAdi)
        assertEquals(Duration.ofMinutes(15), o[1].sure)
    }

    @Test
    fun `ayni paket icin yinelenen on plan olayi yeni oturum acmaz`() {
        // Uygulama içi Activity geçişleri arka arkaya RESUMED üretir;
        // her biri yeni oturum sayılsaydı `acilis` örüntüsü şişerdi
        val o = cikar(on(IG, 10, 0), on(IG, 10, 5), on(IG, 10, 12), arka(IG, 10, 30))
        assertEquals(1, o.size)
        assertEquals(Duration.ofMinutes(30), o[0].sure)
    }

    @Test
    fun `ekran kapaninca acik oturum biter`() {
        val o = cikar(on(IG, 23, 40), ekranKapandi(23, 55))
        assertEquals(1, o.size)
        assertEquals(Duration.ofMinutes(15), o[0].sure)
    }

    @Test
    fun `gun sonunda acik kalan oturum kapatilir`() {
        // 23:30'da açıldı, kapanış olayı hiç gelmedi
        val o = cikar(on(IG, 23, 30))
        assertEquals(1, o.size)
        assertEquals(pencereBit, o[0].bitis)
        assertEquals(Duration.ofMinutes(30), o[0].sure)
    }

    // -----------------------------------------------------------------
    // Gürültü ayıklama
    // -----------------------------------------------------------------
    @Test
    fun `bir saniyeden kisa oturum atilir`() {
        // Launcher'ın bir anlığına öne gelmesi — açılış sayılmamalı
        val o = cikar(
            on("com.android.launcher", 10, 0, 0),
            on(IG, 10, 0, 0),
            arka(IG, 10, 30),
        )
        assertEquals("launcher parlaması sayılmamalı", 1, o.size)
        assertEquals(IG, o[0].paketAdi)
    }

    @Test
    fun `dort saatten uzun oturum kirpilir`() {
        // Ekran kapanma olayı kaçtıysa oturum saatlerce açık kalır
        val o = cikar(on(IG, 8, 0), arka(IG, 20, 0))
        assertEquals(1, o.size)
        assertEquals("4 saate kırpılmalı", Duration.ofHours(4), o[0].sure)
    }

    @Test
    fun `pencere disindaki olaylar yok sayilir`() {
        val dun = HamOlay(IG, OlayTuru.ON_PLANA_GELDI, pencereBas.minusSeconds(3600))
        val yarin = HamOlay(IG, OlayTuru.ON_PLANA_GELDI, pencereBit.plusSeconds(3600))
        val o = OturumCikar.cikar(
            listOf(dun, yarin, on(IG, 12, 0), arka(IG, 12, 10)),
            pencereBas, pencereBit,
        )
        assertEquals(1, o.size)
        assertEquals(Duration.ofMinutes(10), o[0].sure)
    }

    @Test
    fun `sirasiz olaylar once siralanir`() {
        val o = OturumCikar.cikar(
            listOf(arka(IG, 10, 30), on(IG, 10, 0)),
            pencereBas, pencereBit,
        )
        assertEquals(1, o.size)
        assertEquals(Duration.ofMinutes(30), o[0].sure)
    }

    @Test
    fun `baska paketin arka plan olayi acik oturumu kapatmaz`() {
        // WhatsApp arka plana giderken Instagram öndeyse Instagram devam eder
        val o = cikar(on(IG, 10, 0), arka(WA, 10, 10), arka(IG, 10, 30))
        assertEquals(1, o.size)
        assertEquals(Duration.ofMinutes(30), o[0].sure)
    }

    @Test
    fun `ekran acilmasi tek basina oturum baslatmaz`() {
        val o = cikar(HamOlay("android", OlayTuru.EKRAN_ACILDI, an(9, 0)))
        assertTrue("ekran açılması kullanım değil", o.isEmpty())
    }

    // -----------------------------------------------------------------
    @Test
    fun `gercekci bir gun`() {
        val o = cikar(
            on(WA, 8, 30), arka(WA, 8, 35),          // 5 dk
            on(IG, 12, 0), arka(IG, 12, 45),         // 45 dk
            on(WA, 18, 0), on(IG, 18, 10),           // WA 10 dk, IG başlar
            ekranKapandi(18, 40),                     // IG 30 dk
        )
        assertEquals(4, o.size)
        assertEquals(
            listOf(5L, 45L, 10L, 30L),
            o.map { it.sure.toMinutes() },
        )
        assertEquals(
            Duration.ofMinutes(75),
            o.filter { it.paketAdi == IG }.fold(Duration.ZERO) { a, b -> a.plus(b.sure) },
        )
    }
}
