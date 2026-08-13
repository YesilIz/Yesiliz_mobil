package tr.org.yesilay.yesiliz.ui.ekran.profil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.arkaplan.IsPlanlayici
import tr.org.yesilay.yesiliz.data.demo.DemoVeri
import tr.org.yesilay.yesiliz.data.depo.DisaAktarDeposu
import tr.org.yesilay.yesiliz.data.depo.SenkronDeposu
import tr.org.yesilay.yesiliz.data.depo.SozlesmeDeposu
import tr.org.yesilay.yesiliz.domain.sozlesme.SssMaddesi
import tr.org.yesilay.yesiliz.data.depo.VeriYonetimiDeposu
import tr.org.yesilay.yesiliz.domain.hesap.HesapDeposu
import tr.org.yesilay.yesiliz.domain.lig.LigDeposu
import tr.org.yesilay.yesiliz.data.tercih.BildirimSaatleri
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import javax.inject.Inject

/**
 * Profil durumu — gizlilik anahtarları ve hedef DataStore'dan geliyor.
 *
 * İsim oturumdan, lig rozeti gerçek lig deposundan gelir. Misafirin adı
 * olmadığı durumda yalnızca görünür isim sabitine düşülür.
 */
@HiltViewModel
class ProfilViewModel @Inject constructor(
    private val tercihler: Tercihler,
    private val demoVeri: DemoVeri,
    private val veriYonetimi: VeriYonetimiDeposu,
    private val isPlanlayici: IsPlanlayici,
    private val disaAktarDeposu: DisaAktarDeposu,
    private val hesapDeposu: HesapDeposu,
    ligDeposu: LigDeposu,
    private val senkronDeposu: SenkronDeposu,
    private val sozlesmeDeposu: SozlesmeDeposu,
) : ViewModel() {

    private val silmeHali = MutableStateFlow(SilmeHali())
    private val disaAktarmaHatasi = MutableStateFlow(false)

    /**
     * Yardım ekranındaki SSS.
     *
     * `assets/sozlesmeler.json`'dan bir kez okunuyor; ekran açılana kadar
     * boş liste, o da "yükleniyor" olarak çiziliyor.
     */
    private val sssAkisi = MutableStateFlow<List<SssMaddesi>>(emptyList())

    init {
        viewModelScope.launch { sssAkisi.value = sozlesmeDeposu.sss() }
    }

    /**
     * Silme bitti, ekrandan çıkılmalı — tek atımlık olay.
     *
     * `StateFlow` olamaz: yapılandırma değişiminde tekrar tetiklenip
     * kullanıcıyı ikinci kez dışarı atardı.
     */
    private val _silindi = Channel<Unit>(Channel.BUFFERED)
    val silindi: Flow<Unit> = _silindi.receiveAsFlow()

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
            yesilayPaylasimi = paylasim,
            ligdeGorun = ligde,
            molaHatirlaticiAcik = molaHatirlatici,
            gunlukHedefDk = hedef,
            bildirimSaatleri = saatler,
            demoKullanilabilir = demoVeri.kullanilabilir,
        )
    }.combine(tercihler.oturum) { durum, oturum ->
        // Misafir kullanıcının adı yok; sabit ada düşülüyor.
        durum.copy(
            isim = oturum.ad.ifBlank { SABIT_ISIM },
            // Misafirin çıkacağı bir hesap yok; satır gösterilmiyor.
            hesapVarMi = oturum.girisYapildiMi && !oturum.misafirMi,
            eposta = oturum.eposta,
            avatarId = oturum.avatarId,
        )
    }.combine(ligDeposu.ligAkisi()) { durum, lig ->
        durum.copy(ligAdi = lig.ligAdi.takeIf { lig.katildiMi }.orEmpty())
    }.combine(silmeHali) { durum, silme ->
        durum.copy(
            siliniyor = silme.calisiyor,
            sunucudaKaldiUyarisi = silme.sunucudaKaldi,
        )
    }.combine(sssAkisi) { durum, sss ->
        durum.copy(sss = sss)
    }.combine(disaAktarmaHatasi) { durum, hata ->
        durum.copy(disaAktarmaHatasi = hata)
    }.combine(tercihler.sunucuTabani) { durum, taban ->
        durum.copy(sunucuTabani = taban)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
        initialValue = ProfilDurum(isim = SABIT_ISIM),
    )

    /**
     * "Yeşilay ile paylaşım" rıza kapısı (CLAUDE.md §12 K19).
     *
     * Açılınca senkron **hemen** deneniyor. Tercih aslında çalışıyordu
     * ama açıp kapatmanın gözlenebilir hiçbir sonucu yoktu: gecelik
     * worker'a kadar hiçbir şey olmuyor, kullanıcı anahtarın işe yarayıp
     * yaramadığını anlayamıyordu (`debug.md` O-18).
     */
    fun yesilayPaylasimi(acik: Boolean) {
        viewModelScope.launch {
            tercihler.yesilayPaylasimiAyarla(acik)
            if (acik) senkronDeposu.senkronEt()
        }
    }

    /**
     * "Ligde görün" tercihi.
     *
     * Kaydetmenin yanında senkron da tetikleniyor: tercih sunucuda tutulan
     * lig üyeliğine yazılıyor ve gecelik worker'ı beklemek, kullanıcının
     * anahtarı kapattıktan sonra saatlerce ligde görünmeye devam etmesi
     * demekti. Bu çağrı özet paylaşımı rızasından özellikle bağımsızdır;
     * aksi hâlde varsayılan kapalı rıza, gizlilik anahtarını da etkisiz
     * bırakırdı.
     */
    fun ligdeGorun(acik: Boolean) {
        viewModelScope.launch {
            tercihler.ligdeGorunAyarla(acik)
            senkronDeposu.ligGorunurlugunuSenkronEt()
        }
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
        if (disaAktarmaHatasi.value) disaAktarmaHatasi.value = false
        viewModelScope.launch {
            disaAktarmaHatasi.value = !guvenliDisaAktar(
                uret = disaAktarDeposu::disaAktar,
                onHazir = onHazir,
            )
        }
    }

    /**
     * Emülatörde ekranları doldurmak için örnek veri yazar.
     *
     * Release derlemesinde [DemoVeri] no-op; bu satır zaten görünmüyor
     * ([ProfilDurum.demoKullanilabilir] false).
     */
    /**
     * Sunucu adresi (Profil › Geliştirici).
     *
     * Varsayılan iki build türünde de canlı sunucu; burası yalnızca yerel
     * backend'e (docker compose / LAN tüneli) bağlanmak için (`debug.md`
     * O-14). Kaydettikten sonra senkron hemen deneniyor ki adresin doğru
     * olup olmadığı anlaşılsın.
     */
    fun sunucuAdresiKaydet(adres: String) {
        viewModelScope.launch {
            tercihler.sunucuTabaniniKaydet(adres)
            senkronDeposu.senkronEt()
        }
    }

    /** Kayıtlı ezmeyi siler; adres derlemedeki canlı sunucuya döner. */
    fun sunucuAdresiVarsayilanaDon() {
        viewModelScope.launch {
            tercihler.sunucuTabaniniSifirla()
            senkronDeposu.senkronEt()
        }
    }

    fun demoTohumla() {
        viewModelScope.launch { demoVeri.tohumla() }
    }

    /**
     * "Verilerimi kalıcı sil" onayı — cihazdaki her şey gider, sunucudaki
     * hesap kalır. Bitince [silindi] tetikleniyor.
     */
    fun verileriSil() {
        sil { veriYonetimi.tumVerileriSil(); true }
    }

    /**
     * "Hesabımı kalıcı sil" onayı — verilerin üstüne sunucudaki hesap da
     * siliniyor ([VeriYonetimiDeposu.hesabiSil]).
     *
     * Eskiden bu iki akış **aynı** fonksiyonu çağırıyordu; hesap silme
     * sunucuya hiç uğramıyordu (`debug.md` K-2).
     */
    fun hesabiSil() {
        sil { veriYonetimi.hesabiSil() }
    }

    /**
     * Hesaptan çıkış.
     *
     * `HesapDeposu.cikisYap()` üç deponun hepsinde uygulanmıştı ama
     * hiçbir ekrandan çağrılmıyordu; giriş yapan kullanıcı hesabını
     * değiştiremiyor, tek yol "Hesabımı sil" oluyordu (`debug.md` Y-3).
     *
     * Veri **silinmiyor** — yalnızca oturum kapanıyor. Aynı hesapla geri
     * girildiğinde cihazdaki geçmiş yerinde.
     */
    fun cikisYap() {
        viewModelScope.launch {
            hesapDeposu.cikisYap()
            _silindi.send(Unit)
        }
    }

    /**
     * Oturum açmış kullanıcının profil bilgilerini (isim, e-posta, isteğe bağlı şifre ve avatar) günceller.
     */
    fun profilGuncelle(
        ad: String,
        eposta: String,
        yeniSifre: String?,
        avatarId: String,
        onSonuc: (tr.org.yesilay.yesiliz.domain.hesap.HesapSonuc) -> Unit,
    ) {
        viewModelScope.launch {
            val sonuc = hesapDeposu.profilGuncelle(
                ad = ad,
                eposta = eposta,
                yeniSifre = yeniSifre,
                avatarId = avatarId,
            )
            onSonuc(sonuc)
        }
    }

    /** Başarısızlık uyarısını kapatır; oturumu kapatmadan yeniden denetir. */
    fun uyariyiKapat() {
        silmeHali.value = SilmeHali()
    }

    /**
     * Ortak silme sarmalayıcısı.
     *
     * Blok `false` dönerse (sunucudaki hesap silinemedi) ekrandan
     * **çıkılmıyor** ve oturum/veri korunuyor. Kullanıcı kimliğini
     * kaybetmeden aynı isteği yeniden gönderebilir.
     */
    private fun sil(blok: suspend () -> Boolean) {
        if (silmeHali.value.calisiyor) return
        silmeHali.value = SilmeHali(calisiyor = true)
        viewModelScope.launch {
            val tamamSilindi = blok()
            if (tamamSilindi) {
                silmeHali.value = SilmeHali()
                _silindi.send(Unit)
            } else {
                silmeHali.value = SilmeHali(sunucudaKaldi = true)
            }
        }
    }

    private data class SilmeHali(
        val calisiyor: Boolean = false,
        val sunucudaKaldi: Boolean = false,
    )

    private companion object {
        // Misafirin görünür adı — AnaViewModel ve Ödüller ile aynı.
        const val SABIT_ISIM = "Misafir"

        /** Ekran arka plana gidince akışı hemen kesme — dönüşte yeniden kurulmasın. */
        const val AKIS_BEKLEME_MS = 5_000L
    }
}

/** Dışa aktarım hatası ViewModel coroutine'ini çökertmez; iptal ise aynen yayılır. */
internal suspend fun <T> guvenliDisaAktar(
    uret: suspend () -> T,
    onHazir: (T) -> Unit,
): Boolean = try {
    onHazir(uret())
    true
} catch (iptal: CancellationException) {
    throw iptal
} catch (_: Exception) {
    false
}
