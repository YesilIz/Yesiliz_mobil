package tr.org.yesilay.yesiliz.domain.test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FarkindalikTestiTest {

    private fun yanit(vararg puanlar: Int) = puanlar.map { p ->
        TestSecenegi.entries.first { it.puan == p }
    }

    @Test
    fun `tumu nadiren ise dusuk seviye ve sifir puan`() {
        val sonuc = FarkindalikTesti.sonucHesapla(yanit(0, 0, 0, 0, 0))

        assertEquals(0, sonuc.hamPuan)
        assertEquals(TestSeviyesi.DUSUK, sonuc.seviye)
    }

    @Test
    fun `tumu sik ise azami puan ve yuksek seviye`() {
        val sonuc = FarkindalikTesti.sonucHesapla(yanit(2, 2, 2, 2, 2))

        assertEquals(FarkindalikTesti.AZAMI_PUAN, sonuc.hamPuan)
        assertEquals(TestSeviyesi.YUKSEK, sonuc.seviye)
    }

    @Test
    fun `esik degerleri dogru bantliyor`() {
        // 3 → DUŞÜK (orta eşiği 4), 4 → ORTA, 6 → ORTA (yüksek eşiği 7), 7 → YÜKSEK
        assertEquals(TestSeviyesi.DUSUK, FarkindalikTesti.sonucHesapla(yanit(1, 1, 1, 0, 0)).seviye)
        assertEquals(TestSeviyesi.ORTA, FarkindalikTesti.sonucHesapla(yanit(1, 1, 1, 1, 0)).seviye)
        assertEquals(TestSeviyesi.ORTA, FarkindalikTesti.sonucHesapla(yanit(2, 2, 1, 1, 0)).seviye)
        assertEquals(TestSeviyesi.YUKSEK, FarkindalikTesti.sonucHesapla(yanit(2, 2, 2, 1, 0)).seviye)
    }

    @Test
    fun `ipuclari en cok iki tane ve sik isaretlenenlerden`() {
        val sonuc = FarkindalikTesti.sonucHesapla(yanit(2, 2, 2, 0, 0))

        assertEquals(2, sonuc.ipuclari.size)
        assertEquals(FarkindalikTesti.SORULAR[0].ipucu, sonuc.ipuclari[0])
        assertEquals(FarkindalikTesti.SORULAR[1].ipucu, sonuc.ipuclari[1])
    }

    @Test
    fun `sik yoksa ipuclari bazenlerden geliyor`() {
        val sonuc = FarkindalikTesti.sonucHesapla(yanit(0, 1, 0, 0, 0))

        assertEquals(1, sonuc.ipuclari.size)
        assertEquals(FarkindalikTesti.SORULAR[1].ipucu, sonuc.ipuclari[0])
    }

    @Test
    fun `onerilen yazi en yuksek puanli soruya gore secilir`() {
        // 3. soru (yaninda_yokken) en yüksek → nomofobi
        val sonuc = FarkindalikTesti.sonucHesapla(yanit(0, 1, 2, 0, 0))

        assertEquals("nomofobi", sonuc.onerilenYaziId)
    }

    @Test
    fun `hicbir sey isaretlenmemisse aliskanlik yazisi onerilir`() {
        val sonuc = FarkindalikTesti.sonucHesapla(yanit(0, 0, 0, 0, 0))

        assertEquals("aliskanlik-kurmak", sonuc.onerilenYaziId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `eksik yanit kabul edilmiyor`() {
        // Ortalama ile doldurma yok — spec-anket.md §6.4 ilkesi burada da geçerli.
        FarkindalikTesti.sonucHesapla(yanit(1, 1, 1))
    }

    @Test
    fun `ozet metinlerinde teshis dili yok`() {
        val yasakli = listOf("bağımlı", "hasta", "riskli grup", "tanı koy")
        TestSeviyesi.entries.forEach { seviye ->
            val puan = when (seviye) {
                TestSeviyesi.DUSUK -> yanit(0, 0, 0, 0, 0)
                TestSeviyesi.ORTA -> yanit(1, 1, 1, 1, 0)
                TestSeviyesi.YUKSEK -> yanit(2, 2, 2, 2, 2)
            }
            val ozet = FarkindalikTesti.sonucHesapla(puan).ozet.lowercase()
            yasakli.forEach { kelime ->
                assertTrue("'$kelime' geçmemeli: $ozet", !ozet.contains(kelime))
            }
        }
    }
}
