package tr.org.yesilay.yesiliz.ui.ekran.gozmolasi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** 20-20-20 göz molası. Nefes ekranıyla aynı desen, tek fazlı. */
@HiltViewModel
class GozMolasiViewModel @Inject constructor(
    private val puanDeposu: PuanDeposu,
    private val saat: Clock,
) : ViewModel() {

    private val _durum = MutableStateFlow(GozMolasiDurum())
    val durum: StateFlow<GozMolasiDurum> = _durum.asStateFlow()

    private var sayacIsi: Job? = null

    fun basla() {
        if (_durum.value.basladi) return
        _durum.update { it.copy(basladi = true) }
        sayacIsi = viewModelScope.launch {
            var gecen = 0
            while (gecen < GozMolasiHesapla.BAKIS_SANIYE) {
                delay(1_000)
                gecen++
                _durum.update { it.copy(ilerleme = GozMolasiHesapla.ilerleme(gecen)) }
            }
            val yazildi = puanDeposu.gorevTamamla(Gorev.GOZ_MOLASI, LocalDate.now(saat))
            _durum.update { it.copy(puanYazildi = yazildi) }
        }
    }

    fun bastanBaslat() {
        sayacIsi?.cancel()
        _durum.value = GozMolasiDurum()
    }
}
