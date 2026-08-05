package tr.org.yesilay.yesiliz.data.uzak

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
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

    @POST("auth/sifre-sifirla")
    suspend fun sifreSifirla(@Body istek: SifreSifirlaIstek): Map<String, String>

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
    suspend fun ligeKatil(): LigYanit

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

    @GET("ebeveyn/ozet")
    suspend fun ebeveynOzeti(): EbeveynOzetDto
}
