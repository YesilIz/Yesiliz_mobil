package tr.org.yesilay.yesiliz.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Çok günlü senaryolar ve vektörü olmayan yardımcılar.
 *
 * `VektorTest` referans Python ile birebir eşitliği doğruluyor; burası
 * ise **davranışın anlamlı olduğunu** doğruluyor: bir kullanıcı 20 gün
 * boyunca kötüleşip sonra iyileşirse ne görür?
 */
class SenaryoTest {

    private fun ozet(tuketim: Double, gece: Double = 0.0) = GunlukOzet(tuketim, gece)

    // -----------------------------------------------------------------
    // Alpha.dEfektif — referans Python'da karşılığı yok (bkz. Alpha.kt),
    // beklentiler spec-risk.md §4.5'ten elle hesaplandı.
    // -----------------------------------------------------------------
    @Test
    fun `d efektif gecmis gunu yarim sayar`() {
        // 10 canlı gün tam sayılır
        assertEquals(10, Alpha.dEfektif(GecmisPencere(canliGun = 10, gecmisGun = 0)))
        // 10 geçmiş gün × 0.6 = 6
        assertEquals(6, Alpha.dEfektif(GecmisPencere(canliGun = 0, gecmisGun = 10)))
        // 5 canlı + 10 geçmiş = 5 + 6 = 11
        assertEquals(11, Alpha.dEfektif(GecmisPencere(canliGun = 5, gecmisGun = 10)))
        // iOS: geçmiş yok
        assertEquals(0, Alpha.dEfektif(GecmisPencere()))
    }

    @Test
    fun `anket yoksa alpha daima sifir`() {
        for (d in listOf(0, 3, 7, 27, 28, 100)) {
            assertEquals("d=$d", 0.0, Alpha.hesapla(d, anketVar = false), 0.0)
        }
    }

    @Test
    fun `alpha davranis verisi biriktikce azalir`() {
        val degerler = listOf(0, 3, 7, 14, 28, 60).map { Alpha.hesapla(it, anketVar = true) }
        // Monoton azalan olmalı — davranış biriktikçe anket geri çekilir
        degerler.zipWithNext { onceki, sonraki ->
            assertTrue("alpha artmamalı: $degerler", sonraki <= onceki)
        }
        assertEquals(1.0, degerler.first(), 1e-9)
        assertEquals(Sabitler.ALPHA_TABAN, degerler.last(), 1e-9)
    }

    // -----------------------------------------------------------------
    // §4.6 md.1 — çarpan ve sönümleme SADECE tüketime
    // -----------------------------------------------------------------
    @Test
    fun `oruntu cezasi islevsel kullanimi cezalandirmaz`() {
        val kotuOruntu = mapOf("gece" to 1.0, "oturum" to 1.0, "acilis" to 1.0)

        // Sadece üretim: örüntü ne olursa olsun S değişmemeli
        val uretim = mapOf("productivity" to 200.0)
        val temiz = RiskHesapla.hesapla(GunlukGirdi(uretim))
        val kotu = RiskHesapla.hesapla(GunlukGirdi(uretim, kotuOruntu))
        assertEquals(
            "işlevsel yük örüntüden etkilenmemeli",
            temiz.S, kotu.S, 1e-9,
        )

        // Sadece sosyal: aynı örüntü S'i belirgin şekilde yükseltmeli
        val sosyal = mapOf("social" to 200.0)
        val sosyalTemiz = RiskHesapla.hesapla(GunlukGirdi(sosyal))
        val sosyalKotu = RiskHesapla.hesapla(GunlukGirdi(sosyal, kotuOruntu))
        assertTrue(
            "tüketim yükü örüntüden etkilenmeli",
            sosyalKotu.S > sosyalTemiz.S * 1.5,
        )
    }

    @Test
    fun `aktivite sonumlemesi tavanli`() {
        val gun = mapOf("social" to 300.0)
        val hareketsiz = RiskHesapla.hesapla(GunlukGirdi(gun))
        val maraton = RiskHesapla.hesapla(
            GunlukGirdi(gun, adim = 50_000.0, sporDk = 300.0),
        )
        // "Koş → istediğin kadar kaydır" tuzağı: en fazla %15 indirim
        assertEquals(1.0, hareketsiz.D, 1e-9)
        assertEquals(1.0 - Sabitler.MAKS_SONUMLEME, maraton.D, 1e-9)
        assertTrue(maraton.S >= hareketsiz.S * (1.0 - Sabitler.MAKS_SONUMLEME) - 1e-9)
    }

    // -----------------------------------------------------------------
    // §5 çok günlü akış
    // -----------------------------------------------------------------
    @Test
    fun `kullanici kotulesip iyilesirse bant gecikmeli takip eder`() {
        val takipci = BantTakipci()
        val bosGunler = emptyList<GunlukOzet>()
        val bosGecmis = emptyList<Double>()

        // 1. gün: AZ bandında yerleşir
        var sonuc = Bantlama.gunuIsle(20.0, bosGunler, bosGecmis, takipci)
        assertEquals(Bant.AZ, sonuc.gosterilenBant)

        // Kötüleşme: 3 gün üst üste ÇOK adayı → 3. günde geçer
        repeat(2) {
            sonuc = Bantlama.gunuIsle(80.0, bosGunler, bosGecmis, takipci)
            assertEquals("henüz geçmemeli", Bant.AZ, sonuc.gosterilenBant)
            assertNotNull("bekleyen aday gösterilmeli", sonuc.bekleyen)
        }
        sonuc = Bantlama.gunuIsle(80.0, bosGunler, bosGecmis, takipci)
        assertEquals(Bant.COK, sonuc.gosterilenBant)
        assertNull(sonuc.bekleyen)

        // İyileşme: aşağı inmek 7 gün ister
        repeat(6) {
            sonuc = Bantlama.gunuIsle(20.0, bosGunler, bosGecmis, takipci)
            assertEquals("aşağı iniş yavaş olmalı", Bant.COK, sonuc.gosterilenBant)
        }
        sonuc = Bantlama.gunuIsle(20.0, bosGunler, bosGecmis, takipci)
        assertEquals(Bant.AZ, sonuc.gosterilenBant)
    }

    /**
     * §5.4 tuzağı — "daha çok iyileştiğin için baştan başla" cezası olmamalı.
     * `VektorTest` bunu referansla karşılaştırıyor; burada niyeti anlatıyoruz.
     */
    @Test
    fun `derinlesen iyilesme sayaci sifirlamaz`() {
        val takipci = BantTakipci()
        takipci.guncelle(Bant.COK)

        // 3 gün ORTA adayı (aşağı yön), sonra daha da iyi: AZ adayı
        repeat(3) { takipci.guncelle(Bant.ORTA) }
        repeat(3) { takipci.guncelle(Bant.AZ) }
        assertEquals("7 gün dolmadan geçmemeli", Bant.COK, takipci.mevcutBant)

        // 7. aşağı gün
        takipci.guncelle(Bant.AZ)
        assertEquals("aynı yönde 7 gün doldu, geçmeli", Bant.AZ, takipci.mevcutBant)
    }

    @Test
    fun `guvenlik tabani skoru ezer`() {
        val takipci = BantTakipci()
        // Skor düşük ama 5 gün üst üste 8 saat+ tüketim var
        val kronik = List(5) { ozet(tuketim = 500.0) }
        val sonuc = Bantlama.gunuIsle(10.0, kronik, emptyList(), takipci)

        assertEquals("eşik AZ derdi, taban ÇOK'a çekmeli", Bant.COK, sonuc.adayBant)
        assertEquals("5 gün üst üste 8 saat+ tüketim", sonuc.guvenlikKurali)
    }

    @Test
    fun `guvenlik tabani adayi asagi cekmez`() {
        val takipci = BantTakipci()
        // Skor zaten ÇOK; taban ORTA. Aday ÇOK kalmalı, kural uygulanmadı sayılmalı.
        val gunler = List(5) { ozet(tuketim = 400.0) }
        val sonuc = Bantlama.gunuIsle(90.0, gunler, emptyList(), takipci)

        assertEquals(Bant.COK, sonuc.adayBant)
        assertNull("taban aşağı çekmemeli, kural raporlanmamalı", sonuc.guvenlikKurali)
    }

    @Test
    fun `trend bandi degistirmez sadece mesaji`() {
        val takipci = BantTakipci()
        val kotulesen = List(7) { 30.0 } + List(7) { 45.0 }
        val sonuc = Bantlama.gunuIsle(45.0, emptyList(), kotulesen, takipci)

        assertEquals(Trend.KOTULESIYOR, sonuc.trend)
        assertEquals("trend bandı etkilememeli", Bant.ORTA, sonuc.adayBant)
    }

    // -----------------------------------------------------------------
    // §2 mutlak kural: her hesap açıklanabilir olmalı
    // -----------------------------------------------------------------
    @Test
    fun `her hesap surukleyici dondurur`() {
        val sonuc = RiskHesapla.hesapla(
            GunlukGirdi(
                mapOf(
                    "social" to 120.0,
                    "productivity" to 60.0,
                    "games" to 90.0,
                    "education" to 10.0,
                ),
            ),
        )
        // social 120×1.0=120 · games 90×0.8=72 · productivity 60×0.3=18 · education 2
        assertEquals(listOf("social", "games", "productivity"), sonuc.surukleyiciler)
    }

    @Test
    fun `bos gun cokmeden sifir dondurur`() {
        val sonuc = RiskHesapla.hesapla(GunlukGirdi(emptyMap()))
        assertEquals(0.0, sonuc.S, 0.0)
        assertEquals(0.0, sonuc.rB, 0.0)
        assertEquals(Bant.AZ, sonuc.bant)
        assertTrue(sonuc.surukleyiciler.isEmpty())
    }
}
