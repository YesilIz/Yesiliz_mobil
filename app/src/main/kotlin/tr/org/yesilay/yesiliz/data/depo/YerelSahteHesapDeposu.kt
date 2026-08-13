package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.flow.Flow
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.domain.hesap.HesapAlani
import tr.org.yesilay.yesiliz.domain.hesap.HesapDeposu
import tr.org.yesilay.yesiliz.domain.hesap.HesapDogrula
import tr.org.yesilay.yesiliz.domain.hesap.HesapSonuc
import tr.org.yesilay.yesiliz.domain.hesap.Oturum
import tr.org.yesilay.yesiliz.ui.buyukHarf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HesapDeposu]'nun MVP uygulaması.
 *
 * **Şifre hiçbir yere yazılmıyor.** Yalnızca doğrulanıyor ve unutuluyor;
 * yerelde saklamak, güvenli olmayan bir kimlik doğrulamayı güvenliymiş gibi
 * göstermek olurdu. Saklanan tek şey ad ve e-posta — ekranlardaki
 * `SABIT_ISIM = "Misafir"` bunun yerine geçiyor.
 *
 * `sifreSifirla` gerçek bir e-posta göndermiyor; ekran bunu kullanıcıya
 * açıkça söylüyor. Faz 2'de `RetrofitHesapDeposu` bağlanınca ekran
 * değişmiyor.
 */
@Singleton
class YerelSahteHesapDeposu @Inject constructor(
    private val tercihler: Tercihler,
    private val yerelOturumSiniri: YerelOturumSiniri,
) : HesapDeposu {

    override val oturum: Flow<Oturum> = tercihler.oturum

    override suspend fun girisYap(eposta: String, sifre: String): HesapSonuc {
        HesapDogrula.eposta(eposta)?.let { return HesapSonuc.Hata(HesapAlani.EPOSTA, it) }
        HesapDogrula.sifre(sifre)?.let { return HesapSonuc.Hata(HesapAlani.SIFRE, it) }

        // Sunucu yok: e-postanın yerel kısmı ad olarak kullanılıyor.
        // Faz 2'de sunucudan gerçek ad gelecek.
        //
        // `replaceFirstChar { it.uppercase() }` Locale.ROOT kullanıyordu:
        // "irem@…" → "Irem" (yanlış), Türkçede "İrem" olmalı. Yalnızca ilk
        // harf `buyukHarf()` ile Türkçe-doğru büyütülüyor, gerisi aynen
        // kalıyor (`debug.md` üçüncü tarama A2).
        val yerelKisim = eposta.substringBefore('@')
        val ad = yerelKisim.take(1).buyukHarf() + yerelKisim.drop(1)
        yerelOturumSiniri.hesabaGec(eposta)
        tercihler.oturumuKaydet(ad = ad, eposta = eposta.trim(), misafir = false)
        return HesapSonuc.Basarili
    }

    override suspend fun kayitOl(ad: String, eposta: String, sifre: String): HesapSonuc {
        HesapDogrula.ad(ad)?.let { return HesapSonuc.Hata(HesapAlani.AD, it) }
        HesapDogrula.eposta(eposta)?.let { return HesapSonuc.Hata(HesapAlani.EPOSTA, it) }
        HesapDogrula.sifre(sifre)?.let { return HesapSonuc.Hata(HesapAlani.SIFRE, it) }

        yerelOturumSiniri.hesabaGec(eposta)
        tercihler.oturumuKaydet(ad = ad.trim(), eposta = eposta.trim(), misafir = false)
        return HesapSonuc.Basarili
    }

    override suspend fun sifreSifirla(eposta: String): HesapSonuc {
        HesapDogrula.eposta(eposta)?.let { return HesapSonuc.Hata(HesapAlani.EPOSTA, it) }
        return HesapSonuc.Basarili
    }

    override suspend fun misafirDevam() {
        yerelOturumSiniri.misafireGec()
        tercihler.oturumuKaydet(ad = "", eposta = "", misafir = true)
    }

    override suspend fun cikisYap() {
        tercihler.oturumuSil()
    }

    /**
     * Çevrimdışı profil güncellemesi — **şifre hariç**.
     *
     * Ad ve simge cihazda tutulan görünüm bilgisi; sunucuya ulaşılamasa da
     * yazılabilir. Şifre değil: burada saklanan bir parola yok (§12 K16) ve
     * "değiştirildi" demek, kullanıcının eski şifresiyle giriş yapmaya devam
     * ettiğini fark etmemesi demek olurdu.
     */
    override suspend fun profilGuncelle(
        ad: String,
        avatarId: String,
        mevcutSifre: String?,
        yeniSifre: String?,
    ): HesapSonuc {
        HesapDogrula.ad(ad)?.let { return HesapSonuc.Hata(HesapAlani.AD, it) }
        if (!yeniSifre.isNullOrBlank()) {
            return HesapSonuc.Hata(
                HesapAlani.SIFRE,
                "Şifreni değiştirmek için internet bağlantısı gerekiyor.",
            )
        }
        tercihler.profilGuncelle(ad = ad.trim(), avatarId = avatarId.trim())
        return HesapSonuc.Basarili
    }

    /**
     * Sunucusuz hesabın sunucuda kaydı yok — silinecek uzak bir şey de yok.
     * `true` dönmesi doğru: kullanıcıya yanlış bir uyarı gösterilmemeli.
     */
    override suspend fun hesabiSil(): Boolean {
        tercihler.oturumuSil()
        return true
    }
}
