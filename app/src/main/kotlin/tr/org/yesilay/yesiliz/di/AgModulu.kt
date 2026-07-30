package tr.org.yesilay.yesiliz.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import tr.org.yesilay.yesiliz.data.uzak.JetonDeposu
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Ağ katmanı — Faz 2 backend'i (`backend/`).
 *
 * **Sunucu adresi derleme türüne göre.** Emülatörde `localhost` cihazın
 * kendisi demek; makinedeki sunucuya `10.0.2.2` ile ulaşılıyor. Release'de
 * gerçek alan adı gelecek — şimdilik aynı yer tutucu, çünkü henüz bir
 * dağıtım yok ve uydurma bir adres yazmak yanıltıcı olurdu.
 *
 * Zaman aşımları **kısa** (5 sn): backend kapalıyken kullanıcı 30 saniye
 * boş ekrana bakmasın, `Yedekli*Deposu` hızlıca yerel veriye düşsün.
 */
@Module
@InstallIn(SingletonComponent::class)
object AgModulu {

    private const val EMULATOR_TABANI = "http://10.0.2.2:8000/"
    private const val ZAMAN_ASIMI_SN = 5L

    @Provides
    @Singleton
    fun json(): Json = Json {
        // Sunucu yeni alan eklerse eski istemci çökmesin.
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun istemci(jetonDeposu: JetonDeposu): OkHttpClient {
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

        return OkHttpClient.Builder()
            .addInterceptor(kimlik)
            .connectTimeout(ZAMAN_ASIMI_SN, TimeUnit.SECONDS)
            .readTimeout(ZAMAN_ASIMI_SN, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Tek adres var çünkü tek ortam var.
     *
     * Bir dağıtım (staging/production) yapıldığında burası derleme türüne
     * göre ayrılacak — `buildConfigField` ile. Şimdiden iki dal yazıp
     * ikisine de aynı adresi koymak, var olmayan bir yapılandırmayı varmış
     * gibi göstermek olurdu.
     */
    @Provides
    @Singleton
    fun api(istemci: OkHttpClient, json: Json): YesilizApi = Retrofit.Builder()
        .baseUrl(EMULATOR_TABANI)
        .client(istemci)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(YesilizApi::class.java)
}
