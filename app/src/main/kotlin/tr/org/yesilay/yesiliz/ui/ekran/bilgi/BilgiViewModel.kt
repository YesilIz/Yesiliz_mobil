package tr.org.yesilay.yesiliz.ui.ekran.bilgi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.depo.BilgiDeposu
import tr.org.yesilay.yesiliz.domain.bilgi.BilgiBankasi
import javax.inject.Inject

@HiltViewModel
class BilgiViewModel @Inject constructor(
    private val depo: BilgiDeposu,
) : ViewModel() {

    private val _durum = MutableStateFlow(BilgiDurum())
    val durum: StateFlow<BilgiDurum> = _durum.asStateFlow()

    init {
        viewModelScope.launch {
            val yazilar = depo.yazilar()
            _durum.update {
                it.copy(
                    yazilar = yazilar,
                    kategoriler = BilgiBankasi.kategoriler(yazilar),
                    yukleniyor = false,
                )
            }
        }
    }

    /** Aynı çipe ikinci dokunuş filtreyi kaldırır. */
    fun kategoriSecildi(kategori: String) {
        _durum.update {
            it.copy(seciliKategori = if (it.seciliKategori == kategori) null else kategori)
        }
    }
}

/** Yazı detayı — id rotadan geliyor. */
@HiltViewModel
class BilgiYaziViewModel @Inject constructor(
    depo: BilgiDeposu,
    kayitliDurum: SavedStateHandle,
) : ViewModel() {

    private val _durum = MutableStateFlow(BilgiYaziDurum())
    val durum: StateFlow<BilgiYaziDurum> = _durum.asStateFlow()

    init {
        val id: String? = kayitliDurum[YAZI_ID]
        viewModelScope.launch {
            _durum.update {
                it.copy(yazi = id?.let { anahtar -> depo.yazi(anahtar) }, yukleniyor = false)
            }
        }
    }

    companion object {
        const val YAZI_ID = "yaziId"
    }
}
