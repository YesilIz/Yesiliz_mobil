package tr.org.yesilay.yesiliz.ui.ekran.bildirim

import androidx.compose.ui.graphics.vector.ImageVector
import tr.org.yesilay.yesiliz.domain.bildirim.BildirimGrubu

/** Bildirimler ekranı — `docs/tasarim-referans.md §2 / 35`. */
data class BildirimDurum(
    val gruplar: List<BildirimGrubuModel> = emptyList(),
    val okunmamisVarMi: Boolean = false,
    val yukleniyor: Boolean = true,
)

data class BildirimGrubuModel(
    val grup: BildirimGrubu,
    val satirlar: List<BildirimSatiriModel>,
)

data class BildirimSatiriModel(
    val id: Long,
    val baslik: String,
    val govde: String,
    val zamanEtiketi: String,
    val ikon: ImageVector,
    val okundu: Boolean,
)
