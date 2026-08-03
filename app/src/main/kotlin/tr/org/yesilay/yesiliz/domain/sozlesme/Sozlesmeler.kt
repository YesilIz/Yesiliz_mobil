package tr.org.yesilay.yesiliz.domain.sozlesme

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Kullanım Koşulları, Gizlilik Politikası, açık kaynak lisansları ve SSS —
 * `assets/sozlesmeler.json`.
 *
 * ## Niçin var
 *
 * Kayıt ekranı "Kullanım Koşulları'nı ve Gizlilik Politikası'nı okudum"
 * onayını **zorunlu** tutuyordu ama metinleri okumanın hiçbir yolu yoktu:
 * o satır düz metindi, bağlantı değildi. Profil › Hakkında'daki üç satır ve
 * Yardım'daki dört SSS sorusu chevron'lu ve tıklanabilir görünüyor ama
 * `onTikla` almıyordu (`debug.md` Y-6).
 *
 * Onaylanması zorunlu tutulan bir metnin okunamaması, 18 yaş altı KVKK
 * rejimi (CLAUDE.md §4) düşünülünce ayrıca riskliydi.
 *
 * ## Desen
 *
 * [tr.org.yesilay.yesiliz.domain.bilgi.BilgiBankasi] ile birebir aynı:
 * `Context` almıyor (çağıran metni veriyor), `@Serializable` kullanmıyor
 * (`kotlinx-serialization` derleyici eklentisi `:app`'te uygulanmıyor) ve
 * içerik çevrimdışı çalışıyor. Sözleşmeler nadiren değişiyor; sunucudan
 * çekmek gereksiz bir ağ bağımlılığı olurdu.
 *
 * [govde] paragraf listesi. Tek vurgu biçimi `**kalın**` ve o da ekran
 * tarafında işleniyor — markdown çözümleyicisi yok.
 */
data class Sozlesme(
    val id: String,
    val baslik: String,
    val guncelleme: String,
    /**
     * Metnin hukuki incelemeden geçmediğini söyleyen not.
     *
     * Boşsa gösterilmiyor. Açık kaynak lisansları listesi gibi hukuki
     * beyan içermeyen metinlerde uyarıya gerek yok.
     */
    val uyari: String,
    val govde: List<String>,
)

/** Yardım ekranındaki açılır soru. */
data class SssMaddesi(
    val soru: String,
    val cevap: List<String>,
)

object Sozlesmeler {

    /** Kayıt ekranındaki iki bağlantının hedefleri. */
    const val KULLANIM_KOSULLARI = "kullanim-kosullari"
    const val GIZLILIK_POLITIKASI = "gizlilik-politikasi"
    const val ACIK_KAYNAK = "acik-kaynak"

    const val VARLIK_ADI = "sozlesmeler.json"

    private val json = Json { ignoreUnknownKeys = true }

    fun coz(icerik: String): List<Sozlesme> {
        val kok = json.parseToJsonElement(icerik).jsonObject
        val liste = kok["sozlesmeler"]?.jsonArray
            ?: error("$VARLIK_ADI içinde 'sozlesmeler' yok")

        return liste.map { eleman ->
            val nesne = eleman.jsonObject
            Sozlesme(
                id = nesne.metin("id"),
                baslik = nesne.metin("baslik"),
                guncelleme = nesne.metin("guncelleme"),
                uyari = nesne["uyari"]?.jsonPrimitive?.content.orEmpty(),
                govde = nesne.paragraflar("govde"),
            )
        }
    }

    fun sss(icerik: String): List<SssMaddesi> {
        val kok = json.parseToJsonElement(icerik).jsonObject
        val liste = kok["sss"]?.jsonArray ?: error("$VARLIK_ADI içinde 'sss' yok")

        return liste.map { eleman ->
            val nesne = eleman.jsonObject
            SssMaddesi(
                soru = nesne.metin("soru"),
                cevap = nesne.paragraflar("cevap"),
            )
        }
    }

    private fun JsonObject.metin(anahtar: String): String =
        this[anahtar]?.jsonPrimitive?.content
            ?: error("$VARLIK_ADI: '$anahtar' alanı eksik")

    private fun JsonObject.paragraflar(anahtar: String): List<String> =
        this[anahtar]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
}
