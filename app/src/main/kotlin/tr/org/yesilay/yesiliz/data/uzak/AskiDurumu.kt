package tr.org.yesilay.yesiliz.data.uzak

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Askıya alınmış hesabın tespiti — **tek yerde**.
 *
 * Sunucu askıdaki hesaba `403` + `{"detail": {"hata": "hesap_askida",
 * "sebep": "..."}}` dönüyor (`backend/app/guvenlik.py`). Bu yanıtı her
 * deponun ayrı ayrı tanıması gerekseydi biri unutulurdu ve orada 403
 * sessizce "sunucuya ulaşılamıyor"a indirgenirdi: kullanıcı hesabının
 * askıda olduğunu **hiç öğrenemez**, uygulamayı bozuk sanardı
 * (CLAUDE.md §4 "yapılmayan işi yapıldı gösterme", §12 K18/K42 ailesi).
 *
 * Bu yüzden tespit bir OkHttp interceptor'ında: hangi uç çağrılırsa
 * çağrılsın, yanıt buradan geçiyor. Depolar değişmiyor — onlar için 403
 * hâlâ bir hata, ama kullanıcı artık sebebini görüyor.
 *
 * `AskiDurumu` süreç ömürlü: askı bir oturum durumu, kalıcı bir tercih
 * değil. Askı kalkınca ilk başarılı yanıt onu temizliyor, yani kullanıcı
 * uygulamayı yeniden başlatmak zorunda kalmıyor.
 */
@Singleton
class AskiDurumu @Inject constructor() {

    /**
     * Sunucu bakımda mı? (`docs/panel-plani.md §5.7`)
     *
     * Askıyla aynı sınıfta bir durum ama **farklı bir şey söylüyor**:
     * askı hesaba özel ve kalıcı, bakım herkese ve geçici. İkisini aynı
     * ekrana indirgemek, kullanıcıya hesabının kapandığını sandırırdı.
     *
     * Bakım da **çevrimdışıyla karıştırılmamalı**: sunucu cevap veriyor
     * ve ne olduğunu söylüyor. Sessizce "ulaşılamıyor" demek, geçici bir
     * durumu kalıcı bir arıza gibi göstermek olurdu (§4).
     */
    private val _bakim = MutableStateFlow<String?>(null)
    val bakim: StateFlow<String?> = _bakim.asStateFlow()

    fun bakimBildir(mesaj: String) {
        if (_bakim.value != mesaj) {
            Log.w(ETIKET, "Sunucu bakımda")
            _bakim.value = mesaj
        }
    }

    fun bakimBitti() {
        if (_bakim.value != null) {
            Log.i(ETIKET, "Bakım bitmiş görünüyor")
            _bakim.value = null
        }
    }

    private val _hal = MutableStateFlow<Hal?>(null)

    /** `null` ise hesap askıda değil (ya da henüz bilinmiyor). */
    val hal: StateFlow<Hal?> = _hal.asStateFlow()

    data class Hal(val sebep: String)

    fun bildir(sebep: String) {
        val yeni = Hal(sebep)
        if (_hal.value != yeni) {
            Log.w(ETIKET, "Hesap askıya alınmış")
            _hal.value = yeni
        }
    }

    /**
     * Askının kalktığını bildirir.
     *
     * Yalnızca **başarılı** bir yanıttan sonra çağrılıyor; ağ hatası askıyı
     * temizlemiyor. Aksi hâlde uçağa binen kullanıcının askısı kalkmış
     * görünürdü.
     */
    fun temizle() {
        if (_hal.value != null) {
            Log.i(ETIKET, "Askı kalkmış görünüyor")
            _hal.value = null
        }
    }

    private companion object {
        const val ETIKET = "AskiDurumu"
    }
}

/** 403 gövdesindeki ayırt edici kod — sunucudaki `ASKIDA_KODU` ile aynı. */
private const val ASKIDA_KODU = "hesap_askida"

/** 503 gövdesindeki bakım kodu — sunucudaki `bakim.BAKIM_KODU` ile aynı. */
private const val BAKIM_KODU = "bakim"

/**
 * 503 gövdesindeki bakım mesajı; bakım değilse `null`.
 *
 * Sunucu bakım gövdesini `{"hata": "bakim", "mesaj": "..."}` biçiminde
 * yolluyor — askıdan farklı olarak `detail` sarmalayıcısı **yok**, çünkü
 * bunu bir middleware üretiyor, `HTTPException` değil.
 */
internal fun bakimMesajiCoz(govde: String, onHata: (Throwable) -> Unit = {}): String? =
    runCatching {
        val kok = Json.parseToJsonElement(govde).jsonObject
        if (kok["hata"]?.jsonPrimitive?.content != BAKIM_KODU) return@runCatching null
        kok["mesaj"]?.jsonPrimitive?.content.orEmpty()
    }.getOrElse {
        onHata(it)
        null
    }

/**
 * Gövdeyi çözer: askı ise sebep (boş olabilir), değilse `null`.
 *
 * **Gerçekten saf** — `android.util.Log`'a bile dokunmuyor, böylece
 * emülatörsüz koşuyor (`OturumCikar`/`Tetikleyici` ile aynı desen).
 * Yutulan istisnanın izi kaybolmasın diye (CLAUDE.md §9
 * `runCatching{}.getOrDefault` tuzağı) hata çağırana bildiriliyor;
 * loglamak Android tarafının işi.
 *
 * `null` ile boş dize **ayrı**: `null` "bu bir askı yanıtı değil", boş
 * dize "askı ama sebep yazılmamış". İkisini birleştirmek, sebepsiz askıyı
 * hiç göstermemek olurdu.
 */
internal fun askiSebebiCoz(govde: String, onHata: (Throwable) -> Unit = {}): String? =
    runCatching {
        val kok = Json.parseToJsonElement(govde).jsonObject
        val ayrinti = kok["detail"] as? JsonObject ?: return@runCatching null
        if (ayrinti["hata"]?.jsonPrimitive?.content != ASKIDA_KODU) return@runCatching null
        ayrinti["sebep"]?.jsonPrimitive?.content.orEmpty()
    }.getOrElse {
        onHata(it)
        null
    }

/**
 * Yanıtları dinleyip [AskiDurumu]'nu güncelleyen interceptor.
 *
 * Gövde `peekBody` ile okunuyor: `body.string()` akışı tüketir ve
 * Retrofit'in eline boş yanıt geçerdi.
 */
internal fun askiInterceptor(durum: AskiDurumu) = Interceptor { zincir ->
    val yanit: Response = zincir.proceed(zincir.request())
    when {
        yanit.code == KOD_YASAK -> {
            val govde = runCatching { yanit.peekBody(GOVDE_SINIRI).string() }
                .getOrElse { Log.w(ETIKET_AG, "403 gövdesi okunamadı", it); null }
            govde
                ?.let { askiSebebiCoz(it) { hata -> Log.d(ETIKET_AG, "403 askı değil", hata) } }
                ?.let(durum::bildir)
        }
        yanit.code == KOD_BAKIM -> {
            val govde = runCatching { yanit.peekBody(GOVDE_SINIRI).string() }
                .getOrElse { Log.w(ETIKET_AG, "503 gövdesi okunamadı", it); null }
            govde
                ?.let { bakimMesajiCoz(it) { hata -> Log.d(ETIKET_AG, "503 bakım değil", hata) } }
                ?.let(durum::bakimBildir)
        }
        // Sunucu iş görüyor ve hesap çalışıyor: askı ve bakım bitmiş.
        yanit.isSuccessful -> {
            durum.temizle()
            durum.bakimBitti()
        }
    }
    yanit
}

private const val ETIKET_AG = "AskiDurumu"

private const val KOD_YASAK = 403

private const val KOD_BAKIM = 503

/** Hata gövdeleri küçük; 8 KB'den fazlasını belleğe almanın anlamı yok. */
private const val GOVDE_SINIRI = 8L * 1024
