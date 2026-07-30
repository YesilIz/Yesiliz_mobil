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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.data.kullanim.KullanimIzni
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.PuanGunlukDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.risk.Bant
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
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
@HiltViewModel
class AnaViewModel @Inject constructor(
    @ApplicationContext private val baglam: Context,
    private val puanDeposu: PuanDeposu,
    tercihler: Tercihler,
    ozetDao: GunlukOzetDao,
    riskDao: RiskSonucDao,
    puanDao: PuanGunlukDao,
    private val saat: Clock,
) : ViewModel() {

    /**
     * Gün, ViewModel kurulurken sabitleniyor.
     *
     * Gece yarısını geçerse ekran eski günü gösterir; kullanıcı uygulamayı
     * yeniden açtığında düzelir. Saniye başı gün kontrolü bu ekranın
     * karmaşıklığına değmiyor.
     */
    private val bugun: LocalDate = LocalDate.now(saat)

    /** Kullanıcının kendi beyanı — risk skoruna GİRMİYOR, yalnızca gösterim. */
    private val ruhHali = MutableStateFlow(VARSAYILAN_RUH_HALI)

    val durum: StateFlow<AnaDurum> = combine(
        ozetDao.gununAkisi(bugun),
        riskDao.sonuncuAkisi(),
        puanDeposu.toplamPuan,
        puanDao.gununAkisi(bugun),
        tercihler.gunlukHedefDk,
    ) { ozet, risk, toplamPuan, gunlukPuan, hedef ->
        Ara(
            ozet = ozet,
            bant = risk?.let { Bant.valueOf(it.gosterilenBant) },
            toplamPuan = toplamPuan,
            seriGun = gunlukPuan?.seriGun ?: 0,
            hedefDk = hedef,
        )
    }.combine(tercihler.ebeveynEslesmisMi) { ara, ebeveynAktif ->
        ara.copy(ebeveynAktifMi = ebeveynAktif)
    }.combine(puanDeposu.gununGorevleri(bugun)) { ara, tamamlananlar ->
        val tamamlananIdler = tamamlananlar.map { it.gorevId }.toSet()
        AnaDurum(
            selamlama = selamlama(),
            isim = SABIT_ISIM,
            serideGun = ara.seriGun,
            agacEvresi = agacEvresi(ara.seriGun),
            puan = ara.toplamPuan,
            ekranDakika = ara.ozet?.toplamEkranDk()?.roundToInt() ?: 0,
            hedefDakika = ara.hedefDk,
            adim = ara.ozet?.adim?.roundToInt() ?: 0,
            ruhHali = ruhHali.value,
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
            izinVarMi = KullanimIzni.verildiMi(baglam),
            ebeveynAktifMi = ara.ebeveynAktifMi,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
        initialValue = AnaDurum(selamlama = selamlama(), isim = SABIT_ISIM),
    )

    fun ruhHaliDegisti(deger: Float) {
        ruhHali.value = deger
    }

    /**
     * Görevi tamamlar. Tekrar dokununca **geri alınmıyor**: verilen puanı
     * silmek ceza olurdu (CLAUDE.md §4). Günlük limit `PuanDeposu`'nda.
     */
    fun gorevTiklandi(id: String) {
        val gorev = Gorev.idden(id) ?: return
        viewModelScope.launch { puanDeposu.gorevTamamla(gorev, bugun) }
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
    )

    private companion object {
        // Hesap sistemi Faz 2 — o zamana kadar sabit.
        const val SABIT_ISIM = "Turan"

        /** Ekran arka plana gidince akışı hemen kesme. */
        const val AKIS_BEKLEME_MS = 5_000L

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

/** Görev → ikon. Katalog `domain/`de, ikon `ui/`de — katmanlar karışmasın. */
private fun Gorev.ikonu(): ImageVector = when (this) {
    Gorev.NEFES -> Icons.Filled.SelfImprovement
    Gorev.DIJITAL_MOLA -> Icons.Filled.Timer
    Gorev.YURUYUS -> Icons.AutoMirrored.Filled.DirectionsWalk
    Gorev.RUH_HALI -> Icons.Filled.MoodBad
    Gorev.UYKU_BEYANI -> Icons.Filled.Bedtime
    Gorev.GECE_TEMIZ -> Icons.Filled.NightsStay
    Gorev.TUKETIM_HEDEFI -> Icons.Filled.TrackChanges
}
