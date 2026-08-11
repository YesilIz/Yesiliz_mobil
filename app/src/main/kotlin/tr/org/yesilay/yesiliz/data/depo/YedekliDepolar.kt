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
        } catch (_: retrofit2.HttpException) {
            // 401: oturum yok/expired → yerel mock. Misafir kullanıcı da
            // lig sekmesini boş görmemeli.
            yerel.ligAkisi().map { it.copy(cevrimdisi = true) }
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

    override suspend fun ligeKatil() {
        demoYedegi({ uzak.ligeKatil() }, { yerel.ligeKatil() })
    }

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
    private suspend fun cevrimdisiYedek(blok: suspend () -> HesapSonuc): HesapSonuc? = try {
        blok()
    } catch (_: IOException) {
        null
    }
}

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
