package tr.org.yesilay.yesiliz.data.depo

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Panelden yayımlanan duyuruların cihaza inmesi (`docs/panel-plani.md §5.6`).
 *
 * ## PUSH YOK
 *
 * §5.6 kararı: FCM entegre edilmiyor. Duyuru senkronla iniyor ve bildirim
 * **defterine** yazılıyor; kullanıcı uygulamayı açtığında görüyor. Anlık
 * push ayrı bir altyapı, izin akışı ve metin denetimi demek — MVP'nin
 * kritik yolunda değil.
 *
 * ## AYNI DUYURU İKİ KEZ BİLDİRİLMİYOR
 *
 * Cihaz en son gördüğü duyuru id'sini `Tercihler`'de tutuyor ve yalnızca
 * ondan büyüklerini çekiyor. Aynı şeyi iki kez söylemek, bildirim
 * defterini güvenilmez kılardı.
 *
 * ## RIZA KAPISI YOK
 *
 * `SenkronDeposu` "Yeşilay ile paylaşım" rızasına bağlı (K19); bu depo
 * değil. Duyuru **indiriliyor**, kullanıcı verisi gönderilmiyor — K19
 * gönderim kapısı.
 */
@Singleton
class DuyuruDeposu @Inject constructor(
    private val api: YesilizApi,
    private val tercihler: Tercihler,
    private val bildirimDeposu: BildirimDeposu,
) {

    /**
     * Yeni duyuruları indirir ve bildirim defterine yazar.
     *
     * @return yazılan duyuru sayısı
     */
    suspend fun senkronEt(): Int = try {
        val sonGorulen = tercihler.sonDuyuruId.first()
        val gelenler = api.duyurular(sonGorulen)
        if (gelenler.isEmpty()) {
            0
        } else {
            gelenler.forEach { bildirimDeposu.duyuru(it.baslik, it.metin) }
            // İşaret **yazmadan sonra** ilerliyor: araya giren bir hata
            // duyuruyu sessizce atlatmasın, bir sonraki turda yeniden
            // denensin.
            tercihler.sonDuyuruIdKaydet(gelenler.maxOf { it.id })
            gelenler.size
        }
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: Exception) {
        Log.w(ETIKET, "Duyurular alınamadı", hata)
        0
    }

    private companion object {
        const val ETIKET = "DuyuruDepo"
    }
}
