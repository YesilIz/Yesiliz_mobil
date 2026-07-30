package tr.org.yesilay.yesiliz.ui.ekran.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.yerel.dao.FarkindalikTestiDao
import tr.org.yesilay.yesiliz.data.yerel.entity.FarkindalikTestiEntity
import tr.org.yesilay.yesiliz.domain.test.FarkindalikTesti
import tr.org.yesilay.yesiliz.domain.test.TestSecenegi
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TestViewModel @Inject constructor(
    private val dao: FarkindalikTestiDao,
    private val saat: Clock,
) : ViewModel() {

    private val _durum = MutableStateFlow(TestDurum())
    val durum: StateFlow<TestDurum> = _durum.asStateFlow()

    init {
        viewModelScope.launch {
            dao.sonuncu()?.let { onceki ->
                _durum.update { it.copy(oncekiPuan = onceki.hamPuan, oncekiTarih = onceki.tarih) }
            }
        }
    }

    fun yanitla(soruIndeksi: Int, secenek: TestSecenegi) {
        _durum.update { durum ->
            durum.copy(
                yanitlar = durum.yanitlar.toMutableList().also { it[soruIndeksi] = secenek },
            )
        }
    }

    /**
     * Testi bitirir ve sonucu kaydeder.
     *
     * **Puan vermiyor** — görev kataloğu (`docs/spec-puan.md §7.1`) sabit ve
     * içinde "kendini test et" diye bir görev yok. Katalogu tek ekran için
     * genişletmek günlük tavanı (150) ve tüm dengeyi kaydırırdı.
     */
    fun bitir() {
        val yanitlar = _durum.value.yanitlar.filterNotNull()
        if (yanitlar.size != FarkindalikTesti.SORULAR.size) return

        val sonuc = FarkindalikTesti.sonucHesapla(yanitlar)
        _durum.update { it.copy(sonuc = sonuc) }

        viewModelScope.launch {
            dao.ekle(
                FarkindalikTestiEntity(
                    tarih = LocalDate.now(saat),
                    yanitlar = yanitlar.map { it.name },
                    hamPuan = sonuc.hamPuan,
                    seviye = sonuc.seviye.name,
                ),
            )
        }
    }

    /** Sonuç ekranından "tekrar çöz" — yanıtlar sıfırlanır. */
    fun bastanBasla() {
        _durum.update {
            TestDurum(oncekiPuan = it.sonuc?.hamPuan, oncekiTarih = LocalDate.now(saat))
        }
    }
}
