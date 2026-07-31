package tr.org.yesilay.yesiliz.ui.ekran.saglik

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.saglik.SaglikDurumu
import tr.org.yesilay.yesiliz.data.saglik.SaglikOkuyucu
import javax.inject.Inject

data class SaglikIzinDurum(
    val saglikDurumu: SaglikDurumu = SaglikDurumu.YOK,
    val izinVarMi: Boolean = false,
    val yukleniyor: Boolean = true,
) {
    val kullanilabilir: Boolean get() = saglikDurumu == SaglikDurumu.HAZIR
}

@HiltViewModel
class SaglikIzinViewModel @Inject constructor(
    private val okuyucu: SaglikOkuyucu,
) : ViewModel() {

    private val _durum = MutableStateFlow(SaglikIzinDurum())
    val durum: StateFlow<SaglikIzinDurum> = _durum.asStateFlow()

    /** İzin sözleşmesi Composable tarafında kuruluyor — ViewModel Context tutmaz. */
    val gerekliIzinler: Set<String> = okuyucu.gerekliIzinler

    init {
        yenile()
    }

    /**
     * Durumu tazeler.
     *
     * İzin diyaloğundan dönüşte çağrılıyor: Health Connect izni sistem
     * ekranında veriliyor, sonuç geri geldiğinde ekranın kendini
     * güncellemesi gerekiyor.
     */
    fun yenile() {
        viewModelScope.launch {
            _durum.value = SaglikIzinDurum(
                saglikDurumu = okuyucu.durum(),
                izinVarMi = okuyucu.izinVarMi(),
                yukleniyor = false,
            )
        }
    }
}

/**
 * Health Connect izin isteği.
 *
 * `PermissionController.createRequestPermissionResultContract()` sistem
 * ekranını açıyor; sonuç `onSonuc` ile ViewModel'e dönüyor. Sözleşme
 * Composable içinde kuruluyor çünkü `ActivityResultLauncher` bir Activity
 * bağlamı istiyor ve ViewModel Context tutmuyor (CLAUDE.md §6).
 */
@Composable
fun saglikIzinIstegi(izinler: Set<String>, onSonuc: () -> Unit): () -> Unit {
    val baslatici = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { onSonuc() }
    return { baslatici.launch(izinler) }
}
