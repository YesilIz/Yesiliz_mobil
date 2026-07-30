package tr.org.yesilay.yesiliz.ui.ekran.anket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.dao.AnketYanitDao
import tr.org.yesilay.yesiliz.data.yerel.entity.AnketYanitEntity
import tr.org.yesilay.yesiliz.risk.RiskHesapla
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * SAS-SV yeniden değerlendirmesi.
 *
 * Yazma bloğu `OnboardingViewModel.tamamla()` ile **aynı**: aynı entity, aynı
 * `RiskHesapla.anketSkoru`, aynı `SON_ANKET` tarihi. Ortak bir yardımcıya
 * çıkarmak yerine tekrarlandı çünkü onboarding'in yazması hedef ve
 * onboarding bayrağıyla iç içe; ayırmak o akışı da değiştirmeyi gerektirirdi.
 *
 * `AnketYanitDao.sonuncu()` **en yeniyi** döndürüyor, yani `RiskDeposu`
 * bir sonraki hesapta otomatik olarak bu yanıtı kullanıyor — ayrıca bir
 * tetikleme gerekmiyor.
 */
@HiltViewModel
class AnketYenileViewModel @Inject constructor(
    private val dao: AnketYanitDao,
    private val tercihler: Tercihler,
    private val saat: Clock,
) : ViewModel() {

    private val _durum = MutableStateFlow(AnketYenileDurum())
    val durum: StateFlow<AnketYenileDurum> = _durum.asStateFlow()

    init {
        viewModelScope.launch {
            val gecmis = dao.hepsi()
            _durum.update { durum ->
                durum.copy(
                    oncekiPuan = gecmis.lastOrNull()?.hamPuan,
                    oncekiTarih = gecmis.lastOrNull()?.tarih,
                    gecmis = gecmis.map { it.tarih to it.hamPuan },
                )
            }
        }
    }

    fun yanitla(madde: Int, deger: Int) {
        _durum.update { durum ->
            durum.copy(yanitlar = durum.yanitlar.toMutableList().also { it[madde] = deger })
        }
    }

    fun ilerle() {
        _durum.update { if (it.sonSayfaMi) it else it.copy(sayfa = it.sayfa + 1) }
    }

    fun geriSayfa(): Boolean {
        val durum = _durum.value
        if (durum.sayfa == 0) return false
        _durum.update { it.copy(sayfa = it.sayfa - 1) }
        return true
    }

    /**
     * Tamamlar ve kaydeder.
     *
     * Eksik yanıtla kayıt **yapılmıyor** — `docs/spec-anket.md §6.4`:
     * "10 madde zorunlu, ortalama ile doldurma YOK".
     */
    fun bitir() {
        val yanitlar = _durum.value.yanitlar
        if (yanitlar.any { it == 0 }) return

        val hamPuan = yanitlar.sum()
        val bugun = LocalDate.now(saat)

        _durum.update { it.copy(bitti = true, hamPuan = hamPuan) }

        viewModelScope.launch {
            dao.ekle(
                AnketYanitEntity(
                    tarih = bugun,
                    madde1 = yanitlar[0], madde2 = yanitlar[1], madde3 = yanitlar[2],
                    madde4 = yanitlar[3], madde5 = yanitlar[4], madde6 = yanitlar[5],
                    madde7 = yanitlar[6], madde8 = yanitlar[7], madde9 = yanitlar[8],
                    madde10 = yanitlar[9],
                    hamPuan = hamPuan.toDouble(),
                    rS = RiskHesapla.anketSkoru(hamPuan.toDouble()),
                ),
            )
            tercihler.anketTarihiniKaydet(bugun)
            _durum.update { durum ->
                durum.copy(gecmis = durum.gecmis + (bugun to hamPuan.toDouble()))
            }
        }
    }
}
