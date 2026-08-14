package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import tr.org.yesilay.yesiliz.domain.hesap.HesapAlani
import tr.org.yesilay.yesiliz.domain.hesap.HesapDeposu
import tr.org.yesilay.yesiliz.domain.hesap.HesapSonuc
import tr.org.yesilay.yesiliz.domain.hesap.Oturum
import tr.org.yesilay.yesiliz.domain.lig.Arkadas
import tr.org.yesilay.yesiliz.domain.lig.DavetKodu
import tr.org.yesilay.yesiliz.domain.lig.LigDeposu
import tr.org.yesilay.yesiliz.domain.lig.LigOzeti
import tr.org.yesilay.yesiliz.domain.lig.OnerilenKisi
import java.io.IOException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * # Offline-first sarmalayıcı
 *
 * Sunucuya ulaşılabiliyorsa uzak depo, ulaşılamıyorsa yerel depo.
 *
 * **Neden gerekli:** demo günü backend'in ayakta olacağının garantisi yok
 * (sunum salonunda ağ, laptop, kap — üçü de sorun çıkarabilir). Uygulamanın
 * lig sekmesinin bir bağlantı hatasıyla boş kalması, bütün özelliğin
 * yokmuş gibi görünmesi demek. Bu sarmalayıcı o riski sıfırlıyor.
 *
 * **Yalnızca ağ hatası** yerele düşürüyor. Sunucunun verdiği iş hataları
 * (401 yanlış şifre, 409 e-posta kayıtlı) olduğu gibi geçiyor — yanlış
 * şifre girildiğinde sessizce yerel hesaba düşmek, kullanıcıya "giriş
 * yaptın" demek olurdu.
 */
@Singleton
class YedekliLigDeposu @Inject constructor(
    private val uzak: RetrofitLigDeposu,
    private val yerel: YerelSahteLigDeposu,
) : LigDeposu {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun ligAkisi(): Flow<LigOzeti> = flow {
        // Önce tazelemeyi dene; başarısızsa yerele geç.
        //
        // Yerele düşüldüğü ARTIK SÖYLENİYOR (`cevrimdisi`). Gerçek
        // telefonda sunucuya hiç ulaşılamadığı için bu istisna değil
        // varsayılan durumdu ve ekranda sahte veriye bakıldığını gösteren
        // hiçbir işaret yoktu (`debug.md` O-14). Demo günü "backend
        // çalışıyor" diye gösterilemeyecek veriyi ayırt etmenin tek yolu.
        val kaynak = try {
            uzak.tazele()
            uzak.ligAkisi()
        } catch (_: IOException) {
            yerel.ligAkisi().map { it.copy(cevrimdisi = true) }
        } catch (hata: retrofit2.HttpException) {
            // 401 ayrı bir dünya: sunucu ayakta, kimlik yok. Debug'da yine
            // demo veriye düşülüyor (lig sekmesi boş kalmasın) ama ekran
            // "giriş yap" diyebilsin diye SEBEBİ taşınıyor — release'de
            // aynı ayrım kullanıcıyı çıkışsız bir kartta bırakmıyor.
            val oturumYok = hata.code() == HTTP_YETKISIZ
            yerel.ligAkisi().map { it.copy(cevrimdisi = true, oturumYok = oturumYok) }
        }
        emitAll(kaynak)
    }

    override fun arkadaslarAkisi(): Flow<List<Arkadas>> = flow {
        val kaynak = try {
            uzak.tazele()
            uzak.arkadaslarAkisi()
        } catch (_: IOException) {
            yerel.arkadaslarAkisi()
        } catch (_: retrofit2.HttpException) {
            yerel.arkadaslarAkisi()
        }
        emitAll(kaynak)
    }

    override suspend fun davetKodu(): String =
        demoYedegi({ uzak.davetKodu() }, { yerel.davetKodu() })

    override suspend fun ligeKatil(): LigDeposu.KatilimSonuc =
        demoYedegi({ uzak.ligeKatil() }, { yerel.ligeKatil() })

    override suspend fun davetGonder(kod: String) {
        // Örnek kişi çipleri yalnızca debug deposunda var ve isim taşır;
        // bunları gerçek sunucunun kod ucuna yollamak daima 404 üretirdi.
        if (!DavetKodu.gecerliMi(kod)) {
            yerel.davetGonder(kod)
            return
        }
        agdanAl({ uzak.davetGonder(kod) }, { yerel.davetGonder(kod) })
    }

    override suspend fun onerilenler(): List<OnerilenKisi> =
        // Öneri listesi sunucuda yok; her hâlükârda yerelden.
        yerel.onerilenler()
}

/**
 * Hesap işlemleri için aynı desen.
 *
 * Fark: **iş hataları yerele düşmüyor.** Sunucu "şifre yanlış" diyorsa
 * yerel depoya sorup kullanıcıyı içeri almak, kimlik doğrulamayı tamamen
 * anlamsızlaştırırdı. Yalnızca sunucuya *ulaşılamadığında* yerele
 * geçiliyor ve kullanıcıya bu durum söyleniyor.
 */
@Singleton
class YedekliHesapDeposu @Inject constructor(
    private val uzak: RetrofitHesapDeposu,
    private val yerel: YerelSahteHesapDeposu,
) : HesapDeposu {

    override val oturum: Flow<Oturum> = yerel.oturum

    // `?:` yalnızca **ağ hatasında** (null) yerele düşürüyor. Bir ara
    // sürümde bu `if (sonuc is Basarili) … else yerel…`'e çevrilmişti: o
    // hâlde sunucunun 401 "şifre hatalı" yanıtı da yerel sahte depoya
    // gidiyordu ve o depo biçimi geçerli her e-posta/şifreyi kabul edip
    // `Basarili` döndüğü için kullanıcı **yanlış şifreyle içeri giriyordu**.
    override suspend fun girisYap(eposta: String, sifre: String): HesapSonuc =
        cevrimdisiYedek { uzak.girisYap(eposta, sifre) } ?: yerel.girisYap(eposta, sifre)

    override suspend fun kayitOl(ad: String, eposta: String, sifre: String): HesapSonuc =
        cevrimdisiYedek { uzak.kayitOl(ad, eposta, sifre) } ?: yerel.kayitOl(ad, eposta, sifre)

    override suspend fun sifreSifirla(eposta: String): HesapSonuc =
        cevrimdisiYedek { uzak.sifreSifirla(eposta) } ?: yerel.sifreSifirla(eposta)

    override suspend fun misafirDevam() = yerel.misafirDevam()

    override suspend fun cikisYap() {
        // Jetonları da temizlemek için uzak depo çağrılıyor; o da yerel
        // tercihleri siliyor.
        uzak.cikisYap()
    }

    // Sunucu birincil: ad ve simge orada da değişmeli, şifre yalnızca orada
    // değişebilir. Yerele düşüş yine yalnızca ağ hatasında ve o kol şifre
    // değişimini açıkça reddediyor.
    override suspend fun profilGuncelle(
        ad: String,
        avatarId: String,
        mevcutSifre: String?,
        yeniSifre: String?,
    ): HesapSonuc =
        cevrimdisiYedek { uzak.profilGuncelle(ad, avatarId, mevcutSifre, yeniSifre) }
            ?: yerel.profilGuncelle(ad, avatarId, mevcutSifre, yeniSifre)

    /**
     * Misafirin sunucuda kaydı yok; uzak depoyu çağırmak anlamsız 401
     * üretirdi. Hesaplı kullanıcıda uzak silme sonucu olduğu gibi geçiyor —
     * ağ yoksa `false` dönüp ekranın uyarı göstermesini sağlıyor.
     */
    override suspend fun hesabiSil(): Boolean {
        val misafir = yerel.oturum.first().misafirMi
        if (misafir) return yerel.hesabiSil()

        val silindi = uzak.hesabiSil()
        if (silindi) yerel.hesabiSil()
        return silindi
    }

    /** Ağ hatasında `null` döner (çağıran yerele düşer), iş hatasını geçirir. */
    private suspend fun cevrimdisiYedek(blok: suspend () -> HesapSonuc): HesapSonuc? =
        hesapYedegi(blok)
}

/**
 * Hesap işlemlerinin yedek kuralı: **yalnızca ağ hatasında** `null`.
 *
 * Sunucunun ürettiği iş hatası (401 yanlış şifre, 409 e-posta kayıtlı)
 * olduğu gibi dönüyor; çağıran taraf `?:` ile yalnızca `null` durumunda
 * yerele düşüyor. Ayrı ve test edilebilir bir fonksiyon olmasının sebebi
 * bunun bir kez bozulmuş olması: kural `if (sonuc is Basarili) … else
 * yerel…`e çevrilince 401 de yerele düşüyor ve yerel sahte depo biçimi
 * geçerli her şifreyi kabul ettiği için kullanıcı **yanlış şifreyle içeri
 * giriyordu** (`agdanAl`/`demoYedegi` ile aynı desen).
 */
internal suspend fun hesapYedegi(uzak: suspend () -> HesapSonuc): HesapSonuc? = try {
    uzak()
} catch (_: IOException) {
    null
}

/** Sunucu ayakta ama kimlik yok — `GET /lig` zorunlu jeton istiyor. */
internal const val HTTP_YETKISIZ = 401

/** Yalnızca bağlantı hatasında yedeği çalıştıran ortak yardımcı. */
internal suspend fun <T> agdanAl(uzak: suspend () -> T, yerel: suspend () -> T): T = try {
    uzak()
} catch (_: IOException) {
    yerel()
}

/**
 * Debug/demo lig girişleri için: misafirin 401'i de yerel demoya döner.
 * Davet kabulünde kullanılmaz; 400/404 gibi iş hataları orada görünmelidir.
 */
internal suspend fun <T> demoYedegi(uzak: suspend () -> T, yerel: suspend () -> T): T = try {
    uzak()
} catch (_: IOException) {
    yerel()
} catch (hata: HttpException) {
    if (hata.code() == 401 || hata.code() >= 500) yerel() else throw hata
}

/** `flow { }` içinde başka bir akışı yayınlamak için. */
private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAll(kaynak: Flow<T>) {
    kaynak.collect { emit(it) }
}
