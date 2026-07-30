package tr.org.yesilay.yesiliz.ui.ekran.ebeveyn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.ui.bilesen.veri.Cubuk
import javax.inject.Inject

/**
 * Ebeveyn paneli.
 *
 * Eşleşme durumu **gerçek** (`Tercihler`, DataStore) ama çocuğun ekran
 * verisi **mock** — arkasında gerçek bir `EbeveynBaglantiEntity`/backend yok
 * (CLAUDE.md §6, Faz 2'ye ertelendi; `LigViewModel.sahteLig()` ile aynı
 * ruhta). Backend gelince yalnızca [MOCK_*] sabitleri gerçek depoya bağlanır,
 * ekran/Durum değişmez.
 */
@HiltViewModel
class EbeveynViewModel @Inject constructor(
    private val tercihler: Tercihler,
) : ViewModel() {

    private val sihirbaz = MutableStateFlow(SihirbazHali())

    val durum: StateFlow<EbeveynDurum> = combine(
        tercihler.ebeveynEslesmisMi,
        tercihler.ebeveynCihazRolu,
        sihirbaz,
    ) { eslesmisMi, rol, sihirbazHali ->
        EbeveynDurum(
            eslesmisMi = eslesmisMi,
            cihazRolu = rol,
            adim = sihirbazHali.adim,
            secilenRol = sihirbazHali.secilenRol,
            uretilenKod = sihirbazHali.uretilenKod,
            cocukAdi = MOCK_COCUK_ADI,
            cocukYasi = MOCK_COCUK_YASI,
            ekranSuresiOrtalama = MOCK_EKRAN_ORTALAMA,
            tarihAraligi = MOCK_TARIH_ARALIGI,
            ekranCubuklari = MOCK_CUBUKLAR,
            ekranVurgu = MOCK_VURGU,
            bant = MOCK_BANT,
            bantMesaji = MOCK_BANT_MESAJI,
            bildirimler = MOCK_BILDIRIMLER,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
        initialValue = EbeveynDurum(),
    )

    fun rolSec(rol: CihazRolu) {
        sihirbaz.value = sihirbaz.value.copy(secilenRol = rol)
    }

    fun rolOnayla() {
        sihirbaz.value = sihirbaz.value.copy(
            adim = EslestirmeAdimi.KOD,
            uretilenKod = (100000..999999).random().toString(),
        )
    }

    /**
     * MOCK eşleştirme — ikinci bir cihaz/backend olmadığı için girilen kod
     * **doğrulanmıyor**, yalnızca 6 hane olması yeterli. Faz 2'de burası
     * gerçek sunucu çağrısına döner, `EbeveynEkran` değişmez.
     */
    fun eslestir(girilenKod: String) {
        if (girilenKod.length != 6) return
        viewModelScope.launch {
            tercihler.ebeveynEslesmeyiKaydet(sihirbaz.value.secilenRol.deger)
            sihirbaz.value = sihirbaz.value.copy(adim = EslestirmeAdimi.BASARILI)
        }
    }

    fun sihirbaziKapat() {
        sihirbaz.value = SihirbazHali()
    }

    fun eslesmeyiKaldir() {
        viewModelScope.launch { tercihler.ebeveynEslesmeyiKaldir() }
    }

    private data class SihirbazHali(
        val adim: EslestirmeAdimi = EslestirmeAdimi.ROL_SECIMI,
        val secilenRol: CihazRolu = CihazRolu.EBEVEYN,
        val uretilenKod: String = "",
    )

    private companion object {
        const val AKIS_BEKLEME_MS = 5_000L

        // --- mock çocuk paneli — bkz. sınıf dokümanı ---
        const val MOCK_COCUK_ADI = "Elif"
        const val MOCK_COCUK_YASI = 14
        const val MOCK_EKRAN_ORTALAMA = "3s 40dk"
        const val MOCK_TARIH_ARALIGI = "22–28 Tem, 2026"
        const val MOCK_VURGU = 4
        const val MOCK_BANT = "ORTA"
        const val MOCK_BANT_MESAJI = "Geçen haftaya göre iyileşiyor"
        val MOCK_CUBUKLAR = listOf(
            Cubuk("Pzt", 210f),
            Cubuk("Sal", 195f),
            Cubuk("Çar", 240f),
            Cubuk("Per", 180f),
            Cubuk("Cum", 220f),
            Cubuk("Cmt", 260f),
            Cubuk("Paz", 205f),
        )
        val MOCK_BILDIRIMLER = listOf(
            EbeveynBildirimi("Uygulama 2 gündür açılmadı", "2g"),
            EbeveynBildirimi("Haftalık hedefe ulaştı", "1g"),
        )
    }
}
