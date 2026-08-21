package tr.org.yesilay.yesiliz.ui.ekran.yedam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.depo.YedamDeposu
import javax.inject.Inject

/**
 * Erişim kaydı ve rıza geri çekme (M5, M6).
 *
 * Rızanın varlığı **sunucudan** okunuyor, cihazda bayrak tutulmuyor:
 * yerel bir kopya, sunucuda süresi dolmuş bir rızayı "duruyor" göstermeye
 * yol açardı (CLAUDE.md §12 K25 ailesi).
 */
@HiltViewModel
class YedamGizlilikViewModel @Inject constructor(
    private val depo: YedamDeposu,
) : ViewModel() {

    private val _durum = MutableStateFlow(YedamGizlilikDurum())
    val durum: StateFlow<YedamGizlilikDurum> = _durum.asStateFlow()

    init {
        tazele()
    }

    fun tazele() {
        viewModelScope.launch {
            _durum.value = _durum.value.copy(yukleniyor = true, cevrimdisi = false)
            val kayitlar = depo.erisimKaydim()
            val basvuru = depo.durum()
            _durum.value = _durum.value.copy(
                yukleniyor = false,
                // Sunucuya ulaşılamadı: boş listeyle karıştırmamak şart,
                // "kimse bakmamış" ile "bilmiyoruz" aynı şey değil.
                cevrimdisi = kayitlar == null,
                kayitlar = kayitlar.orEmpty().map { ErisimGorunumu(it.zaman, it.kapsam) },
                rizaVar = basvuru?.rizaSonGecerlilik != null,
            )
        }
    }

    /**
     * Rızayı geri çeker.
     *
     * Başarı **sunucudan** geliyor. Ağ hatasında "geri çektin" demek,
     * erişim sürerken kesildiğini söylemek olurdu — eşleştirmenin
     * çevrimdışı yedeğe düşmemesiyle aynı gerekçe (K25).
     */
    fun geriCek() {
        if (_durum.value.geriCekiliyor) return
        _durum.value = _durum.value.copy(geriCekiliyor = true, geriCekilemedi = false)
        viewModelScope.launch {
            val oldu = depo.rizayiGeriCek()
            _durum.value = _durum.value.copy(
                geriCekiliyor = false,
                geriCekildi = oldu,
                geriCekilemedi = !oldu,
                rizaVar = if (oldu) false else _durum.value.rizaVar,
            )
            if (oldu) tazele()
        }
    }
}
