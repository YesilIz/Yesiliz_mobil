package tr.org.yesilay.yesiliz.domain.puan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `docs/spec-puan.md §7.4` ve `§7.5` doğrulaması.
 *
 * En kritik iddia **hiçbir yolun negatif puan üretememesi**: CLAUDE.md §4
 * ceza puanını yasaklıyor, "ceza suçluluk üretir, uygulamayı sildirir".
 */
class PuanHesaplaTest {

    @Test
    fun `formul spekteki ornekle ayni sonucu verir`() {
        // 20 dk spor (60) + 4000 adım (20) + görev 15 + hedef 25 + gece 10 = 130
        val sonuc = PuanHesapla.gunlukPuan(
            gorevPuani = 15,
            sporDk = 20.0,
            adim = 4_000.0,
            hedefTutturuldu = true,
            geceTemiz = true,
            seriGun = 0,
        )
        assertEquals(130, sonuc.ham)
        assertEquals("çarpan 1.0 iken net = ham", 130, sonuc.net)
    }

    @Test
    fun `gunluk tavan asilmaz`() {
        val sonuc = PuanHesapla.gunlukPuan(
            gorevPuani = 500,
            sporDk = 300.0,
            adim = 100_000.0,
            hedefTutturuldu = true,
            geceTemiz = true,
            seriGun = 100,
        )
        assertEquals(
            "tek günde patlama engellenmeli (§9.2)",
            PuanSabitleri.GUNLUK_TAVAN.toInt(),
            sonuc.net,
        )
    }

    @Test
    fun `spor ve adim kendi tavanlarina takilir`() {
        val cok = PuanHesapla.gunlukPuan(
            gorevPuani = 0,
            sporDk = 120.0,
            adim = 50_000.0,
            hedefTutturuldu = false,
            geceTemiz = false,
            seriGun = 0,
        )
        // spor tavanı 90 + adım tavanı 40 = 130
        assertEquals(130, cok.ham)
    }

    @Test
    fun `seri carpani ikiyi gecmez`() {
        assertEquals(1.0, PuanHesapla.seriCarpani(0), 1e-9)
        assertEquals(1.5, PuanHesapla.seriCarpani(5), 1e-9)
        assertEquals(2.0, PuanHesapla.seriCarpani(10), 1e-9)
        assertEquals("tavan 2.0", 2.0, PuanHesapla.seriCarpani(999), 1e-9)
    }

    @Test
    fun `bir gun kacirma seriyi sifirlamaz dondurur`() {
        // §7.4 kayıp koruması
        assertEquals("kesintisiz → artar", 6, PuanHesapla.yeniSeri(5, aradakiBosGun = 0))
        assertEquals("bir gün kaçtı → donar", 5, PuanHesapla.yeniSeri(5, aradakiBosGun = 1))
        assertEquals("iki gün kaçtı → sıfırlanır", 1, PuanHesapla.yeniSeri(5, aradakiBosGun = 2))
    }

    @Test
    fun `hicbir girdi negatif puan uretemez`() {
        // Ceza yolu YOK (CLAUDE.md §4). Uç değerlerin hepsi >= 0 vermeli.
        val girdiler = listOf(
            Triple(0, 0.0, 0.0),
            Triple(0, -0.0, -0.0),
            Triple(0, 0.0, 0.0),
        )
        girdiler.forEach { (gorev, spor, adim) ->
            listOf(true, false).forEach { hedef ->
                listOf(true, false).forEach { gece ->
                    listOf(0, 1, 50).forEach { seri ->
                        val sonuc = PuanHesapla.gunlukPuan(gorev, spor, adim, hedef, gece, seri)
                        assertTrue("ham negatif olmamalı: $sonuc", sonuc.ham >= 0)
                        assertTrue("net negatif olmamalı: $sonuc", sonuc.net >= 0)
                    }
                }
            }
        }
    }

    @Test
    fun `bos gun sifir puan verir ama negatif degil`() {
        val sonuc = PuanHesapla.gunlukPuan(
            gorevPuani = 0,
            sporDk = 0.0,
            adim = 0.0,
            hedefTutturuldu = false,
            geceTemiz = false,
            seriGun = 0,
        )
        assertEquals(0, sonuc.ham)
        assertEquals(0, sonuc.net)
    }
}
