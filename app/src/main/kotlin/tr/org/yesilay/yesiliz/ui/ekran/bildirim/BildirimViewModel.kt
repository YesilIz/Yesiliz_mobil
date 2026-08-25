package tr.org.yesilay.yesiliz.ui.ekran.bildirim

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.depo.BildirimDeposu
import tr.org.yesilay.yesiliz.data.yerel.entity.BildirimEntity
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

    /** Son silinen kayıt — "Geri al" bunun üzerinden çalışıyor. */
    private val _sonSilinen = MutableStateFlow<BildirimEntity?>(null)
    val sonSilinen: StateFlow<BildirimEntity?> = _sonSilinen.asStateFlow()

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

    /**
     * Kaydırarak silme.
     *
     * Silinen satır [sonSilinen]'de tutuluyor ki "Geri al" gerçekten geri
     * alabilsin. Geri alınamayan bir silme, yanlışlıkla kaydıran kullanıcıya
     * kaydı kalıcı olarak kaybettirirdi.
     */
    fun sil(id: Long) {
        viewModelScope.launch {
            val kayit = depo.getir(id) ?: return@launch
            depo.sil(id)
            _sonSilinen.value = kayit
        }
    }

    fun silmeyiGeriAl() {
        val kayit = _sonSilinen.value ?: return
        _sonSilinen.value = null
        viewModelScope.launch { depo.geriYukle(kayit) }
    }

    fun geriAlmaSuresiDoldu() {
        _sonSilinen.value = null
    }

    /** "Tümünü temizle" — ekran önce onay soruyor, burada geri alma yok. */
    fun hepsiniSil() {
        viewModelScope.launch {
            depo.hepsiniSil()
            _sonSilinen.value = null
        }
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
        BildirimTuru.MOLA -> Icons.Filled.Timer
        BildirimTuru.EBEVEYN -> Icons.Filled.FamilyRestroom
        BildirimTuru.HATIRLATMA -> Icons.Filled.Notifications
        // Nötr bir ikon: düzeltme bir uyarı ya da ceza değil (K56).
        BildirimTuru.DUZELTME -> Icons.Filled.Balance
        BildirimTuru.DUYURU -> Icons.Filled.Campaign
        null -> Icons.Filled.CheckCircle
    }
