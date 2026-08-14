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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.arkaplan.IsPlanlayici
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.gosterilebilirBant
import tr.org.yesilay.yesiliz.data.zaman.GuncelGun
import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.ui.bilesen.veri.Cubuk
import tr.org.yesilay.yesiliz.ui.bilesen.veri.Dilim
import tr.org.yesilay.yesiliz.ui.tema.adi
import tr.org.yesilay.yesiliz.ui.tema.grafikRengi
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
    private val isPlanlayici: IsPlanlayici,
    riskDao: RiskSonucDao,
    guncelGun: GuncelGun,
) : ViewModel() {

    private val secilenAralik = MutableStateFlow(Aralik.HAFTA)

    private val _tazeleniyor = MutableStateFlow(false)
    val tazeleniyor: StateFlow<Boolean> = _tazeleniyor.asStateFlow()

    /** Grafikte dokunulan gün; -1 = seçim yok, ekran ortalamayı gösteriyor. */
    private val secilenIndeks = MutableStateFlow(SECIM_YOK)

    val durum: StateFlow<IstatistikDurum> = combine(
        secilenAralik,
        guncelGun.akis,
    ) { aralik, bugun -> aralik to bugun }
        .flatMapLatest { (aralik, bugun) ->
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
        .combine(secilenIndeks) { durum, indeks -> secimiUygula(durum, indeks) }
        .combine(riskDao.sonuncuAkisi()) { durum, risk ->
            durum.copy(riskBandi = risk?.gosterilebilirBant())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
            initialValue = IstatistikDurum(),
        )

    /**
     * Yenile düğmesi — gecelik işi hemen kuyruğa alır.
     *
     * Burada eskiden `pencereyiCanliTazele` vardı: ekran her açıldığında ve
     * her aralık değişiminde penceredeki günleri ViewModel'in kendisi
     * yeniden işliyordu. Üç sorunu vardı — (1) ViewModel'den depoya YAZMAK
     * katman ihlali (CLAUDE.md §6: UI yalnızca okur), (2) worker'ın
     * uyguladığı kurulum tarihi ve `enErken` korumalarını atlıyordu, (3)
     * "adım sıfırsa yeniden işle" koşulu, adım gerçekten sıfır olan her
     * günde sonsuza kadar tetikleniyordu. Telafi işi tek bir yerde
     * (`GunlukOzetWorker`) kalsın diye tazeleme ona devredildi.
     */
    fun tazele() {
        if (_tazeleniyor.value) return
        _tazeleniyor.value = true
        viewModelScope.launch {
            isPlanlayici.elleTazele()
            // İş kuyrukta; kısa bir görsel geri bildirim yeter. Room akışı
            // sonuç yazıldığı anda ekranı zaten güncelliyor.
            delay(TAZELEME_GERI_BILDIRIM_MS)
            _tazeleniyor.value = false
        }
    }

    fun aralikSecildi(aralik: Aralik) {
        // Aralık değişince gün seçimi anlamını yitiriyor — "geçen haftanın
        // 3. çubuğu" bu haftanın 3. çubuğu değil.
        secilenIndeks.value = SECIM_YOK
        secilenAralik.value = aralik
    }

    /** Aynı çubuğa ikinci dokunuş seçimi kaldırıyor. */
    fun gunSecildi(indeks: Int) {
        secilenIndeks.value = if (secilenIndeks.value == indeks) SECIM_YOK else indeks
    }

    fun secimiKaldir() {
        secilenIndeks.value = SECIM_YOK
    }

    /**
     * Seçili gün varsa ekranın tamamı o güne dönüyor: ekran süresi kartının
     * başlığı, büyük sayısı, kategori dağılımı ve adım kartı.
     *
     * Yalnızca grafiği vurgulayıp kartları ortalamada bırakmak, Apple Ekran
     * Süresi'nden gelen beklentiyi karşılamaz — kullanıcı çubuğa dokunduğunda
     * **o günü** görmek istiyor.
     */
    private fun secimiUygula(durum: IstatistikDurum, indeks: Int): IstatistikDurum {
        val gunler = durum.gunler
        if (indeks !in gunler.indices) return durum.copy(seciliIndeks = SECIM_YOK, seciliGun = null)

        val gun = gunler[indeks]
        val ekranDk = gun.toplamEkranDk()
        return durum.copy(
            seciliIndeks = indeks,
            seciliGun = gun.tarih,
            seciliBaslik = gunBasligi(gun.tarih),
            ekranSecili = ekranDk.roundToInt(),
            adimSecili = gun.adim.roundToInt(),
            ekranSeciliFark = ortalamayaGoreFark(ekranDk, durum.ekranOrtalama.toDouble(), "dk"),
            adimSeciliFark = ortalamayaGoreFark(gun.adim, durum.adimOrtalama.toDouble(), "adım"),
            kategoriler = kategoriDagilimi(listOf(gun)),
        )
    }

    private fun ortalamayaGoreFark(deger: Double, ortalama: Double, birim: String): String {
        val fark = abs(deger - ortalama).roundToInt()
        if (fark == 0) return "Ortalamayla aynı"
        val yon = if (deger < ortalama) "daha az" else "daha fazla"
        return "Ortalamaya göre $fark $birim $yon"
    }

    private fun gunBasligi(tarih: LocalDate): String = tarih.format(GUN_BASLIK_BICIM)

    /** Boş listede `Double.NaN` yerine 0 — `average()` boşta NaN döndürüyor. */
    private fun List<Double>.ortalamaVeyaSifir(): Double = if (isEmpty()) 0.0 else average()

    private fun durumKur(
        aralik: Aralik,
        bas: LocalDate,
        bit: LocalDate,
        simdiki: List<GunlukOzetEntity>,
        onceki: List<GunlukOzetEntity>,
    ): IstatistikDurum {
        if (simdiki.isEmpty() && onceki.isEmpty()) {
            return IstatistikDurum(aralik = aralik, gunSayisi = 0)
        }

        val simdikiHarita = simdiki.associateBy { it.tarih }
        val gunFarki = java.time.temporal.ChronoUnit.DAYS.between(bas, bit).toInt()
        val tumGunler = (0..gunFarki).map { i ->
            val gun = bas.plusDays(i.toLong())
            simdikiHarita[gun] ?: GunlukOzetEntity(tarih = gun)
        }

        // ORTALAMA yalnızca ölçülmüş günlerden. Ölçülmemiş gün grafikte boş
        // çubuk olarak duruyor (kullanıcı boşluğu görsün) ama ortalamayı
        // aşağı çekmiyor: "o gün telefonu hiç açmadım" ile "o günü
        // ölçemedik" farklı şeyler ve ikincisini sıfır saymak ekran süresini
        // olduğundan iyi gösterirdi.
        val olculenler = tumGunler.filter { it.olayVarMi }
        val olculenOnceki = onceki.filter { it.olayVarMi }

        val ekranOrt = olculenler.map { it.toplamEkranDk() }.ortalamaVeyaSifir()
        // Adım ayrı süzülüyor: adım verisi ekran ölçümünden bağımsız gelebilir
        // (saatten/bileklikten). Sıfır adımlı günü ortalamaya katmak, adım
        // izni olmayan kullanıcının ortalamasını sıfıra çekiyordu.
        val adimOrt = tumGunler.filter { it.adim > 0.0 }.map { it.adim }.ortalamaVeyaSifir()
        val ekranOnceki = olculenOnceki.takeIf { it.isNotEmpty() }
            ?.map { it.toplamEkranDk() }?.average()
        val adimOnceki = onceki.filter { it.adim > 0.0 }.takeIf { it.isNotEmpty() }
            ?.map { it.adim }?.average()

        val ekranCubuklari = tumGunler.map {
            Cubuk(aralik.etiketle(it.tarih), it.toplamEkranDk().toFloat(), veriVar = it.olayVarMi)
        }
        val adimCubuklari = tumGunler.map {
            Cubuk(aralik.etiketle(it.tarih), it.adim.toFloat(), veriVar = it.adim > 0.0)
        }

        return IstatistikDurum(
            aralik = aralik,
            gunSayisi = tumGunler.size,
            olculenGunSayisi = olculenler.size,
            adimKaynagi = olculenAdimKaynagi(tumGunler),
            gunler = tumGunler,
            tarihAraligi = tarihAraligi(bas, bit),
            ekranOrtalama = ekranOrt.roundToInt(),
            ekranCubuklari = ekranCubuklari,
            ekranVurgu = tumGunler.indices.maxByOrNull { tumGunler[it].toplamEkranDk() } ?: -1,
            ekranFark = farkMetni(aralik, ekranOrt, ekranOnceki, "dk"),
            ekranAzaldi = ekranOnceki == null || ekranOrt <= ekranOnceki,
            kategoriler = kategoriDagilimi(tumGunler),
            adimOrtalama = adimOrt.roundToInt(),
            adimCubuklari = adimCubuklari,
            adimVurgu = tumGunler.indices.maxByOrNull { tumGunler[it].adim } ?: -1,
            adimFark = farkMetni(aralik, adimOrt, adimOnceki, "adım"),
            adimArtti = adimOnceki == null || adimOrt >= adimOnceki,
        )
    }

    /**
     * Penceredeki adımların ağırlıklı kaynağı — ekranda etiket olarak.
     *
     * Kullanıcı Fit/Samsung Health ile uygulamanın sayısı arasındaki %10-20
     * farkı hata sanıyor. Fark gerçek: o uygulamalar ham sayacı kendi
     * hareket sınıflandırıcılarından geçirip yanlış pozitifleri eliyor, ham
     * `TYPE_STEP_COUNTER` elemiyor. Hangi kaynağın kullanıldığını söylemek
     * farkı açıklanabilir kılıyor.
     */
    private fun olculenAdimKaynagi(gunler: List<GunlukOzetEntity>): String? = gunler
        .filter { it.adim > 0.0 }
        .groupingBy { it.adimKaynagi }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?.let { anahtar ->
            when (anahtar) {
                "SAGLIK" -> "Health Connect"
                "SENSOR" -> "Telefon sayacı"
                else -> null
            }
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

        /** Yenile ikonunun en az bu kadar dönmesi — dokunuş görsel karşılık bulsun. */
        const val TAZELEME_GERI_BILDIRIM_MS = 900L
        const val SECIM_YOK = -1
        val TR: Locale = Locale.forLanguageTag("tr")
        val GUN_AY_BICIM: DateTimeFormatter = DateTimeFormatter.ofPattern("d", TR)
        val TAM_BICIM: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", TR)

        /** "24 Temmuz Perşembe" — seçili gün kartının başlığı. */
        val GUN_BASLIK_BICIM: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE", TR)
    }
}

internal fun GunlukOzetEntity.toplamEkranDk(): Double =
    socialDk + entertainmentDk + gamesDk + communicationDk +
        productivityDk + educationDk + healthDk + otherDk
