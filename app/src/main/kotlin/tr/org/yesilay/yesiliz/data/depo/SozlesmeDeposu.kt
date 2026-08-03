package tr.org.yesilay.yesiliz.data.depo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tr.org.yesilay.yesiliz.domain.sozlesme.Sozlesme
import tr.org.yesilay.yesiliz.domain.sozlesme.Sozlesmeler
import tr.org.yesilay.yesiliz.domain.sozlesme.SssMaddesi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sözleşme metinleri ve SSS — `assets/sozlesmeler.json`.
 *
 * [BilgiDeposu] ile aynı desen: dosya küçük ve hiç değişmiyor, ilk
 * okumadan sonra tekrar diske gidilmiyor.
 */
@Singleton
class SozlesmeDeposu @Inject constructor(
    @ApplicationContext private val baglam: Context,
) {

    private var sozlesmeler: List<Sozlesme>? = null
    private var sorular: List<SssMaddesi>? = null

    suspend fun hepsi(): List<Sozlesme> = sozlesmeler ?: withContext(Dispatchers.IO) {
        Sozlesmeler.coz(icerik()).also { sozlesmeler = it }
    }

    suspend fun sozlesme(id: String): Sozlesme? = hepsi().firstOrNull { it.id == id }

    suspend fun sss(): List<SssMaddesi> = sorular ?: withContext(Dispatchers.IO) {
        Sozlesmeler.sss(icerik()).also { sorular = it }
    }

    private fun icerik(): String =
        baglam.assets.open(Sozlesmeler.VARLIK_ADI).bufferedReader().use { it.readText() }
}
