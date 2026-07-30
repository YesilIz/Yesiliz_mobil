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
import tr.org.yesilay.yesiliz.data.depo.EbeveynDeposu
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.ui.bilesen.veri.Cubuk
import javax.inject.Inject

/**
 * Ebeveyn paneli.
 *
 * **Artık gerçek backend'e bağlı** (`backend/app/uclar/ebeveyn.py`):
 * eşleştirme kodu sunucuda üretilip doğrulanıyor, çocuğun haftalık özeti
 * sunucudan geliyor. Sunucuya ulaşılamazsa [EbeveynDeposu] eski mock
 * davranışına düşüyor — demo günü backend kapalıyken akış kesilmesin
 * (`Yedekli*Deposu` ile aynı gerekçe).
 *
 * Ekran ve `EbeveynDurum` **hiç değişmedi**; mock sabitleri yalnızca
 * çevrimdışı yedek olarak duruyor.
 */
@HiltViewModel
class EbeveynViewModel @Inject constructor(
    private val tercihler: Tercihler,
    private val ebeveynDeposu: EbeveynDeposu,
) : ViewModel() {

    /** Sunucudan gelen özet; `null` iken mock yedeğe düşülüyor. */
    private val uzakOzet = MutableStateFlow<tr.org.yesilay.yesiliz.data.uzak.EbeveynOzetDto?>(null)
    private val eslestirmeHatasi = MutableStateFlow<String?>(null)

    private val sihirbaz = MutableStateFlow(SihirbazHali())

    val durum: StateFlow<EbeveynDurum> = combine(
        tercihler.ebeveynEslesmisMi,
        tercihler.ebeveynCihazRolu,
        sihirbaz,
        uzakOzet,
        eslestirmeHatasi,
    ) { eslesmisMi, rol, sihirbazHali, ozet, hata ->
        EbeveynDurum(
            eslesmisMi = eslesmisMi,
            cihazRolu = rol,
            adim = sihirbazHali.adim,
            secilenRol = sihirbazHali.secilenRol,
            uretilenKod = sihirbazHali.uretilenKod,
            eslestirmeHatasi = hata,
            cocukAdi = ozet?.cocukAdi ?: MOCK_COCUK_ADI,
            cocukYasi = MOCK_COCUK_YASI,
            ekranSuresiOrtalama = ozet?.let { sureMetni(it.ortalamaDk) } ?: MOCK_EKRAN_ORTALAMA,
            tarihAraligi = MOCK_TARIH_ARALIGI,
            ekranCubuklari = ozet?.cubuklar() ?: MOCK_CUBUKLAR,
            ekranVurgu = ozet?.let { enYuksek(it.gunlukDakikalar) } ?: MOCK_VURGU,
            bant = ozet?.bant ?: MOCK_BANT,
            bantMesaji = ozet?.trend?.let(::trendMesaji) ?: MOCK_BANT_MESAJI,
            bildirimler = ozet?.let(::bildirimler) ?: MOCK_BILDIRIMLER,
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
        sihirbaz.value = sihirbaz.value.copy(adim = EslestirmeAdimi.KOD)
        viewModelScope.launch {
            val (kod, _) = ebeveynDeposu.kodUret(sihirbaz.value.secilenRol.deger)
            sihirbaz.value = sihirbaz.value.copy(uretilenKod = kod)
        }
    }

    /**
     * Kodu **sunucuda doğrular**.
     *
     * Eski hâli yalnızca 6 hane olmasına bakıyordu; artık kodun gerçekten
     * var olması, süresinin dolmamış olması (10 dk) ve karşı rolde
     * üretilmiş olması gerekiyor. Hata mesajı ekranda gösteriliyor.
     */
    fun eslestir(girilenKod: String) {
        viewModelScope.launch {
            eslestirmeHatasi.value = null
            val hata = ebeveynDeposu.eslestir(girilenKod, sihirbaz.value.secilenRol.deger)
            if (hata == null) {
                sihirbaz.value = sihirbaz.value.copy(adim = EslestirmeAdimi.BASARILI)
                ozetiTazele()
            } else {
                eslestirmeHatasi.value = hata
            }
        }
    }

    /** Panel açıldığında çocuğun özetini çeker. */
    fun ozetiTazele() {
        viewModelScope.launch { uzakOzet.value = ebeveynDeposu.cocukOzeti() }
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

    /** `240` → `4s 0dk`. */
    private fun sureMetni(dakika: Int): String = "${dakika / 60}s ${dakika % 60}dk"

    private fun enYuksek(dakikalar: List<Int>): Int =
        dakikalar.indices.maxByOrNull { dakikalar[it] } ?: -1

    private fun trendMesaji(trend: String): String = when (trend) {
        "IYILESIYOR" -> "Geçen haftaya göre iyileşiyor"
        "KOTULESIYOR" -> "Geçen haftaya göre artış var"
        else -> "Geçen haftaya göre benzer"
    }

    private fun tr.org.yesilay.yesiliz.data.uzak.EbeveynOzetDto.cubuklar(): List<Cubuk> =
        gunlukDakikalar.mapIndexed { i, dakika -> Cubuk(GUN_ADLARI[i % GUN_ADLARI.size], dakika.toFloat()) }

    /**
     * Sunucudan gelen özetten türetilen bildirimler.
     *
     * Sunucu bir bildirim listesi göndermiyor — ebeveyne ne söyleneceği
     * cihazın kararı. Heartbeat kesilmesi `spec-veri.md`'deki "çocuk
     * uygulamayı silmiş olabilir" uyarısına dönüşüyor.
     */
    private fun bildirimler(ozet: tr.org.yesilay.yesiliz.data.uzak.EbeveynOzetDto): List<EbeveynBildirimi> {
        val liste = mutableListOf<EbeveynBildirimi>()
        ozet.sonSinyalSaatOnce?.let { saat ->
            if (saat >= SESSIZLIK_ESIK_SAAT) {
                liste += EbeveynBildirimi(
                    "Uygulama ${saat / 24} gündür açılmadı",
                    "${saat / 24}g",
                )
            }
        }
        if (ozet.trend == "IYILESIYOR") {
            liste += EbeveynBildirimi("Ekran süresi geçen haftaya göre azalıyor", "1g")
        }
        return liste
    }

    private companion object {
        const val AKIS_BEKLEME_MS = 5_000L

        /** Bu kadar saat sinyal yoksa ebeveyne bildiriliyor (spec-veri.md). */
        const val SESSIZLIK_ESIK_SAAT = 48

        val GUN_ADLARI = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

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
