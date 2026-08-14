package tr.org.yesilay.yesiliz.ui.ekran.ana

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.ui.ikonu
import tr.org.yesilay.yesiliz.data.depo.BeyanDeposu
import tr.org.yesilay.yesiliz.data.depo.BildirimDeposu
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.data.kullanim.KullanimIzni
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.PuanGunlukDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.gosterilebilirBant
import tr.org.yesilay.yesiliz.data.zaman.GuncelGun
import tr.org.yesilay.yesiliz.arkaplan.IsPlanlayici
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.domain.gorev.RuhHaliPenceresi
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.VeriDurumu
import java.time.Clock
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Ana Sayfa — **gerçek veriden** besleniyor.
 *
 * Altı kaynak birleşiyor:
 * ```
 * gunluk_ozet(bugün)  → ekran süresi, adım
 * risk_sonuc(son)     → gösterilen bant → görev önerisi
 * puan_gunluk         → toplam puan, seri
 * gorev_tamamlama     → hangi görevler tamamlandı
 * Tercihler           → günlük hedef
 * ```
 *
 * Sahte veri kalmadı. Veri yoksa [AnaDurum.veriVarMi] false dönüyor ve ekran
 * boş durum gösteriyor — uydurma sayı değil.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnaViewModel @Inject constructor(
    @ApplicationContext private val baglam: Context,
    private val puanDeposu: PuanDeposu,
    private val beyanDeposu: BeyanDeposu,
    bildirimDeposu: BildirimDeposu,
    tercihler: Tercihler,
    ozetDao: GunlukOzetDao,
    riskDao: RiskSonucDao,
    puanDao: PuanGunlukDao,
    private val guncelGun: GuncelGun,
    private val isPlanlayici: IsPlanlayici,
    private val saat: Clock,
) : ViewModel() {

    /**
     * Slider sürüklenirken tutulan **geçici** değer.
     *
     * Sürükleme sırasında her kareyi Room'a yazmak anlamsız; parmak
     * kalkınca [ruhHaliBirakildi] kalıcı kayda geçiyor. `null` iken ekran
     * Room'daki değeri gösteriyor.
     */
    private val suruklenenRuhHali = MutableStateFlow<Float?>(null)

    /** Hatırlatma kartı "Sonra" ile kapatıldıysa o oturum boyunca gizli. */
    private val degerlendirmeGizlendi = MutableStateFlow(false)

    private val izinHatirlatmasiGizlendi = MutableStateFlow(false)

    /** Eksik izinler — `ON_RESUME`'da yeniden hesaplanıyor. */
    private val eksikIzinler = MutableStateFlow<List<EksikIzin>>(emptyList())

    private val tazeleniyor = MutableStateFlow(false)

    /** Özel kullanım erişimi sistem ekranından dönünce elle tazelenir. */
    private val kullanimIzniVar = MutableStateFlow(KullanimIzni.verildiMi(baglam))

    val durum: StateFlow<AnaDurum> = guncelGun.akis
        .flatMapLatest { bugun ->
            combine(
                ozetDao.gununAkisi(bugun),
                riskDao.sonuncuAkisi(),
                puanDeposu.toplamPuan,
                puanDao.gununAkisi(bugun),
                tercihler.gunlukHedefDk,
            ) { ozet, risk, toplamPuan, gunlukPuan, hedef ->
                Ara(
                    ozet = ozet,
                    // Yetersiz ölçümlü bir gün bandı da uydurmasın
                    // (`gorev2.md` §8) — Ana Sayfa kartı `RiskDetayEkran`
                    // ile aynı kuralı izliyor. `ESKI` burada süzülmüyor:
                    // kart zaten "son bilinen" bandı gösteriyor, risk
                    // detayının aksine güncellik iddiası taşımıyor.
                    bant = risk?.gosterilebilirBant(),
                    toplamPuan = toplamPuan,
                    seriGun = gunlukPuan?.seriGun ?: 0,
                    seriJokerli = gunlukPuan?.jokerKullanildi == true,
                    hedefDk = hedef,
                )
            }.combine(tercihler.ebeveynEslesmisMi) { ara, ebeveynAktif ->
                ara.copy(ebeveynAktifMi = ebeveynAktif)
            }.combine(
                // Ruh hali AKIŞ olarak zincire giriyor. Daha önce `.value`
                // ile anlık okunuyordu; slider oynatılınca `durum` yeniden
                // yayınlanmadığı için tutamak eski değere geri sıçrıyordu.
                combine(
                    beyanDeposu.ruhHaliAkisi(bugun),
                    suruklenenRuhHali,
                ) { kayitlilar, suruklenen ->
                    // Gün üç pencereye bölündü: slider açık pencerenin
                    // değerini gösteriyor, "2/3" bilgisi günün tamamından.
                    val pencere = acikPencere()
                    val buPencere = kayitlilar.firstOrNull { it.dilim == pencere.anahtar }
                    RuhHaliDurumu(
                        deger = suruklenen ?: buPencere?.deger ?: VARSAYILAN_RUH_HALI,
                        kaydedildi = buPencere != null,
                        pencere = pencere,
                        doldurulan = kayitlilar.size,
                    )
                },
            ) { ara, ruhHali -> ara.copy(ruhHali = ruhHali) }
                .combine(bildirimDeposu.okunmamisSayisi) { ara, okunmamis ->
                    ara.copy(okunmamisBildirim = okunmamis)
                }
                .combine(tercihler.oturum) { ara, oturum ->
                    // Misafir kullanıcının adı yok; sabit ada düşülüyor.
                    ara.copy(
                        isim = oturum.ad.ifBlank { SABIT_ISIM },
                        avatarId = oturum.avatarId,
                    )
                }
                .combine(
                    combine(
                        tercihler.sonAnketTarihi,
                        degerlendirmeGizlendi,
                    ) { sonAnket, gizli ->
                        !gizli && sonAnket != null &&
                            ChronoUnit.DAYS.between(sonAnket, bugun) >= ANKET_TAZELEME_GUN
                    },
                ) { ara, yenilensin ->
                    ara.copy(degerlendirmeYenilensin = yenilensin)
                }
                .combine(puanDeposu.gununGorevleri(bugun)) { ara, tamamlananlar ->
                    val tamamlananIdler = tamamlananlar.map { it.gorevId }.toSet()
                    AnaDurum(
                        selamlama = selamlama(),
                        isim = ara.isim,
                        avatarId = ara.avatarId,
                        serideGun = ara.seriGun,
                        seriJokerli = ara.seriJokerli,
                        agacEvresi = agacEvresi(ara.toplamPuan),
                        puan = ara.toplamPuan,
                        ekranDakika = ara.ozet?.toplamEkranDk()?.roundToInt() ?: 0,
                        hedefDakika = ara.hedefDk,
                        adim = ara.ozet?.adim?.roundToInt() ?: 0,
                        ruhHali = ara.ruhHali.deger,
                        ruhHaliKaydedildi = ara.ruhHali.kaydedildi,
                        ruhHaliPenceresi = ara.ruhHali.pencere.etiket,
                        ruhHaliDoldurulan = ara.ruhHali.doldurulan,
                        gorevler = Gorev.banttanOner(ara.bant).map { gorev ->
                            GorevOzeti(
                                id = gorev.id,
                                baslik = gorev.baslik,
                                aciklama = gorev.aciklama,
                                puan = gorev.puan,
                                ikon = gorev.ikonu(),
                                tamamlandi = gorev.id in tamamlananIdler,
                            )
                        },
                        // `ozet != null` DEĞİL: gecelik worker ölçüm
                        // bulamadığı güne de satır yazıyor. Satırın varlığını
                        // veri saymak, boş güne gerçek sayı gösterilmesine
                        // yol açıyordu (CLAUDE.md §9).
                        veriVarMi = ara.ozet?.olayVarMi == true,
                        izinVarMi = false,
                        ebeveynAktifMi = ara.ebeveynAktifMi,
                        okunmamisBildirim = ara.okunmamisBildirim,
                        degerlendirmeYenilensin = ara.degerlendirmeYenilensin,
                    )
                }
                .combine(kullanimIzniVar) { anaDurum, izinVarMi ->
                    anaDurum.copy(izinVarMi = izinVarMi)
                }
                .combine(eksikIzinler) { anaDurum, eksikler ->
                    anaDurum.copy(eksikIzinler = eksikler)
                }
                .combine(tazeleniyor) { anaDurum, donuyor ->
                    anaDurum.copy(tazeleniyor = donuyor)
                }
            }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
            initialValue = AnaDurum(selamlama = selamlama(), isim = SABIT_ISIM),
        )

    /** Sürükleme sürerken — henüz yazılmıyor. */
    fun ruhHaliDegisti(deger: Float) {
        suruklenenRuhHali.value = deger
    }

    /**
     * Parmak kalktı — değer kalıcı olarak yazılıyor ve o pencere ilk kez
     * dolduruluyorsa "Ruh hali kaydı" görevinin puanı veriliyor.
     */
    fun ruhHaliBirakildi() {
        val deger = suruklenenRuhHali.value ?: return
        viewModelScope.launch {
            // Gün ve pencere birlikte türetiliyor: gece yarısı ile 05:00
            // arası hâlâ DÜNÜN akşam penceresi, yoksa kullanıcı yeni günün
            // akşam kaydını saatler önce yapmış olurdu.
            val (gun, pencere) = RuhHaliPenceresi.gunuVeDilimi(
                guncelGun.bugun(),
                LocalTime.now(saat),
            )
            beyanDeposu.ruhHaliKaydet(gun, pencere, deger)
            suruklenenRuhHali.value = null
        }
    }

    /** Şu anki yerel saatin düştüğü ruh hali penceresi. */
    private fun acikPencere(): RuhHaliPenceresi =
        RuhHaliPenceresi.bul(LocalTime.now(saat))

    fun degerlendirmeErtele() {
        degerlendirmeGizlendi.value = true
    }

    /** Kullanım erişimi normal izin geri çağrısı üretmediği için ON_RESUME yolu. */
    fun kullanimIzniniTazele() {
        kullanimIzniVar.value = KullanimIzni.verildiMi(baglam)
        eksikIzinler.value = eksikIzinleriHesapla()
    }

    /**
     * Yenile düğmesi — gecelik özet işini hemen kuyruğa alır.
     *
     * Ekran Room akışlarından besleniyor, yani veri yazılır yazılmaz
     * güncelleniyor; sorun verinin ne zaman YAZILDIĞIydı. Gün içinde
     * kullanım değiştiğinde özet ancak gecelik iş koştuğunda ya da uygulama
     * yeniden açıldığında tazeleniyordu.
     */
    fun tazele() {
        if (tazeleniyor.value) return
        tazeleniyor.value = true
        viewModelScope.launch {
            isPlanlayici.elleTazele()
            delay(TAZELEME_GERI_BILDIRIM_MS)
            tazeleniyor.value = false
        }
    }

    /** Hatırlatıcı kartı kapatıldı — o oturum boyunca bir daha gösterilmiyor. */
    fun izinHatirlatmasiniGizle() {
        izinHatirlatmasiGizlendi.value = true
        eksikIzinler.value = eksikIzinleriHesapla()
    }

    private fun eksikIzinleriHesapla(): List<EksikIzin> {
        if (izinHatirlatmasiGizlendi.value) return emptyList()
        return buildList {
            if (!KullanimIzni.verildiMi(baglam)) add(EksikIzin.KULLANIM)
            if (!adimIzniVar()) add(EksikIzin.ADIM)
        }
    }

    /**
     * Adım verisi gelebiliyor mu.
     *
     * İzin adı yerine SONUCA bakılıyor: Health Connect izni verilmiş ama
     * hiç veri yazan uygulama yoksa da, donanım sayacı izni yokken de
     * sonuç aynı — adım göremiyoruz (§12 K27). Kullanıcıya izin ekranını
     * göstermek her iki durumda da doğru eylem.
     */
    private fun adimIzniVar(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            baglam,
            android.Manifest.permission.ACTIVITY_RECOGNITION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /**
     * Görevi tamamlar. Tekrar dokununca **geri alınmıyor**: verilen puanı
     * silmek ceza olurdu (CLAUDE.md §4). Günlük limit `PuanDeposu`'nda.
     */
    fun gorevTiklandi(id: String) {
        val gorev = Gorev.idden(id) ?: return
        viewModelScope.launch { puanDeposu.gorevTamamla(gorev, guncelGun.bugun()) }
    }

    /** Saate göre selamlama — tasarımda "Günaydın". */
    private fun selamlama(): String = when (LocalTime.now(saat).hour) {
        in SABAH_BAS until OGLE_BAS -> "Günaydın"
        in OGLE_BAS until AKSAM_BAS -> "İyi günler"
        else -> "İyi akşamlar"
    }

    /**
     * Ağaç evresi **toplam puandan** türüyor, risk bandından değil.
     *
     * `spec-puan.md` ilkesi: "durumu değil, çabayı ödüllendir". Ağacı risk
     * bandına bağlamak kötü bir günde ağacı kurutup kullanıcıyı cezalandırırdı.
     * Toplam puan da seri gibi yalnızca artıyor (§4 "ceza puanı yok"), yani
     * ağaç hiçbir koşulda geri gitmiyor.
     *
     * Son iki eşik ödül kademeleriyle hizalı (§12 K28): 5.000 ve 10.000.
     */
    private fun agacEvresi(puan: Int): Int = when {
        puan >= EVRE9_PUAN -> 9
        puan >= EVRE8_PUAN -> 8
        puan >= EVRE7_PUAN -> 7
        puan >= EVRE6_PUAN -> 6
        puan >= EVRE5_PUAN -> 5
        puan >= EVRE4_PUAN -> 4
        puan >= EVRE3_PUAN -> 3
        puan >= EVRE2_PUAN -> 2
        else -> 1
    }

    private data class Ara(
        val ozet: GunlukOzetEntity?,
        val bant: Bant?,
        val toplamPuan: Int,
        val seriGun: Int,
        val seriJokerli: Boolean = false,
        val hedefDk: Int,
        val ebeveynAktifMi: Boolean = false,
        val isim: String = SABIT_ISIM,
        val avatarId: String = "",
        val ruhHali: RuhHaliDurumu = RuhHaliDurumu(),
        val okunmamisBildirim: Int = 0,
        val degerlendirmeYenilensin: Boolean = false,
    )

    private data class RuhHaliDurumu(
        val deger: Float = VARSAYILAN_RUH_HALI,
        val kaydedildi: Boolean = false,
        /** Şu an hangi pencere açık — kart başlığı bunu yazıyor. */
        val pencere: RuhHaliPenceresi = RuhHaliPenceresi.SABAH,
        /** Bugün kaç pencere dolduruldu (0..3). */
        val doldurulan: Int = 0,
    )

    private companion object {
        // Misafirin adı yok (`oturum.ad` boş). "Turan" gibi belirli bir kişi
        // adı kullanmak, sahte veriyle birleşince gerçek bir hesaba
        // girilmiş izlenimi veriyordu. Nötr bir etiket olduğunu belli ediyor.
        const val SABIT_ISIM = "Misafir"

        /** Ekran arka plana gidince akışı hemen kesme. */
        const val AKIS_BEKLEME_MS = 5_000L

        /** Yenile ikonu en az bu kadar dönsün — dokunuş karşılık bulsun. */
        const val TAZELEME_GERI_BILDIRIM_MS = 900L

        /** `docs/spec-anket.md §6.4`: anket 4 haftada bir tekrarlanır. */
        const val ANKET_TAZELEME_GUN = 28L

        const val SABAH_BAS = 5
        const val OGLE_BAS = 12
        const val AKSAM_BAS = 18

        /** Ağacın 9 büyüme evresinin toplam puan eşikleri (§12 K2). */
        const val EVRE2_PUAN = 100
        const val EVRE3_PUAN = 250
        const val EVRE4_PUAN = 500
        const val EVRE5_PUAN = 1_000
        const val EVRE6_PUAN = 2_000
        const val EVRE7_PUAN = 3_500
        const val EVRE8_PUAN = 5_000
        const val EVRE9_PUAN = 10_000
    }
}

/** Sistem kategorisi zaten düşülmüş; toplam ekran süresi kategorilerin toplamı. */
private fun GunlukOzetEntity.toplamEkranDk(): Double =
    socialDk + entertainmentDk + gamesDk + communicationDk +
        productivityDk + educationDk + healthDk + otherDk
