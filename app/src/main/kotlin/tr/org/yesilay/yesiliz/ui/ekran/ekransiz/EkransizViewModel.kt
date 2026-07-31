package tr.org.yesilay.yesiliz.ui.ekran.ekransiz

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
class EkransizViewModel @Inject constructor(
    private val puanDeposu: PuanDeposu,
    private val saat: Clock,
) : ViewModel() {

    private val _durum = MutableStateFlow(EkransizDurum())
    val durum: StateFlow<EkransizDurum> = _durum.asStateFlow()

    fun sec(secenek: EkransizSecenek) {
        if (_durum.value.kaydedildi) return
        _durum.update { it.copy(secilen = secenek) }
    }

    /**
     * Kaydeder.
     *
     * Seçimin KENDİSİ hiçbir yere yazılmıyor -- yalnızca görev
     * tamamlanıyor. Ne yaptığını bilmemize gerek yok; amaç kullanıcının
     * kendisine görünür kılmak (`EkransizSecenek` kdoc'u).
     */
    fun kaydet() {
        if (_durum.value.secilen == null || _durum.value.kaydedildi) return
        viewModelScope.launch {
            val yazildi = puanDeposu.gorevTamamla(Gorev.EKRANSIZ, LocalDate.now(saat))
            _durum.update { it.copy(kaydedildi = true, puanYazildi = yazildi) }
        }
    }
}
