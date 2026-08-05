package tr.org.yesilay.yesiliz.data.uzak

import kotlinx.serialization.Serializable

/**
 * Ağ modelleri — `backend/app/semalar.py` ile **birebir** aynı alan adları.
 *
 * `@SerialName` haritalaması bilinçli olarak yok: iki taraf aynı yazımı
 * kullanınca alan eklerken tek yerde unutma riski kalmıyor. Sunucu tarafı
 * da bu yüzden `snake_case` yerine Kotlin'in yazımını izliyor.
 *
 * **Ham kullanım olayı burada YOK.** Uygulama adı, oturum başlangıç/bitişi
 * hiçbir DTO'da geçmiyor — sunucuya yalnızca günlük özet ve skor gidiyor
 * (CLAUDE.md §4).
 */

// --- hesap ---

@Serializable
data class KayitIstek(val ad: String, val eposta: String, val sifre: String)

@Serializable
data class GirisIstek(val eposta: String, val sifre: String)

@Serializable
data class YenileIstek(val yenileme: String)

@Serializable
data class SifreSifirlaIstek(val eposta: String)

@Serializable
data class JetonlarDto(
    val erisim: String,
    val yenileme: String,
    val ad: String,
    val eposta: String,
)

// --- özet senkronu ---

@Serializable
data class GunlukOzetDto(
    val tarih: String,
    val socialDk: Double = 0.0,
    val entertainmentDk: Double = 0.0,
    val gamesDk: Double = 0.0,
    val communicationDk: Double = 0.0,
    val productivityDk: Double = 0.0,
    val educationDk: Double = 0.0,
    val healthDk: Double = 0.0,
    val otherDk: Double = 0.0,
    val gece: Double = 0.0,
    val oturum: Double = 0.0,
    val acilis: Double = 0.0,
    val sabah: Double = 0.0,
    val uykuOncesi: Double = 0.0,
    val yerDegis: Double = 0.0,
    val adim: Double = 0.0,
    val sporDk: Double = 0.0,
    val S: Double = 0.0,
)

@Serializable
data class RiskSonucDto(
    val tarih: String,
    val S: Double,
    val R: Double,
    val gosterilenBant: String,
    val trend: String,
)

@Serializable
data class SenkronIstek(
    val ozetler: List<GunlukOzetDto> = emptyList(),
    val riskler: List<RiskSonucDto> = emptyList(),
    val haftalikPuan: Int = 0,
    /**
     * "Ligde görün" tercihi.
     *
     * Ayrı bir uç yerine senkrona bindirildi: tercih zaten her gece koşan
     * bu çağrıyla taşınabiliyor (`debug.md` Y-5).
     */
    val ligdeGorunur: Boolean? = null,
)

@Serializable
data class SenkronYanit(val ozetSayisi: Int, val riskSayisi: Int)

// --- lig ---

@Serializable
data class LigSatiriDto(
    val sira: Int,
    val isim: String,
    val puan: Int,
    val benMiyim: Boolean = false,
    val arkadasMi: Boolean = false,
)

@Serializable
data class LigYanit(
    val ligAdi: String,
    val kalanGun: Int,
    val yukselen: Int,
    val dusen: Int,
    val siralama: List<LigSatiriDto>,
    val katildiMi: Boolean,
    /** Kullanıcı başkalarının sıralamasında görünüyor mu. */
    val gorunuyorMuyum: Boolean = true,
)

@Serializable
data class ArkadasDto(val isim: String, val davetBekliyorMu: Boolean)

@Serializable
data class DavetIstek(val kod: String)

@Serializable
data class KodYanit(val kod: String)

// --- ödül ---

@Serializable
data class OdulSecimIstek(val kademeId: String, val secenekId: String)

@Serializable
data class OdulSecimiDto(val kademeId: String, val secenekId: String)

// --- ebeveyn ---

@Serializable
data class EslestirmeKoduIstek(val rol: String)

@Serializable
data class EslestirIstek(val kod: String, val rol: String)

@Serializable
data class EbeveynOzetDto(
    val cocukAdi: String,
    val gunlukDakikalar: List<Int>,
    val ortalamaDk: Int,
    val bant: String? = null,
    val trend: String? = null,
    val sonSinyalSaatOnce: Int? = null,
)
