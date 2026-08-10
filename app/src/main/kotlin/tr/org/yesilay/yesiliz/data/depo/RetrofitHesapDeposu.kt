package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.GirisIstek
import tr.org.yesilay.yesiliz.data.uzak.JetonDeposu
import tr.org.yesilay.yesiliz.data.uzak.JetonlarDto
import tr.org.yesilay.yesiliz.data.uzak.KayitIstek
import tr.org.yesilay.yesiliz.data.uzak.SifreSifirlaIstek
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import tr.org.yesilay.yesiliz.domain.hesap.HesapAlani
import tr.org.yesilay.yesiliz.domain.hesap.HesapDeposu
import tr.org.yesilay.yesiliz.domain.hesap.HesapSonuc
import tr.org.yesilay.yesiliz.domain.hesap.Oturum
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerçek hesap işlemleri — `backend/app/uclar/hesap.py`.
 *
 * Sunucu hatalarını **alan bazlı** hatalara çeviriyor ki ekranlar
 * `YerelSahteHesapDeposu` ile aynı biçimde davransın: 409 → e-posta alanı,
 * 401 → şifre alanı. Ağ hatası burada yakalanmıyor; onu
 * [YedekliHesapDeposu] ele alıyor.
 */
@Singleton
class RetrofitHesapDeposu @Inject constructor(
    private val api: YesilizApi,
    private val jetonDeposu: JetonDeposu,
    private val tercihler: Tercihler,
) : HesapDeposu {

    override val oturum: Flow<Oturum> = tercihler.oturum

    override suspend fun girisYap(eposta: String, sifre: String): HesapSonuc = cagir {
        api.giris(GirisIstek(eposta = eposta.trim().lowercase(), sifre = sifre))
    }

    override suspend fun kayitOl(ad: String, eposta: String, sifre: String): HesapSonuc = cagir {
        api.kayit(KayitIstek(ad = ad.trim(), eposta = eposta.trim().lowercase(), sifre = sifre))
    }

    override suspend fun sifreSifirla(eposta: String): HesapSonuc = try {
        api.sifreSifirla(SifreSifirlaIstek(eposta.trim().lowercase()))
        HesapSonuc.Basarili
    } catch (hata: HttpException) {
        HesapSonuc.Hata(alan(hata), mesaj(hata))
    }

    override suspend fun misafirDevam() {
        // Misafir sunucuya hiç uğramıyor: hesabı olmayan kullanıcının
        // sunucuda kaydı da olmamalı.
        tercihler.oturumuKaydet(ad = "", eposta = "", misafir = true)
    }

    override suspend fun cikisYap() {
        jetonDeposu.temizle()
        tercihler.oturumuSil()
    }

    override suspend fun hesabiSil(): Boolean {
        // Sunucu çağrısı ÖNCE: jetonu silersek kimliğimizi kaybeder,
        // hesabı bir daha silemeyiz.
        // 401 dahil her hata `false`: jeton geçersizse sunucudaki hesap
        // yerinde duruyor demektir ve kullanıcıya "silindi" denemez.
        val silindi = hesapSilmeIstegi { api.hesabiSil() }
        if (!silindi) return false
        cikisYap()
        return true
    }

    private suspend fun cagir(blok: suspend () -> JetonlarDto): HesapSonuc = try {
        val jetonlar = blok()
        jetonDeposu.kaydet(jetonlar.erisim, jetonlar.yenileme)
        tercihler.oturumuKaydet(ad = jetonlar.ad, eposta = jetonlar.eposta, misafir = false)
        HesapSonuc.Basarili
    } catch (hata: HttpException) {
        HesapSonuc.Hata(alan(hata), mesaj(hata))
    }

    /** HTTP kodunu forma geri bildirilebilir bir alana çeviriyor. */
    private fun alan(hata: HttpException): HesapAlani = hesapHataAlani(hata.code())

    private fun mesaj(hata: HttpException): String = hesapHataMesaji(hata.code())
}

private const val KOD_CAKISMA = 409
private const val KOD_YETKISIZ = 401
private const val KOD_DOGRULAMA = 422
private const val KOD_KULLANILAMIYOR = 503

/** HTTP hesap hatasını forma geri bildirilebilir alana çevirir. */
internal fun hesapHataAlani(kod: Int): HesapAlani = when (kod) {
    KOD_CAKISMA -> HesapAlani.EPOSTA
    KOD_YETKISIZ -> HesapAlani.SIFRE
    // 422 sunucunun girdi doğrulaması (geçersiz e-posta biçimi, kısa
    // şifre). Cihaz tarafı da doğruluyor ama sunucu daha katı olabilir.
    KOD_DOGRULAMA -> HesapAlani.EPOSTA
    else -> HesapAlani.GENEL
}

/** Sunucu ayrıntısını/sırrını taşımadan kullanıcıya güvenli hata metni üretir. */
internal fun hesapHataMesaji(kod: Int): String = when (kod) {
    KOD_CAKISMA -> "Bu e-posta zaten kayıtlı."
    KOD_YETKISIZ -> "E-posta ya da şifre hatalı."
    KOD_DOGRULAMA -> "Bu e-posta adresi kabul edilmedi. Gerçek bir adres yazmayı dene."
    KOD_KULLANILAMIYOR ->
        "Şifre sıfırlama e-postası henüz kullanılamıyor. Biraz sonra tekrar deneyebilirsin."
    else -> "Şu an bağlanılamıyor. Biraz sonra tekrar deneyebilirsin."
}

/** Beklenen uzak hatayı görünür `false` yapar; iş iptalini asla yutmaz. */
internal suspend fun hesapSilmeIstegi(istek: suspend () -> Unit): Boolean = try {
    istek()
    true
} catch (iptal: CancellationException) {
    throw iptal
} catch (_: Exception) {
    false
}
