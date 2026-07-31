package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.JetonDeposu
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb
import tr.org.yesilay.yesiliz.domain.hesap.HesapDeposu
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * "Verilerimi kalıcı sil" (ekran 25) ve "Hesabımı kalıcı sil" (ekran 26).
 *
 * **Artık aynı işlem değiller** — eskiden ikisi de tek fonksiyonu çağırıyordu
 * ve o fonksiyon ana iş parçacığında `clearAllTables()` çağırdığı için
 * uygulamayı çökertip hiçbir şey silmiyordu (`debug.md` K-2).
 *
 * | | verileri sil | hesabı sil |
 * |---|---|---|
 * | Room (12 tablo) | ✓ | ✓ |
 * | `Tercihler` DataStore | ✓ | ✓ |
 * | JWT (`jetonlar` DataStore) | ✓ | ✓ |
 * | Sunucudaki hesap | — | ✓ (`DELETE /auth/hesap`) |
 *
 * Jetonlar iki akışta da gidiyor: [Tercihler.sifirla] oturumu zaten
 * siliyor, geriye geçerli bir JWT bırakmak "oturum yok ama jeton var" gibi
 * tutarsız bir duruma düşürürdü.
 *
 * **Dispatcher şart.** `clearAllTables()` askıya alınabilir değil; Room ana
 * iş parçacığında veritabanı erişimini `IllegalStateException` ile
 * reddediyor ve `viewModelScope` varsayılan olarak `Main.immediate`.
 */
@Singleton
class VeriYonetimiDeposu @Inject constructor(
    private val db: YesilizDb,
    private val tercihler: Tercihler,
    private val jetonDeposu: JetonDeposu,
    private val kategoriDeposu: KategoriDeposu,
    // `Provider`: `HesapDeposu` → `YedekliHesapDeposu` zinciri ağ katmanını
    // kuruyor; onu silme anına kadar geciktirmek grafiği hafif tutuyor.
    private val hesapDeposu: Provider<HesapDeposu>,
) {

    /** Cihazdaki tüm veri. Sunucudaki hesap **kalır**. */
    suspend fun tumVerileriSil() = withContext(Dispatchers.IO) {
        yereliTemizle()
    }

    /**
     * Cihazdaki tüm veri **ve** sunucudaki hesap.
     *
     * @return sunucudaki kayıt gerçekten silindi mi. `false` ise cihaz
     *   temizlendi ama sunucuda kopya kaldı; ekran bunu söylemek zorunda.
     */
    suspend fun hesabiSil(): Boolean = withContext(Dispatchers.IO) {
        // Sunucu çağrısı yerel temizlikten ÖNCE: jeton silinirse kimlik
        // kaybolur ve hesap bir daha silinemez.
        val sunucudanSilindi = hesapDeposu.get().hesabiSil()
        yereliTemizle()
        sunucudanSilindi
    }

    private suspend fun yereliTemizle() {
        db.clearAllTables()
        tercihler.sifirla()
        jetonDeposu.temizle()

        // `clearAllTables` kategori tablosunun 3.106 seed satırını da
        // siliyor. `seedEt` normalde yalnızca `AcilisViewModel.init`'te
        // koşuyor; burada yeniden çağrılmazsa aynı oturumda her uygulama
        // "kategorisiz" kalır ve risk hesabı bozulur.
        kategoriDeposu.seedEt(zorla = true)
    }
}
