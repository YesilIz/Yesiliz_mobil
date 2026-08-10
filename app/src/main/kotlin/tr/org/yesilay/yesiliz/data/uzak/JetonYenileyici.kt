package tr.org.yesilay.yesiliz.data.uzak

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Yenileme denemesinin sonucu.
 *
 * Ağ hatası ile sunucunun reddi **ayrı** — CLAUDE.md §12 K18'in ayrımı burada
 * da geçerli. Geçici bir bağlantı kopukluğu yüzünden kullanıcıyı oturumdan
 * atmak, `Yedekli*Deposu`'nun tam olarak önlemek için yazıldığı hata.
 */
sealed interface YenilemeSonuc {

    data class Basarili(val jetonlar: JetonlarDto) : YenilemeSonuc

    /** Sunucu reddetti: yenileme jetonu geçersiz ya da 30 günü dolmuş. */
    data object Reddedildi : YenilemeSonuc

    /** Sunucuya ulaşılamadı — oturum korunur, sonraki istek yeniden dener. */
    data object AgHatasi : YenilemeSonuc
}

/**
 * Erişim jetonunu tazeleyen taraf.
 *
 * Arayüz olması [JetonAuthenticator]'ın emülatörsüz test edilebilmesi için —
 * `LigDeposu` / `HesapDeposu` ile aynı desen.
 */
interface JetonYenileyici {
    fun yenile(yenilemeJetonu: String): YenilemeSonuc
}

/**
 * `POST auth/yenile` — **kendi, yalıtılmış istemcisiyle**.
 *
 * ## Niçin ayrı bir Retrofit
 *
 * İki sorunu aynı anda çözüyor:
 *
 * 1. **Dagger döngüsü.** `YesilizApi` → `OkHttpClient` → `Authenticator` →
 *    `YesilizApi` bir halka kurardı. Yenileme çağrısı ana istemciden
 *    geçmeyince halka kopuyor.
 * 2. **Sonsuz özyineleme.** Yenileme isteğinin kendisi 401 alırsa, ana
 *    istemcinin authenticator'ı yine yenilemeye kalkardı. Bu istemcide ne
 *    kimlik interceptor'ı ne authenticator var — 401 olduğu gibi dönüyor.
 *
 * Bağlantı havuzu ana istemciyle paylaşılmıyor; yenileme 30 dakikada bir
 * yapılan tek bir çağrı olduğu için bunun maliyeti ölçülemez düzeyde.
 */
class RetrofitJetonYenileyici(
    taban: String,
    json: Json,
    zamanAsimiSn: Long,
    /**
     * Sunucu adresini çalışma zamanında yazan interceptor.
     *
     * Yenileme istemcisi ana istemciden ayrı kurulduğu için (Dagger
     * döngüsü) aynı adres değiştiriciyi ayrıca alması gerekiyor; yoksa
     * kullanıcı sunucu adresini değiştirdiğinde jeton yenileme eski
     * adrese gitmeye devam ederdi (`debug.md` O-14).
     */
    tabanDegistirici: Interceptor? = null,
) : JetonYenileyici {

    private interface YenilemeApi {
        @POST("auth/yenile")
        suspend fun yenile(@Body istek: YenileIstek): JetonlarDto
    }

    private val api: YenilemeApi = Retrofit.Builder()
        .baseUrl(taban)
        .client(
            OkHttpClient.Builder()
                .apply { tabanDegistirici?.let { addInterceptor(it) } }
                .connectTimeout(zamanAsimiSn, TimeUnit.SECONDS)
                .readTimeout(zamanAsimiSn, TimeUnit.SECONDS)
                .build(),
        )
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(YenilemeApi::class.java)

    override fun yenile(yenilemeJetonu: String): YenilemeSonuc = runBlocking {
        try {
            YenilemeSonuc.Basarili(api.yenile(YenileIstek(yenilemeJetonu)))
        } catch (_: IOException) {
            YenilemeSonuc.AgHatasi
        } catch (hata: HttpException) {
            yenilemeHttpSonucu(hata.code())
        }
    }
}

/**
 * Yalnızca istemcinin yenileme jetonunun geçersiz olduğunu kanıtlayan
 * yanıtlar oturumu kapatabilir. 429 ve 5xx gibi geçici sunucu yanıtlarında
 * kimlik bilgileri korunur; sonraki istek yeniden dener.
 */
internal fun yenilemeHttpSonucu(kod: Int): YenilemeSonuc = when (kod) {
    400, 401, 403, 422 -> YenilemeSonuc.Reddedildi
    else -> YenilemeSonuc.AgHatasi
}
