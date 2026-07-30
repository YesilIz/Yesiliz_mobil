package tr.org.yesilay.yesiliz.ui.tema

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * DESIGN.md §1 köşe yarıçapları.
 *
 * > "Şekil dili organik ve dostane. Marka sesini korumak için keskin köşe
 * > tamamen kaçınılır." — tasarım sistemi
 */
object Yaricap {
    /** kart — 20dp, modern ve dostane */
    val Kart = 20.dp

    /** buton — 16dp */
    val Buton = 16.dp

    /** chip, etiket — 12dp */
    val Cip = 12.dp

    /** bottom sheet üst köşeleri — 28dp */
    val AltSayfa = 28.dp

    /** girdi alanı — 12dp */
    val Girdi = 12.dp
}

/**
 * Material 3 slotlarına eşleme.
 * `extraLarge` bottom sheet için; M3 ModalBottomSheet bu slotu kullanıyor.
 */
val YesilizSekiller = Shapes(
    extraSmall = RoundedCornerShape(Yaricap.Cip),
    small = RoundedCornerShape(Yaricap.Cip),
    medium = RoundedCornerShape(Yaricap.Buton),
    large = RoundedCornerShape(Yaricap.Kart),
    extraLarge = RoundedCornerShape(Yaricap.AltSayfa),
)

/** İlerleme çubuğu ve rozetler: hap şeklinde (tam yuvarlak). */
val HapSekli = RoundedCornerShape(percent = 50)
