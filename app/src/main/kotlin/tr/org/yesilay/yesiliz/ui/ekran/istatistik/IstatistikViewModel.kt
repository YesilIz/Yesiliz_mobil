package tr.org.yesilay.yesiliz.ui.ekran.istatistik

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
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.ui.bilesen.veri.Cubuk
import tr.org.yesilay.yesiliz.ui.bilesen.veri.Dilim
import tr.org.yesilay.yesiliz.ui.tema.adi
import tr.org.yesilay.yesiliz.ui.tema.grafikRengi
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * İstatistik — **gerçek `gunluk_ozet` satırlarından** çiziliyor.
 *
 * Segment (Gün / Hafta / Ay) gerçekten pencereyi değiştiriyor: seçim
 * `flatMapLatest` ile yeni bir Room akışına dönüyor, sorgu takvim hizalı.
 *
 * Karşılaştırma metni **betimleyici**: "18 dk daha az" — "iyi/kötü" yargısı
 * yok (CLAUDE.md §4).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IstatistikViewModel @Inject constructor(
    private val ozetDao: GunlukOzetDao,
    riskDao: RiskSonucDao,
    private val saat: Clock,
) : ViewModel() {

    private val secilenAralik = MutableStateFlow(Aralik.HAFTA)

    val durum: StateFlow<IstatistikDurum> = secilenAralik
        .flatMapLatest { aralik ->
            val bugun = LocalDate.now(saat)
            val pencere = aralik.pencere(bugun)
            // Karşılaştırma için bir önceki pencereyi de çekiyoruz; tek sorgu
            // yerine iki sorgu, ama aralık küçük (en fazla 60 satır).
            val oncekiPencere = aralik.oncekiPencere(bugun)

            ozetDao.araliktakilerAkisi(oncekiPencere.first, pencere.second)
                .map { hepsi ->
                    val simdiki = hepsi.filter { it.tarih >= pencere.first }
                    val onceki = hepsi.filter { it.tarih < pencere.first }
                    durumKur(aralik, pencere.first, pencere.second, simdiki, onceki)
                }
        }
        .combine(riskDao.sonuncuAkisi()) { durum, risk ->
            durum.copy(riskBandi = risk?.let { Bant.valueOf(it.gosterilenBant) })
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
            initialValue = IstatistikDurum(),
        )

    fun aralikSecildi(aralik: Aralik) {
        secilenAralik.value = aralik
    }

    private fun durumKur(
        aralik: Aralik,
        bas: LocalDate,
        bit: LocalDate,
        simdiki: List<GunlukOzetEntity>,
        onceki: List<GunlukOzetEntity>,
    ): IstatistikDurum {
        if (simdiki.isEmpty()) {
            return IstatistikDurum(aralik = aralik, gunSayisi = 0)
        }

        val ekranOrt = simdiki.map { it.toplamEkranDk() }.average()
        val adimOrt = simdiki.map { it.adim }.average()
        val ekranOnceki = onceki.takeIf { it.isNotEmpty() }?.map { it.toplamEkranDk() }?.average()
        val adimOnceki = onceki.takeIf { it.isNotEmpty() }?.map { it.adim }?.average()

        val ekranCubuklari = simdiki.map { Cubuk(aralik.etiketle(it.tarih), it.toplamEkranDk().toFloat()) }
        val adimCubuklari = simdiki.map { Cubuk(aralik.etiketle(it.tarih), it.adim.toFloat()) }

        return IstatistikDurum(
            aralik = aralik,
            gunSayisi = simdiki.size,
            tarihAraligi = tarihAraligi(bas, bit),
            ekranOrtalama = ekranOrt.roundToInt(),
            ekranCubuklari = ekranCubuklari,
            // En yüksek gün vurgulanıyor — tasarımda öne çıkan çubuk bu.
            ekranVurgu = simdiki.indices.maxByOrNull { simdiki[it].toplamEkranDk() } ?: -1,
            ekranFark = farkMetni(aralik, ekranOrt, ekranOnceki, "dk"),
            ekranAzaldi = ekranOnceki == null || ekranOrt <= ekranOnceki,
            kategoriler = kategoriDagilimi(simdiki),
            adimOrtalama = adimOrt.roundToInt(),
            adimCubuklari = adimCubuklari,
            adimVurgu = simdiki.indices.maxByOrNull { simdiki[it].adim } ?: -1,
            adimFark = farkMetni(aralik, adimOrt, adimOnceki, "adım"),
            adimArtti = adimOnceki == null || adimOrt >= adimOnceki,
        )
    }

    /**
     * Kategori dağılımı — `sistem` yok.
     *
     * Sistem paketleri risk hesabına girmediği gibi (CLAUDE.md §12 K9) zaten
     * Room'a da yazılmıyor; dağılımda görünmemesi tutarlı.
     */
    private fun kategoriDagilimi(gunler: List<GunlukOzetEntity>): List<Dilim> {
        val toplamlar = linkedMapOf(
            Kategori.SOCIAL to gunler.sumOf { it.socialDk },
            Kategori.ENTERTAINMENT to gunler.sumOf { it.entertainmentDk },
            Kategori.COMMUNICATION to gunler.sumOf { it.communicationDk },
            Kategori.GAMES to gunler.sumOf { it.gamesDk },
            Kategori.PRODUCTIVITY to gunler.sumOf { it.productivityDk },
            Kategori.EDUCATION to gunler.sumOf { it.educationDk },
            Kategori.HEALTH to gunler.sumOf { it.healthDk },
            Kategori.OTHER to gunler.sumOf { it.otherDk },
        )
        val genelToplam = toplamlar.values.sum()
        if (genelToplam <= 0.0) return emptyList()

        return toplamlar
            .filter { it.value > 0.0 }
            .map { (kategori, dakika) ->
                Dilim(kategori.adi, (dakika / genelToplam).toFloat(), kategori.grafikRengi)
            }
            .sortedByDescending { it.oran }
    }

    /**
     * "Geçen haftaya göre 18 dk daha az" — yargısız, betimleyici.
     *
     * "iyi/kötü" demiyor; yalnızca yönü ve miktarı söylüyor (CLAUDE.md §4).
     * Karşılaştırılacak önceki dönem yoksa boş dönüyor ve ekran satırı
     * hiç göstermiyor — uydurma bir karşılaştırma yazmaktansa susmak doğru.
     */
    private fun farkMetni(
        aralik: Aralik,
        simdi: Double,
        onceki: Double?,
        birim: String,
    ): String {
        if (onceki == null) return ""
        val fark = abs(simdi - onceki).roundToInt()
        if (fark == 0) return "${aralik.karsilastirmaOznesi} değişmedi"
        val yon = if (simdi < onceki) "daha az" else "daha fazla"
        return "${aralik.karsilastirmaOznesi} $fark $birim $yon"
    }

    private fun tarihAraligi(bas: LocalDate, bit: LocalDate): String =
        if (bas == bit) {
            bas.format(TAM_BICIM)
        } else {
            "${bas.format(GUN_AY_BICIM)}–${bit.format(TAM_BICIM)}"
        }

    private companion object {
        const val AKIS_BEKLEME_MS = 5_000L
        val TR: Locale = Locale.forLanguageTag("tr")
        val GUN_AY_BICIM: DateTimeFormatter = DateTimeFormatter.ofPattern("d", TR)
        val TAM_BICIM: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", TR)
    }
}

private fun GunlukOzetEntity.toplamEkranDk(): Double =
    socialDk + entertainmentDk + gamesDk + communicationDk +
        productivityDk + educationDk + healthDk + otherDk
