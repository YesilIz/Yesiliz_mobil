package tr.org.yesilay.yesiliz.ui.ekran.yuruyus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tr.org.yesilay.yesiliz.data.depo.YuruyusOturumDeposu
import javax.inject.Inject

/**
 * Aktif yürüyüş — ekranın durum kaynağı.
 *
 * **Sayaç burada değil, [tr.org.yesilay.yesiliz.arkaplan.YuruyusServisi]'nde.**
 * Eskiden `viewModelScope`'ta koşuyordu ve ekran kapanınca duruyordu
 * (`debug.md` İ-3, §12 K15 tersine çevrildi). ViewModel artık yalnızca
 * servisin yazdığı oturumu okuyup ekranın anlayacağı biçime çeviriyor.
 *
 * Servisi başlatmak/durdurmak **Composable'ın işi**: `Context` gerekiyor
 * ve ViewModel Context tutmuyor (CLAUDE.md §6) -- `ProfilEkran`'daki
 * overlay izni isteğiyle aynı desen.
 */
@HiltViewModel
class YuruyusViewModel @Inject constructor(
    private val oturumDeposu: YuruyusOturumDeposu,
) : ViewModel() {

    val durum: StateFlow<YuruyusDurum> = oturumDeposu.oturum
        .map { oturum ->
            val ilerleme = YuruyusHesapla.ilerleme(oturum.gecenSaniye)
            YuruyusDurum(
                calisiyorMu = oturum.calisiyor,
                gecenSaniye = ilerleme.gecenSaniye,
                kalanMetni = YuruyusHesapla.sayacMetni(ilerleme.kalanSaniye),
                oran = ilerleme.oran,
                kazanilanPuan = ilerleme.kazanilanPuan,
                tamamlandiMi = ilerleme.tamamlandiMi || oturum.tamamlandi,
                adim = oturum.adim,
                puanVerildiMi = oturum.puanVerildi,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
            initialValue = YuruyusDurum(
                kalanMetni = YuruyusHesapla.sayacMetni(
                    YuruyusHesapla.HEDEF_DAKIKA * SANIYE_DAKIKA,
                ),
            ),
        )

    /**
     * Oturumu sıfırlar.
     *
     * Servisi durdurmak Composable'ın işi (Context gerekiyor); burada
     * yalnızca paylaşılan durum temizleniyor.
     */
    fun sifirla() = oturumDeposu.sifirla()

    private companion object {
        const val AKIS_BEKLEME_MS = 5_000L
        const val SANIYE_DAKIKA = 60
    }
}
