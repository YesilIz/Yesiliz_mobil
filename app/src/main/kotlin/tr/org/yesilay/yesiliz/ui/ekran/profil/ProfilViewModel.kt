package tr.org.yesilay.yesiliz.ui.ekran.profil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.arkaplan.IsPlanlayici
import tr.org.yesilay.yesiliz.data.demo.DemoVeri
import tr.org.yesilay.yesiliz.data.depo.DisaAktarDeposu
import tr.org.yesilay.yesiliz.data.depo.VeriYonetimiDeposu
import tr.org.yesilay.yesiliz.data.tercih.BildirimSaatleri
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import javax.inject.Inject

/**
 * Profil durumu — gizlilik anahtarları ve hedef DataStore'dan geliyor.
 *
 * İsim ve lig adı hâlâ sabit: hesap sistemi (giriş/kayıt ekranları) ve gerçek
 * lig Faz 2'de backend'le gelecek. Bunlar **bilinçli** sahte veri, ötekiler
 * gerçek.
 */
@HiltViewModel
class ProfilViewModel @Inject constructor(
    private val tercihler: Tercihler,
    private val demoVeri: DemoVeri,
    private val veriYonetimi: VeriYonetimiDeposu,
    private val isPlanlayici: IsPlanlayici,
    private val disaAktarDeposu: DisaAktarDeposu,
) : ViewModel() {

    val durum: StateFlow<ProfilDurum> = combine(
        tercihler.yesilayPaylasimi,
        tercihler.ligdeGorun,
        tercihler.molaHatirlaticiAcikMi,
        tercihler.gunlukHedefDk,
        tercihler.bildirimSaatleri,
    ) { paylasim, ligde, molaHatirlatici, hedef, saatler ->
        // `combine` en fazla beş akış alıyor; oturum ayrı bir halkada
        // ekleniyor.
        ProfilDurum(
            isim = SABIT_ISIM,
            ligAdi = SABIT_LIG,
            yesilayPaylasimi = paylasim,
            ligdeGorun = ligde,
            molaHatirlaticiAcik = molaHatirlatici,
            gunlukHedefDk = hedef,
            bildirimSaatleri = saatler,
            demoKullanilabilir = demoVeri.kullanilabilir,
        )
    }.combine(tercihler.oturum) { durum, oturum ->
        // Misafir kullanıcının adı yok; sabit ada düşülüyor.
        durum.copy(isim = oturum.ad.ifBlank { SABIT_ISIM })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
        initialValue = ProfilDurum(isim = SABIT_ISIM, ligAdi = SABIT_LIG),
    )

    fun yesilayPaylasimi(acik: Boolean) {
        viewModelScope.launch { tercihler.yesilayPaylasimiAyarla(acik) }
    }

    fun ligdeGorun(acik: Boolean) {
        viewModelScope.launch { tercihler.ligdeGorunAyarla(acik) }
    }

    /**
     * Overlay iznini istemek ([NudgeIzni]) Composable'ın işi — ViewModel
     * Context tutmaz (CLAUDE.md §6). Burada yalnızca kullanıcının **niyeti**
     * kaydediliyor; asıl servis `MainActivity.onResume()`'da bu bayrak +
     * gerçek izin durumuna göre başlatılıp durduruluyor.
     */
    fun molaHatirlaticiAyarla(acik: Boolean) {
        viewModelScope.launch { tercihler.molaHatirlaticiAyarla(acik) }
    }

    /**
     * Günlük hatırlatma ayarı.
     *
     * Kaydetmenin yanında WorkManager planını da güncelliyor: kullanıcı
     * saati değiştirip ekranı kapattığında planın eski saatte kalması,
     * ayarın hiç çalışmaması demek olurdu.
     */
    fun bildirimSaatleriAyarla(saatler: BildirimSaatleri) {
        viewModelScope.launch {
            tercihler.bildirimSaatleriniKaydet(saatler)
            isPlanlayici.hatirlatmayiPlanla(saatler)
        }
    }

    /**
     * Geçmişi JSON + CSV olarak dışa aktarır ve paylaş sayfasını açar.
     *
     * Niyeti başlatmak Composable'ın işi (ViewModel Context tutmaz,
     * CLAUDE.md §6); burada yalnızca dosyalar üretilip niyet hazırlanıyor.
     */
    fun gecmisiDisaAktar(onHazir: (android.content.Intent) -> Unit) {
        viewModelScope.launch { onHazir(disaAktarDeposu.disaAktar()) }
    }

    /**
     * Emülatörde ekranları doldurmak için örnek veri yazar.
     *
     * Release derlemesinde [DemoVeri] no-op; bu satır zaten görünmüyor
     * ([ProfilDurum.demoKullanilabilir] false).
     */
    fun demoTohumla() {
        viewModelScope.launch { demoVeri.tohumla() }
    }

    /**
     * "Verilerimi/Hesabımı kalıcı sil" onayından çağrılır — ikisi de aynı
     * işlemi yapıyor (bkz. [VeriYonetimiDeposu]).
     */
    fun verileriSil() {
        viewModelScope.launch { veriYonetimi.tumVerileriSil() }
    }

    private companion object {
        // Hesap sistemi Faz 2 — o zamana kadar sabit.
        const val SABIT_ISIM = "Turan"
        const val SABIT_LIG = "Filiz Ligi"

        /** Ekran arka plana gidince akışı hemen kesme — dönüşte yeniden kurulmasın. */
        const val AKIS_BEKLEME_MS = 5_000L
    }
}
