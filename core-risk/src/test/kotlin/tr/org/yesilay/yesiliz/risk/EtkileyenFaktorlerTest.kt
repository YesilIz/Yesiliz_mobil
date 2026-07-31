package tr.org.yesilay.yesiliz.risk

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * `tests/test_vectors.json` → `etkileyen_faktorler` grubu.
 *
 * Bu fonksiyonun referans Python'da (`docs/referans/yesiliz_hesap.py`)
 * karşılığı yok — Kotlin-only. Vektörler yine de `tests/vektor_uret.py`
 * içinde AYNI formülle üretiliyor ki iki taraf birbirinden sapmasın
 * (CLAUDE.md §7 test kuralı, spesifik referans dosyası olmasa da geçerli).
 */
class EtkileyenFaktorlerTest {

    private val kok: JsonObject by lazy {
        val yol = System.getProperty("vektorDosyasi")
            ?: error("vektorDosyasi sistem özelliği yok — core-risk/build.gradle.kts'e bak")
        Json.parseToJsonElement(File(yol).readText()).jsonObject
    }

    @Test
    fun `sabitler referansla ayni`() {
        val s = kok["sabitler"]!!.jsonObject
        assertEquals(s["FAKTOR_ESIK_DUSUK"]!!.jsonPrimitive.double, Sabitler.FAKTOR_ESIK_DUSUK, 0.0)
        assertEquals(s["FAKTOR_ESIK_ORTA"]!!.jsonPrimitive.double, Sabitler.FAKTOR_ESIK_ORTA, 0.0)
        assertEquals(
            s["FAKTOR_ASGARI_DK"]!!.jsonPrimitive.double,
            Sabitler.FAKTOR_ASGARI_DK,
            0.0,
        )
    }

    @Test
    fun `etkileyen faktorler`() {
        val vektorler = kok["vektorler"]!!.jsonObject["etkileyen_faktorler"]!!.jsonArray
        assert(vektorler.isNotEmpty())

        for (v in vektorler) {
            val o = v.jsonObject
            val ad = o["ad"]!!.jsonPrimitive.content
            val girdi = o["girdi"]!!.jsonObject
            val beklenenListe = o["beklenen"]!!.jsonObject["faktorler"]!!.jsonArray

            val gun = GunlukGirdi(
                kategoriDakika = girdi["kategori_dakika"]!!.jsonObject
                    .mapValues { it.value.jsonPrimitive.double },
                oruntu = girdi["oruntu"]!!.jsonObject.mapValues { it.value.jsonPrimitive.double },
                adim = girdi["adim"]!!.jsonPrimitive.double,
                sporDk = girdi["spor_dk"]!!.jsonPrimitive.double,
            )

            val gercek = EtkileyenFaktorler.hesapla(gun)
            assertEquals("$ad · faktör sayısı", beklenenListe.size, gercek.size)

            beklenenListe.forEachIndexed { i, beklenenElem ->
                val b = beklenenElem.jsonObject
                val g = gercek[i]
                assertEquals("$ad · faktör $i ad", b["ad"]!!.jsonPrimitive.content, g.ad)
                assertEquals(
                    "$ad · faktör $i seviye",
                    FaktorSeviyesi.valueOf(b["seviye"]!!.jsonPrimitive.content),
                    g.seviye,
                )
                assertEquals("$ad · faktör $i oran", b["oran"]!!.jsonPrimitive.double, g.oran, 1e-4)
            }
        }
    }
}
