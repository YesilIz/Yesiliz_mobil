package tr.org.yesilay.yesiliz.data.uzak

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Sunucu arayüzü — `backend/app/uclar/` ile birebir.
 *
 * Yollar İngilizce/kısa, Kotlin adları Türkçe (CLAUDE.md §7: "API/model
 * isimleri İngilizce" kuralı HTTP yolları için geçerli; sınıf ve fonksiyon
 * adları projenin geri kalanıyla tutarlı kalıyor).
 */
interface YesilizApi {

    @GET("saglik")
    suspend fun saglik(): Map<String, String>

    // --- hesap ---

    @POST("auth/kayit")
    suspend fun kayit(@Body istek: KayitIstek): JetonlarDto

    @POST("auth/giris")
    suspend fun giris(@Body istek: GirisIstek): JetonlarDto

    @POST("auth/yenile")
    suspend fun yenile(@Body istek: YenileIstek): JetonlarDto

    @POST("auth/cikis")
    suspend fun cikis()

    @POST("auth/sifre-sifirla")
    suspend fun sifreSifirla(@Body istek: SifreSifirlaIstek): Map<String, String>

    /** Ad, profil simgesi ve (mevcut şifre doğrulanarak) şifre değişimi. */
    @PATCH("auth/profil")
    suspend fun profilGuncelle(@Body istek: ProfilIstek): ProfilDto

    /**
     * Hesabı ve sunucudaki tüm satırlarını kalıcı siler.
     *
     * Cihaz "Hesabımı kalıcı sil" akışında çağırıyor; bu uç olmadan yerel
     * veri silinip sunucudaki kopya yerinde kalırdı.
     */
    @DELETE("auth/hesap")
    suspend fun hesabiSil(): Map<String, String>

    // --- senkron ---

    @POST("ozet")
    suspend fun ozetGonder(@Body istek: SenkronIstek): SenkronYanit

    @GET("ozet")
    suspend fun ozetleriGetir(
        @Query("bas") bas: String,
        @Query("bit") bit: String,
    ): List<GunlukOzetDto>

    @POST("heartbeat")
    suspend fun heartbeat(): Map<String, String>

    // --- lig ---

    @GET("lig")
    suspend fun lig(): LigYanit

    @POST("lig/katil")
    suspend fun ligeKatil(@Body istek: LigKatilIstek): LigYanit

    @POST("lig/gorunurluk")
    suspend fun ligGorunurlugu(@Body istek: LigGorunurlukIstek): Map<String, String>

    @GET("arkadas")
    suspend fun arkadaslar(): List<ArkadasDto>

    @GET("arkadas/kod")
    suspend fun davetKodu(): KodYanit

    @POST("arkadas/davet")
    suspend fun davetGonder(@Body istek: DavetIstek): List<ArkadasDto>

    // --- ödül ---

    @POST("odul/sec")
    suspend fun odulSec(@Body istek: OdulSecimIstek): OdulSecimiDto

    @GET("odul/secimlerim")
    suspend fun odulSecimlerim(): List<OdulSecimiDto>

    // --- ebeveyn ---

    @POST("ebeveyn/kod")
    suspend fun eslestirmeKodu(@Body istek: EslestirmeKoduIstek): KodYanit

    @POST("ebeveyn/eslestir")
    suspend fun eslestir(@Body istek: EslestirIstek): Map<String, String>

    @DELETE("ebeveyn/eslesme")
    suspend fun eslesmeyiKaldir(): Map<String, String>

    @GET("ebeveyn/ozet")
    suspend fun ebeveynOzeti(): EbeveynOzetDto

    /** 18 altı çocuğun YEDAM başvurusuna eşleşmiş ebeveynin onayı. */
    @POST("ebeveyn/yedam-onay")
    suspend fun yedamOnayi(@Body istek: EbeveynYedamOnayIstek): Map<String, String>

    // --- YEDAM ---

    @POST("yedam/basvuru")
    suspend fun yedamBasvur(@Body istek: YedamBasvuruIstek): YedamBasvuruYanit

    @GET("yedam/basvurum")
    suspend fun yedamDurumu(): YedamDurumYanit

    /** Erişim **aynı saniyede** kesilir, rıza kapsamındaki veri silinir. */
    @DELETE("yedam/riza")
    suspend fun yedamRizasiniGeriCek(): Map<String, String>

    /** Şeffaflık kaydı — rıza geri çekilse de okunabiliyor. */
    @GET("yedam/erisim-kaydim")
    suspend fun yedamErisimKaydim(): List<ErisimKaydiDto>

    /** Günlük gönderim. `/ozet`'ten AYRI: iki rıza bağımsız (U1). */
    @POST("yedam/surukleyici")
    suspend fun yedamSurukleyici(@Body istek: SurukleyiciIstek): Map<String, Int>

    /** Rıza anındaki tek seferlik geçmiş yüklemesi. */
    @POST("yedam/gecmis")
    suspend fun yedamGecmis(@Body istek: YedamGecmisIstek): Map<String, Int>
}
