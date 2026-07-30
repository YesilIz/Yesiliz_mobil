package tr.org.yesilay.yesiliz.ui.ekran.lig

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lig durumu.
 *
 * Backend §11'de en sonda; mock repository deseni sayesinde demo günü bu sekme
 * de çalışıyor (CLAUDE.md §12 K7). `RetrofitLigDeposu` geldiğinde yalnızca
 * bu sınıfın veri kaynağı değişir, ekran ve durum modeli aynı kalır.
 */
@HiltViewModel
class LigViewModel @Inject constructor() : ViewModel() {

    private val _durum = MutableStateFlow(sahteLig())
    val durum: StateFlow<LigDurum> = _durum.asStateFlow()

    private fun sahteLig() = LigDurum(
        ligAdi = "Filiz Ligi",
        kalanGun = 5,
        siralama = listOf(
            LigSatiriModel(1, "Elif", 1_420),
            LigSatiriModel(2, "Mert", 1_380),
            LigSatiriModel(3, "Zeynep", 1_310),
            LigSatiriModel(4, "Ahmet", 1_275),
            LigSatiriModel(5, "Buse", 1_220),
            LigSatiriModel(6, "Kerem", 1_190),
            LigSatiriModel(7, "İrem", 1_140),
            LigSatiriModel(8, "Turan", 1_095, benMiyim = true),
            LigSatiriModel(9, "Deniz", 1_040),
            LigSatiriModel(10, "Ada", 980),
        ),
    )
}
