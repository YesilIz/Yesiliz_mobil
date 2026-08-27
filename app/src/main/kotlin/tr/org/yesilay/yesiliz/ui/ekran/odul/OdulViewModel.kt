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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.ui.ikonu
import tr.org.yesilay.yesiliz.data.depo.OdulDeposu
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.data.yerel.entity.GorevTamamlamaEntity
import tr.org.yesilay.yesiliz.data.zaman.GuncelGun
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
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
    guncelGun: GuncelGun,
) : ViewModel() {

    /**
     * Sunucudan gelen açık ödül seçenekleri (K28 kapısı).
     *
     * Boş başlıyor ve boş kalırsa kademe **kapalı** görünüyor: sunucuya
     * ulaşılamadığında karşılanamayacak bir ödülü seçtirmemek için
     * fail-closed (§4, K37).
     */
    private val _acikSecenekler = MutableStateFlow<Set<Pair<String, String>>>(emptySet())

    init {
        viewModelScope.launch {
            _acikSecenekler.value = odulDeposu.acikSecenekler()
        }
    }

    val durum: StateFlow<OdulDurum> = combine(
        // Kademe **kazanılmıştan** okunuyor (U6, K28): hak edilmiş bir
        // ödül panelden yapılan bir düşürmeyle geri gitmiyor.
        puanDeposu.kazanilmisPuan,
        puanDeposu.aktifGunSayisi,
        combine(puanDeposu.sonKayitlar(), guncelGun.akis) { kayitlar, bugun ->
            kayitlar.map { it.kaydaCevir(bugun) }
        },
        odulDeposu.secimlerAkisi,
        // `combine` en fazla beş akış için tipli; altıncısı çiftleniyor.
        combine(tercihler.oturum, _acikSecenekler, ::Pair),
    ) { puan, aktifGun, gecmis, secimler, oturumVeStok ->
        val (oturum, acikSecenekler) = oturumVeStok
        // Maddi ödül kapısı artık derlemeye gömülü değil, sunucudaki stok
        // (K28, A4'ün teknik ayağı). Ulaşılamazsa kapalı: "bilmiyoruz"u
        // "açık" saymak, karşılanamayacak bir ödülü seçtirmek olurdu.
        //
        // **Kapı yalnızca teslimat gerektiren kademelere uygulanıyor.**
        // Eskiden koşulsuzdu ve sunucu yalnızca kuponları tanıdığı için
        // dijital rozet de `false`'a düşüyordu: 500 puana ulaşan HER
        // kullanıcı, tedarik zinciri gerektirmeyen bir başarı kartı için
        // "teslimat etkin değil" görüyordu (§12 K64).
        val ham = KADEMELER.map { kademe ->
            kademe
                .copy(teslimatEtkinMi = kademe.teslimatAcikMi(acikSecenekler))
                .duruma(puan, aktifGun, secimler[kademe.id])
        }
        // Kilitli kademelerin EN DÜŞÜĞÜ "aktif" — üzerinde ilerleme çubuğu
        // gösterilen, kullanıcının hedeflediği ödül bu. Seçim bekleyen bir
        // kademe zaten kazanılmış sayılıyor, "sıradaki" olamaz.
        val siradaki = ham.filter { it.hal == OdulHali.KILITLI }.minByOrNull { it.esik }
        val basamaklar = ham.map {
            if (it.id == siradaki?.id) it.copy(hal = OdulHali.AKTIF) else it
        }

        OdulDurum(
            // Misafirin adı yok; uygulama içi başarı kartında sabite düşülüyor.
            isim = oturum.ad.ifBlank { SABIT_ISIM },
            puan = puan,
            sonrakiEsik = siradaki?.esik ?: ham.maxOf { it.esik },
            basamaklar = basamaklar.sortedByDescending { it.esik },
            gecmis = gecmis,
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

    /**
     * `docs/spec-puan.md §9.1` — kademe tanımı.
     *
     * Metin **teslimat durumuna göre** değişiyor. Eskiden tek bir ad ve tek
     * bir açıklama vardı, ikisi de "planlanan" dilindeydi ve kapı sunucudaki
     * stoğa taşındığında (K62) güncellenmemişti: panelden parti yüklenince
     * kademe seçilebilir oluyor ama kullanıcı "Planlanan market kuponu —
     * sponsorluk ve teslimat altyapısı henüz etkin değil" yazan bir seçeneği
     * seçiyordu. K62'nin "parti yüklenince kademe açılıyor" vaadi, metin
     * derlemeye gömülü kaldığı için APK yayını olmadan tamamlanmıyordu.
     */
    internal data class SecenekTanimi(
        val id: String,
        /** Teslimat açıkken görünen ad. */
        val ad: String,
        val ikon: ImageVector,
        /** Teslimat açıkken görünen açıklama. */
        val aciklama: String,
        /** Teslimat kapalıyken görünen ad; dijital ödüllerde ikisi aynı. */
        val planlananAd: String = ad,
        /** Teslimat kapalıyken görünen açıklama. */
        val planlananAciklama: String = aciklama,
    ) {
        fun gorunen(teslimatEtkinMi: Boolean) = Secenek(
            id = id,
            ad = if (teslimatEtkinMi) ad else planlananAd,
            ikon = ikon,
            aciklama = if (teslimatEtkinMi) aciklama else planlananAciklama,
        )
    }

    internal data class Kademe(
        val id: String,
        val esik: Int,
        val aktifGunKosulu: Int,
        val secenekler: List<SecenekTanimi>,
        /**
         * Kademe fiziksel bir teslimat/bağış gerektiriyor mu?
         *
         * `false` olan tek kademe dijital rozet: karşılığı uygulama içi bir
         * başarı kartı (K28), tedarik zinciri yok ve sunucunun
         * `SECILEBILIR_ODULLER` sözlüğünde de yok — yani stok kapısı ona
         * uygulanırsa kademe sonsuza kadar kapalı kalır.
         */
        val stogaBagli: Boolean = true,
        val teslimatEtkinMi: Boolean = true,
    ) {
        /** Stoğa bağlı olmayan kademe her zaman açık; gerisi sunucudaki stoğa bakar. */
        fun teslimatAcikMi(acikSecenekler: Set<Pair<String, String>>): Boolean =
            !stogaBagli || secenekler.any { (id to it.id) in acikSecenekler }

        fun duruma(puan: Int, aktifGun: Int, secilenId: String?): Odul {
            val hakEdildi = puan >= esik && aktifGun >= aktifGunKosulu
            val cokluSecenek = secenekler.size > 1
            val secilen = secilenId?.let { id -> secenekler.firstOrNull { it.id == id } }
            val gosterim = (secilen ?: secenekler.first()).gorunen(teslimatEtkinMi)

            val hal = odulHaliniBelirle(
                hakEdildi = hakEdildi,
                cokluSecenek = cokluSecenek,
                secimVarMi = secilen != null,
                teslimatEtkinMi = teslimatEtkinMi,
            )

            return Odul(
                id = id,
                ad = gosterim.ad,
                esik = esik,
                ikon = gosterim.ikon,
                hal = hal,
                aciklama = gosterim.aciklama,
                teslimatEtkinMi = teslimatEtkinMi,
                secenekler = secenekler.map { it.gorunen(teslimatEtkinMi) },
            )
        }
    }

    /**
     * `internal`, `private` değil: kademe tablosu ve stok kapısının ona nasıl
     * uygulandığı testten görülebilmeli. Kapı bir kez koşulsuz uygulandığı
     * için dijital rozet de kapanmıştı (§12 K64) ve bunu yakalayacak bir
     * test yoktu.
     */
    internal companion object {
        /** Misafirin görünür adı — AnaViewModel ve Profil ile aynı. */
        const val SABIT_ISIM = "Misafir"

        const val AKIS_BEKLEME_MS = 5_000L

        /** Üç kademede birden görünüyor; tanımı tek yerde dursun. */
        val FIDAN_BAGISI = SecenekTanimi(
            id = "fidan_bagisi",
            ad = "Fidan bağışı",
            ikon = Icons.Filled.Park,
            aciklama = "Senin adına bir fidan dikilir; bağış belgesi " +
                "uygulamada görünür.",
            planlananAd = "Planlanan fidan desteği",
            planlananAciklama = MADDI_ODUL_ACIKLAMASI,
        )

        val KADEMELER = listOf(
            Kademe(
                id = "rozet",
                esik = 500,
                aktifGunKosulu = 0,
                // Uygulama içi başarı kartı — teslim edilecek bir şey yok,
                // stok kapısı uygulanmıyor (bkz. [Kademe.stogaBagli]).
                stogaBagli = false,
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
                secenekler = listOf(FIDAN_BAGISI),
            ),
            Kademe(
                id = "kupon_market",
                esik = 5_000,
                aktifGunKosulu = 30,
                secenekler = listOf(
                    SecenekTanimi(
                        id = "market_kuponu",
                        ad = "Market kuponu",
                        ikon = Icons.Filled.ShoppingBasket,
                        aciklama = "Yeşilay'ın anlaşmalı olduğu marketlerde " +
                            "kullanabileceğin bir hediye kuponu.",
                        planlananAd = "Planlanan market kuponu",
                        planlananAciklama = MADDI_ODUL_ACIKLAMASI,
                    ),
                    FIDAN_BAGISI,
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
                        aciklama = "Spor malzemesi ya da kitap için " +
                            "kullanabileceğin bir hediye kuponu.",
                        planlananAd = "Planlanan spor / kitap kuponu",
                        planlananAciklama = MADDI_ODUL_ACIKLAMASI,
                    ),
                    FIDAN_BAGISI,
                ),
            ),
        )
    }
}

/** A4 kapanmadan kullanıcıya kupon, bağış veya teslimat sözü verilmez. */
internal const val MADDI_ODUL_ACIKLAMASI =
    "Bu ödül için sponsorluk ve teslimat altyapısı henüz etkin değil. " +
        "Puan eşiği yalnızca ilerleme hedefidir; kupon ya da bağış oluşturmaz."

internal fun odulHaliniBelirle(
    hakEdildi: Boolean,
    cokluSecenek: Boolean,
    secimVarMi: Boolean,
    teslimatEtkinMi: Boolean,
): OdulHali = when {
    hakEdildi && !teslimatEtkinMi -> OdulHali.PLANLANAN
    !hakEdildi -> OdulHali.KILITLI
    cokluSecenek && !secimVarMi -> OdulHali.SECIM_BEKLIYOR
    else -> OdulHali.KAZANILDI
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
