package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.HapSekli
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.Yesil50

/**
 * Hap şeklinde etiket.
 *
 * Tasarımda üç yerde: "Başlangıç değerlendirmen hazır", "14 yaşında",
 * "↑ İlk 3 yükselir", "Filiz Ligi". Hepsi aynı ölçü, farklı zemin.
 */
@Composable
fun Cip(
    metin: String,
    modifier: Modifier = Modifier,
    ikon: ImageVector? = null,
    zemin: Color = Yesil50,
    renk: Color = MetinBirincil,
    onTikla: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(HapSekli)
            .background(zemin)
            .then(if (onTikla != null) Modifier.clickable(onClick = onTikla) else Modifier)
            .padding(horizontal = Bosluk.M, vertical = Bosluk.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Bosluk.Xs),
    ) {
        if (ikon != null) {
            Icon(
                imageVector = ikon,
                contentDescription = null,
                tint = renk,
                modifier = Modifier.size(Bosluk.M),
            )
        }
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = renk,
        )
    }
}
