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
            hedefOrani = 1.0,
            geceOrani = 1.0,
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
            hedefOrani = 1.0,
            geceOrani = 1.0,
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
            hedefOrani = 0.0,
            geceOrani = 0.0,
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
            // 0..1 dışındaki değerler de denenmeli: `coerceIn` olmasaydı
            // eksi bir oran negatif puan üretirdi.
            listOf(-1.0, 0.0, 0.5, 1.0, 2.0).forEach { hedef ->
                listOf(-1.0, 0.0, 0.5, 1.0, 2.0).forEach { gece ->
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
            hedefOrani = 0.0,
            geceOrani = 0.0,
            seriGun = 0,
        )
        assertEquals(0, sonuc.ham)
        assertEquals(0, sonuc.net)
    }

    @Test
    fun `hedef puani oransal ve tavanli`() {
        // Hedefin altında ya da tam üstünde tam puan.
        assertEquals(1.0, PuanHesapla.hedefeYakinlik(toplamEkranDk = 60.0, hedefDk = 120), 1e-9)
        assertEquals(1.0, PuanHesapla.hedefeYakinlik(toplamEkranDk = 120.0, hedefDk = 120), 1e-9)
        // Aşıldıkça doğrusal iniyor: 180 dk, 120'lik hedefin yarı yolu.
        assertEquals(0.5, PuanHesapla.hedefeYakinlik(toplamEkranDk = 180.0, hedefDk = 120), 1e-9)
        // Hedefin iki katında sıfır -- ve altına inmiyor.
        assertEquals(0.0, PuanHesapla.hedefeYakinlik(toplamEkranDk = 240.0, hedefDk = 120), 1e-9)
        assertEquals(0.0, PuanHesapla.hedefeYakinlik(toplamEkranDk = 999.0, hedefDk = 120), 1e-9)
    }

    @Test
    fun `hedef sifirsa oran sifir`() {
        // Sıfıra bölme yok; hedef kurulmamışsa bu terim hiç puan vermiyor.
        assertEquals(0.0, PuanHesapla.hedefeYakinlik(toplamEkranDk = 10.0, hedefDk = 0), 1e-9)
    }

    @Test
    fun `gece temizligi esikte tam puan verir`() {
        assertEquals(1.0, PuanHesapla.geceTemizligi(0.0), 1e-9)
        assertEquals(1.0, PuanHesapla.geceTemizligi(PuanSabitleri.GECE_ESIK_DK), 1e-9)
        assertEquals(0.5, PuanHesapla.geceTemizligi(PuanSabitleri.GECE_ESIK_DK * 1.5), 1e-9)
        assertEquals(0.0, PuanHesapla.geceTemizligi(PuanSabitleri.GECE_ESIK_DK * 2), 1e-9)
        assertEquals(0.0, PuanHesapla.geceTemizligi(600.0), 1e-9)
    }

    /**
     * İ-4'ün asıl amacı: eskiden hedefin bir dakika üstü 25 puanın
     * tamamını götürüyordu.
     */
    @Test
    fun `hedefin bir dakika ustunde kalmak butun puani goturmez`() {
        val tam = PuanHesapla.gunlukPuan(
            gorevPuani = 0,
            sporDk = 0.0,
            adim = 0.0,
            hedefOrani = PuanHesapla.hedefeYakinlik(120.0, 120),
            geceOrani = 0.0,
            seriGun = 0,
        )
        val birDakikaAsim = PuanHesapla.gunlukPuan(
            gorevPuani = 0,
            sporDk = 0.0,
            adim = 0.0,
            hedefOrani = PuanHesapla.hedefeYakinlik(121.0, 120),
            geceOrani = 0.0,
            seriGun = 0,
        )
        assertEquals(25, tam.ham)
        assertTrue("bir dakika aşım puanı sıfırlamamalı", birDakikaAsim.ham >= 24)
    }
}
