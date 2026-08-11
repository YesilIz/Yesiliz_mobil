package tr.org.yesilay.yesiliz.ui.ekran.anket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.arkaplan.IsPlanlayici
import tr.org.yesilay.yesiliz.data.depo.RiskDeposu
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.dao.AnketYanitDao
import tr.org.yesilay.yesiliz.data.yerel.entity.AnketYanitEntity
import tr.org.yesilay.yesiliz.risk.RiskHesapla
import tr.org.yesilay.yesiliz.ui.kalicilastirVeIlerle
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
 * ## Riski yeniden hesaplamak ZORUNLU
 *
 * Burası bir zamanlar "`sonuncu()` en yeniyi döndürüyor, ayrıca bir
 * tetikleme gerekmiyor" diyordu. Doğru ama yanıltıcıydı: o "bir sonraki
 * hesap" gecelik `GunlukOzetWorker`'dı. Ekranlar `risk_sonuc` tablosunu
 * okuyor (`AnaViewModel`, `RiskDetayViewModel` — `sonuncuAkisi()`), o
 * tabloya kimse yazmayınca kullanıcı anketi yeniliyor ve **bant ertesi
 * geceye kadar değişmiyordu**. [bitir] artık hesabı kendisi tetikliyor.
 */
@HiltViewModel
class AnketYenileViewModel @Inject constructor(
    private val dao: AnketYanitDao,
    private val tercihler: Tercihler,
    private val riskDeposu: RiskDeposu,
    private val isPlanlayici: IsPlanlayici,
    private val saat: Clock,
) : ViewModel() {

    private val _durum = MutableStateFlow(AnketYenileDurum())
    val durum: StateFlow<AnketYenileDurum> = _durum.asStateFlow()
    private var bitirmeIsi: Job? = null

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
        if (bitirmeIsi?.isActive == true) return
        val yanitlar = _durum.value.yanitlar
        if (yanitlar.any { it == 0 }) return

        val hamPuan = yanitlar.sum()
        val bugun = LocalDate.now(saat)

        bitirmeIsi = viewModelScope.launch {
            kalicilastirVeIlerle(
                kalicilastir = {
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

                    // Yeni anket puanı ancak `risk_sonuc`'a yazılınca ekranlara
                    // ulaşıyor. İkisi birlikte gerekiyor:
                    //  - `gunuHesapla` bugünün özeti VARSA anında günceller
                    //  - `simdiTazele` özet henüz yoksa onu da kurar.
                    riskDeposu.gunuHesapla(bugun)
                    isPlanlayici.simdiTazele()
                },
                ilerle = {
                    _durum.update { durum ->
                        durum.copy(
                            bitti = true,
                            hamPuan = hamPuan,
                            gecmis = durum.gecmis + (bugun to hamPuan.toDouble()),
                        )
                    }
                },
            )
        }
    }
}
