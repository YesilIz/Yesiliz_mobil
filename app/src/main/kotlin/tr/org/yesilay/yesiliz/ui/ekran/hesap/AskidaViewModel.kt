package tr.org.yesilay.yesiliz.ui.ekran.hesap

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.uzak.AskiDurumu
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import javax.inject.Inject

/**
 * Askı ekranının "yeniden dene"si.
 *
 * Kimlikli bir çağrı yapmak yeterli: askı sürüyorsa sunucu yine `403`
 * dönüyor ve interceptor durumu koruyor; kalkmışsa yanıt başarılı geliyor
 * ve `AskiDurumu.temizle()` ekranı kapatıyor. Yani "askı kalktı mı?"
 * sorusunun ayrı bir ucu yok — cevabı zaten her yanıt taşıyor.
 *
 * `lig()` seçildi çünkü kimlik istiyor ve **rıza kapısına bağlı değil**
 * (K19): paylaşımı kapalı kullanıcı da bu ekranı kapatabilmeli.
 */
@HiltViewModel
class AskidaViewModel @Inject constructor(
    private val api: YesilizApi,
    askiDurumu: AskiDurumu,
) : ViewModel() {

    val hal: StateFlow<AskiDurumu.Hal?> = askiDurumu.hal

    /** Sunucu bakımda mı? `null` ise değil (M9, §5.7). */
    val bakim: StateFlow<String?> = askiDurumu.bakim

    private val _deneniyor = MutableStateFlow(false)
    val deneniyor: StateFlow<Boolean> = _deneniyor.asStateFlow()

    fun tekrarDene() {
        if (_deneniyor.value) return
        _deneniyor.value = true
        viewModelScope.launch {
            try {
                api.lig()
            } catch (iptal: CancellationException) {
                throw iptal
            } catch (hata: Exception) {
                // Beklenen durum: askı sürüyorsa 403. Durum güncellemesini
                // interceptor yapıyor, burada yalnızca iz bırakılıyor.
                Log.d(ETIKET, "Askı kontrolü başarısız", hata)
            } finally {
                _deneniyor.value = false
            }
        }
    }

    private companion object {
        const val ETIKET = "AskidaViewModel"
    }
}
