package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Devam eden yürüyüş oturumu — servis ile ekran arasındaki tek gerçek.
 *
 * ## Niçin ayrı bir depo
 *
 * Sayaç eskiden `YuruyusViewModel`'in içindeydi ve `viewModelScope`'ta
 * koşuyordu: ekran kapanınca ya da uygulamadan çıkınca **sayaç
 * duruyordu** (`debug.md` İ-3). Kullanıcı telefonu cebine koyup yürüdüğünde
 * hiçbir şey sayılmıyordu — yani "aktif yürüyüş" özelliği tam da
 * kullanılacağı anda çalışmıyordu.
 *
 * Sayaç artık `YuruyusServisi`'nde. Servis ile ekranın ortak durumu
 * burada duruyor: servis yazıyor, ViewModel okuyor. `@Singleton` olduğu
 * için ekran kapanıp açılsa da aynı örnek.
 */
@Singleton
class YuruyusOturumDeposu @Inject constructor() {

    private val _oturum = MutableStateFlow(YuruyusOturum())
    val oturum: StateFlow<YuruyusOturum> = _oturum.asStateFlow()

    fun basladi() {
        _oturum.value = YuruyusOturum(calisiyor = true)
    }

    fun tikla(gecenSaniye: Int, adim: Int?) {
        _oturum.update { it.copy(gecenSaniye = gecenSaniye, adim = adim ?: it.adim) }
    }

    fun duraklat() {
        _oturum.update { it.copy(calisiyor = false) }
    }

    fun surdur() {
        _oturum.update { it.copy(calisiyor = true) }
    }

    fun tamamlandi(hedefTamamlandi: Boolean, puanVerildi: Boolean) {
        _oturum.update {
            it.copy(
                calisiyor = false,
                tamamlandi = true,
                hedefTamamlandi = hedefTamamlandi,
                puanVerildi = puanVerildi,
            )
        }
    }

    fun sifirla() {
        _oturum.value = YuruyusOturum()
    }
}

/**
 * @param gecenSaniye oturum başladığından beri geçen süre
 * @param adim Health Connect'ten okunan fark; veri yoksa `null`
 */
data class YuruyusOturum(
    val calisiyor: Boolean = false,
    val gecenSaniye: Int = 0,
    val adim: Int? = null,
    val tamamlandi: Boolean = false,
    val hedefTamamlandi: Boolean = false,
    val puanVerildi: Boolean = false,
)
