package tr.org.yesilay.yesiliz.risk

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `tests/test_vectors.json` üzerinden Kotlin portunun referans Python ile
 * **birebir aynı** sonucu verdiğini doğrular (CLAUDE.md §7).
 *
 * Vektörler `tests/vektor_uret.py` tarafından `docs/referans/yesiliz_hesap.py`
 * çalıştırılarak üretiliyor; yuvarlanmamış ham float saklanıyor ki 1e-4
 * tolerans anlamlı olsun.
 *
 * Sabitler veya referans Python değişirse:
 * ```
 * python3 tests/vektor_uret.py
 * ./gradlew :core-risk:test
 * ```
 */
class VektorTest {

    private val kok: JsonObject by lazy {
        val yol = System.getProperty("vektorDosyasi")
            ?: error("vektorDosyasi sistem özelliği yok — core-risk/build.gradle.kts'e bak")
        val dosya = File(yol)
        assertTrue("Vektör dosyası bulunamadı: $yol", dosya.exists())
        Json.parseToJsonElement(dosya.readText()).jsonObject
    }

    private val tolerans: Double by lazy { kok["tolerans"]!!.d() }
    private val vektorler: JsonObject by lazy { kok["vektorler"]!!.jsonObject }

    private fun grup(ad: String): JsonArray = vektorler[ad]!!.jsonArray

    // -----------------------------------------------------------------
    // Sabitler — Kotlin ile Python ayrışmışsa diğer testler yanıltıcı olur,
    // bu yüzden önce burası kontrol edilir.
    // -----------------------------------------------------------------
    @Test
    fun `sabitler referansla ayni`() {
        val s = kok["sabitler"]!!.jsonObject
        assertEquals(s["S_REF"]!!.d(), Sabitler.S_REF, 0.0)
        assertEquals(s["SASSV_KESME"]!!.d(), Sabitler.SASSV_KESME, 0.0)
        assertEquals(s["MAKS_SONUMLEME"]!!.d(), Sabitler.MAKS_SONUMLEME, 0.0)
        assertEquals(s["BANT_AZ_UST"]!!.d(), Sabitler.BANT_AZ_UST, 0.0)
        assertEquals(s["BANT_ORTA_UST"]!!.d(), Sabitler.BANT_ORTA_UST, 0.0)
        assertEquals(s["GUVENLIK_GUN"]!!.d().toInt(), Sabitler.GUVENLIK_GUN)
        assertEquals(s["GUN_YUKARI"]!!.d().toInt(), Sabitler.GUN_YUKARI)
        assertEquals(s["GUN_ASAGI"]!!.d().toInt(), Sabitler.GUN_ASAGI)

        val agirlik = s["KATEGORI_AGIRLIK"]!!.jsonObject
        assertEquals(agirlik.size, Sabitler.KATEGORI_AGIRLIK.size)
        agirlik.forEach { (kategori, deger) ->
            assertEquals(kategori, deger.d(), Sabitler.KATEGORI_AGIRLIK[kategori]!!, 0.0)
        }

        val oruntu = s["ORUNTU_KATSAYI"]!!.jsonObject
        assertEquals(oruntu.size, Sabitler.ORUNTU_KATSAYI.size)
        oruntu.forEach { (ad, deger) ->
            assertEquals(ad, deger.d(), Sabitler.ORUNTU_KATSAYI[ad]!!, 0.0)
        }

        assertEquals(
            s["TUKETIM"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet(),
            Sabitler.TUKETIM,
        )
    }

    // -----------------------------------------------------------------
    // §4.7 doğrulama profilleri + kenar durumlar
    // -----------------------------------------------------------------
    @Test
    fun `dogrulama profilleri`() = riskVektorleriniKosur("dogrulama_profilleri")

    @Test
    fun `risk skoru kenar durumlari`() = riskVektorleriniKosur("risk_skoru_kenar")

    private fun riskVektorleriniKosur(grupAdi: String) {
        val vektorler = grup(grupAdi)
        assertTrue("$grupAdi boş", vektorler.isNotEmpty())

        for (v in vektorler) {
            val o = v.jsonObject
            val ad = o["ad"]!!.jsonPrimitive.content
            val girdi = o["girdi"]!!.jsonObject
            val beklenen = o["beklenen"]!!.jsonObject

            val anketPuani = girdi["anket_puani"]!!.dOrNull()
            val sonuc = RiskHesapla.hesapla(
                gun = GunlukGirdi(
                    kategoriDakika = girdi["kategori_dakika"]!!.jsonObject
                        .mapValues { it.value.d() },
                    oruntu = girdi["oruntu"]!!.jsonObject.mapValues { it.value.d() },
                    adim = girdi["adim"]!!.d(),
                    sporDk = girdi["spor_dk"]!!.d(),
                ),
                anket = anketPuani?.let {
                    AnketGirdi(
                        hamPuan = it,
                        kacGunOnce = girdi["anket_kac_gun_once"]!!.d().toInt(),
                    )
                },
                d = girdi["kacinci_gun"]!!.d().toInt(),
            )

            esit(ad, "S", beklenen["S"]!!.d(), sonuc.S)
            esit(ad, "P", beklenen["P"]!!.d(), sonuc.P)
            esit(ad, "D", beklenen["D"]!!.d(), sonuc.D)
            esit(ad, "B_tuketim", beklenen["B_tuketim"]!!.d(), sonuc.bTuketim)
            esit(ad, "B_islevsel", beklenen["B_islevsel"]!!.d(), sonuc.bIslevsel)
            esit(ad, "R_b", beklenen["R_b"]!!.d(), sonuc.rB)
            esit(ad, "alpha", beklenen["alpha"]!!.d(), sonuc.alpha)
            esit(ad, "R", beklenen["R"]!!.d(), sonuc.R)

            val beklenenRs = beklenen["R_s"]!!.dOrNull()
            if (beklenenRs == null) {
                assertNull("$ad · R_s null olmalı", sonuc.rS)
            } else {
                esit(ad, "R_s", beklenenRs, sonuc.rS!!)
            }

            assertEquals(
                "$ad · bant",
                Bant.etiketten(beklenen["bant"]!!.jsonPrimitive.content),
                sonuc.bant,
            )
            assertEquals(
                "$ad · surukleyiciler",
                beklenen["surukleyiciler"]!!.jsonArray.map { it.jsonPrimitive.content },
                sonuc.surukleyiciler,
            )
        }
    }

    // -----------------------------------------------------------------
    // §5 Bantlama
    // -----------------------------------------------------------------
    @Test
    fun `esik sinirlari`() {
        for (v in grup("esikten_bant")) {
            val o = v.jsonObject
            val skor = o["girdi"]!!.jsonObject["skor"]!!.d()
            val beklenen = Bant.etiketten(
                o["beklenen"]!!.jsonObject["bant"]!!.jsonPrimitive.content,
            )
            assertEquals("skor=$skor", beklenen, Bantlama.esiktenBant(skor))
        }
    }

    @Test
    fun `guvenlik tabani`() {
        for (v in grup("guvenlik_tabani")) {
            val o = v.jsonObject
            val ad = o["ad"]!!.jsonPrimitive.content
            val gunler = o["girdi"]!!.jsonObject["gunluk_veri"]!!.jsonArray.map {
                val g = it.jsonObject
                GunlukOzet(
                    tuketimDk = g["tuketim_dk"]!!.d(),
                    geceDk = g["gece_dk"]!!.d(),
                )
            }
            val beklenen = o["beklenen"]!!.jsonObject
            val sonuc = Bantlama.guvenlikTabani(gunler)

            val beklenenTaban = beklenen["taban"]!!.metinVeyaNull()
            assertEquals("$ad · taban", beklenenTaban?.let { Bant.etiketten(it) }, sonuc.taban)
            assertEquals("$ad · kural", beklenen["kural"]!!.metinVeyaNull(), sonuc.kural)
        }
    }

    @Test
    fun `histerezis`() {
        for (v in grup("histerezis")) {
            val o = v.jsonObject
            val ad = o["ad"]!!.jsonPrimitive.content
            val adaylar = o["girdi"]!!.jsonObject["aday_dizisi"]!!.jsonArray
                .map { Bant.etiketten(it.jsonPrimitive.content) }
            val beklenen = o["beklenen"]!!.jsonObject

            val takipci = BantTakipci()
            val gosterilen = adaylar.map { takipci.guncelle(it) }

            assertEquals(
                "$ad · gösterilen dizi",
                beklenen["gosterilen_dizisi"]!!.jsonArray
                    .map { Bant.etiketten(it.jsonPrimitive.content) },
                gosterilen,
            )
            assertEquals(
                "$ad · son bant",
                beklenen["son_bant"]!!.metinVeyaNull()?.let { Bant.etiketten(it) },
                takipci.mevcutBant,
            )
            assertEquals("$ad · son durum", beklenen["son_durum"]!!.metinVeyaNull(), takipci.durum())
        }
    }

    @Test
    fun `trend`() {
        for (v in grup("trend")) {
            val o = v.jsonObject
            val ad = o["ad"]!!.jsonPrimitive.content
            val gecmis = o["girdi"]!!.jsonObject["skor_gecmisi"]!!.jsonArray.map { it.d() }
            val beklenen = Trend.etiketten(
                o["beklenen"]!!.jsonObject["trend"]!!.jsonPrimitive.content,
            )
            assertEquals(ad, beklenen, Bantlama.trendBul(gecmis))
        }
    }

    /**
     * `kategorilendirme` grubu BİLEREK burada koşulmuyor.
     *
     * Paket adı → kategori eşlemesi `app/domain/kategori/` içinde yaşayacak
     * (CLAUDE.md §6) — core-risk kategori adlarını hazır alır, paket adı
     * hiç görmez. O vektörler `app` modülünün `KategoriEsleyiciTest`'inde
     * koşuluyor.
     */
    @Test
    fun `kategorilendirme vektorleri app modulune ait`() {
        assertTrue(
            "kategorilendirme vektörleri kaybolmuş — app testinde lazım",
            grup("kategorilendirme").isNotEmpty(),
        )
    }

    // -----------------------------------------------------------------
    private fun esit(ad: String, alan: String, beklenen: Double, gercek: Double) =
        assertEquals("$ad · $alan", beklenen, gercek, tolerans)

    private fun JsonElement.d(): Double = jsonPrimitive.double

    private fun JsonElement.dOrNull(): Double? =
        if (this is JsonNull) null else jsonPrimitive.double

    private fun JsonElement.metinVeyaNull(): String? =
        if (this is JsonNull) null else jsonPrimitive.content
}
