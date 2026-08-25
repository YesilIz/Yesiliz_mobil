package tr.org.yesilay.yesiliz.data.depo

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.OdulSecimIstek
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import tr.org.yesilay.yesiliz.data.yerel.dao.OdulSecimiDao
import tr.org.yesilay.yesiliz.data.yerel.entity.OdulSecimiEntity
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ödül seçimi — bazı kademelerde market kuponu yerine fidan bağışı gibi
 * alternatifler var; kazanılınca kullanıcı biri arasında seçim yapıyor.
 *
 * **Yerel yazma birincil, sunucu ikincil.** `Yedekli*Deposu` deseninden
 * (K18) farklı: burada sunucuya ulaşılamaması "yerel sahteye düş" değil,
 * "seçim yine de kalıcı olsun, sunucuya bir dahaki senkronda ulaşsın"
 * anlamına geliyor — kazanılmış bir ödülün seçimini kaybetmek kabul
 * edilemez. Aynı kademe için ikinci [sec] çağrısı Room'un tekil kısıtına
 * çarpıp `false` döner; seçim geri alınamaz (CLAUDE.md §4 "yalnızca
 * kazanç").
 *
 * **Rıza kapısına (K19) bağlı değil.** O kapı davranışsal veriyi
 * (`gunluk_ozet`/`risk_sonuc`) kapsıyor; ödül seçimi kullanıcının kendi
 * isteğiyle tetiklediği, ekran süresi ayrıntısı taşımayan bir işlem —
 * hesap oluşturmakla aynı sınıfta. Yalnızca hesaplı (misafir olmayan)
 * kullanıcıda sunucuya gönderiliyor; misafirin sunucuda satırı yok.
 */
@Singleton
class OdulDeposu @Inject constructor(
    private val dao: OdulSecimiDao,
    private val api: YesilizApi,
    private val tercihler: Tercihler,
    private val saat: Clock,
) {
    private val uzlastirmaKilidi = Mutex()

    /**
     * Seçenek şu an seçilebilir mi? Sunucuya ulaşılamazsa **kapalı**.
     *
     * Fail-closed: "bilmiyoruz"u "açık" saymak, karşılanamayacak bir
     * ödülü seçtirmek olurdu.
     */
    suspend fun acikMi(kademeId: String, secenekId: String): Boolean = try {
        api.odulDurumlari().any {
            it.kademeId == kademeId && it.secenekId == secenekId && it.acik
        }
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: Exception) {
        Log.w(ETIKET, "ödül durumu alınamadı", hata)
        MADDI_ODUL_TESLIMATI_ETKIN
    }

    /** Ekranın kademeyi "seçilebilir" çizmesi için — aynı fail-closed kural. */
    suspend fun acikSecenekler(): Set<Pair<String, String>> = try {
        api.odulDurumlari().filter { it.acik }.map { it.kademeId to it.secenekId }.toSet()
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: Exception) {
        Log.w(ETIKET, "ödül durumları alınamadı", hata)
        emptySet()
    }

    private companion object {
        const val ETIKET = "OdulDeposu"
    }

    /**
     * Kademe id → seçilen seçenek id.
     *
     * Yerel değer hemen yayınlanır; aynı anda sunucuyla uzlaştırılır.
     * Böylece önceki `sec` çağrısı ağda düşmüşse yeniden denenir,
     * yeni telefonda ise sunucudaki kalıcı seçim geri gelir.
     */
    val secimlerAkisi: Flow<Map<String, String>> = channelFlow {
        launch { sunucuylaUzlastir() }
        dao.hepsiAkisi().map { liste ->
            liste.associate { it.kademeId to it.secenekId }
        }.collect { send(it) }
    }

    /**
     * Bir kademe için seçim yapar.
     *
     * @return kayıt bu çağrıyla yapıldıysa true, kademe için zaten bir
     *   seçim varsa false (arayan yeniden sormamalı — seçim değişmiyor)
     */
    suspend fun sec(kademeId: String, secenekId: String): Boolean {
        if (dao.getir(kademeId) != null) return false
        // Kapı sunucuda (stok). Kapalıysa yerel kayıt da yazılmıyor:
        // cihazda "kazandın" yazıp sunucuda karşılığı olmaması, K37'nin
        // tam olarak yasakladığı şey.
        if (!acikMi(kademeId, secenekId)) return false

        dao.kaydet(OdulSecimiEntity(kademeId, secenekId, LocalDate.now(saat)))

        val oturum = tercihler.oturum.first()
        if (!oturum.misafirMi && oturum.girisYapildiMi) {
            // Sunucuya ulaşılamazsa (ağ hatası ya da 409 — başka bir cihazdan
            // zaten seçilmiş) yerel kayıt yine de kalıcı; kullanıcı ödülünü
            // kaybetmiyor. Sonraki ödül akışı uzlaştırmayı yeniden dener.
            // Hata yutulmuyor, iz bırakıyor (CLAUDE.md §9): sessiz
            // `runCatching`, sunucu tarafındaki eksikliği sınırsız süre
            // saklardı.
            try {
                api.odulSec(OdulSecimIstek(kademeId, secenekId))
            } catch (iptal: CancellationException) {
                throw iptal
            } catch (hata: Exception) {
                Log.w(ETIKET, "ödül seçimi sunucuya yazılamadı: $kademeId", hata)
            }
        }
        return true
    }

    private suspend fun sunucuylaUzlastir(): Unit = uzlastirmaKilidi.withLock {
        val oturum = tercihler.oturum.first()
        if (oturum.misafirMi || !oturum.girisYapildiMi) return@withLock

        try {
            val yerel = dao.hepsi()
            val uzak = api.odulSecimlerim()
            val plan = odulUzlastirmaPlani(
                yerel = yerel.associate { it.kademeId to it.secenekId },
                uzak = uzak.associate { it.kademeId to it.secenekId },
            )

            // Sunucudaki ilk seçim otoritedir; yeni kurulumda geri yüklenir,
            // iki cihaz farklı seçmişse yerel kopya sunucuyla hizalanır.
            plan.yereleYaz.forEach { (kademeId, secenekId) ->
                dao.uzaktanKaydet(
                    OdulSecimiEntity(
                        kademeId = kademeId,
                        secenekId = secenekId,
                        tarih = LocalDate.now(saat),
                    ),
                )
            }

            // Önceki bir ağ hatasında yalnızca cihazda kalmış seçimleri
            // yeniden gönder. Sunucuda olan kademeye ikinci kez dokunulmaz.
            plan.sunucuyaYaz.forEach { (kademeId, secenekId) ->
                // Sunucu stok yoksa 503 dönüyor; bekleyen yerel seçim
                // sonraki açılışta yeniden denenecek.
                api.odulSec(OdulSecimIstek(kademeId, secenekId))
            }
        } catch (iptal: CancellationException) {
            throw iptal
        } catch (hata: Exception) {
            Log.w(ETIKET, "ödül seçimleri sunucuyla uzlaştırılamadı", hata)
        }
    }
}

/**
 * Maddi ödül kapısının **başlangıç** değeri.
 *
 * Eskiden derlemeye gömülü bir `false`'tu ve A4 çözülünce APK yayını
 * gerektiriyordu. Kapı artık sunucudaki **stok**: panelden parti
 * yüklenince açılıyor, bitince kapanıyor (`docs/panel-plani.md §5.4`).
 *
 * `false` başlangıç değeri fail-closed: sunucuya ulaşılamadıysa kademe
 * kapalı görünüyor. "Bilmiyoruz"u "açık" saymak, karşılanamayacak bir
 * ödülü seçtirmek olurdu (§4, K37).
 */
internal const val MADDI_ODUL_TESLIMATI_ETKIN = false

internal data class OdulUzlastirmaPlani(
    val yereleYaz: Map<String, String>,
    val sunucuyaYaz: Map<String, String>,
)

/** Sunucudaki ilk seçim kazanır; yalnızca uzakta eksik yerel kayıt yeniden gider. */
internal fun odulUzlastirmaPlani(
    yerel: Map<String, String>,
    uzak: Map<String, String>,
): OdulUzlastirmaPlani = OdulUzlastirmaPlani(
    yereleYaz = uzak,
    sunucuyaYaz = yerel.filterKeys { it !in uzak },
)
