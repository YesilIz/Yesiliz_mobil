package tr.org.yesilay.yesiliz.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector
import tr.org.yesilay.yesiliz.domain.gorev.Gorev

/**
 * Görevin ikonu ve süresi — katalog `domain/`de, görünüm `ui/`de kalsın
 * diye burada (CLAUDE.md §6: katmanlar karışmıyor).
 *
 * Eşleme iki ayrı ViewModel'de kopyalanmıştı; `Gorev`'e yeni bir değer
 * eklendiğinde ikisinin de düzeltilmesi gerekiyordu. Tek yerde.
 */
fun Gorev?.ikonu(): ImageVector = when (this) {
    Gorev.NEFES -> Icons.Filled.SelfImprovement
    Gorev.DIJITAL_MOLA -> Icons.Filled.Timer
    Gorev.YURUYUS -> Icons.AutoMirrored.Filled.DirectionsWalk
    Gorev.RUH_HALI -> Icons.Filled.MoodBad
    Gorev.UYKU_BEYANI -> Icons.Filled.Bedtime
    Gorev.GECE_TEMIZ -> Icons.Filled.NightsStay
    Gorev.TUKETIM_HEDEFI -> Icons.Filled.TrackChanges
    Gorev.ODAK -> Icons.Filled.CenterFocusStrong
    Gorev.GOZ_MOLASI -> Icons.Filled.RemoveRedEye
    Gorev.GECE_KAPANISI -> Icons.Filled.Bedtime
    Gorev.EKRANSIZ -> Icons.Filled.Yard
    null -> Icons.Filled.Park
}

/**
 * Süre çipi için dakika değeri.
 *
 * Süresi olmayan görevlerde `null` — çip hiç gösterilmiyor. Odak seansının
 * süresi kullanıcı tarafından seçildiği için burada da yok.
 */
fun Gorev.suresiDk(): Int? = when (this) {
    Gorev.NEFES -> 2
    Gorev.DIJITAL_MOLA -> 15
    Gorev.YURUYUS -> 15
    Gorev.GOZ_MOLASI -> 1
    Gorev.RUH_HALI,
    Gorev.UYKU_BEYANI,
    Gorev.GECE_TEMIZ,
    Gorev.TUKETIM_HEDEFI,
    Gorev.ODAK,
    Gorev.GECE_KAPANISI,
    Gorev.EKRANSIZ,
    -> null
}
