package tr.org.yesilay.yesiliz.data.depo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import tr.org.yesilay.yesiliz.data.kullanim.GecmisGunSayaci
import tr.org.yesilay.yesiliz.data.kullanim.KullanimIzni
import tr.org.yesilay.yesiliz.data.kullanim.KullanimOkuyucu
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.risk.Sabitler
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Kurulumda geçmiş veri" — `Tercihler.gecmisGunSayisi`'nin gerçek üreticisi.
 *
 * [Tercihler.kurulumuIsaretle] ilk açılışta çalışır ve izin henüz
 * verilmemiş olabileceği için güvenle `gecmisGun = 0` yazar. Bu sınıf o
 * değeri, izin **onaylandıktan sonra** gerçek `queryAndAggregateUsageStats`
 * hacmiyle bir kez günceller — ne zaman verilirse verilsin (onboarding
 * içinde ya da sonradan Ayarlar'dan).
 *
 * [hesaplaGerekirse] her çağrıldığında iki ucuz kontrolden geçiyor (izin,
 * sonra tek bir DataStore okuması), bu yüzden `MainActivity.onResume()`
 * gibi sık tetiklenen bir noktadan tekrar tekrar çağrılması güvenli.
 */
@Singleton
class GecmisVeriDeposu @Inject constructor(
    @ApplicationContext private val baglam: Context,
    private val tercihler: Tercihler,
    private val kullanimOkuyucu: KullanimOkuyucu,
    private val saat: Clock,
) {
    suspend fun hesaplaGerekirse() {
        if (!KullanimIzni.verildiMi(baglam)) return
        if (tercihler.gecmisGunHesaplandiMi.first()) return

        val kurulumGunu = tercihler.ilkKurulumTarihi.first() ?: LocalDate.now(saat)
        val sureler = kullanimOkuyucu.gunlukToplamSureler(kurulumGunu, Sabitler.ALPHA_IKINCI_ESIK)
        tercihler.gecmisGunuKaydet(GecmisGunSayaci.say(sureler))
    }
}
