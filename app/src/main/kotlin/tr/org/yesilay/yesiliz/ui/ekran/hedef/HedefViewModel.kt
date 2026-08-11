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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.zaman.GuncelGun
import javax.inject.Inject
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HedefViewModel @Inject constructor(
    private val tercihler: Tercihler,
    ozetDao: GunlukOzetDao,
    guncelGun: GuncelGun,
) : ViewModel() {

    /** Kullanıcının ekrandaki seçimi; `null` iken kayıtlı hedef gösteriliyor. */
    private val secilen = MutableStateFlow<Int?>(null)
    private val kaydedildi = MutableStateFlow(false)

    val durum: StateFlow<HedefDurum> = combine(
        tercihler.gunlukHedefDk,
        tercihler.ebeveynEslesmisMi,
        guncelGun.akis.flatMapLatest(ozetDao::gununAkisi),
        secilen,
        kaydedildi,
    ) { hedef, ebeveyn, ozet, secim, kayit ->
        HedefDurum(
            kayitliHedefDk = hedef,
            secilenDk = secim ?: hedef,
            bugunDk = ozet?.toplamEkranDk()?.roundToInt(),
            ebeveynAktifMi = ebeveyn,
            kaydedildi = kayit,
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

    fun kaydet() {
        val dakika = durum.value.secilenDk
        viewModelScope.launch {
            tercihler.hedefiKaydet(dakika)
            // Seçim bırakılıyor ki ekran tekrar kayıtlı değeri okusun —
            // iki ayrı kaynağın (seçim ve DataStore) ayrışması engelleniyor.
            secilen.value = null
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
