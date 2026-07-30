package tr.org.yesilay.yesiliz.ui.ekran.odul

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.data.yerel.entity.GorevTamamlamaEntity
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Ödüller — **gerçek puandan** besleniyor.
 *
 * Kademeler ve ek koşullar `docs/spec-puan.md §9.1`'den. Ödül yalnızca puan
 * eşiğiyle değil **aktif gün** koşuluyla da açılıyor: tek günde patlama yapıp
 * ödül almayı engelliyor (§9.2 anti-gaming).
 */
@HiltViewModel
class OdulViewModel @Inject constructor(
    private val puanDeposu: PuanDeposu,
    tercihler: tr.org.yesilay.yesiliz.data.tercih.Tercihler,
    private val saat: Clock,
) : ViewModel() {

    val durum: StateFlow<OdulDurum> = combine(
        puanDeposu.toplamPuan,
        puanDeposu.aktifGunSayisi,
        puanDeposu.sonKayitlar(),
        tercihler.oturum,
    ) { puan, aktifGun, kayitlar, oturum ->
        val ham = KADEMELER.map { it.duruma(puan, aktifGun) }
        // Kilitli kademelerin EN DÜŞÜĞÜ "aktif" — üzerinde ilerleme çubuğu
        // gösterilen, kullanıcının hedeflediği ödül bu.
        val siradaki = ham.filter { it.hal != OdulHali.KAZANILDI }.minByOrNull { it.esik }
        val basamaklar = ham.map {
            if (it.id == siradaki?.id) it.copy(hal = OdulHali.AKTIF) else it
        }

        OdulDurum(
            // Misafirin adı yok; sertifikada sabite düşülüyor.
            isim = oturum.ad.ifBlank { SABIT_ISIM },
            puan = puan,
            sonrakiEsik = siradaki?.esik ?: ham.maxOf { it.esik },
            basamaklar = basamaklar.sortedByDescending { it.esik },
            gecmis = kayitlar.map { it.kaydaCevir(LocalDate.now(saat)) },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
        initialValue = OdulDurum(),
    )

    /** `docs/spec-puan.md §9.1` — kademe tanımı. */
    private data class Kademe(
        val id: String,
        val ad: String,
        val esik: Int,
        val aktifGunKosulu: Int,
        val ikon: ImageVector,
        val aciklama: String,
    ) {
        fun duruma(puan: Int, aktifGun: Int) = Odul(
            id = id,
            ad = ad,
            esik = esik,
            ikon = ikon,
            hal = when {
                puan >= esik && aktifGun >= aktifGunKosulu -> OdulHali.KAZANILDI
                else -> OdulHali.KILITLI
            },
            aciklama = aciklama,
        )
    }

    private companion object {
        /** Hesap sistemi öncesi ad — AnaViewModel ve Profil ile aynı. */
        const val SABIT_ISIM = "Turan"

        const val AKIS_BEKLEME_MS = 5_000L

        val KADEMELER = listOf(
            Kademe(
                id = "rozet",
                ad = "Dijital rozet",
                esik = 500,
                aktifGunKosulu = 0,
                ikon = Icons.Filled.MilitaryTech,
                aciklama = "İlk 500 puanı topladığın için profilinde görünen bir " +
                    "rozet kazanırsın.",
            ),
            Kademe(
                id = "fidan",
                ad = "Adına fidan bağışı",
                esik = 2_000,
                aktifGunKosulu = 14,
                ikon = Icons.Filled.Park,
                aciklama = "Ekran süreni azaltarak kazandığın 2.000 puanla senin " +
                    "adına 1 fidan toprakla buluşur.",
            ),
            Kademe(
                id = "kupon_market",
                ad = "Yeşilay Market kuponu",
                esik = 5_000,
                aktifGunKosulu = 30,
                ikon = Icons.Filled.ShoppingBasket,
                aciklama = "5.000 puana ulaştığında Yeşilay Market'te geçerli bir " +
                    "alışveriş kuponu kazanırsın.",
            ),
            Kademe(
                id = "kupon_spor",
                ad = "Spor / kitap kuponu",
                esik = 10_000,
                aktifGunKosulu = 45,
                ikon = Icons.Filled.SportsScore,
                aciklama = "10.000 puana ulaştığında spor veya kitap alışverişinde " +
                    "geçerli bir kupon kazanırsın.",
            ),
        )
    }
}

/**
 * Görev kaydını puan geçmişi satırına çevirir.
 *
 * Puan **her zaman pozitif** — `gorev_tamamlama` tablosuna negatif değer
 * yazan bir yol yok (CLAUDE.md §4).
 */
private fun GorevTamamlamaEntity.kaydaCevir(bugun: LocalDate): PuanKaydi {
    val gorev = Gorev.idden(gorevId)
    return PuanKaydi(
        baslik = gorev?.baslik ?: gorevId,
        zaman = zamanMetni(tarih, bugun),
        puan = puan,
        ikon = gorev.ikonu(),
    )
}

private fun zamanMetni(tarih: LocalDate, bugun: LocalDate): String =
    when (val gun = ChronoUnit.DAYS.between(tarih, bugun).toInt()) {
        0 -> "Bugün"
        1 -> "Dün"
        else -> "$gun gün önce"
    }

private fun Gorev?.ikonu(): ImageVector = when (this) {
    Gorev.NEFES -> Icons.Filled.SelfImprovement
    Gorev.DIJITAL_MOLA -> Icons.Filled.Timer
    Gorev.YURUYUS -> Icons.AutoMirrored.Filled.DirectionsWalk
    Gorev.RUH_HALI -> Icons.Filled.MoodBad
    Gorev.UYKU_BEYANI -> Icons.Filled.Bedtime
    Gorev.GECE_TEMIZ -> Icons.Filled.NightsStay
    Gorev.TUKETIM_HEDEFI -> Icons.Filled.TrackChanges
    null -> Icons.Filled.Park
}
