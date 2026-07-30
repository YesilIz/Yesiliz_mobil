package tr.org.yesilay.yesiliz.domain.lig

import kotlinx.coroutines.flow.Flow

/**
 * Lig sıralaması ve arkadaş defteri — **arayüz**.
 *
 * CLAUDE.md §12 K7'de vaat edilen mock repository deseni: MVP'de
 * [tr.org.yesilay.yesiliz.data.depo.YerelSahteLigDeposu] uyguluyor, Faz 2'de
 * `RetrofitLigDeposu` aynı arayüzü backend'e bağlıyor. ViewModel ve
 * Composable **hiç değişmiyor** — yalnızca DI modülü.
 *
 * Bu arayüz daha önce yalnızca dokümanda vardı; sıralama `LigViewModel`
 * içinde gömülü sabitti. Arkadaş kavramı gelince arayüzü gerçekten yazmak
 * gerekti.
 */
interface LigDeposu {

    /** Kullanıcının ligdeki durumu. Arkadaş yoksa [LigOzeti.katildiMi] false. */
    fun ligAkisi(): Flow<LigOzeti>

    fun arkadaslarAkisi(): Flow<List<Arkadas>>

    /** Cihaz başına bir kez üretilip saklanan davet kodu. */
    suspend fun davetKodu(): String

    /** Boş durumdaki "Lige katıl" — mevcut sıralamaya dahil eder. */
    suspend fun ligeKatil()

    /** Önerilen kişilerden birine davet gönderir. */
    suspend fun davetGonder(isim: String)

    /** Henüz eklenmemiş öneri listesi. */
    suspend fun onerilenler(): List<OnerilenKisi>
}

data class LigOzeti(
    val ligAdi: String,
    val kalanGun: Int,
    val yukselen: Int,
    val dusen: Int,
    val siralama: List<LigSatiri>,
    /** Kullanıcı bir lige katıldı mı — false ise boş durum gösteriliyor. */
    val katildiMi: Boolean,
)

data class LigSatiri(
    val sira: Int,
    val isim: String,
    val puan: Int,
    val benMiyim: Boolean = false,
    /** Arkadaş listesinden gelen satırlar ayırt edilebilsin. */
    val arkadasMi: Boolean = false,
)

data class Arkadas(
    val isim: String,
    val davetBekliyorMu: Boolean,
)

data class OnerilenKisi(
    val isim: String,
    /** "3 ortak arkadaş" / "Rehberinden" gibi alt metin. */
    val altMetin: String,
    val davetGonderildiMi: Boolean = false,
)
