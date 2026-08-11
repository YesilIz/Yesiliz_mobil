package tr.org.yesilay.yesiliz.ui.ekran.riskdetay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tr.org.yesilay.yesiliz.data.depo.RiskDeposu
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.yerel.entity.gosterilebilirBant
import tr.org.yesilay.yesiliz.data.yerel.entity.trendVeyaNull
import tr.org.yesilay.yesiliz.data.yerel.entity.veriDurumuVeyaYetersiz
import tr.org.yesilay.yesiliz.data.zaman.GuncelGun
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.EtkileyenFaktor
import tr.org.yesilay.yesiliz.risk.Sabitler
import tr.org.yesilay.yesiliz.risk.Trend
import tr.org.yesilay.yesiliz.risk.VeriDurumu
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Risk analizi — `risk_sonuc`'un son satırı + o günün "etkileyen faktörleri".
 *
 * `RiskSonucDao.sonuncuAkisi()` zaten `AnaViewModel`'de kullanılıyor; burada
 * aynı akış, artı [RiskDeposu.etkileyenFaktorler] ile tek sorguya daha
 * (o günün ham girdisi) uzanıyor — çok günlü hesap DEĞİL, tek bir satırın
 * detayı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RiskDetayViewModel @Inject constructor(
    riskDao: RiskSonucDao,
    private val riskDeposu: RiskDeposu,
    guncelGun: GuncelGun,
) : ViewModel() {

    val durum: StateFlow<RiskDetayDurum> = combine(
        riskDao.sonuncuAkisi(),
        guncelGun.akis,
    ) { risk, bugun -> risk to bugun }
        .flatMapLatest { (risk, bugun) ->
            if (risk == null) {
                flowOf(RiskDetayDurum(veriVarMi = false))
            } else {
                faktorlerAkisi(risk.tarih).map { faktorler ->
                    when (etkinVeriDurumu(risk.veriDurumu, risk.tarih, bugun)) {
                        // Yetersiz/eski ölçüme dayanan bir bant, "veri yok"
                        // ekranından daha yanıltıcı olurdu — 34 saniyelik
                        // kullanımı olan biri "AZ RİSKLİ" görmemeli
                        // (`gorev2.md` §8, aynı sınıf hata: debug.md K-7).
                        VeriDurumu.YETERSIZ -> RiskDetayDurum(veriVarMi = true)
                        VeriDurumu.ESKI -> RiskDetayDurum(
                            veriVarMi = true,
                            bosDurumBaslik = "Risk analizin güncellenmeyi bekliyor",
                            bosDurumAciklama = "Uygulamayı son açtığından beri birkaç " +
                                "gün geçti; yeni bir analiz için uygulamayı kullanmaya " +
                                "devam et.",
                        )
                        VeriDurumu.HESAPLANDI -> {
                            val bant = risk.gosterilebilirBant()
                                ?: return@map RiskDetayDurum(veriVarMi = true)
                            RiskDetayDurum(
                                veriVarMi = true,
                                bant = bant,
                                bantEtiketiKisa = bantKisaEtiket(bant),
                                aciklama = aciklama(bant),
                                trendMesaji = trendMesaji(risk.trend.trendVeyaNull()),
                                oran = (risk.R / 100.0).toFloat().coerceIn(0f, 1f),
                                faktorler = faktorler,
                                // YEDAM teklifi yalnızca yüksek bantta.
                                oneriler = if (bant == Bant.COK) {
                                    VarsayilanOneriler + YedamOnerisi
                                } else {
                                    VarsayilanOneriler
                                },
                            )
                        }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
            initialValue = RiskDetayDurum(),
        )

    /**
     * [risk_sonuc.veriDurumu] hesap ANINDA yazıldı ([VeriDurumu.ESKI] hiç
     * üretmez — `RiskDeposu` her günü kendi tarihi için taze hesaplıyor).
     * Eskilik GÖSTERİM anında türetiliyor: son satır bugünden
     * [Sabitler.VERI_ESKI_ESIK_GUN] günden fazla eskiyse, kullanıcı
     * uygulamayı birkaç gündür açmamış demektir ve o bant artık güncel
     * değildir.
     */
    private fun etkinVeriDurumu(
        kayitliDurum: String,
        tarih: LocalDate,
        bugun: LocalDate,
    ): VeriDurumu {
        if (kayitliDurum.veriDurumuVeyaYetersiz() != VeriDurumu.HESAPLANDI) {
            return VeriDurumu.YETERSIZ
        }
        val gunGecmisi = ChronoUnit.DAYS.between(tarih, bugun)
        return if (gunGecmisi > Sabitler.VERI_ESKI_ESIK_GUN) VeriDurumu.ESKI else VeriDurumu.HESAPLANDI
    }

    private fun faktorlerAkisi(tarih: LocalDate): Flow<List<EtkileyenFaktor>> = flow {
        emit(riskDeposu.etkileyenFaktorler(tarih) ?: emptyList())
    }

    private fun bantKisaEtiket(bant: Bant): String = when (bant) {
        Bant.AZ -> "Düşük"
        Bant.ORTA -> "Orta"
        Bant.COK -> "Yüksek"
    }

    /**
     * Betimleyici cümle — bant adı hiçbir zaman tek başına, teşhis dili yok
     * (CLAUDE.md §4). Ekran 05 (İlk sonuç) ile aynı ton.
     */
    private fun aciklama(bant: Bant): String = when (bant) {
        Bant.AZ ->
            "Son 7 günlük kullanım örüntün sağlıklı bir düzeyde görünüyor."
        Bant.ORTA ->
            "Son 7 günlük kullanım örüntün, dikkat edilmesi gereken birkaç " +
                "alan olduğunu gösteriyor."
        Bant.COK ->
            "Son 7 günlük kullanım örüntün, üzerinde durulması gereken " +
                "belirgin alanlar olduğunu gösteriyor."
    }

    private fun trendMesaji(trend: Trend?): String? = when (trend) {
        Trend.IYILESIYOR -> "Geçen aya göre iyileşiyor"
        Trend.KOTULESIYOR -> "Geçen aya göre kötüleşiyor"
        Trend.SABIT -> "Geçen aya göre aynı seviyede"
        Trend.BELIRSIZ -> null
        null -> null
    }

    private companion object {
        const val AKIS_BEKLEME_MS = 5_000L
    }
}
