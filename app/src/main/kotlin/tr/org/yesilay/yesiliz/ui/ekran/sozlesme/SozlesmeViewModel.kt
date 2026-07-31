package tr.org.yesilay.yesiliz.ui.ekran.sozlesme

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.depo.SozlesmeDeposu
import tr.org.yesilay.yesiliz.ui.gezinme.Rota
import javax.inject.Inject

@HiltViewModel
class SozlesmeViewModel @Inject constructor(
    private val depo: SozlesmeDeposu,
    kayitliDurum: SavedStateHandle,
) : ViewModel() {

    private val _durum = MutableStateFlow(SozlesmeDurum())
    val durum: StateFlow<SozlesmeDurum> = _durum.asStateFlow()

    init {
        val id = kayitliDurum.get<String>(Rota.SOZLESME_ARG).orEmpty()
        viewModelScope.launch {
            _durum.value = SozlesmeDurum(sozlesme = depo.sozlesme(id), yukleniyor = false)
        }
    }
}
