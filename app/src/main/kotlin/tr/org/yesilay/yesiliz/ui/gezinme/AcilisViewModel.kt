package tr.org.yesilay.yesiliz.ui.gezinme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.arkaplan.IsPlanlayici
import tr.org.yesilay.yesiliz.data.depo.KategoriDeposu
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Açılışta hangi rotadan başlanacağını belirler.
 *
 * Onboarding'i bitiren kullanıcı uygulamayı her açtığında karşılama ekranını
 * yeniden görmemeli. Bayrak DataStore'da; okunana kadar `null` dönüyor ve
 * NavHost hiç kurulmuyor — yanlış rotayla kurulup sonra atlamak ekranın
 * bir kare boyunca yanlış içerik göstermesine yol açardı.
 *
 * Ayrıca ilk açılışın iki yan işini burada yapıyor:
 *  - kategori tablosunu Room'a seed etmek (3.106 satır, yalnızca bir kez)
 *  - kurulum tarihini işaretlemek (alpha eğrisinin `d` girdisi)
 */
@HiltViewModel
class AcilisViewModel @Inject constructor(
    private val tercihler: Tercihler,
    private val kategoriDeposu: KategoriDeposu,
    private val isPlanlayici: IsPlanlayici,
    private val saat: Clock,
) : ViewModel() {

    /**
     * Hesap akışının bitince onboarding'e mi, mevcut profile mi döneceğinin
     * tek doğruluk kaynağı. Oturum durumu bu karar için yeterli değil: çıkış
     * ve yeniden giriş sırasında değişiyor, onboarding tamamlanma durumu ise
     * cihazdaki kurulum/veri sürekliliğini temsil ediyor.
     */
    val onboardingBitti: StateFlow<Boolean?> = tercihler.onboardingBitti
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    /**
     * Açılış rotası: hesap → onboarding → ana.
     *
     * Hesap ekranı yalnızca **ilk açılışta** görünüyor; misafir seçen
     * kullanıcıya bir daha sorulmuyor (`Oturum.karsilamaGerekliMi`).
     */
    val baslangicRotasi: StateFlow<String?> = combine(
        onboardingBitti,
        tercihler.oturum,
    ) { onboardingBitti, oturum ->
        when {
            onboardingBitti == null -> null
            oturum.karsilamaGerekliMi -> Rota.GIRIS
            onboardingBitti -> Rota.ANA
            else -> Rota.ONBOARDING
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    init {
        isPlanlayici.gecelikPlanla()

        viewModelScope.launch {
            kategoriDeposu.seedEt()
            // Güvenli varsayılan: izin bu anda verilmemiş olabilir, gerçek sayı
            // `GecmisVeriDeposu.hesaplaGerekirse` tarafından izin onaylandıktan
            // sonra ayrıca yazılıyor (`Tercihler.gecmisGunuKaydet`).
            tercihler.kurulumuIsaretle(LocalDate.now(saat), gecmisGun = 0)

            // Tazeleme kurulum tarihi YAZILDIKTAN SONRA. `simdiTazele`
            // burada `init`'in başındaydı ve worker `ilkKurulumTarihi`'ni
            // henüz `null` görüyordu: kurulumdan önceki günleri eleyen
            // filtre ilk koşuda hiç uygulanmıyor, telefonun eski kullanım
            // istatistiklerinden 7 günlük satır yazılıyordu. Temiz
            // kurulumda kullanıcı hâlâ kazanmadığı bir seri ve puan
            // görüyordu (`debug.md` K-4'ün kalıntısı).
            isPlanlayici.simdiTazele()
        }
    }
}
