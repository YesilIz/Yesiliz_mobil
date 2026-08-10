package tr.org.yesilay.yesiliz.data.depo

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    private companion object {
        const val ETIKET = "OdulDeposu"
    }

    /** Kademe id → seçilen seçenek id. */
    val secimlerAkisi: Flow<Map<String, String>> = dao.hepsiAkisi().map { liste ->
        liste.associate { it.kademeId to it.secenekId }
    }

    /**
     * Bir kademe için seçim yapar.
     *
     * @return kayıt bu çağrıyla yapıldıysa true, kademe için zaten bir
     *   seçim varsa false (arayan yeniden sormamalı — seçim değişmiyor)
     */
    suspend fun sec(kademeId: String, secenekId: String): Boolean {
        if (dao.getir(kademeId) != null) return false

        dao.kaydet(OdulSecimiEntity(kademeId, secenekId, LocalDate.now(saat)))

        val oturum = tercihler.oturum.first()
        if (!oturum.misafirMi && oturum.girisYapildiMi) {
            // Sunucuya ulaşılamazsa (ağ hatası ya da 409 — başka bir cihazdan
            // zaten seçilmiş) yerel kayıt yine de kalıcı; kullanıcı ödülünü
            // kaybetmiyor. **Yeniden deneme yok:** çağrı bir kez gidiyor,
            // düşerse sunucudaki kopya eksik kalıyor. Telefon değişince
            // `GET /odul/secimlerim` o seçimi geri getiremez — açık iş.
            //
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
}
