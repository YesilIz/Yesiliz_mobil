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
import tr.org.yesilay.yesiliz.data.depo.EbeveynOzetSonucu
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.EbeveynOzetDto
import tr.org.yesilay.yesiliz.ui.bilesen.veri.Cubuk
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Ebeveyn paneli.
 *
 * **Artık gerçek backend'e bağlı** (`backend/app/uclar/ebeveyn.py`):
 * eşleştirme kodu sunucuda üretilip doğrulanıyor, çocuğun haftalık özeti
 * sunucudan geliyor. Kimlik ve mahremiyet belirleyen bu ekranda ağ hatası
 * gerçekmiş gibi sabit bir çocuk profiline dönüştürülmez.
 */
@HiltViewModel
class EbeveynViewModel @Inject constructor(
    private val tercihler: Tercihler,
    private val ebeveynDeposu: EbeveynDeposu,
) : ViewModel() {

    private val ozetHali = MutableStateFlow(OzetHali())
    private val eslestirmeHatasi = MutableStateFlow<String?>(null)
    private val kaldirmaHali = MutableStateFlow(KaldirmaHali())

    private val sihirbaz = MutableStateFlow(SihirbazHali())

    private val hataVeKaldirma = combine(eslestirmeHatasi, kaldirmaHali) { hata, kaldirma ->
        hata to kaldirma
    }

    val durum: StateFlow<EbeveynDurum> = combine(
        tercihler.ebeveynEslesmisMi,
        tercihler.ebeveynCihazRolu,
        sihirbaz,
        ozetHali,
        hataVeKaldirma,
    ) { eslesmisMi, rol, sihirbazHali, guncelOzetHali, (hata, kaldirma) ->
        val ozet = guncelOzetHali.veri
        EbeveynDurum(
            eslesmisMi = eslesmisMi,
            cihazRolu = rol,
            adim = sihirbazHali.adim,
            secilenRol = sihirbazHali.secilenRol,
            uretilenKod = sihirbazHali.uretilenKod,
            kodYukleniyor = sihirbazHali.kodYukleniyor,
            kodSunucudanMi = sihirbazHali.kodSunucudanMi,
            eslestirmeHatasi = hata,
            eslesmeKaldiriliyor = kaldirma.calisiyor,
            eslesmeKaldirmaHatasi = kaldirma.hata,
            ozetYukleniyor = guncelOzetHali.yukleniyor,
            ozetHatasi = guncelOzetHali.hata,
            cocukAdi = ozet?.cocukAdi.orEmpty(),
            ekranSuresiOrtalama = ozet?.let { sureMetni(it.ortalamaDk) }.orEmpty(),
            tarihAraligi = ozet?.tarihAraligi() ?: "Son 7 gün",
            ekranCubuklari = ozet?.cubuklar().orEmpty(),
            ekranVurgu = ozet?.let { enYuksek(it.gunlukDakikalar) } ?: -1,
            bant = ozet?.bant.riskBandiMetni(),
            bantMesaji = when {
                ozet?.bant == null -> "Yeterli veri oluştuğunda burada görünür."
                else -> ozet.trend.trendMesajiVeyaNull()
                    ?: "Değişimi karşılaştırmak için henüz yeterli geçmiş yok."
            },
            bildirimler = ozet?.let(::bildirimler).orEmpty(),
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
        kodUret()
    }

    fun kodUret() {
        if (sihirbaz.value.kodYukleniyor) return
        eslestirmeHatasi.value = null
        sihirbaz.value = sihirbaz.value.copy(
            uretilenKod = "",
            kodYukleniyor = true,
            kodSunucudanMi = false,
        )
        viewModelScope.launch {
            val sonuc = ebeveynDeposu.kodUret(sihirbaz.value.secilenRol.deger)
            sihirbaz.value = sihirbaz.value.copy(
                uretilenKod = sonuc.kod,
                kodYukleniyor = false,
                kodSunucudanMi = sonuc.hata == null,
            )
            eslestirmeHatasi.value = sonuc.hata
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
                if (sihirbaz.value.secilenRol == CihazRolu.EBEVEYN) ozetiTazele()
            } else {
                eslestirmeHatasi.value = hata
            }
        }
    }

    /** Panel açıldığında çocuğun özetini çeker. */
    fun ozetiTazele() {
        if (ozetHali.value.baslatildi && ozetHali.value.yukleniyor) return
        ozetHali.value = OzetHali(yukleniyor = true, baslatildi = true)
        viewModelScope.launch {
            val sonuc = ebeveynDeposu.cocukOzeti()
            ozetHali.value = ozetTamamlandi(sonuc)
        }
    }

    fun sihirbaziKapat() {
        sihirbaz.value = SihirbazHali()
    }

    fun eslesmeyiKaldir() {
        if (kaldirmaHali.value.calisiyor) return
        kaldirmaHali.value = KaldirmaHali(calisiyor = true)
        viewModelScope.launch {
            val hata = ebeveynDeposu.eslesmeyiKaldir()
            kaldirmaHali.value = KaldirmaHali(hata = hata)
        }
    }

    private data class SihirbazHali(
        val adim: EslestirmeAdimi = EslestirmeAdimi.ROL_SECIMI,
        val secilenRol: CihazRolu = CihazRolu.EBEVEYN,
        val uretilenKod: String = "",
        val kodYukleniyor: Boolean = false,
        val kodSunucudanMi: Boolean = false,
    )

    private data class KaldirmaHali(
        val calisiyor: Boolean = false,
        val hata: String? = null,
    )

    /** `240` → `4s 0dk`. */
    private fun sureMetni(dakika: Int): String = "${dakika / 60}s ${dakika % 60}dk"

    private fun enYuksek(dakikalar: List<Int>): Int =
        dakikalar.indices.maxByOrNull { dakikalar[it] } ?: -1

    private fun EbeveynOzetDto.cubuklar(): List<Cubuk> =
        gunlukDakikalar.mapIndexed { i, dakika ->
            val gun = gunlukTarihler.getOrNull(i)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.dayOfWeek
                ?.value
                ?.let { GUN_ADLARI[it - 1] }
                ?: GUN_ADLARI[i % GUN_ADLARI.size]
            Cubuk(gun, dakika.toFloat())
        }

    private fun EbeveynOzetDto.tarihAraligi(): String {
        val tarihler = gunlukTarihler.mapNotNull {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }
        if (tarihler.isEmpty()) return "Son 7 gün"
        val ilk = tarihler.first()
        val son = tarihler.last()
        return if (ilk == son) {
            son.format(TARIH_TEK)
        } else {
            "${ilk.format(TARIH_KISA)} – ${son.format(TARIH_TEK)}"
        }
    }

    /**
     * Sunucudan gelen özetten türetilen bildirimler.
     *
     * Sunucu bir bildirim listesi göndermiyor — ebeveyne ne söyleneceği
     * cihazın kararı. Heartbeat kesilmesi `spec-veri.md`'deki "çocuk
     * uygulamayı silmiş olabilir" uyarısına dönüşüyor.
     */
    private fun bildirimler(ozet: EbeveynOzetDto): List<EbeveynBildirimi> {
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
        val TURKCE = Locale.forLanguageTag("tr-TR")
        val TARIH_KISA: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", TURKCE)
        val TARIH_TEK: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM uuuu", TURKCE)

    }
}

/** Sunucu enum'unu ebeveyne teknik ya da bilinmeyen bir etiket olarak gösterme. */
internal fun String?.riskBandiMetni(): String = when (this) {
    "AZ" -> "Düşük risk"
    "ORTA" -> "Orta risk"
    "COK" -> "Yüksek risk"
    else -> "Henüz hesaplanmadı"
}

/** `BELIRSIZ` ya da yeni bir değer, gerçekte sabitmiş gibi sunulmaz. */
internal fun String?.trendMesajiVeyaNull(): String? = when (this) {
    "IYILESIYOR" -> "Geçen haftaya göre iyileşiyor"
    "KOTULESIYOR" -> "Geçen haftaya göre artış var"
    "SABIT" -> "Geçen haftaya göre benzer"
    else -> null
}

internal data class OzetHali(
    val yukleniyor: Boolean = true,
    val baslatildi: Boolean = false,
    val veri: EbeveynOzetDto? = null,
    val hata: String? = null,
)

/** Tamamlanmış ağ sonucu hiçbir zaman yükleme ekranında kalmaz. */
internal fun ozetTamamlandi(sonuc: EbeveynOzetSonucu): OzetHali = OzetHali(
    yukleniyor = false,
    baslatildi = true,
    veri = sonuc.veri,
    hata = sonuc.hata,
)
