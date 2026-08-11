package tr.org.yesilay.yesiliz.ui.ekran.bildirim

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
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
import tr.org.yesilay.yesiliz.data.depo.BildirimDeposu
import tr.org.yesilay.yesiliz.domain.bildirim.BildirimGrubu
import tr.org.yesilay.yesiliz.domain.bildirim.BildirimGrupla
import tr.org.yesilay.yesiliz.domain.bildirim.BildirimTuru
import tr.org.yesilay.yesiliz.data.zaman.GuncelGun
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class BildirimViewModel @Inject constructor(
    private val depo: BildirimDeposu,
    private val saat: Clock,
    guncelGun: GuncelGun,
) : ViewModel() {

    val durum: StateFlow<BildirimDurum> = combine(
        depo.sonlar(),
        guncelGun.akis,
    ) { kayitlar, bugun ->
        val simdi = Instant.now(saat)
        val zon = saat.zone

        // Gruplar, kayıtların gerçekten düştüğü gruplarla sınırlı —
        // boş "DÜN" başlığı göstermek ekranı yalancı doldurur.
        val gruplu = kayitlar.groupBy { BildirimGrupla.grubu(it.zaman, bugun, zon) }

        BildirimDurum(
                gruplar = BildirimGrubu.entries.mapNotNull { grup ->
                    gruplu[grup]?.takeIf { it.isNotEmpty() }?.let { satirlar ->
                        BildirimGrubuModel(
                            grup = grup,
                            satirlar = satirlar.map { kayit ->
                                BildirimSatiriModel(
                                    id = kayit.id,
                                    baslik = kayit.baslik,
                                    govde = kayit.govde,
                                    zamanEtiketi = BildirimGrupla.zamanEtiketi(
                                        kayit.zaman,
                                        simdi,
                                        zon,
                                    ),
                                    ikon = ikonu(kayit.tur),
                                    okundu = kayit.okundu,
                                )
                            },
                        )
                    }
                },
                okunmamisVarMi = kayitlar.any { !it.okundu },
                yukleniyor = false,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
            initialValue = BildirimDurum(),
        )

    fun tumunuOku() {
        viewModelScope.launch { depo.tumunuOku() }
    }

    private companion object {
        const val AKIS_BEKLEME_MS = 5_000L
    }
}

/** Tür → ikon. Katalog `domain/`de, ikon `ui/`de — katmanlar karışmasın. */
private fun ikonu(turAnahtari: String): ImageVector =
    when (BildirimTuru.anahtardan(turAnahtari)) {
        BildirimTuru.PUAN -> Icons.Filled.Eco
        BildirimTuru.HEDEF -> Icons.Filled.TrackChanges
        BildirimTuru.SERI -> Icons.Filled.LocalFireDepartment
        BildirimTuru.LIG -> Icons.Filled.EmojiEvents
        BildirimTuru.MOLA -> Icons.Filled.Timer
        BildirimTuru.DEGERLENDIRME -> Icons.AutoMirrored.Filled.FactCheck
        BildirimTuru.EBEVEYN -> Icons.Filled.FamilyRestroom
        BildirimTuru.HATIRLATMA -> Icons.Filled.Notifications
        null -> Icons.Filled.CheckCircle
    }
