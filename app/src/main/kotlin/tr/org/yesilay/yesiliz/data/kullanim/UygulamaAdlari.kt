package tr.org.yesilay.yesiliz.data.kullanim

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Paket adı → kullanıcıya gösterilen uygulama adı.
 *
 * **`QUERY_ALL_PACKAGES` istemiyor.** Manifest'teki `<queries>` bloğu
 * yalnızca LAUNCHER niyetine sahip uygulamaları görünür kılıyor; bu, ekran
 * süresi olan uygulamalar için yeterli ve Play Store'un hassas izin
 * politikasına takılmıyor.
 *
 * Adlar önbelleğe alınıyor: `PackageManager` sorgusu ucuz değil ve kategori
 * ekranı listeyi her kaydırmada çiziyor.
 */
@Singleton
class UygulamaAdlari @Inject constructor(
    @ApplicationContext private val baglam: Context,
) {

    private val onbellek = HashMap<String, String>()

    /** Ad bulunamazsa paket adının son parçası dönüyor (ör. `instagram`). */
    fun ad(paketAdi: String): String = onbellek.getOrPut(paketAdi) {
        runCatching {
            val pm = baglam.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(paketAdi, 0)).toString()
        }.getOrElse { paketAdi.substringAfterLast('.') }
    }

    /**
     * Cihazdaki başlatılabilir uygulamalar.
     *
     * Kategorize edilecekler ızgarasının kaynağı. Kendi paketimiz listede
     * yok — kullanıcıdan Yeşilİz'i kategorize etmesini istemek anlamsız.
     */
    fun yuklenenler(): List<String> {
        val pm = baglam.packageManager
        val niyet = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return runCatching {
            pm.queryIntentActivities(niyet, PackageManager.MATCH_ALL)
                .map { it.activityInfo.packageName }
                .distinct()
                .filter { it != baglam.packageName }
        }.getOrDefault(emptyList())
    }
}
