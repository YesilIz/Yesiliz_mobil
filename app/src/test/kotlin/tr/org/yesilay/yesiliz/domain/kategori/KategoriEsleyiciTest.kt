package tr.org.yesilay.yesiliz.domain.kategori

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.org.yesilay.yesiliz.risk.GunlukGirdi
import tr.org.yesilay.yesiliz.risk.RiskHesapla
import tr.org.yesilay.yesiliz.risk.Sabitler
import java.io.File

/**
 * `tests/test_vectors.json` içindeki **kategorilendirme** vektörlerini koşar.
 *
 * Bu vektörler core-risk'te değil burada test ediliyor: paket adı → kategori
 * eşlemesi app katmanının işi, motor kategori adlarını hazır alır
 * (CLAUDE.md §6). `VektorTest` bu ayrımı not düşüyor.
 */
class KategoriEsleyiciTest {

    private val tabloIcerigi: String by lazy {
        val yol = System.getProperty("kategoriTablosu")
            ?: error("kategoriTablosu sistem özelliği yok — app/build.gradle.kts'e bak")
        File(yol).also { assertTrue("Tablo bulunamadı: $yol", it.exists()) }.readText()
    }

    private val tablo: Map<String, Kategori> by lazy { KategoriTablosu.coz(tabloIcerigi) }
    private val esleyici: KategoriEsleyici by lazy { KategoriEsleyici(tablo) }

    private val vektorler by lazy {
        val yol = System.getProperty("vektorDosyasi")!!
        Json.parseToJsonElement(File(yol).readText())
            .jsonObject["vektorler"]!!.jsonObject["kategorilendirme"]!!.jsonArray
    }

    // -----------------------------------------------------------------
    // Ortak vektörler
    // -----------------------------------------------------------------
    @Test
    fun `kategorilendirme vektorleri referansla ayni`() {
        assertTrue("vektör yok", vektorler.isNotEmpty())

        for (v in vektorler) {
            val o = v.jsonObject
            val ad = o["ad"]!!.jsonPrimitive.content
            val girdi = o["girdi"]!!.jsonObject

            val kullanim = girdi["kullanim_listesi"]!!.jsonArray.map {
                val cift = it.jsonArray
                PaketSuresi(cift[0].jsonPrimitive.content, cift[1].jsonPrimitive.double)
            }

            val secimler = girdi["kullanici_secimleri"]!!.let { eleman ->
                if (eleman is kotlinx.serialization.json.JsonNull) emptyMap()
                else eleman.jsonObject.mapValues { Kategori.anahtardan(it.value.jsonPrimitive.content) }
            }

            val beklenen = o["beklenen"]!!.jsonObject["kategori_dakika"]!!.jsonObject
                .mapValues { it.value.jsonPrimitive.double }

            val gercek = KategoriEsleyici(tablo, secimler).kategorilereDagit(kullanim)

            assertEquals("$ad · anahtarlar", beklenen.keys, gercek.keys)
            for ((kategori, dakika) in beklenen) {
                assertEquals("$ad · $kategori", dakika, gercek[kategori]!!, 1e-9)
            }
        }
    }

    // -----------------------------------------------------------------
    // Tablonun kendisi
    // -----------------------------------------------------------------
    @Test
    fun `tablodaki her kategori tanimli`() {
        assertTrue("tablo şüpheli derecede küçük: ${tablo.size}", tablo.size > 2000)
        // coz() bilinmeyen anahtarda zaten patlar; burada dağılımı doğruluyoruz
        val dagilim = tablo.values.groupingBy { it }.eachCount()
        for (kategori in Kategori.entries) {
            assertTrue("$kategori için hiç paket yok", (dagilim[kategori] ?: 0) > 0)
        }
    }

    @Test
    fun `risk kategorileri core-risk ile ortusuyor`() {
        val motorunBildigi = Sabitler.KATEGORI_AGIRLIK.keys
        val bizimHesabaGirenler = Kategori.entries
            .filter { it.hesabaGirer }
            .map { it.anahtar }
            .toSet()
        assertEquals(
            "app'in kategori kümesi motorunkiyle aynı olmalı",
            motorunBildigi, bizimHesabaGirenler,
        )
    }

    @Test
    fun `sistem paketleri hesaba girmez`() {
        val sistemPaketi = tablo.entries.first { it.value == Kategori.SISTEM }.key
        val dagilim = esleyici.kategorilereDagit(
            listOf(
                PaketSuresi(sistemPaketi, 300.0),
                PaketSuresi("com.instagram.android", 60.0),
            ),
        )
        assertEquals("yalnızca instagram sayılmalı", setOf("social"), dagilim.keys)
        assertEquals(60.0, dagilim["social"]!!, 0.0)
    }

    // -----------------------------------------------------------------
    // Öncelik sırası
    // -----------------------------------------------------------------
    @Test
    fun `kullanici secimi tabloyu ezer`() {
        val esleyici = KategoriEsleyici(
            tablo,
            mapOf("com.instagram.android" to Kategori.PRODUCTIVITY),
        )
        assertEquals(Kategori.PRODUCTIVITY, esleyici.bul("com.instagram.android"))
    }

    @Test
    fun `tablo android beyanini ezer`() {
        // Instagram tabloda social; Android "productivity" dese bile tablo kazanır
        assertEquals(
            Kategori.SOCIAL,
            esleyici.bul("com.instagram.android", androidKategorisi = 7),
        )
    }

    @Test
    fun `tabloda yoksa android beyani kullanilir`() {
        assertEquals(
            Kategori.GAMES,
            esleyici.bul("com.bilinmeyen.oyun", androidKategorisi = 0),
        )
    }

    @Test
    fun `hicbiri yoksa other`() {
        assertEquals(Kategori.OTHER, esleyici.bul("com.bilinmeyen.uygulama"))
        assertEquals(
            "CATEGORY_UNDEFINED yedek sayılmaz",
            Kategori.OTHER,
            esleyici.bul("com.bilinmeyen.uygulama", androidKategorisi = -1),
        )
    }

    // -----------------------------------------------------------------
    // Uçtan uca: tablo → motor
    // -----------------------------------------------------------------
    @Test
    fun `ham kullanimdan risk skoruna kadar calisir`() {
        val gun = esleyici.kategorilereDagit(
            listOf(
                PaketSuresi("com.instagram.android", 120.0),
                PaketSuresi("com.zhiliaoapp.musically", 90.0),
                PaketSuresi("com.whatsapp", 40.0),
                PaketSuresi("com.google.android.apps.docs", 50.0),
            ),
        )
        val sonuc = RiskHesapla.hesapla(GunlukGirdi(gun, mapOf("gece" to 0.3)))

        assertTrue("skor üretilmeli", sonuc.R > 0.0)
        assertNotNull(sonuc.bant)
        assertFalse("sürükleyiciler boş olmamalı", sonuc.surukleyiciler.isEmpty())
        assertEquals("en büyük sürükleyici instagram'ın kategorisi", "social", sonuc.surukleyiciler.first())
    }
}
