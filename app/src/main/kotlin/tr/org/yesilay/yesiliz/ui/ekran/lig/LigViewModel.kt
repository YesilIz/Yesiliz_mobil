package tr.org.yesilay.yesiliz.ui.ekran.lig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.domain.lig.LigDeposu
import javax.inject.Inject

/**
 * Lig durumu.
 *
 * Sıralama artık [LigDeposu] arayüzünden geliyor (CLAUDE.md §12 K7); gömülü
 * sabit liste `YerelSahteLigDeposu`'na taşındı. Faz 2'de `RetrofitLigDeposu`
 * bağlanınca bu sınıf **hiç değişmiyor**.
 */
@HiltViewModel
class LigViewModel @Inject constructor(
    private val depo: LigDeposu,
) : ViewModel() {

    val durum: StateFlow<LigDurum> = depo.ligAkisi()
        .map { ozet ->
            LigDurum(
                ligAdi = ozet.ligAdi,
                kalanGun = ozet.kalanGun,
                yukselen = ozet.yukselen,
                dusen = ozet.dusen,
                siralama = ozet.siralama.map {
                    LigSatiriModel(
                        sira = it.sira,
                        isim = it.isim,
                        puan = it.puan,
                        benMiyim = it.benMiyim,
                        arkadasMi = it.arkadasMi,
                    )
                },
                katildiMi = ozet.katildiMi,
                gorunuyorum = ozet.gorunuyorum,
                yukleniyor = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
            initialValue = LigDurum(),
        )

    fun ligeKatil() {
        viewModelScope.launch { depo.ligeKatil() }
    }

    private companion object {
        const val AKIS_BEKLEME_MS = 5_000L
    }
}

/** Arkadaş ekle — tasarım 36. */
@HiltViewModel
class ArkadasEkleViewModel @Inject constructor(
    private val depo: LigDeposu,
) : ViewModel() {

    private val _durum = MutableStateFlow(ArkadasEkleDurum())
    val durum: StateFlow<ArkadasEkleDurum> = _durum.asStateFlow()

    init {
        yenile()
    }

    private fun yenile() {
        viewModelScope.launch {
            _durum.update {
                it.copy(
                    davetKodu = depo.davetKodu(),
                    onerilenler = depo.onerilenler(),
                    yukleniyor = false,
                )
            }
        }
    }

    fun davetGonder(isim: String) {
        viewModelScope.launch {
            depo.davetGonder(isim)
            yenile()
        }
    }

    /** Panoya kopyalama Composable'da; burada yalnızca geri bildirim. */
    fun kopyalandiIsaretle() {
        _durum.update { it.copy(kopyalandi = true) }
    }
}
