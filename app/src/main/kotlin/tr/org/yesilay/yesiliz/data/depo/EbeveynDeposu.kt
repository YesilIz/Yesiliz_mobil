package tr.org.yesilay.yesiliz.data.depo

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
     * @return kod ve bunun sunucudan mı geldiği
     */
    suspend fun kodUret(rol: String): Pair<String, Boolean> = try {
        api.eslestirmeKodu(EslestirmeKoduIstek(rol)).kod to true
    } catch (_: Exception) {
        // Çevrimdışı: yerel kod üretiliyor ama karşı cihaz bunu
        // doğrulayamaz. Ekran "çevrimdışı" rozetiyle bunu belirtiyor.
        "%06d".format((0..999_999).random()) to false
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
                else -> "Eşleştirme tamamlanamadı."
            }
        } catch (_: Exception) {
            // Ağ hatası. Eşleşme İŞARETLENMİYOR: kodu doğrulayan tek yer
            // sunucu, ona ulaşılamıyorsa doğrulanmış bir eşleşme yok.
            "Sunucuya ulaşılamadı. Eşleştirmeyi bağlantın olduğunda tekrar dene."
        }
    }

    /** Çocuğun **özeti** — ham veri değil. Sunucu yoksa `null`. */
    suspend fun cocukOzeti(): EbeveynOzetDto? = try {
        api.ebeveynOzeti()
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val KOD_UZUNLUK = 6
        const val KOD_BULUNAMADI = 404
        const val KOD_SURESI_DOLDU = 410
    }
}
