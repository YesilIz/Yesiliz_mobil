package tr.org.yesilay.yesiliz.domain.bilgi

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Bilgi Bankası yazısı — `assets/bilgi_bankasi.json`.
 *
 * İçerik **cihazda**, ağ yok. Bilgi bankasını sunucudan çekmek Faz 2'de bile
 * planlanmıyor: metinler nadiren değişiyor ve uygulamanın çevrimdışı
 * çalışması, veri politikasının (CLAUDE.md §4) doğal bir sonucu.
 *
 * [govde] paragraf listesi; markdown çözümleyicisi yok. Tek bir vurgu biçimi
 * (`**kalın**`) destekleniyor, o da ekran tarafında işleniyor.
 */
data class BilgiYazisi(
    val id: String,
    val baslik: String,
    val ozet: String,
    val kategori: String,
    val okumaDk: Int,
    val govde: List<String>,
)

/**
 * JSON çözümleyicisi — [tr.org.yesilay.yesiliz.domain.kategori.KategoriTablosu]
 * ile **aynı desen**: `Context` almaz (çağıran metni verir) ve `@Serializable`
 * kullanmaz.
 *
 * `@Serializable` kullanılmamasının sebebi: `kotlinx-serialization` derleyici
 * eklentisi `:app` modülünde uygulanmıyor, yalnızca JSON çalışma zamanı
 * bağımlılığı var. Tek bir dosya için AGP 9 eklenti yapılandırmasına
 * dokunmak, CLAUDE.md §9'daki bilinen tuzağa (AGP 9 + eklenti çakışması)
 * gereksiz yere yaklaşmak olurdu.
 */
object BilgiBankasi {

    private val json = Json { ignoreUnknownKeys = true }

    fun coz(icerik: String): List<BilgiYazisi> {
        val kok = json.parseToJsonElement(icerik).jsonObject
        val yazilar = kok["yazilar"]?.jsonArray
            ?: error("bilgi_bankasi.json içinde 'yazilar' yok")

        return yazilar.map { eleman ->
            val nesne = eleman.jsonObject
            BilgiYazisi(
                id = nesne.metin("id"),
                baslik = nesne.metin("baslik"),
                ozet = nesne.metin("ozet"),
                kategori = nesne.metin("kategori"),
                okumaDk = nesne["okumaDk"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                govde = nesne["govde"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: emptyList(),
            )
        }
    }

    fun surum(icerik: String): Int =
        json.parseToJsonElement(icerik).jsonObject["surum"]
            ?.jsonPrimitive?.content?.toIntOrNull() ?: 0

    /** Kategori çipleri — dosyadaki görülme sırasını koruyor. */
    fun kategoriler(yazilar: List<BilgiYazisi>): List<String> =
        yazilar.map { it.kategori }.distinct()

    private fun kotlinx.serialization.json.JsonObject.metin(anahtar: String): String =
        this[anahtar]?.jsonPrimitive?.content
            ?: error("bilgi_bankasi.json: '$anahtar' alanı eksik")
}
