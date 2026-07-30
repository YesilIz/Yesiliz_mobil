package tr.org.yesilay.yesiliz.ui.ekran.riskdetay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tr.org.yesilay.yesiliz.data.depo.RiskDeposu
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.EtkileyenFaktor
import tr.org.yesilay.yesiliz.risk.Trend
import java.time.LocalDate
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
) : ViewModel() {

    val durum: StateFlow<RiskDetayDurum> = riskDao.sonuncuAkisi()
        .flatMapLatest { risk ->
            if (risk == null) {
                flowOf(RiskDetayDurum(veriVarMi = false))
            } else {
                faktorlerAkisi(risk.tarih).map { faktorler ->
                    val bant = Bant.valueOf(risk.gosterilenBant)
                    RiskDetayDurum(
                        veriVarMi = true,
                        bant = bant,
                        bantEtiketiKisa = bantKisaEtiket(bant),
                        aciklama = aciklama(bant),
                        trendMesaji = trendMesaji(Trend.valueOf(risk.trend)),
                        oran = (risk.R / 100.0).toFloat().coerceIn(0f, 1f),
                        faktorler = faktorler,
                        oneriler = VarsayilanOneriler,
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
            initialValue = RiskDetayDurum(),
        )

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

    private fun trendMesaji(trend: Trend): String? = when (trend) {
        Trend.IYILESIYOR -> "Geçen aya göre iyileşiyor"
        Trend.KOTULESIYOR -> "Geçen aya göre kötüleşiyor"
        Trend.SABIT -> "Geçen aya göre aynı seviyede"
        Trend.BELIRSIZ -> null
    }

    private companion object {
        const val AKIS_BEKLEME_MS = 5_000L
    }
}
