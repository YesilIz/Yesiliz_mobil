package tr.org.yesilay.yesiliz.ui.ekran.aktivite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.data.zaman.GuncelGun
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.ui.gezinme.Rota
import tr.org.yesilay.yesiliz.ui.ikonu
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AktiviteViewModel @Inject constructor(
    private val puanDeposu: PuanDeposu,
    guncelGun: GuncelGun,
) : ViewModel() {

    val durum: StateFlow<AktiviteDurum> =
        guncelGun.akis.flatMapLatest(puanDeposu::gununGorevleri)
            .map { tamamlamalar ->
                val sayilar = tamamlamalar.groupingBy { it.gorevId }.eachCount()
                AktiviteDurum(
                    kartlar = AKTIVITELER.map { (gorev, rota) ->
                        AktiviteKarti(
                            gorev = gorev,
                            ikon = gorev.ikonu(),
                            rota = rota,
                            bugunYapilan = sayilar[gorev.id] ?: 0,
                        )
                    },
                    bugunKazanilan = tamamlamalar
                        .filter { it.gorevId in AKTIVITE_IDLERI }
                        .sumOf { it.puan },
                    yukleniyor = false,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
                initialValue = AktiviteDurum(),
            )

    private companion object {
        const val AKIS_BEKLEME_MS = 5_000L

        /**
         * Sekmede gösterilen aktiviteler ve rotaları.
         *
         * Sıra bilinçli: en kısası (göz molası) ile en uzunu (odak) uçlarda
         * değil; kullanıcı listeyi tarayınca "şu an hangisine vaktim var"
         * sorusunu kolayca yanıtlayabilsin diye süreye göre artan.
         */
        val AKTIVITELER = listOf(
            Gorev.GOZ_MOLASI to Rota.GOZ_MOLASI,
            Gorev.NEFES to Rota.NEFES,
            Gorev.EKRANSIZ to Rota.EKRANSIZ,
            Gorev.YURUYUS to Rota.YURUYUS,
            Gorev.ODAK to Rota.ODAK,
            Gorev.GECE_KAPANISI to Rota.GECE_KAPANISI,
        )

        val AKTIVITE_IDLERI = AKTIVITELER.map { it.first.id }.toSet()
    }
}
