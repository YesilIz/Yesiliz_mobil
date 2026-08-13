package tr.org.yesilay.yesiliz.data.depo

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.GirisIstek
import tr.org.yesilay.yesiliz.data.uzak.JetonDeposu
import tr.org.yesilay.yesiliz.data.uzak.JetonlarDto
import tr.org.yesilay.yesiliz.data.uzak.KayitIstek
import tr.org.yesilay.yesiliz.data.uzak.ProfilIstek
import tr.org.yesilay.yesiliz.data.uzak.SifreSifirlaIstek
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import tr.org.yesilay.yesiliz.domain.hesap.HesapAlani
import tr.org.yesilay.yesiliz.domain.hesap.HesapDeposu
import tr.org.yesilay.yesiliz.domain.hesap.HesapDogrula
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
    private val yerelOturumSiniri: YerelOturumSiniri,
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
        yerelOturumSiniri.misafireGec()
        tercihler.oturumuKaydet(ad = "", eposta = "", misafir = true)
    }

    override suspend fun cikisYap() {
        try {
            api.cikis()
        } catch (iptal: CancellationException) {
            // Yerel oturumu yine kapat; kullanıcının "çıkış" eylemi
            // yarım kalıp ekranda oturum açık görünmesin.
            throw iptal
        } catch (hata: Exception) {
            // Çevrimdışı çıkış engellenmez. Sunucu jetonu süresi
            // dolana dek kalabilir; bu nedenle sessizce yutulmaz.
            Log.w(ETIKET, "sunucu oturumu iptal edilemedi", hata)
        } finally {
            // Ağ çağrısı ekran kapanırken iptal edilse de kullanıcının
            // açık "çıkış" eylemi cihazda yarım kalmasın.
            yerelOturumuTemizle()
        }
    }

    /**
     * `PATCH /auth/profil`.
     *
     * Bu uç yazılmadan önce burası hiçbir ağ çağrısı yapmadan DataStore'a
     * yazıp `Basarili` dönüyordu: sunucudaki ad değişmiyordu ve girilen yeni
     * şifre **hiçbir yere gitmiyordu** — kullanıcı şifresini değiştirdiğini
     * sanıp eski şifreyle giriş yapmaya devam ediyordu (CLAUDE.md §12 K37).
     * Yerel yazma artık sunucu 200 döndükten **sonra**, sunucunun döndürdüğü
     * değerlerle yapılıyor.
     */
    override suspend fun profilGuncelle(
        ad: String,
        avatarId: String,
        mevcutSifre: String?,
        yeniSifre: String?,
    ): HesapSonuc {
        HesapDogrula.ad(ad)?.let { return HesapSonuc.Hata(HesapAlani.AD, it) }
        if (!yeniSifre.isNullOrBlank()) {
            HesapDogrula.sifre(yeniSifre)?.let { return HesapSonuc.Hata(HesapAlani.SIFRE, it) }
            if (mevcutSifre.isNullOrBlank()) {
                return HesapSonuc.Hata(
                    HesapAlani.SIFRE,
                    "Şifreni değiştirmek için mevcut şifreni de gir.",
                )
            }
        }
        return try {
            val profil = api.profilGuncelle(
                ProfilIstek(
                    ad = ad.trim(),
                    avatar = avatarId.trim(),
                    mevcutSifre = mevcutSifre?.takeIf { !yeniSifre.isNullOrBlank() },
                    yeniSifre = yeniSifre?.takeIf { it.isNotBlank() },
                ),
            )
            tercihler.profilGuncelle(ad = profil.ad, avatarId = profil.avatar)
            HesapSonuc.Basarili
        } catch (hata: HttpException) {
            // 401 burada "oturum yok" değil "mevcut şifre hatalı" anlamına
            // geliyor; ortak metin ("E-posta ya da şifre hatalı") bu ekranda
            // yanıltıcı olurdu.
            if (hata.code() == KOD_YETKISIZ) {
                HesapSonuc.Hata(HesapAlani.SIFRE, "Mevcut şifren hatalı.")
            } else {
                HesapSonuc.Hata(alan(hata), mesaj(hata))
            }
        }
    }

    override suspend fun hesabiSil(): Boolean {
        // Sunucu çağrısı ÖNCE: jetonu silersek kimliğimizi kaybeder,
        // hesabı bir daha silemeyiz.
        // 401 dahil her hata `false`: jeton geçersizse sunucudaki hesap
        // yerinde duruyor demektir ve kullanıcıya "silindi" denemez.
        val silindi = hesapSilmeIstegi { api.hesabiSil() }
        if (!silindi) return false
        // DELETE başarılı olduğunda kullanıcı ve bütün yenileme oturumları
        // artık yoktur. Burada tekrar `/auth/cikis` çağırmak kaçınılmaz
        // 401 + gereksiz yenileme denemesi üretir. Yalnızca cihazı temizle.
        yerelOturumuTemizle()
        return true
    }

    /** Sunucu sonucu alındıktan sonra iptal gelse bile yarım oturum bırakma. */
    private suspend fun yerelOturumuTemizle() = withContext(NonCancellable) {
        jetonDeposu.temizle()
        tercihler.oturumuSil()
    }

    private suspend fun cagir(blok: suspend () -> JetonlarDto): HesapSonuc = try {
        val jetonlar = blok()
        // Jeton/oturum yeni hesaba yazılmadan önce cihazdaki eski hesap
        // verisini ayır. Aynı hesaba dönüş ve misafir→hesap dönüşümü
        // bu sınırda bilinçli olarak korunur.
        yerelOturumSiniri.hesabaGec(jetonlar.eposta)
        jetonDeposu.kaydet(jetonlar.erisim, jetonlar.yenileme)
        tercihler.oturumuKaydet(
            ad = jetonlar.ad,
            eposta = jetonlar.eposta,
            misafir = false,
            // Simge sunucudan geliyor: yeni telefona giren kullanıcı
            // seçtiği avatarı geri buluyor.
            avatarId = jetonlar.avatar,
        )
        HesapSonuc.Basarili
    } catch (hata: HttpException) {
        HesapSonuc.Hata(alan(hata), mesaj(hata))
    }

    /** HTTP kodunu forma geri bildirilebilir bir alana çeviriyor. */
    private fun alan(hata: HttpException): HesapAlani = hesapHataAlani(hata.code())

    private fun mesaj(hata: HttpException): String = hesapHataMesaji(hata.code())
}

private const val ETIKET = "HesapDeposu"

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
