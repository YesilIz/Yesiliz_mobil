package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.flow.Flow
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.domain.hesap.HesapAlani
import tr.org.yesilay.yesiliz.domain.hesap.HesapDeposu
import tr.org.yesilay.yesiliz.domain.hesap.HesapDogrula
import tr.org.yesilay.yesiliz.domain.hesap.HesapSonuc
import tr.org.yesilay.yesiliz.domain.hesap.Oturum
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HesapDeposu]'nun MVP uygulaması.
 *
 * **Şifre hiçbir yere yazılmıyor.** Yalnızca doğrulanıyor ve unutuluyor;
 * yerelde saklamak, güvenli olmayan bir kimlik doğrulamayı güvenliymiş gibi
 * göstermek olurdu. Saklanan tek şey ad ve e-posta — ekranlardaki
 * `SABIT_ISIM = "Turan"` bunun yerine geçiyor.
 *
 * `sifreSifirla` gerçek bir e-posta göndermiyor; ekran bunu kullanıcıya
 * açıkça söylüyor. Faz 2'de `RetrofitHesapDeposu` bağlanınca ekran
 * değişmiyor.
 */
@Singleton
class YerelSahteHesapDeposu @Inject constructor(
    private val tercihler: Tercihler,
) : HesapDeposu {

    override val oturum: Flow<Oturum> = tercihler.oturum

    override suspend fun girisYap(eposta: String, sifre: String): HesapSonuc {
        HesapDogrula.eposta(eposta)?.let { return HesapSonuc.Hata(HesapAlani.EPOSTA, it) }
        HesapDogrula.sifre(sifre)?.let { return HesapSonuc.Hata(HesapAlani.SIFRE, it) }

        // Sunucu yok: e-postanın yerel kısmı ad olarak kullanılıyor.
        // Faz 2'de sunucudan gerçek ad gelecek.
        val ad = eposta.substringBefore('@').replaceFirstChar { it.uppercase() }
        tercihler.oturumuKaydet(ad = ad, eposta = eposta.trim(), misafir = false)
        return HesapSonuc.Basarili
    }

    override suspend fun kayitOl(ad: String, eposta: String, sifre: String): HesapSonuc {
        HesapDogrula.ad(ad)?.let { return HesapSonuc.Hata(HesapAlani.AD, it) }
        HesapDogrula.eposta(eposta)?.let { return HesapSonuc.Hata(HesapAlani.EPOSTA, it) }
        HesapDogrula.sifre(sifre)?.let { return HesapSonuc.Hata(HesapAlani.SIFRE, it) }

        tercihler.oturumuKaydet(ad = ad.trim(), eposta = eposta.trim(), misafir = false)
        return HesapSonuc.Basarili
    }

    override suspend fun sifreSifirla(eposta: String): HesapSonuc {
        HesapDogrula.eposta(eposta)?.let { return HesapSonuc.Hata(HesapAlani.EPOSTA, it) }
        return HesapSonuc.Basarili
    }

    override suspend fun misafirDevam() {
        tercihler.oturumuKaydet(ad = "", eposta = "", misafir = true)
    }

    override suspend fun cikisYap() {
        tercihler.oturumuSil()
    }
}
