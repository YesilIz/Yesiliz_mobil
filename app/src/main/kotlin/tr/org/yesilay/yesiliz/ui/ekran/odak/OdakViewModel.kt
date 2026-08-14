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
import tr.org.yesilay.yesiliz.zaman.GecenSureHesapla
import tr.org.yesilay.yesiliz.zaman.GecenSureSaati
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
    private val gecenSureSaati: GecenSureSaati,
) : ViewModel() {

    private val _durum = MutableStateFlow(OdakDurum())
    val durum: StateFlow<OdakDurum> = _durum.asStateFlow()

    private var gecenSaniye = 0
    private var baslangicMs = 0L
    private var sayacIsi: Job? = null
    private var bitirmeIsi: Job? = null

    /**
     * Seans başlamadan önce süre seçilebiliyor.
     *
     * Hem hızlı seçim çipleri hem tekerlek buraya bağlı; tek giriş noktası
     * olduğu için sınır kontrolü de tek yerde.
     */
    fun sureSecildi(dakika: Int) {
        if (_durum.value.basladi) return
        val guvenli = dakika.coerceIn(OdakHesapla.EN_KISA_DK, OdakHesapla.EN_UZUN_DK)
        _durum.update { it.copy(sureDk = guvenli) }
    }

    fun basla() {
        if (_durum.value.basladi) return
        gecenSaniye = 0
        baslangicMs = gecenSureSaati.simdiMs()
        _durum.update {
            it.copy(basladi = true, ekranAcildi = false, bitti = false, puanYazildi = false)
        }
        sayacIsi = viewModelScope.launch {
            val toplam = _durum.value.sureDk * 60
            while (gecenSaniye < toplam) {
                delay(1_000)
                // `delay`, ekran kapalıyken Android tarafından ertelenebilir.
                // Tik saymak yerine derin uykuyu da içeren monoton saatten
                // gerçek geçen süreyi yeniden kur.
                gecenSaniye = GecenSureHesapla.saniye(
                    baslangicMs = baslangicMs,
                    simdiMs = gecenSureSaati.simdiMs(),
                )
                _durum.update { it.copy(ilerleme = OdakHesapla.ilerleme(gecenSaniye, toplam)) }
            }
            tamamla()
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
        val toplam = _durum.value.sureDk * 60
        val simdiMs = gecenSureSaati.simdiMs()
        gecenSaniye = GecenSureHesapla.saniye(baslangicMs, simdiMs)
        _durum.update { it.copy(ilerleme = OdakHesapla.ilerleme(gecenSaniye, toplam)) }

        // Telefon ancak süre dolduktan sonra açıldıysa bu başarılı
        // seansı bozmaz. Uyku sırasında ertelenen sayaç callback'i henüz
        // çalışmamış olsa bile burada tamamlanır.
        if (GecenSureHesapla.dolduMu(baslangicMs, simdiMs, toplam)) {
            tamamla()
        } else {
            _durum.update { it.copy(ekranAcildi = true) }
        }
    }

    /** Kullanıcı seansı elle bırakıyor. */
    fun vazgec() {
        sayacIsi?.cancel()
        bitirmeIsi?.cancel()
        gecenSaniye = 0
        _durum.value = OdakDurum(sureDk = _durum.value.sureDk)
    }

    private fun tamamla() {
        if (_durum.value.bitti || bitirmeIsi?.isActive == true) return
        sayacIsi?.cancel()
        bitirmeIsi = viewModelScope.launch { bitir() }
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
