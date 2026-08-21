package tr.org.yesilay.yesiliz.data.depo

import android.util.Log
import kotlinx.coroutines.CancellationException
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import tr.org.yesilay.yesiliz.data.yerel.dao.PuanDuzeltmeDao
import tr.org.yesilay.yesiliz.data.yerel.entity.PuanDuzeltmeEntity
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Panelden yapılan puan düzeltmelerinin cihaza inmesi (`CLAUDE.md §12 K56`).
 *
 * ## TEKRAR KORUMASI
 *
 * `PuanDuzeltmeEntity`'nin birincil anahtarı **sunucu id'si** ve ekleme
 * `OnConflictStrategy.IGNORE` ile yapılıyor: aynı düzeltmenin ikinci kez
 * indirilmesi no-op. Bir deltayı iki kez uygulamak geri dönülemez bir
 * kullanıcı zararı ve koruma tek bir bayrağa bırakılamayacak kadar
 * önemli — sunucudaki `cihaz_uyguladi` yalnızca bilgilendirici.
 *
 * ## RIZA KAPISI YOK
 *
 * `SenkronDeposu` "Yeşilay ile paylaşım" rızasına bağlı (K19); bu depo
 * değil. Düzeltme davranışsal veri taşımıyor — kullanıcının kendi
 * bakiyesine yapılmış bir işlem ve ödül seçimiyle (K28) aynı sınıfta.
 * Rızaya bağlansaydı paylaşımı kapalı kullanıcının puanı hiç
 * düzeltilemezdi.
 */
@Singleton
class PuanDuzeltmeDeposu @Inject constructor(
    private val api: YesilizApi,
    private val dao: PuanDuzeltmeDao,
    private val bildirimDeposu: BildirimDeposu,
    private val puanDeposu: PuanDeposu,
) {

    /**
     * Sunucudaki düzeltmeleri indirir, yenileri bildirir.
     *
     * Hata **yutuluyor ama loglanıyor** (§9): bu bir yan iş, kullanıcıya
     * gösterilecek bir şey yok ve bir sonraki gecelik koşum tekrar
     * deneyecek.
     *
     * @return indirilen yeni düzeltme sayısı
     */
    suspend fun senkronEt(): Int = try {
        val uzak = api.puanDuzeltmeleri()
        val eklendi = dao.ekle(
            uzak.map { dto ->
                PuanDuzeltmeEntity(
                    id = dto.id,
                    delta = dto.delta,
                    gerekce = dto.gerekce,
                    zaman = Instant.parse(dto.zaman),
                )
            },
        ).count { it != -1L }

        bildirilmeyenleriBildir()
        // Yüksek-su-seviyesi ekleme sonrası tazeleniyor: pozitif bir
        // düzeltme ağaç evresini ilerletmeli, negatifi geri almamalı (U6).
        puanDeposu.kazanilmisiTazele()
        eklendi
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: Exception) {
        Log.w(ETIKET, "Puan düzeltmeleri alınamadı", hata)
        0
    }

    /**
     * Henüz bildirilmemiş düzeltmeleri bildirim defterine yazar.
     *
     * İndirme ile bildirim ayrı: ağ hatası yüzünden yarım kalan bir
     * indirme, daha önce inmiş ama bildirilmemiş bir düzeltmeyi de
     * sessiz bırakırdı.
     */
    private suspend fun bildirilmeyenleriBildir() {
        val yeniler = dao.bildirilmeyenler()
        if (yeniler.isEmpty()) return
        yeniler.forEach { bildirimDeposu.puanDuzeltildi(it.delta, it.gerekce) }
        dao.bildirildiIsaretle(yeniler.map { it.id })
    }

    private companion object {
        const val ETIKET = "PuanDuzeltmeDepo"
    }
}
