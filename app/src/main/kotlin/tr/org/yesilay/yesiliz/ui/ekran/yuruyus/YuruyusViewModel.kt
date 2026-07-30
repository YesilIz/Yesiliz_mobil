package tr.org.yesilay.yesiliz.ui.ekran.yuruyus

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
import tr.org.yesilay.yesiliz.data.depo.BildirimDeposu
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.data.saglik.SaglikOkuyucu
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Aktif yürüyüş — canlı sayaç.
 *
 * **Foreground service YOK.** Arka planda adım takibi ayrı bir izin, ayrı bir
 * bildirim kanalı ve ayrı bir pil bütçesi demek; MVP'nin kritik yolunda
 * değil. Ekran açık kaldığı sürece sayıyor (`keepScreenOn`), bu da ekranda
 * açıkça yazıyor — kullanıcı telefonu cebine koyup sayacın durduğunu
 * sonradan öğrenmesin.
 *
 * Puan **bitişte** veriliyor, başlangıçta değil ([Gorev.YURUYUS] 45 puan =
 * 15 dk × 3). Yarıda bırakılırsa puan yok — `NefesViewModel` ile aynı ilke.
 */
@HiltViewModel
class YuruyusViewModel @Inject constructor(
    private val puanDeposu: PuanDeposu,
    private val bildirimDeposu: BildirimDeposu,
    private val saglikOkuyucu: SaglikOkuyucu,
    private val saat: Clock,
) : ViewModel() {

    private val _durum = MutableStateFlow(YuruyusDurum(kalanMetni = baslangicMetni()))
    val durum: StateFlow<YuruyusDurum> = _durum.asStateFlow()

    private var gecenSaniye = 0
    private var sayacIsi: Job? = null

    /** Yürüyüşe başlarkenki adım — fark, yürüyüşün kendi adımı. */
    private var baslangicAdimi: Int? = null

    fun baslatDuraklat() {
        if (_durum.value.calisiyorMu) {
            sayacIsi?.cancel()
            _durum.update { it.copy(calisiyorMu = false) }
        } else {
            _durum.update { it.copy(calisiyorMu = true) }
            sayaciBaslat()
        }
    }

    fun bastanBaslat() {
        sayacIsi?.cancel()
        gecenSaniye = 0
        baslangicAdimi = null
        _durum.value = YuruyusDurum(kalanMetni = baslangicMetni())
    }

    private fun sayaciBaslat() {
        sayacIsi?.cancel()
        sayacIsi = viewModelScope.launch {
            if (baslangicAdimi == null) baslangicAdimi = adimOku()

            while (true) {
                delay(SANIYE_MS)
                gecenSaniye++
                guncelle()

                if (_durum.value.tamamlandiMi) {
                    tamamla()
                    break
                }
                // Adım her saniye sorulmuyor: Health Connect sorgusu ucuz değil
                // ve adım sayacı zaten bu çözünürlükte güncellenmiyor.
                if (gecenSaniye % ADIM_ARALIK_SN == 0) adimiGuncelle()
            }
        }
    }

    private fun guncelle() {
        val ilerleme = YuruyusHesapla.ilerleme(gecenSaniye)
        _durum.update {
            it.copy(
                gecenSaniye = ilerleme.gecenSaniye,
                kalanMetni = YuruyusHesapla.sayacMetni(ilerleme.kalanSaniye),
                oran = ilerleme.oran,
                kazanilanPuan = ilerleme.kazanilanPuan,
                tamamlandiMi = ilerleme.tamamlandiMi,
            )
        }
    }

    private suspend fun tamamla() {
        _durum.update { it.copy(calisiyorMu = false) }
        if (_durum.value.puanVerildiMi) return

        val bugun = LocalDate.now(saat)
        if (puanDeposu.gorevTamamla(Gorev.YURUYUS, bugun)) {
            bildirimDeposu.gorevTamamlandi(Gorev.YURUYUS)
        }
        _durum.update { it.copy(puanVerildiMi = true) }
    }

    private suspend fun adimiGuncelle() {
        val simdiki = adimOku() ?: return
        val bas = baslangicAdimi ?: simdiki.also { baslangicAdimi = it }
        _durum.update { it.copy(adim = (simdiki - bas).coerceAtLeast(0)) }
    }

    /** Health Connect yoksa ya da izin verilmediyse `null` — ekran adımı gizler. */
    private suspend fun adimOku(): Int? {
        if (!saglikOkuyucu.kullanilabilir() || !saglikOkuyucu.izinVarMi()) return null
        return saglikOkuyucu.gununAktivitesi(LocalDate.now(saat)).adim.roundToInt()
    }

    private fun baslangicMetni(): String =
        YuruyusHesapla.sayacMetni(YuruyusHesapla.HEDEF_DAKIKA * SANIYE_DAKIKA)

    private companion object {
        const val SANIYE_MS = 1_000L
        const val SANIYE_DAKIKA = 60
        const val ADIM_ARALIK_SN = 30
    }
}
