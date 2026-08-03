package tr.org.yesilay.yesiliz.ui.ekran.gecekapanisi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel
class GeceKapanisiViewModel @Inject constructor(
    private val puanDeposu: PuanDeposu,
    private val saat: Clock,
) : ViewModel() {

    private val _durum = MutableStateFlow(GeceKapanisiDurum())
    val durum: StateFlow<GeceKapanisiDurum> = _durum.asStateFlow()

    fun adimDegis(indeks: Int) {
        _durum.update {
            val yeni = if (indeks in it.isaretliler) {
                it.isaretliler - indeks
            } else {
                it.isaretliler + indeks
            }
            it.copy(isaretliler = yeni)
        }
    }

    fun tamamla() {
        if (!_durum.value.hepsiIsaretli || _durum.value.tamamlandi) return
        viewModelScope.launch {
            val yazildi = puanDeposu.gorevTamamla(Gorev.GECE_KAPANISI, LocalDate.now(saat))
            _durum.update { it.copy(tamamlandi = true, puanYazildi = yazildi) }
        }
    }
}
