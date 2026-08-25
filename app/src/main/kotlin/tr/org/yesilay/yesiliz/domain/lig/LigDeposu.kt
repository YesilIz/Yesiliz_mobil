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
    suspend fun ligeKatil(): KatilimSonuc

    /** Başka bir kullanıcının davet kodunu kabul eder; debug örneklerinde isim alabilir. */
    suspend fun davetGonder(kod: String)

    /**
     * Katılım denemesinin sonucu.
     *
     * `ligeKatil()` eskiden `Unit` dönüyordu ve `SunucuLigDeposu` hatayı
     * sessizce yutuyordu: kullanıcı "Lige katıl" butonuna basıyor, hiçbir
     * şey olmuyor, hiçbir mesaj çıkmıyordu. Katılamayan kullanıcı da boş
     * lig ekranında kalıyordu — "lig görünmüyor" şikâyetinin ikinci ayağı
     * buydu (CLAUDE.md §12 K37: yapılmayan iş yapıldı gösterilmez).
     */
    enum class KatilimSonuc {
        BASARILI,

        /** Sunucu ayakta ama kimlik yok — kullanıcı giriş yapmalı. */
        OTURUM_YOK,

        /** Ağ/sunucu hatası — sonra tekrar denenebilir. */
        ULASILAMADI,
    }
}

data class LigOzeti(
    val ligAdi: String,
    val kalanGun: Int,
    val yukselen: Int,
    val dusen: Int,
    val siralama: List<LigSatiri>,
    /** Kullanıcı bir lige katıldı mı — false ise boş durum gösteriliyor. */
    val katildiMi: Boolean,
    /**
     * Kullanıcı başkalarının sıralamasında görünüyor mu.
     *
     * `false` iken ekran bunu **söylüyor**. Sessizce gizlenmek, tercihin
     * çalışıp çalışmadığını belirsiz bırakırdı — zaten eski hâlinde tercih
     * hiçbir şey yapmıyordu (`debug.md` Y-5).
     */
    val gorunuyorum: Boolean = true,
    /**
     * Bu veri sunucudan değil **yerel yedekten** geliyor.
     *
     * `Yedekli*Deposu` ağ hatasında sessizce sahte veriye düşüyordu ve
     * ekranda bunu söyleyen hiçbir işaret yoktu; gerçek telefonda sunucuya
     * hiç ulaşılamadığı için bu istisna değil varsayılan durumdu
     * (`debug.md` O-14). Bilinçli bir tasarım kararı (CLAUDE.md §12 K18)
     * ama görünmez olması değil.
     */
    val cevrimdisi: Boolean = false,
    /**
     * Sunucu **ulaşılabilir ama kimlik yok** — `GET /lig` 401 döndü.
     *
     * [cevrimdisi] ile aynı kefeye konamaz. Lig uçları zorunlu jeton
     * istiyor; misafir kullanıcı, hiç giriş yapmamış kullanıcı ve yenileme
     * jetonu da düşmüş kullanıcı 401 alıyor. Bu üç durumda ekran "Lig şu
     * anda kullanılamıyor" diyordu — sunucu ayaktayken bile. Kullanıcının
     * yapması gereken şey belliydi (giriş yap) ama ekran onu söylemiyordu:
     * bir telefonda lig görünürken diğerinde görünmemesinin sebebi buydu.
     */
    val oturumYok: Boolean = false,
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
