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
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
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
                ) { kayitli, suruklenen ->
                    RuhHaliDurumu(
                        deger = suruklenen ?: kayitli?.deger ?: VARSAYILAN_RUH_HALI,
                        kaydedildi = kayitli != null,
                    )
                },
            ) { ara, ruhHali -> ara.copy(ruhHali = ruhHali) }
                .combine(bildirimDeposu.okunmamisSayisi) { ara, okunmamis ->
                    ara.copy(okunmamisBildirim = okunmamis)
                }
                .combine(tercihler.oturum) { ara, oturum ->
                    // Misafir kullanıcının adı yok; sabit ada düşülüyor.
                    ara.copy(isim = oturum.ad.ifBlank { SABIT_ISIM })
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
                        serideGun = ara.seriGun,
                        agacEvresi = agacEvresi(ara.seriGun),
                        puan = ara.toplamPuan,
                        ekranDakika = ara.ozet?.toplamEkranDk()?.roundToInt() ?: 0,
                        hedefDakika = ara.hedefDk,
                        adim = ara.ozet?.adim?.roundToInt() ?: 0,
                        ruhHali = ara.ruhHali.deger,
                        ruhHaliKaydedildi = ara.ruhHali.kaydedildi,
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
                        veriVarMi = ara.ozet != null,
                        izinVarMi = false,
                        ebeveynAktifMi = ara.ebeveynAktifMi,
                        okunmamisBildirim = ara.okunmamisBildirim,
                        degerlendirmeYenilensin = ara.degerlendirmeYenilensin,
                    )
                }
                .combine(kullanimIzniVar) { anaDurum, izinVarMi ->
                    anaDurum.copy(izinVarMi = izinVarMi)
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
     * Parmak kalktı — değer kalıcı olarak yazılıyor ve o gün ilk kez
     * kaydediliyorsa "Ruh hali kaydı" görevinin puanı veriliyor.
     */
    fun ruhHaliBirakildi() {
        val deger = suruklenenRuhHali.value ?: return
        viewModelScope.launch {
            beyanDeposu.ruhHaliKaydet(guncelGun.bugun(), deger)
            suruklenenRuhHali.value = null
        }
    }

    fun degerlendirmeErtele() {
        degerlendirmeGizlendi.value = true
    }

    /** Kullanım erişimi normal izin geri çağrısı üretmediği için ON_RESUME yolu. */
    fun kullanimIzniniTazele() {
        kullanimIzniVar.value = KullanimIzni.verildiMi(baglam)
    }

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
     * Ağaç evresi **seriden** türüyor, risk bandından değil.
     *
     * `spec-puan.md` ilkesi: "durumu değil, çabayı ödüllendir". Ağacı risk
     * bandına bağlamak kötü bir günde ağacı kurutup kullanıcıyı cezalandırırdı.
     * Seriye bağlıyken ağaç yalnızca büyüyor.
     */
    private fun agacEvresi(seriGun: Int): Int = when {
        seriGun >= EVRE6_GUN -> 6
        seriGun >= EVRE5_GUN -> 5
        seriGun >= EVRE4_GUN -> 4
        seriGun >= EVRE3_GUN -> 3
        seriGun >= EVRE2_GUN -> 2
        else -> 1
    }

    private data class Ara(
        val ozet: GunlukOzetEntity?,
        val bant: Bant?,
        val toplamPuan: Int,
        val seriGun: Int,
        val hedefDk: Int,
        val ebeveynAktifMi: Boolean = false,
        val isim: String = SABIT_ISIM,
        val ruhHali: RuhHaliDurumu = RuhHaliDurumu(),
        val okunmamisBildirim: Int = 0,
        val degerlendirmeYenilensin: Boolean = false,
    )

    private data class RuhHaliDurumu(
        val deger: Float = VARSAYILAN_RUH_HALI,
        val kaydedildi: Boolean = false,
    )

    private companion object {
        // Misafirin adı yok (`oturum.ad` boş). "Turan" gibi belirli bir kişi
        // adı kullanmak, sahte veriyle birleşince gerçek bir hesaba
        // girilmiş izlenimi veriyordu. Nötr bir etiket olduğunu belli ediyor.
        const val SABIT_ISIM = "Misafir"

        /** Ekran arka plana gidince akışı hemen kesme. */
        const val AKIS_BEKLEME_MS = 5_000L

        /** `docs/spec-anket.md §6.4`: anket 4 haftada bir tekrarlanır. */
        const val ANKET_TAZELEME_GUN = 28L

        const val SABAH_BAS = 5
        const val OGLE_BAS = 12
        const val AKSAM_BAS = 18

        const val EVRE2_GUN = 3
        const val EVRE3_GUN = 7
        const val EVRE4_GUN = 14
        const val EVRE5_GUN = 30
        const val EVRE6_GUN = 60
    }
}

/** Sistem kategorisi zaten düşülmüş; toplam ekran süresi kategorilerin toplamı. */
private fun GunlukOzetEntity.toplamEkranDk(): Double =
    socialDk + entertainmentDk + gamesDk + communicationDk +
        productivityDk + educationDk + healthDk + otherDk
