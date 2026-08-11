package tr.org.yesilay.yesiliz.data.depo

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.GunlukOzetDto
import tr.org.yesilay.yesiliz.data.uzak.LigGorunurlukIstek
import tr.org.yesilay.yesiliz.data.uzak.RiskSonucDto
import tr.org.yesilay.yesiliz.data.uzak.SenkronIstek
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.PuanGunlukDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.RiskSonucEntity
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.WeekFields
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Günlük özetin sunucuya yüklenmesi.
 *
 * ## RIZA KAPISI
 *
 * **Yeşilay paylaşım onayı kapalıysa hiçbir şey gönderilmiyor** — bu
 * kontrol en başta, ağ çağrısından önce. Onay varsayılan olarak kapalı
 * (`docs/spec-veri.md`, opt-in). Kullanıcı onayı geri çekince gönderim
 * o an duruyor; daha önce gönderilmiş veriyi silmek ayrı bir iş
 * (`VeriYonetimiDeposu` yerel siliyor, sunucu tarafı Faz 3).
 *
 * ## NE GİDİYOR
 *
 * Yalnızca `gunluk_ozet` ve `risk_sonuc`. **`kullanim_olay` gitmiyor** —
 * uygulama adları ve oturum bilgisi cihazda kalıyor (CLAUDE.md §4).
 */
@Singleton
class SenkronDeposu @Inject constructor(
    private val api: YesilizApi,
    private val tercihler: Tercihler,
    private val ozetDao: GunlukOzetDao,
    private val riskDao: RiskSonucDao,
    private val puanDao: PuanGunlukDao,
    private val saat: Clock,
) {

    /**
     * Son [GUN] günü gönderir.
     *
     * Hata **sessizce yutuluyor**: senkron bir yan iş, başarısız olduğunda
     * kullanıcıya hiçbir şey göstermemek doğru. Gecelik worker yarın
     * tekrar deneyecek; sunucu tarafı idempotent.
     *
     * @return gönderildiyse true (test ve teşhis için)
     */
    suspend fun senkronEt(): Boolean {
        if (!tercihler.yesilayPaylasimi.first()) return false
        if (tercihler.oturum.first().misafirMi) return false

        return try {
            val ozetler = ozetDao.sonGunler(GUN)
            if (ozetler.isEmpty()) return false

            api.ozetGonder(
                SenkronIstek(
                    ozetler = ozetler.map { it.dto() },
                    riskler = riskDao.sonGunler(GUN).map { it.dto() },
                    haftalikPuan = haftalikPuan(),
                    // "Ligde görün" tercihi burada taşınıyor; ayrı bir uç
                    // yok (`debug.md` Y-5).
                    ligdeGorunur = tercihler.ligdeGorun.first(),
                ),
            )
            true
        } catch (iptal: CancellationException) {
            throw iptal
        } catch (hata: Exception) {
            Log.w(ETIKET, "Senkron başarısız, yarın tekrar denenecek", hata)
            false
        }
    }

    /**
     * Cihazın canlı olduğunu bildirir — ebeveyn paneli bunu okuyor.
     *
     * Giriş yapılmamışsa **hiç denenmiyor**: jetonsuz istek sunucuda 401
     * üretiyor ve günlüğü kirletiyor.
     */
    suspend fun heartbeat() {
        val oturum = tercihler.oturum.first()
        if (oturum.misafirMi || !oturum.girisYapildiMi) return
        try {
            api.heartbeat()
        } catch (iptal: CancellationException) {
            throw iptal
        } catch (hata: Exception) {
            Log.w(ETIKET, "Heartbeat gönderilemedi", hata)
        }
    }

    /**
     * Lig gizliliğini özet/puan paylaşımı rızasından bağımsız yollar.
     * Bu bir davranış özeti değildir; kullanıcının kimlere görünmek
     * istediğine dair açık ayarıdır. Hata halinde günlük worker yeniden
     * dener, profil akışı da kullanıcı değiştirir değiştirmez çağırır.
     */
    suspend fun ligGorunurlugunuSenkronEt(): Boolean {
        val oturum = tercihler.oturum.first()
        if (oturum.misafirMi || !oturum.girisYapildiMi) return false
        return try {
            api.ligGorunurlugu(
                LigGorunurlukIstek(gorunur = tercihler.ligdeGorun.first()),
            )
            true
        } catch (iptal: CancellationException) {
            throw iptal
        } catch (hata: Exception) {
            Log.w(ETIKET, "Lig görünürlüğü eşitlenemedi; yeniden denenecek", hata)
            false
        }
    }

    /**
     * Bu haftanın toplam puanı.
     *
     * Lig sıralaması bunun üzerinden; haftalık pencere sunucudakiyle aynı
     * tanımda (ISO haftası, pazartesi başlangıçlı).
     */
    private suspend fun haftalikPuan(): Int {
        val bugun = LocalDate.now(saat)
        val pazartesi = bugun.with(WeekFields.ISO.dayOfWeek(), 1)
        return puanDao.aralikToplami(pazartesi, bugun)
    }

    private companion object {
        /** Gecelik worker'ın telafi penceresiyle aynı. */
        const val GUN = 7
        const val ETIKET = "Senkron"
    }
}

private fun GunlukOzetEntity.dto() = GunlukOzetDto(
    tarih = tarih.toString(),
    socialDk = socialDk,
    entertainmentDk = entertainmentDk,
    gamesDk = gamesDk,
    communicationDk = communicationDk,
    productivityDk = productivityDk,
    educationDk = educationDk,
    healthDk = healthDk,
    otherDk = otherDk,
    gece = gece,
    oturum = oturum,
    acilis = acilis,
    sabah = sabah,
    uykuOncesi = uykuOncesi,
    yerDegis = yerDegis,
    adim = adim,
    sporDk = sporDk,
    S = S,
)

private fun RiskSonucEntity.dto() = RiskSonucDto(
    tarih = tarih.toString(),
    S = S,
    R = R,
    gosterilenBant = gosterilenBant,
    trend = trend,
)
