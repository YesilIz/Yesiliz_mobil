package tr.org.yesilay.yesiliz.data.depo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
) {

    private var onbellek: List<BilgiYazisi>? = null

    suspend fun yazilar(): List<BilgiYazisi> = onbellek ?: withContext(Dispatchers.IO) {
        val icerik = baglam.assets.open(VARLIK_ADI).bufferedReader().use { it.readText() }
        BilgiBankasi.coz(icerik).also { onbellek = it }
    }

    suspend fun yazi(id: String): BilgiYazisi? = yazilar().firstOrNull { it.id == id }

    private companion object {
        const val VARLIK_ADI = "bilgi_bankasi.json"
    }
}
