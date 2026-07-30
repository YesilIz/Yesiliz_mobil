package tr.org.yesilay.yesiliz.domain.kullanim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Örüntü çıkarımı testleri.
 *
 * Zaman dilimi sabit (`Europe/Istanbul`) ve tarih sabit — çıkarım saf
 * fonksiyon olduğu için testler belirlenimci.
 */
class OruntuCikarTest {

    private val zon: ZoneId = ZoneId.of("Europe/Istanbul")
    private val tarih: LocalDate = LocalDate.of(2026, 7, 28)

    /** Verilen yerel saatte başlayan, [dk] dakikalık olay. */
    private fun olay(
        saat: Int,
        dakika: Int,
        dk: Long,
        kategori: Kategori = Kategori.SOCIAL,
        paket: String = "com.instagram.android",
        gun: LocalDate = tarih,
    ): KullanimOlay {
        val bas: Instant = gun.atTime(LocalTime.of(saat, dakika)).atZone(zon).toInstant()
        return KullanimOlay(paket, kategori, bas, bas.plusSeconds(dk * 60))
    }

    private fun cikar(vararg olaylar: KullanimOlay) =
        OruntuCikar.cikar(olaylar.toList(), tarih, zon)

    // -----------------------------------------------------------------
    @Test
    fun `bos gun sifir dondurur`() {
        val c = OruntuCikar.cikar(emptyList(), tarih, zon)
        assertEquals(0.0, c.toplamDk, 0.0)
        assertEquals(0, c.acilisSayisi)
        assertTrue(c.kategoriDakika.isEmpty())
        assertNull(c.uykuPenceresi)
    }

    @Test
    fun `sistem paketleri hesaba girmez`() {
        val c = cikar(
            olay(10, 0, 60, Kategori.SISTEM, "com.android.systemui"),
            olay(12, 0, 30, Kategori.SOCIAL),
        )
        assertEquals(30.0, c.toplamDk, 1e-9)
        assertEquals(setOf("social"), c.kategoriDakika.keys)
        assertEquals(1, c.acilisSayisi)
    }

    // -----------------------------------------------------------------
    // gece — spec-risk.md §4.2, tanım spesifikasyondan
    // -----------------------------------------------------------------
    @Test
    fun `gece orani 00-06 penceresini kullanir`() {
        // 02:00'de 60 dk (gece) + 14:00'te 60 dk (gündüz) → oran 0.5
        val c = cikar(olay(2, 0, 60), olay(14, 0, 60))
        assertEquals(60.0, c.geceKullanimDk, 1e-9)
        assertEquals(0.5, c.oruntu["gece"]!!, 1e-9)
    }

    @Test
    fun `gece penceresinin sinirinda kesisim dogru`() {
        // 05:30'da başlayıp 06:30'a kadar süren olay → sadece 30 dk gece
        val c = cikar(olay(5, 30, 60))
        assertEquals(30.0, c.geceKullanimDk, 1e-9)
        assertEquals(0.5, c.oruntu["gece"]!!, 1e-9)
    }

    @Test
    fun `gunduz kullaniminda gece sifir`() {
        val c = cikar(olay(9, 0, 120), olay(20, 0, 60))
        assertEquals(0.0, c.oruntu["gece"]!!, 1e-9)
    }

    // -----------------------------------------------------------------
    // oturum
    // -----------------------------------------------------------------
    @Test
    fun `kesintisiz 45 dakikalik tuketim blok sayilir`() {
        val c = cikar(olay(21, 0, 45))
        assertEquals(1.0, c.oruntu["oturum"]!!, 1e-9)
    }

    @Test
    fun `kisa tuketim blok sayilmaz`() {
        val c = cikar(olay(21, 0, 10), olay(23, 0, 10))
        assertEquals(0.0, c.oruntu["oturum"]!!, 1e-9)
    }

    @Test
    fun `iki dakikalik bosluk blogu bozmaz`() {
        // 20 dk Instagram → 2 dk boşluk → 20 dk TikTok = 42 dk blok
        val c = cikar(
            olay(21, 0, 20, Kategori.SOCIAL),
            olay(21, 22, 20, Kategori.ENTERTAINMENT, "com.zhiliaoapp.musically"),
        )
        assertEquals("uygulama değiştirmek tıkınmayı kesmez", 1.0, c.oruntu["oturum"]!!, 1e-9)
    }

    @Test
    fun `uzun bosluk blogu bozar`() {
        // 20 dk → 30 dk boşluk → 20 dk : iki ayrı blok, ikisi de 30 dk altı
        val c = cikar(
            olay(21, 0, 20),
            olay(21, 50, 20),
        )
        assertEquals(0.0, c.oruntu["oturum"]!!, 1e-9)
    }

    @Test
    fun `uretim kullanimi oturum sayilmaz`() {
        // §4.6 md.2: 2 saatlik CapCut derin çalışmadır, tıkınma değil
        val c = cikar(olay(14, 0, 120, Kategori.PRODUCTIVITY, "com.lemon.lvoverseas"))
        assertEquals(0.0, c.oruntu["oturum"]!!, 1e-9)
    }

    // -----------------------------------------------------------------
    // uyku penceresi, sabah, uyku_oncesi
    // -----------------------------------------------------------------
    @Test
    fun `gunduz boslugu uyku sayilmaz`() {
        // 07:30'da biter, 22:30'da başlar → 15 saatlik boşluk.
        // Telefonu hiç açmadan geçen bir iş günü. Uyku DEĞİL.
        val c = cikar(olay(7, 0, 30), olay(22, 30, 30))
        assertNull(
            "orta noktası öğleden sonraya düşen boşluk uyku olamaz",
            c.uykuPenceresi,
        )
        assertEquals(0.0, c.oruntu["sabah"]!!, 1e-9)
        assertEquals(0.0, c.oruntu["uyku_oncesi"]!!, 1e-9)
    }

    @Test
    fun `gece yarisindan sabaha bosluk uyku sayilir`() {
        // 01:00'de 30 dk, sonra 09:00'da 30 dk → 7.5 saat boşluk
        val c = cikar(olay(1, 0, 30), olay(9, 0, 30))
        val uyku = c.uykuPenceresi
        assertNotNull(uyku)
        assertEquals(7.5, uyku!!.saat, 1e-9)
    }

    @Test
    fun `uyanir uyanmaz uzun oturum sabah degerini yukseltir`() {
        // Uyku 01:30 → 09:00. Uyanınca 30 dk kaydırma → sabah 1.0
        val c = cikar(olay(1, 0, 30), olay(9, 0, 30))
        assertEquals(1.0, c.oruntu["sabah"]!!, 1e-9)
    }

    @Test
    fun `alarmi kapatip birakmak sabah degerini dusuk tutar`() {
        // Uyku 01:30 → 09:00. Uyanınca 3 dk → 3/30 = 0.1
        val c = cikar(olay(1, 0, 30), olay(9, 0, 3))
        assertEquals(0.1, c.oruntu["sabah"]!!, 1e-9)
    }

    @Test
    fun `uykudan onceki 30 dakika kullanimi olculur`() {
        // 01:00–01:30 kullanım, ardından uyku → pencere tamamen dolu
        val c = cikar(olay(1, 0, 30), olay(9, 0, 20))
        assertEquals(1.0, c.oruntu["uyku_oncesi"]!!, 1e-9)
    }

    @Test
    fun `uyku yoksa sabah ve uyku_oncesi sifir`() {
        val c = cikar(olay(10, 0, 30), olay(12, 0, 30), olay(14, 0, 30))
        assertNull(c.uykuPenceresi)
        assertEquals(0.0, c.oruntu["sabah"]!!, 1e-9)
        assertEquals(0.0, c.oruntu["uyku_oncesi"]!!, 1e-9)
    }

    // -----------------------------------------------------------------
    // acilis
    // -----------------------------------------------------------------
    @Test
    fun `acilis sayisi olay sayisidir`() {
        val c = cikar(olay(9, 0, 5), olay(10, 0, 5), olay(11, 0, 5))
        assertEquals(3, c.acilisSayisi)
        assertTrue("3 açılış tavanın çok altında", c.oruntu["acilis"]!! < 0.1)
    }

    @Test
    fun `acilis orani tavani asmaz`() {
        val olaylar = (0 until 200).map { i ->
            olay(8 + i / 60, i % 60, 1, paket = "com.instagram.android")
        }
        val c = OruntuCikar.cikar(olaylar, tarih, zon)
        assertTrue("oran 1.0'ı aşmamalı", c.oruntu["acilis"]!! <= 1.0)
    }

    // -----------------------------------------------------------------
    // yer_degis — çok günlü
    // -----------------------------------------------------------------
    @Test
    fun `ekran artip adim duserse yer degis sinyali verir`() {
        val ekran = listOf(200.0, 200.0, 200.0, 300.0, 300.0, 300.0)
        val adim = listOf(8000.0, 8000.0, 8000.0, 5000.0, 5000.0, 5000.0)
        val deger = YerDegisHesapla.hesapla(ekran, adim)
        assertTrue("sinyal olmalı: $deger", deger > 0.5)
    }

    @Test
    fun `ikisi de artarsa sinyal yok`() {
        val ekran = listOf(200.0, 200.0, 300.0, 300.0)
        val adim = listOf(5000.0, 5000.0, 8000.0, 8000.0)
        assertEquals(0.0, YerDegisHesapla.hesapla(ekran, adim), 1e-9)
    }

    @Test
    fun `yetersiz gun sifir dondurur`() {
        assertEquals(0.0, YerDegisHesapla.hesapla(listOf(1.0, 2.0), listOf(1.0, 2.0)), 1e-9)
    }

    // -----------------------------------------------------------------
    @Test
    fun `cikarim core-risk girdisine dogrudan verilebilir`() {
        val c = cikar(
            olay(1, 0, 60, Kategori.SOCIAL),
            olay(9, 0, 30, Kategori.COMMUNICATION, "com.whatsapp"),
            olay(14, 0, 90, Kategori.PRODUCTIVITY, "com.google.android.apps.docs"),
        )
        val sonuc = tr.org.yesilay.yesiliz.risk.RiskHesapla.hesapla(
            tr.org.yesilay.yesiliz.risk.GunlukGirdi(
                kategoriDakika = c.kategoriDakika,
                oruntu = c.oruntu,
                adim = 4000.0,
                sporDk = 0.0,
            ),
        )
        assertTrue(sonuc.R > 0.0)
        assertEquals("social", sonuc.surukleyiciler.first())
    }
}
