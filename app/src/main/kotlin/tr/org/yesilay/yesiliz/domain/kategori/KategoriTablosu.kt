package tr.org.yesilay.yesiliz.domain.kategori

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * `assets/kategori_tablosu.json` çözümleyicisi.
 *
 * Dosya `tools/kategori_tablosu_uret.py` tarafından üretiliyor:
 * LMU açık veri seti (doi:10.5334/jopd.59) + referans tablo + Türkiye eklemeleri.
 *
 * Bu sınıf **Context almaz** — çağıran açtığı akışın içeriğini metin olarak
 * verir. Böylece birim testte gerçek dosya, uygulamada `assets` kullanılır,
 * sınıf ikisini de bilmez.
 *
 * Bu tablo ilk açılışta Room'daki `KategoriEslemeEntity`'ye
 * seed edilir (bkz. `data/depo/KategoriDeposu`); kullanıcı override'ları da orada
 * saklanacak.
 */
object KategoriTablosu {

    private val json = Json { ignoreUnknownKeys = true }

    /** JSON içeriğinden paket → kategori haritası üretir. */
    fun coz(icerik: String): Map<String, Kategori> {
        val kok = json.parseToJsonElement(icerik).jsonObject
        val paketler = kok["paketler"]?.jsonObject
            ?: error("kategori_tablosu.json içinde 'paketler' yok")

        return paketler.entries.associate { (paket, kategoriElemani) ->
            paket to Kategori.anahtardan(kategoriElemani.jsonPrimitive.content)
        }
    }

    /** Tablonun üretildiği sürüm — uyumsuzluk tespiti için. */
    fun surum(icerik: String): Int =
        json.parseToJsonElement(icerik).jsonObject["surum"]
            ?.jsonPrimitive?.content?.toIntOrNull() ?: 0
}
