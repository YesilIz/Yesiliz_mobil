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
                ilerleme = NefesFazHesapla.ilerlemeyiHesapla(
                    it.seansSaniye,
                    gecenSaniye,
                    it.fazSaniye,
                ),
                oynatiliyor = true,
                tamamlandi = false,
                puanYazildi = false,
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
                ilerleme = NefesFazHesapla.ilerlemeyiHesapla(it.seansSaniye, gecenSaniye, yeniFazSaniye),
            )
        }
    }

    /**
     * Seans süresi seçici — [SEANS_SECENEKLERI] arasında döngüler.
     *
     * Tempodan farklı olarak sayacı **sıfırlıyor**: 5 dakikalık seansı
     * 1 dakikaya indirince geçen sürenin korunması, kullanıcıyı doğrudan
     * bitmiş bir seansa düşürürdü.
     */
    fun seansSuresiDegistir() {
        val guncelDk = _durum.value.seansSaniye / 60
        val indeks = SEANS_SECENEKLERI.indexOf(guncelDk)
        val yeniDk = SEANS_SECENEKLERI[(indeks + 1) % SEANS_SECENEKLERI.size]

        sayacIsi?.cancel()
        gecenSaniye = 0
        _durum.update {
            it.copy(
                seansSaniye = yeniDk * 60,
                ilerleme = NefesFazHesapla.ilerlemeyiHesapla(yeniDk * 60, 0, it.fazSaniye),
                oynatiliyor = true,
                tamamlandi = false,
                puanYazildi = false,
            )
        }
        sayaciBaslat()
    }

    private fun sayaciBaslat() {
        sayacIsi = viewModelScope.launch {
            while (gecenSaniye < _durum.value.seansSaniye) {
                delay(1_000)
                gecenSaniye++
                val ilerleme = NefesFazHesapla.ilerlemeyiHesapla(
                    _durum.value.seansSaniye,
                    gecenSaniye,
                    _durum.value.fazSaniye,
                )
                _durum.update { it.copy(ilerleme = ilerleme) }
                if (ilerleme.tamamlandiMi) {
                    // Puan yazımının SONUCU duruma giriyor: günlük limit
                    // dolmuşsa "+5 puan" demek yalan olurdu. Eskiden hiçbir
                    // şey söylenmiyordu (`debug.md` Y-8).
                    val yazildi = puanDeposu.gorevTamamla(Gorev.NEFES, LocalDate.now(saat))
                    _durum.update {
                        it.copy(oynatiliyor = false, tamamlandi = true, puanYazildi = yazildi)
                    }
                    break
                }
            }
        }
    }

    private companion object {
        val FAZ_SECENEKLERI = listOf(4, 5, 6)
    }
}
