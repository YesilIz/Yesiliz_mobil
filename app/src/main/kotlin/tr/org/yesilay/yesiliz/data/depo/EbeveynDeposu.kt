package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.CancellationException
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.EbeveynOzetDto
import tr.org.yesilay.yesiliz.data.uzak.EslestirIstek
import tr.org.yesilay.yesiliz.data.uzak.EslestirmeKoduIstek
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ebeveyn eşleştirme ve çocuk özeti.
 *
 * Eşleştirme artık **sunucuda doğrulanıyor** (`backend/app/uclar/ebeveyn.py`):
 * kodun var olması, 10 dakikalık süresinin dolmamış olması, daha önce
 * kullanılmamış olması ve karşı rolde üretilmiş olması aranıyor. Cihazdaki
 * eski hâli yalnızca "6 hane mi" diye bakıyordu.
 *
 * ## Eşleştirme çevrimdışı yedeğe DÜŞMÜYOR
 *
 * `Yedekli*Deposu` deseninin (CLAUDE.md §12 K18) bilinçli istisnası.
 * Eşleştirme, ağ hatasında eşleşmeyi yerel olarak işaretleyip "başarılı"
 * dönüyordu; gerçek telefonda sunucuya hiç ulaşılamadığı için (O-14) bu
 * istisna değil **tek** davranış hâline gelmişti ve rastgele herhangi bir
 * 6 haneli sayı ebeveyn panelini açıyordu (`debug.md` K-8).
 *
 * Bu, çocuğun ekran verisine erişim veren bir akış: yanlış şifreyle giriş
 * yaptırmakla aynı sınıfta. Kod üretimi ([kodUret]) çevrimdışı çalışmaya
 * devam ediyor — orada yanlış kimliğe erişim verilmiyor, yalnızca ekran
 * gösteriliyor ve `cevrimdisiMi` ile bunun doğrulanamayacağı söyleniyor.
 */
@Singleton
class EbeveynDeposu @Inject constructor(
    private val api: YesilizApi,
    private val tercihler: Tercihler,
) {

    /**
     * Eşleştirme kodu üretir.
     *
     * Sunucuya ulaşılamıyorsa kullanılamayacak rastgele bir kod
     * gösterilmez; doğrulanabilir hata döner.
     */
    suspend fun kodUret(rol: String): KodUretSonucu = try {
        KodUretSonucu(kod = api.eslestirmeKodu(EslestirmeKoduIstek(rol)).kod)
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: retrofit2.HttpException) {
        KodUretSonucu(
            hata = if (hata.code() == 401) {
                "Kod oluşturmak için yeniden giriş yapmalısın."
            } else {
                "Eşleştirme kodu oluşturulamadı. Biraz sonra tekrar dene."
            },
        )
    } catch (_: Exception) {
        KodUretSonucu(hata = "Sunucuya ulaşılamadı. Bağlantını kontrol edip tekrar dene.")
    }

    /**
     * Kodu doğrulayıp bağlantıyı kurar.
     *
     * @return `null` başarılı; aksi hâlde kullanıcıya gösterilecek mesaj
     */
    suspend fun eslestir(kod: String, rol: String): String? {
        if (kod.length != KOD_UZUNLUK) return "Kod 6 haneli olmalı."

        return try {
            api.eslestir(EslestirIstek(kod = kod, rol = rol))
            tercihler.ebeveynEslesmeyiKaydet(rol)
            null
        } catch (hata: retrofit2.HttpException) {
            when (hata.code()) {
                KOD_BULUNAMADI -> "Kod geçersiz ya da daha önce kullanılmış."
                KOD_SURESI_DOLDU -> "Kodun süresi dolmuş. Yeni bir kod üretin."
                KOD_CAKISMASI -> "Cihazlardan biri zaten başka bir cihazla eşleştirilmiş."
                else -> "Eşleştirme tamamlanamadı."
            }
        } catch (iptal: CancellationException) {
            throw iptal
        } catch (_: Exception) {
            // Ağ hatası. Eşleşme İŞARETLENMİYOR: kodu doğrulayan tek yer
            // sunucu, ona ulaşılamıyorsa doğrulanmış bir eşleşme yok.
            "Sunucuya ulaşılamadı. Eşleştirmeyi bağlantın olduğunda tekrar dene."
        }
    }

    /** Çocuğun **özeti** — ham veri değil; sahte yedeğe düşmez. */
    suspend fun cocukOzeti(): EbeveynOzetSonucu = try {
        EbeveynOzetSonucu(veri = api.ebeveynOzeti())
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: retrofit2.HttpException) {
        if (hata.code() == 404) {
            // Diğer cihaz bağı kaldırdıysa bu cihazın yerel bayrağı
            // eskidir. Sunucunun yetki kararını yerelde de yansıt.
            tercihler.ebeveynEslesmeyiKaldir()
            EbeveynOzetSonucu(hata = "Eşleştirme artık etkin değil.")
        } else {
            EbeveynOzetSonucu(hata = "Özet alınamadı. Biraz sonra tekrar dene.")
        }
    } catch (_: Exception) {
        EbeveynOzetSonucu(
            hata = "Sunucuya ulaşılamıyor. Bağlantını kontrol edip tekrar dene.",
        )
    }

    /**
     * Sunucudaki bağı kaldırır; yalnızca doğrulanmış başarıdan sonra
     * cihaz bayrağını temizler. Aksi hâlde diğer cihazın özet erişimi
     * sürerken bu telefonda eşleşme yokmuş gibi davranılmaz.
     *
     * @return `null` başarı; aksi hâlde tekrar denenebilir hata metni.
     */
    suspend fun eslesmeyiKaldir(): String? = try {
        api.eslesmeyiKaldir()
        tercihler.ebeveynEslesmeyiKaldir()
        null
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: retrofit2.HttpException) {
        if (hata.code() == 401) {
            "Eşleştirmeyi kaldırmak için yeniden giriş yapmalısın."
        } else {
            "Eşleştirme kaldırılamadı. Biraz sonra tekrar dene."
        }
    } catch (_: Exception) {
        "Sunucuya ulaşılamadı. Bağlantını kontrol edip tekrar dene."
    }

    private companion object {
        const val KOD_UZUNLUK = 6
        const val KOD_BULUNAMADI = 404
        const val KOD_SURESI_DOLDU = 410
        const val KOD_CAKISMASI = 409
    }
}

data class KodUretSonucu(
    val kod: String = "",
    val hata: String? = null,
)

data class EbeveynOzetSonucu(
    val veri: EbeveynOzetDto? = null,
    val hata: String? = null,
)
