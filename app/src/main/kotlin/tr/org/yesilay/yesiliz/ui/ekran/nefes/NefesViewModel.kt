package tr.org.yesilay.yesiliz.ui.ekran.nefes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

/**
 * Nefes egzersizi — kutu nefesi sayacı.
 *
 * Süre sayacı `viewModelScope` içinde çalışıyor ki ekran döndürülünce/
 * konfigürasyon değişince sıfırlanmasın. Tamamlanınca [Gorev.NEFES] bir kez
 * tamamlanır ([PuanDeposu.gorevTamamla] günlük limiti zaten kendi içinde
 * koruyor — 2. kez günde limit doluysa sessizce no-op).
 *
 * Erken kapatma (× butonu) puan **vermiyor** — bitirmek gerekiyor,
 * "sadece kazanç" ilkesiyle tutarlı (CLAUDE.md §4).
 */
@HiltViewModel
class NefesViewModel @Inject constructor(
    private val puanDeposu: PuanDeposu,
    private val saat: Clock,
) : ViewModel() {

    private val _durum = MutableStateFlow(NefesDurum())
    val durum: StateFlow<NefesDurum> = _durum.asStateFlow()

    private var gecenSaniye = 0
    private var sayacIsi: Job? = null

    init {
        sayaciBaslat()
    }

    /** Oynat/duraklat düğmesi. */
    fun oynatDuraklat() {
        if (_durum.value.oynatiliyor) {
            sayacIsi?.cancel()
            _durum.update { it.copy(oynatiliyor = false) }
        } else {
            _durum.update { it.copy(oynatiliyor = true) }
            sayaciBaslat()
        }
    }

    /** Baştan başlat düğmesi. */
    fun bastanBaslat() {
        sayacIsi?.cancel()
        gecenSaniye = 0
        _durum.update {
            it.copy(
                ilerleme = NefesFazHesapla.ilerlemeyiHesapla(TOPLAM_SANIYE, gecenSaniye, it.fazSaniye),
                oynatiliyor = true,
            )
        }
        sayaciBaslat()
    }

    /**
     * Tempo seçici — [FAZ_SECENEKLERI] arasında döngüler.
     *
     * Devam eden oturumu sıfırlamıyor: [NefesFazHesapla] geçen saniyeyi
     * yeni tempoyla anında yeniden yorumluyor.
     */
    fun fazSuresiDegistir() {
        val guncelIndeks = FAZ_SECENEKLERI.indexOf(_durum.value.fazSaniye)
        val yeniFazSaniye = FAZ_SECENEKLERI[(guncelIndeks + 1) % FAZ_SECENEKLERI.size]
        _durum.update {
            it.copy(
                fazSaniye = yeniFazSaniye,
                ilerleme = NefesFazHesapla.ilerlemeyiHesapla(TOPLAM_SANIYE, gecenSaniye, yeniFazSaniye),
            )
        }
    }

    private fun sayaciBaslat() {
        sayacIsi = viewModelScope.launch {
            while (gecenSaniye < TOPLAM_SANIYE) {
                delay(1_000)
                gecenSaniye++
                val ilerleme = NefesFazHesapla.ilerlemeyiHesapla(TOPLAM_SANIYE, gecenSaniye, _durum.value.fazSaniye)
                _durum.update { it.copy(ilerleme = ilerleme) }
                if (ilerleme.tamamlandiMi) {
                    _durum.update { it.copy(oynatiliyor = false) }
                    puanDeposu.gorevTamamla(Gorev.NEFES, LocalDate.now(saat))
                    break
                }
            }
        }
    }

    private companion object {
        /** `Gorev.NEFES.aciklama` = "2 dakikalık kutu nefesi" ile birebir. */
        const val TOPLAM_SANIYE = 120

        val FAZ_SECENEKLERI = listOf(4, 5, 6)
    }
}
