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
 *
 * Uygulama arka plana düşerse seans **duraklıyor** ([arkaPlanaGitti]) ve
 * dönüşte kaldığı yerden devam ediyor ([onPlanaDondu]); ayrılışı ekran
 * `EkranDonusTakibi` ile ayırt ediyor. Denetim olmadan seans arka planda
 * tamamlanıp puan yazıyordu — kullanıcı ekranı açıp HOME'a basarak 5 puan
 * alabiliyordu (4. tarama T4-010). Gerekçe [NefesSayac] KDoc'unda.
 */
@HiltViewModel
class NefesViewModel @Inject constructor(
    private val puanDeposu: PuanDeposu,
    private val saat: Clock,
) : ViewModel() {

    private val _durum = MutableStateFlow(NefesDurum())
    val durum: StateFlow<NefesDurum> = _durum.asStateFlow()

    private val sayac = NefesSayac()
    private var sayacIsi: Job? = null

    init {
        sayaciBaslat()
    }

    /** Oynat/duraklat düğmesi. */
    fun oynatDuraklat() {
        if (sayac.calisiyorMu) {
            sayacIsi?.cancel()
            sayac.duraklat()
        } else {
            sayac.devamEt()
            sayaciBaslat()
        }
        _durum.update { it.copy(oynatiliyor = sayac.calisiyorMu) }
    }

    /**
     * Uygulama arka plana düştü — seans duraklıyor.
     *
     * Ekran hâlâ açık ve kullanıcı egzersizi yapıyor olamaz; arka planda
     * geçen saniye tamamlanmaya sayılmamalı ve puan yazılmamalı
     * (4. tarama T4-010, [NefesSayac] KDoc'u).
     */
    fun arkaPlanaGitti() {
        if (_durum.value.tamamlandi) return
        sayacIsi?.cancel()
        sayac.arkaPlanaGitti()
        _durum.update { it.copy(oynatiliyor = false) }
    }

    /** Kullanıcı ekrana döndü — seans kaldığı yerden devam ediyor. */
    fun onPlanaDondu() {
        if (_durum.value.tamamlandi) return
        sayac.onPlanaDondu()
        if (sayac.calisiyorMu) sayaciBaslat()
        _durum.update { it.copy(oynatiliyor = sayac.calisiyorMu) }
    }

    /** Baştan başlat düğmesi. */
    fun bastanBaslat() {
        sayacIsi?.cancel()
        sayac.sifirla()
        _durum.update {
            it.copy(
                ilerleme = NefesFazHesapla.ilerlemeyiHesapla(
                    it.seansSaniye,
                    sayac.gecenSaniye,
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
                ilerleme = NefesFazHesapla.ilerlemeyiHesapla(
                    it.seansSaniye,
                    sayac.gecenSaniye,
                    yeniFazSaniye,
                ),
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
        sayac.sifirla()
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
        sayacIsi?.cancel()
        sayacIsi = viewModelScope.launch {
            while (!sayac.tamamlandiMi(_durum.value.seansSaniye)) {
                delay(1_000)
                // Duraklamış sayaçta saniye sayılmaz: arka planda geçen
                // süre tamamlanmaya girmemeli (T4-010).
                if (!sayac.tik()) break
                val ilerleme = NefesFazHesapla.ilerlemeyiHesapla(
                    _durum.value.seansSaniye,
                    sayac.gecenSaniye,
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
