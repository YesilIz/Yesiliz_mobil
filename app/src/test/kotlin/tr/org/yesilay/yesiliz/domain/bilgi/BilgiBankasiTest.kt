package tr.org.yesilay.yesiliz.domain.bilgi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.org.yesilay.yesiliz.domain.test.FarkindalikTesti
import tr.org.yesilay.yesiliz.domain.test.TestSecenegi
import java.io.File

/**
 * `assets/bilgi_bankasi.json` gerçek dosya üzerinden doğrulanıyor.
 *
 * Dosya yolu `inputs`'a bildirilmemiş bir kaynak değil, modülün kendi
 * `assets` klasörü — Gradle testi UP-TO-DATE sayarsa bile dosya değişimi
 * derleme girdisi olarak zaten algılanıyor (CLAUDE.md §9 tuzağı burada
 * geçerli değil, çünkü dosya `src/main/assets` altında).
 */
class BilgiBankasiTest {

    private val icerik: String =
        File("src/main/assets/bilgi_bankasi.json").readText()

    private val yazilar = BilgiBankasi.coz(icerik)

    @Test
    fun `dosya cozumleniyor ve bos degil`() {
        assertTrue("en az 10 yazı bekleniyor", yazilar.size >= 10)
        assertEquals(1, BilgiBankasi.surum(icerik))
    }

    @Test
    fun `her yazinin zorunlu alanlari dolu`() {
        yazilar.forEach { yazi ->
            assertTrue("id boş: $yazi", yazi.id.isNotBlank())
            assertTrue("başlık boş: ${yazi.id}", yazi.baslik.isNotBlank())
            assertTrue("özet boş: ${yazi.id}", yazi.ozet.isNotBlank())
            assertTrue("kategori boş: ${yazi.id}", yazi.kategori.isNotBlank())
            assertTrue("okuma süresi yok: ${yazi.id}", yazi.okumaDk > 0)
            assertTrue("gövde boş: ${yazi.id}", yazi.govde.isNotEmpty())
        }
    }

    @Test
    fun `id ler benzersiz`() {
        assertEquals(yazilar.size, yazilar.map { it.id }.distinct().size)
    }

    @Test
    fun `id ler ascii — rota parcasi olarak kullaniliyor`() {
        // Türkçe karakter içeren bir id, `bilgi/{yaziId}` rotasında kaçış
        // gerektirir; ASCII tutmak bu sorunu tamamen ortadan kaldırıyor.
        val kalip = Regex("^[a-z0-9-]+$")
        yazilar.forEach { yazi ->
            assertTrue("id ASCII değil: ${yazi.id}", kalip.matches(yazi.id))
        }
    }

    @Test
    fun `farkindalik testinin onerdigi her yazi mevcut`() {
        val idler = yazilar.map { it.id }.toSet()

        // Testin tüm dallarını gez: her soru için "sık sık" senaryosu +
        // hiçbir şey işaretlenmemiş senaryo.
        val senaryolar = FarkindalikTesti.SORULAR.indices.map { i ->
            List(FarkindalikTesti.SORULAR.size) { j ->
                if (i == j) TestSecenegi.SIK else TestSecenegi.NADIREN
            }
        } + listOf(List(FarkindalikTesti.SORULAR.size) { TestSecenegi.NADIREN })

        senaryolar.forEach { yanitlar ->
            val onerilen = FarkindalikTesti.sonucHesapla(yanitlar).onerilenYaziId
            assertTrue("bilgi bankasında yok: $onerilen", onerilen in idler)
        }
    }

    @Test
    fun `metinlerde emir kipi ve korkutma dili yok`() {
        // CLAUDE.md §4: emir kipi ve korkutma Yeşilay marka kılavuzunda yasak.
        val yasakli = listOf("bağımlısın", "hemen bırak", "tehlikedesin", "zararlısın")
        yazilar.forEach { yazi ->
            val tam = (listOf(yazi.baslik, yazi.ozet) + yazi.govde)
                .joinToString(" ")
                .lowercase()
            yasakli.forEach { kelime ->
                assertFalse("'$kelime' geçiyor (${yazi.id})", tam.contains(kelime))
            }
        }
    }
}
