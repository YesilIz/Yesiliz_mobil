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
import tr.org.yesilay.yesiliz.data.saglik.AdimSayacOkuyucu
import tr.org.yesilay.yesiliz.data.saglik.SaglikDurumu
import tr.org.yesilay.yesiliz.data.saglik.SaglikOkuyucu
import javax.inject.Inject

data class SaglikIzinDurum(
    val saglikDurumu: SaglikDurumu = SaglikDurumu.YOK,
    val izinVarMi: Boolean = false,
    val arkaPlanIzniVarMi: Boolean = false,
    val istenenIzinler: Set<String> = emptySet(),
    val sayacCihazdaVarMi: Boolean = false,
    val sayacIzniVarMi: Boolean = false,
    val yukleniyor: Boolean = true,
) {
    val kullanilabilir: Boolean get() = saglikDurumu == SaglikDurumu.HAZIR

    /**
     * Donanım sayacı var ama izni yok.
     *
     * Health Connect'ten bağımsız bir eksiklik: Health Connect hiç kurulu
     * olmasa bile bu izin verilirse adım sayılıyor. O yüzden ekranda ayrı
     * bir satır olarak duruyor, Health Connect dallarının içine gömülmüyor.
     */
    val sayacIzniEksik: Boolean get() = sayacCihazdaVarMi && !sayacIzniVarMi

    /**
     * Okuma izni var ama arka plan izni yok — adım verisi yalnızca uygulama
     * açıkken okunabiliyor, gecelik özet boş kalıyor. Ekran bunu ayrı bir
     * uyarı olarak gösteriyor.
     */
    val arkaPlanEksik: Boolean get() = izinVarMi && !arkaPlanIzniVarMi
}

@HiltViewModel
class SaglikIzinViewModel @Inject constructor(
    private val okuyucu: SaglikOkuyucu,
    private val sayacOkuyucu: AdimSayacOkuyucu,
) : ViewModel() {

    private val _durum = MutableStateFlow(SaglikIzinDurum())
    val durum: StateFlow<SaglikIzinDurum> = _durum.asStateFlow()

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
                arkaPlanIzniVarMi = okuyucu.arkaPlanIzniVarMi(),
                istenenIzinler = okuyucu.istenenIzinler(),
                sayacCihazdaVarMi = sayacOkuyucu.cihazDestekliyor(),
                sayacIzniVarMi = sayacOkuyucu.izinVarMi(),
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
 *
 * İstenecek küme çağrı anında veriliyor: hangi izinlerin isteneceği cihazın
 * Health Connect sürümüne bağlı ve durum yüklenene kadar bilinmiyor.
 */
@Composable
fun saglikIzinIstegi(onSonuc: () -> Unit): (Set<String>) -> Unit {
    val baslatici = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { onSonuc() }
    return { izinler -> baslatici.launch(izinler) }
}
