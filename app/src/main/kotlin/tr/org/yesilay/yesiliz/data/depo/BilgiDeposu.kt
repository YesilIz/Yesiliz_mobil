package tr.org.yesilay.yesiliz.data.depo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import tr.org.yesilay.yesiliz.domain.bilgi.BilgiBankasi
import tr.org.yesilay.yesiliz.domain.bilgi.BilgiYazisi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bilgi Bankası içeriğini `assets`'ten okur ve bellekte tutar.
 *
 * Dosya küçük (birkaç on KB) ve hiç değişmiyor; ilk okumadan sonra tekrar
 * diske gidilmiyor. Room'a seed etmenin bir kazancı olmazdı — sorgulanacak
 * bir şey yok, tam liste zaten tek seferde gerekiyor.
 */
@Singleton
class BilgiDeposu @Inject constructor(
    @ApplicationContext private val baglam: Context,
    private val api: YesilizApi,
) {

    private var onbellek: List<BilgiYazisi>? = null

    /**
     * Yazılar — **önce sunucu, sonra gömülü set** (S5 kararı).
     *
     * Sunucu boş liste dönerse ya da ulaşılamazsa gömülü sete düşülüyor:
     * çevrimdışı garantisi bozulmuyor. Bir yazı düzeltmesi için APK
     * yayını beklemek de gerekmiyor — `docs/panel-plani.md §5.5`.
     *
     * **Sunucuda içerik yoksa "yazı yok" denmiyor.** Boş bir bilgi
     * bankası göstermek, var olan bir özelliği yok gibi sunmak olurdu
     * (CLAUDE.md §4).
     */
    suspend fun yazilar(): List<BilgiYazisi> = onbellek ?: withContext(Dispatchers.IO) {
        (sunucudan() ?: gomuluSet()).also { onbellek = it }
    }

    /**
     * Sunucudaki içeriği tazeler; başarılıysa önbelleği değiştirir.
     *
     * Gecelik worker çağırıyor. Hata **yutuluyor ama loglanıyor** (§9):
     * bu bir yan iş ve kullanıcı gömülü setle çalışmaya devam ediyor.
     */
    suspend fun tazele(): Boolean = withContext(Dispatchers.IO) {
        val gelen = sunucudan()
        if (gelen != null) {
            onbellek = gelen
            true
        } else {
            false
        }
    }

    private suspend fun sunucudan(): List<BilgiYazisi>? = try {
        val yanit = api.bilgiBankasi()
        // Boş liste "içerik yok" demek, "yazı yok" değil: gömülü set
        // devrede kalıyor ve kullanıcı bir şey kaybetmiyor.
        if (yanit.yazilar.isEmpty()) {
            null
        } else {
            yanit.yazilar.map {
                BilgiYazisi(
                    id = it.id,
                    baslik = it.baslik,
                    ozet = it.ozet,
                    kategori = it.kategori,
                    okumaDk = it.okumaDk,
                    govde = it.govde,
                )
            }
        }
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: Exception) {
        Log.w(ETIKET, "Bilgi bankası sunucudan alınamadı, gömülü set kullanılıyor", hata)
        null
    }

    private fun gomuluSet(): List<BilgiYazisi> {
        val icerik = baglam.assets.open(VARLIK_ADI).bufferedReader().use { it.readText() }
        return BilgiBankasi.coz(icerik)
    }

    suspend fun yazi(id: String): BilgiYazisi? = yazilar().firstOrNull { it.id == id }

    private companion object {
        const val VARLIK_ADI = "bilgi_bankasi.json"
        const val ETIKET = "BilgiDepo"
    }
}
