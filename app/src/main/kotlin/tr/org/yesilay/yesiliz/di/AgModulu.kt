package tr.org.yesilay.yesiliz.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import tr.org.yesilay.yesiliz.BuildConfig
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.JetonAuthenticator
import tr.org.yesilay.yesiliz.data.uzak.JetonDeposu
import tr.org.yesilay.yesiliz.data.uzak.JetonKaynak
import tr.org.yesilay.yesiliz.data.uzak.JetonYenileyici
import tr.org.yesilay.yesiliz.data.uzak.RetrofitJetonYenileyici
import tr.org.yesilay.yesiliz.data.uzak.VarsayilanJetonKaynak
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Ağ katmanı — Faz 2 backend'i (`backend/`).
 *
 * **Sunucu adresi çalışma zamanında okunuyor** (`Tercihler.sunucuTabani`),
 * varsayılanı build türünden geliyor (`BuildConfig.API_TABANI`): release'de
 * gerçek HTTPS alan adı, debug'da emülatörün `10.0.2.2`'si. Adres ayrıca
 * Profil › Geliştirici'den değiştirilebiliyor — gerçek cihazda geliştirme
 * makinesinin LAN adresine bağlanabilmek için (`debug.md` O-14).
 *
 * Zaman aşımları **kısa** (5 sn): backend kapalıyken kullanıcı 30 saniye
 * boş ekrana bakmasın, `Yedekli*Deposu` hızlıca yerel veriye düşsün.
 */
@Module
@InstallIn(SingletonComponent::class)
object AgModulu {

    /**
     * Retrofit'in derleme anında bir taban adrese ihtiyacı var; gerçek
     * adres [tabanDegistirici] tarafından her istekte yazılıyor.
     *
     * `Tercihler.VARSAYILAN_SUNUCU` artık `BuildConfig.API_TABANI`'den
     * geldiği için burası da `const` olamıyor — Kotlin `const val`
     * yalnızca derleme zamanı sabitlerini kabul ediyor.
     */
    private val YER_TUTUCU_TABAN = Tercihler.VARSAYILAN_SUNUCU

    /**
     * Zaman aşımı.
     *
     * 5 saniyeydi ve gerçek şebekede kısaydı: yavaş bir bağlantıda
     * çalışan sunucu bile ulaşılamaz sayılıyordu. 10 saniye hâlâ
     * `Yedekli*Deposu`'nun makul sürede yerele düşmesine izin veriyor.
     */
    private const val ZAMAN_ASIMI_SN = 10L

    @Provides
    @Singleton
    fun json(): Json = Json {
        // Sunucu yeni alan eklerse eski istemci çökmesin.
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * `JetonKaynak` ağ katmanının iç detayı, bir depo değil — bu yüzden
     * bağlaması `DepoModulu`'nda değil burada.
     */
    @Provides
    @Singleton
    fun jetonKaynak(varsayilan: VarsayilanJetonKaynak): JetonKaynak = varsayilan

    /**
     * Yenileme istemcisi ana istemciden **ayrı** kuruluyor — gerekçesi
     * [JetonYenileyici] kdoc'unda (Dagger döngüsü + sonsuz özyineleme).
     */
    @Provides
    @Singleton
    fun jetonYenileyici(json: Json, tercihler: Tercihler): JetonYenileyici =
        RetrofitJetonYenileyici(
            taban = YER_TUTUCU_TABAN,
            json = json,
            zamanAsimiSn = ZAMAN_ASIMI_SN,
            tabanDegistirici = tabanDegistirici(tercihler),
        )

    @Provides
    @Singleton
    fun jetonAuthenticator(
        yenileyici: JetonYenileyici,
        kaynak: JetonKaynak,
    ): Authenticator = JetonAuthenticator(yenileyici, kaynak)

    /**
     * Her isteğin adresini `Tercihler`'den okunan tabana çevirir.
     *
     * `runBlocking` burada [JetonDeposu.erisimSenkron] ile aynı gerekçeyle
     * kaçınılmaz: OkHttp interceptor'ı askıya alınabilir değil. DataStore
     * okuması ilk seferden sonra bellekten geliyor.
     */
    private fun tabanDegistirici(tercihler: Tercihler) = Interceptor { zincir ->
        val istek = zincir.request()
        val taban = runBlocking { tercihler.sunucuTabani.first() }.toHttpUrlOrNull()
        if (taban == null) {
            zincir.proceed(istek)
        } else {
            val yeniUrl = istek.url.newBuilder()
                .scheme(taban.scheme)
                .host(taban.host)
                .port(taban.port)
                .build()
            zincir.proceed(istek.newBuilder().url(yeniUrl).build())
        }
    }

    @Provides
    @Singleton
    fun istemci(
        jetonDeposu: JetonDeposu,
        authenticator: Authenticator,
        tercihler: Tercihler,
    ): OkHttpClient {
        val kimlik = Interceptor { zincir ->
            val jeton = jetonDeposu.erisimSenkron()
            val istek = if (jeton == null) {
                zincir.request()
            } else {
                zincir.request().newBuilder()
                    .header("Authorization", "Bearer $jeton")
                    .build()
            }
            zincir.proceed(istek)
        }

        val olusturucu = OkHttpClient.Builder()
            // Adres değiştirici kimlikten ÖNCE: yeniden yazılan URL'ye
            // Authorization başlığı eklensin.
            .addInterceptor(tabanDegistirici(tercihler))
            .addInterceptor(kimlik)
            // 401 gelince erişim jetonunu bir kez yenileyip isteği tekrarlar.
            // Olmadığında 30 dakikalık jeton süresi dolar dolmaz her kimlikli
            // çağrı sessizce yedek/sahte veriye düşüyordu.
            .authenticator(authenticator)
            .connectTimeout(ZAMAN_ASIMI_SN, TimeUnit.SECONDS)
            .readTimeout(ZAMAN_ASIMI_SN, TimeUnit.SECONDS)

        // Debug günlüğü yalnızca yöntem/URL/yanıt kodu yazar. BODY düzeyi
        // giriş-kayıt şifrelerini ve yanıttaki erişim/yenileme jetonlarını;
        // HEADERS düzeyi de Authorization başlığını logcat'e sızdırır.
        // Hata türünü (401 mi, 500 mü, zaman aşımı mı) BASIC düzeyiyle de
        // ayırt edebiliyoruz. Release'de interceptor hiç eklenmiyor.
        if (BuildConfig.DEBUG) {
            olusturucu.addInterceptor(guvenliHttpGunlugu())
        }

        return olusturucu.build()
    }

    @Provides
    @Singleton
    fun api(istemci: OkHttpClient, json: Json): YesilizApi = Retrofit.Builder()
        .baseUrl(YER_TUTUCU_TABAN)
        .client(istemci)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(YesilizApi::class.java)
}

/**
 * Debug ağ günlüğünün güvenlik sınırı.
 *
 * Başlıklar BASIC düzeyinde yazılmıyor; yine de ileride seviye değiştirilse
 * bile iki yeniden kullanılabilir kimlik bilgisini redaksiyon listesinde
 * tutuyoruz. Düz metin JSON şifresi alan bazlı güvenle maskelenemediği için
 * gövde günlüğüne hiçbir koşulda geçilmiyor.
 */
internal fun guvenliHttpGunlugu(): HttpLoggingInterceptor =
    HttpLoggingInterceptor().apply {
        redactHeader("Authorization")
        redactHeader("Cookie")
        level = HttpLoggingInterceptor.Level.BASIC
    }
