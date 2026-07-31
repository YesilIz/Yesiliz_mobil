package tr.org.yesilay.yesiliz.ui.ekran.odak

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
 * Odak seansı.
 *
 * Sayaç `viewModelScope`'ta: ekran kapansa bile ViewModel yaşıyor ve
 * süre işlemeye devam ediyor. Bu **gerekli** — görevin tamamı ekranın
 * kapalı olmasına dayanıyor.
 *
 * Foreground service YOK. Odak seansı ekran kapalıyken de sayıyor ama
 * uygulama sistem tarafından öldürülürse seans kayboluyor; bunu telafi
 * etmek için servis açmak, `Gorev.ODAK`'ın 20 puanına değmeyen bir pil
 * ve izin yükü olurdu. Yürüyüşte durum farklı (`YuruyusServisi`): orada
 * kullanıcı hareket hâlinde ve seans yarım saati aşabiliyor.
 */
@HiltViewModel
class OdakViewModel @Inject constructor(
    private val puanDeposu: PuanDeposu,
    private val saat: Clock,
) : ViewModel() {

    private val _durum = MutableStateFlow(OdakDurum())
    val durum: StateFlow<OdakDurum> = _durum.asStateFlow()

    private var gecenSaniye = 0
    private var sayacIsi: Job? = null

    /** Seans başlamadan önce süre değiştirilebiliyor. */
    fun sureDegistir() {
        if (_durum.value.basladi) return
        val indeks = OdakHesapla.SURELER.indexOf(_durum.value.sureDk)
        _durum.update {
            it.copy(sureDk = OdakHesapla.SURELER[(indeks + 1) % OdakHesapla.SURELER.size])
        }
    }

    fun basla() {
        if (_durum.value.basladi) return
        gecenSaniye = 0
        _durum.update {
            it.copy(basladi = true, ekranAcildi = false, bitti = false, puanYazildi = false)
        }
        sayacIsi = viewModelScope.launch {
            val toplam = _durum.value.sureDk * 60
            while (gecenSaniye < toplam) {
                delay(1_000)
                gecenSaniye++
                _durum.update { it.copy(ilerleme = OdakHesapla.ilerleme(gecenSaniye, toplam)) }
            }
            bitir()
        }
    }

    /**
     * Kullanıcı ekranı açtı — seans bozuldu.
     *
     * `MainActivity` yaşam döngüsünden değil, ekranın kendi
     * `ON_RESUME`'undan geliyor: uygulama ön plana döndüğünde seans hâlâ
     * sürüyorsa telefon açılmış demektir.
     */
    fun ekranAcildi() {
        if (!_durum.value.basladi || _durum.value.bitti) return
        _durum.update { it.copy(ekranAcildi = true) }
    }

    /** Kullanıcı seansı elle bırakıyor. */
    fun vazgec() {
        sayacIsi?.cancel()
        gecenSaniye = 0
        _durum.value = OdakDurum(sureDk = _durum.value.sureDk)
    }

    private suspend fun bitir() {
        val d = _durum.value
        val basarili = OdakHesapla.basariliMi(tamamlandiMi = true, ekranAcildiMi = d.ekranAcildi)
        val yazildi = if (basarili) {
            puanDeposu.gorevTamamla(Gorev.ODAK, LocalDate.now(saat))
        } else {
            false
        }
        _durum.update { it.copy(bitti = true, puanYazildi = yazildi) }
    }
}
