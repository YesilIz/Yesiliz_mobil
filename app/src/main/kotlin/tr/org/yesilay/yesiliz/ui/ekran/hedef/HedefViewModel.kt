package tr.org.yesilay.yesiliz.ui.ekran.hedef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.zaman.GuncelGun
import tr.org.yesilay.yesiliz.nudge.NudgeAyari
import javax.inject.Inject
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HedefViewModel @Inject constructor(
    private val tercihler: Tercihler,
    ozetDao: GunlukOzetDao,
    guncelGun: GuncelGun,
) : ViewModel() {

    /** Kullanıcının ekrandaki seçimi; `null` iken kayıtlı değer gösteriliyor. */
    private val secilen = MutableStateFlow<Int?>(null)
    private val secilenNudge = MutableStateFlow<Int?>(null)
    private val kaydedildi = MutableStateFlow(false)

    /**
     * Kayıtlı nudge eşiği dakika olarak. Kullanıcı hiç seçmediyse etkin
     * varsayılandan geliyor ([NudgeAyari.dakikadan]) — ama o varsayılan
     * geliştirme dalında 15 saniye (§12 K50), yani dakikaya çevrilince 0
     * olurdu. Ekranda gösterilebilir bir değer olması için sınıra çekiliyor.
     */
    private val nudgeSecimi = tercihler.nudgeEsikDk.map { secim ->
        if (secim == null) {
            NudgeSecim(NudgeAyari.kirp(NudgeAyari.VARSAYILAN.esik.toMinutes().toInt()), false)
        } else {
            NudgeSecim(NudgeAyari.kirp(secim), true)
        }
    }

    private data class NudgeSecim(val dakika: Int, val secilmisMi: Boolean)

    private data class Kayitli(
        val hedefDk: Int,
        val nudge: NudgeSecim,
        val molaAcik: Boolean,
        val ebeveyn: Boolean,
    )

    private val kayitli = combine(
        tercihler.gunlukHedefDk,
        nudgeSecimi,
        tercihler.molaHatirlaticiAcikMi,
        tercihler.ebeveynEslesmisMi,
        ::Kayitli,
    )

    private data class Secimler(val hedef: Int?, val nudge: Int?, val kaydedildi: Boolean)

    private val secimler = combine(secilen, secilenNudge, kaydedildi, ::Secimler)

    val durum: StateFlow<HedefDurum> = combine(
        kayitli,
        secimler,
        guncelGun.akis.flatMapLatest(ozetDao::gununAkisi),
    ) { kayit, secim, ozet ->
        HedefDurum(
            kayitliHedefDk = kayit.hedefDk,
            secilenDk = secim.hedef ?: kayit.hedefDk,
            bugunDk = ozet?.toplamEkranDk()?.roundToInt(),
            ebeveynAktifMi = kayit.ebeveyn,
            kaydedildi = secim.kaydedildi,
            kayitliNudgeDk = kayit.nudge.dakika,
            secilenNudgeDk = secim.nudge ?: kayit.nudge.dakika,
            molaHatirlaticiAcik = kayit.molaAcik,
            nudgeSecilmisMi = kayit.nudge.secilmisMi,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
        initialValue = HedefDurum(),
    )

    fun arttir() = ayarla(durum.value.secilenDk + HedefSinir.ADIM)

    fun azalt() = ayarla(durum.value.secilenDk - HedefSinir.ADIM)

    fun ayarla(dakika: Int) {
        secilen.value = HedefSinir.kirp(dakika)
        kaydedildi.value = false
    }

    fun nudgeArttir() = nudgeAyarla(durum.value.secilenNudgeDk + NudgeAyari.ADIM_DK)

    fun nudgeAzalt() = nudgeAyarla(durum.value.secilenNudgeDk - NudgeAyari.ADIM_DK)

    /** Aralık dışına çıkan her değer [NudgeAyari.kirp] ile sınıra çekiliyor. */
    fun nudgeAyarla(dakika: Int) {
        secilenNudge.value = NudgeAyari.kirp(dakika)
        kaydedildi.value = false
    }

    /**
     * Eşik seçimini siler; etkin değer yeniden derlemeye gömülü varsayılana
     * döner (§12 K50 — geliştirme dalında cihazda doğrulama eşiği).
     */
    fun nudgeVarsayilanaDon() {
        viewModelScope.launch {
            tercihler.nudgeEsiginiSifirla()
            secilenNudge.value = null
        }
    }

    /**
     * Mola hatırlatıcıyı açar/kapatır.
     *
     * Anahtar eskiden Profil › Gizlilik'teydi, eşik ise burada: tek bir
     * özelliğin açma düğmesi ile ayarı iki ayrı ekranda, iki ayrı kategori
     * altındaydı — üstelik mola hatırlatıcı bir gizlilik ayarı değil,
     * cihazdan hiçbir veri çıkarmıyor (5. tarama G5-034). Anahtar eşiğin
     * yanına geldi; yazma tek yerden, `MainActivity` tercihi dinlediği
     * için servis başlatma/durdurma kendiliğinden çalışıyor.
     *
     * **Kaydet düğmesini beklemiyor**: hedef ve eşik taslak değerler
     * (`secilen`/`secilenNudge`) üzerinden ilerliyor, bu ise bir anahtar —
     * kullanıcı dokunduğu anda uygulanmasını bekler.
     */
    fun molaHatirlaticiAyarla(acik: Boolean) {
        viewModelScope.launch { tercihler.molaHatirlaticiAyarla(acik) }
    }

    fun kaydet() {
        val anlik = durum.value
        viewModelScope.launch {
            if (anlik.hedefDegistiMi) tercihler.hedefiKaydet(anlik.secilenDk)
            if (anlik.nudgeDegistiMi) tercihler.nudgeEsiginiKaydet(anlik.secilenNudgeDk)
            // Seçim bırakılıyor ki ekran tekrar kayıtlı değeri okusun —
            // iki ayrı kaynağın (seçim ve DataStore) ayrışması engelleniyor.
            secilen.value = null
            secilenNudge.value = null
            kaydedildi.value = true
        }
    }

    private companion object {
        const val AKIS_BEKLEME_MS = 5_000L
    }
}

private fun GunlukOzetEntity.toplamEkranDk(): Double =
    socialDk + entertainmentDk + gamesDk + communicationDk +
        productivityDk + educationDk + healthDk + otherDk
