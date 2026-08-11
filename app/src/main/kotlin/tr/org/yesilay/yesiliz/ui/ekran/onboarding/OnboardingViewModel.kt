package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import android.content.Context
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
import tr.org.yesilay.yesiliz.data.kullanim.KullanimIzni
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.dao.AnketYanitDao
import tr.org.yesilay.yesiliz.data.yerel.entity.AnketYanitEntity
import tr.org.yesilay.yesiliz.risk.RiskHesapla
import tr.org.yesilay.yesiliz.ui.kalicilastirVeIlerle
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Onboarding akışı — 7 ekranın ortak durumu.
 *
 * Akış bitince [tamamla] iki yere yazıyor:
 *  - `anket_yanit` (Room) — 10 madde + ham puan + `rS`
 *  - `Tercihler` (DataStore) — hedef, son anket tarihi, "bitti" bayrağı
 *
 * Son anket tarihi kritik: `RiskDeposu` bunu `AnketGirdi.kacGunOnce`'a
 * çevirip alpha eğrisine veriyor, anket 60 günde tamamen sönüyor.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val anketDao: AnketYanitDao,
    private val tercihler: Tercihler,
    private val riskDeposu: RiskDeposu,
    private val isPlanlayici: IsPlanlayici,
    private val saat: Clock,
) : ViewModel() {

    private val _durum = MutableStateFlow(OnboardingDurum())
    val durum: StateFlow<OnboardingDurum> = _durum.asStateFlow()
    private var tamamlamaIsi: Job? = null

    fun yasDegisti(delta: Int) {
        _durum.update { it.copy(yas = (it.yas + delta).coerceIn(YAS_ALT, YAS_UST)) }
    }

    fun cinsiyetSecildi(cinsiyet: Cinsiyet) {
        _durum.update { it.copy(cinsiyet = cinsiyet) }
    }

    /** 18 yaş altı ebeveyn onayı beyanı (`debug.md` Y-11). */
    fun ebeveynOnayiDegisti(onayli: Boolean) {
        _durum.update { it.copy(ebeveynOnayiVerildi = onayli) }
    }

    fun yanitVerildi(madde: Int, deger: Int) {
        _durum.update { durum ->
            durum.copy(
                yanitlar = durum.yanitlar.toMutableList().also { it[madde] = deger },
            )
        }
    }

    /** `IzinEkran`'ın `ON_RESUME` gözlemcisinden çağrılır — Ayarlar'dan dönüşte günceller. */
    fun izinKontrolEt(baglam: Context) {
        _durum.update { it.copy(izinVerildiMi = KullanimIzni.verildiMi(baglam)) }
    }

    fun hedefDegisti(dakika: Int) {
        _durum.update { it.copy(hedefDakika = dakika.coerceIn(HEDEF_ALT, it.mevcutOrtalama)) }
    }

    /** Hızlı seçim çipleri: mevcut ortalamadan −15 / −30 / −45 dk. */
    fun hizliHedef(dusus: Int) {
        _durum.update { it.copy(hedefDakika = (it.mevcutOrtalama - dusus).coerceAtLeast(HEDEF_ALT)) }
    }

    /**
     * Akış sonu — anketi ve tercihleri kalıcılaştırır.
     *
     * Anket **eksikse yazılmaz** (`docs/spec-anket.md §6.4`: 10 madde zorunlu,
     * ortalamayla doldurma yok). Kullanıcı anketi atladıysa yalnızca hedef ve
     * "bitti" bayrağı kaydedilir; alpha eğrisi anketsiz kolda kalır, bu doğru
     * davranış.
     */
    fun tamamla(onTamamlandi: () -> Unit) {
        if (tamamlamaIsi?.isActive == true) return
        val durum = _durum.value
        tamamlamaIsi = viewModelScope.launch {
            kalicilastirVeIlerle(
                kalicilastir = {
                    val bugun = LocalDate.now(saat)
                    var anketTarihi: LocalDate? = null

                    if (durum.anketTamam) {
                        val hamPuan = durum.hamPuan.toDouble()
                        anketDao.ekle(
                            AnketYanitEntity(
                                tarih = bugun,
                                madde1 = durum.yanitlar[0], madde2 = durum.yanitlar[1],
                                madde3 = durum.yanitlar[2], madde4 = durum.yanitlar[3],
                                madde5 = durum.yanitlar[4], madde6 = durum.yanitlar[5],
                                madde7 = durum.yanitlar[6], madde8 = durum.yanitlar[7],
                                madde9 = durum.yanitlar[8], madde10 = durum.yanitlar[9],
                                hamPuan = hamPuan,
                                rS = RiskHesapla.anketSkoru(hamPuan),
                            ),
                        )
                        anketTarihi = bugun
                    }

                    tercihler.onboardingiBitir(
                        hedefDk = durum.hedefDakika,
                        anketTarihi = anketTarihi,
                    )

                    // Anket doldurulduysa risk hemen hesaplanmalı — yoksa kullanıcı
                    // ilk gün boyunca anketini hiç dikkate almamış bir bant görür.
                    // `AnketYenileViewModel.bitir()` ile aynı ikili; gerekçesi orada.
                    if (anketTarihi != null) {
                        riskDeposu.gunuHesapla(bugun)
                        isPlanlayici.simdiTazele()
                    }
                },
                ilerle = onTamamlandi,
            )
        }
    }

    companion object {
        /** minSdk hedef kitlesi: ölçek 10 yaş altına uygulanmıyor. */
        const val YAS_ALT = 10
        const val YAS_UST = 99

        /** Günlük hedefin taban değeri — altına inmek gerçekçi değil. */
        const val HEDEF_ALT = 60
    }
}
