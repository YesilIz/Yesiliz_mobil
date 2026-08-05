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
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.ui.ikonu
import tr.org.yesilay.yesiliz.data.depo.OdulDeposu
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
 *
 * Bazı kademelerde tek bir ödül yerine seçenek var (§12 K28: market kuponu
 * kullanmak istemeyen kullanıcıya fidan bağışı alternatifi). Seçim
 * [OdulDeposu]'nda kalıcı — bir kere yapılınca değişmiyor.
 */
@HiltViewModel
class OdulViewModel @Inject constructor(
    private val puanDeposu: PuanDeposu,
    private val odulDeposu: OdulDeposu,
    tercihler: tr.org.yesilay.yesiliz.data.tercih.Tercihler,
    private val saat: Clock,
) : ViewModel() {

    val durum: StateFlow<OdulDurum> = combine(
        puanDeposu.toplamPuan,
        puanDeposu.aktifGunSayisi,
        puanDeposu.sonKayitlar(),
        odulDeposu.secimlerAkisi,
        tercihler.oturum,
    ) { puan, aktifGun, kayitlar, secimler, oturum ->
        val ham = KADEMELER.map { it.duruma(puan, aktifGun, secimler[it.id]) }
        // Kilitli kademelerin EN DÜŞÜĞÜ "aktif" — üzerinde ilerleme çubuğu
        // gösterilen, kullanıcının hedeflediği ödül bu. Seçim bekleyen bir
        // kademe zaten kazanılmış sayılıyor, "sıradaki" olamaz.
        val siradaki = ham.filter { it.hal == OdulHali.KILITLI }.minByOrNull { it.esik }
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

    /**
     * Seçim bekleyen bir kademe için kullanıcının kararını kaydeder.
     *
     * Kademe için zaten bir seçim varsa [OdulDeposu.sec] `false` döner ve
     * hiçbir şey değişmez — seçim geri alınamaz (CLAUDE.md §4).
     */
    fun secYap(kademeId: String, secenekId: String) {
        viewModelScope.launch { odulDeposu.sec(kademeId, secenekId) }
    }

    /** `docs/spec-puan.md §9.1` — kademe tanımı. */
    private data class SecenekTanimi(
        val id: String,
        val ad: String,
        val ikon: ImageVector,
        val aciklama: String,
    )

    private data class Kademe(
        val id: String,
        val esik: Int,
        val aktifGunKosulu: Int,
        val secenekler: List<SecenekTanimi>,
    ) {
        fun duruma(puan: Int, aktifGun: Int, secilenId: String?): Odul {
            val hakEdildi = puan >= esik && aktifGun >= aktifGunKosulu
            val cokluSecenek = secenekler.size > 1
            val secilen = secilenId?.let { id -> secenekler.firstOrNull { it.id == id } }
            val gosterim = secilen ?: secenekler.first()

            val hal = when {
                !hakEdildi -> OdulHali.KILITLI
                cokluSecenek && secilen == null -> OdulHali.SECIM_BEKLIYOR
                else -> OdulHali.KAZANILDI
            }

            return Odul(
                id = id,
                ad = gosterim.ad,
                esik = esik,
                ikon = gosterim.ikon,
                hal = hal,
                aciklama = gosterim.aciklama,
                secenekler = secenekler.map { Secenek(it.id, it.ad, it.ikon, it.aciklama) },
            )
        }
    }

    private companion object {
        /** Hesap sistemi öncesi ad — AnaViewModel ve Profil ile aynı. */
        const val SABIT_ISIM = "Turan"

        const val AKIS_BEKLEME_MS = 5_000L

        val KADEMELER = listOf(
            Kademe(
                id = "rozet",
                esik = 500,
                aktifGunKosulu = 0,
                secenekler = listOf(
                    SecenekTanimi(
                        id = "rozet",
                        ad = "Dijital rozet",
                        ikon = Icons.Filled.MilitaryTech,
                        aciklama = "İlk 500 puanı topladığın için profilinde görünen " +
                            "bir rozet kazanırsın.",
                    ),
                ),
            ),
            Kademe(
                id = "fidan",
                esik = 2_000,
                aktifGunKosulu = 14,
                secenekler = listOf(
                    SecenekTanimi(
                        id = "fidan_bagisi",
                        ad = "Adına fidan bağışı",
                        ikon = Icons.Filled.Park,
                        aciklama = "Ekran süreni azaltarak kazandığın 2.000 puanla " +
                            "senin adına 1 fidan toprakla buluşur.",
                    ),
                ),
            ),
            Kademe(
                id = "kupon_market",
                esik = 5_000,
                aktifGunKosulu = 30,
                secenekler = listOf(
                    SecenekTanimi(
                        id = "market_kuponu",
                        ad = "Yeşilay Market kuponu",
                        ikon = Icons.Filled.ShoppingBasket,
                        aciklama = "5.000 puana ulaştığında Yeşilay Market'te " +
                            "geçerli bir alışveriş kuponu kazanırsın.",
                    ),
                    SecenekTanimi(
                        id = "fidan_bagisi",
                        ad = "Adına fidan bağışı",
                        ikon = Icons.Filled.Park,
                        aciklama = "Market kuponu yerine, 5.000 puanla senin " +
                            "adına 1 fidan toprakla buluşur.",
                    ),
                ),
            ),
            Kademe(
                id = "kupon_spor",
                esik = 10_000,
                aktifGunKosulu = 45,
                secenekler = listOf(
                    SecenekTanimi(
                        id = "kupon_spor_kitap",
                        ad = "Spor / kitap kuponu",
                        ikon = Icons.Filled.SportsScore,
                        aciklama = "10.000 puana ulaştığında spor veya kitap " +
                            "alışverişinde geçerli bir kupon kazanırsın.",
                    ),
                    SecenekTanimi(
                        id = "fidan_bagisi",
                        ad = "Adına fidan bağışı",
                        ikon = Icons.Filled.Park,
                        aciklama = "Kupon yerine, 10.000 puanla senin adına 1 " +
                            "fidan toprakla buluşur.",
                    ),
                ),
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

