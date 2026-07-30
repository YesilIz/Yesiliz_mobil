package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yesil200
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Ayar satırı — ikon + etiket + sağda `›`.
 * Profil ve Ebeveyn panelindeki listeler bunu kullanıyor.
 */
@Composable
fun AyarSatiri(
    ikon: ImageVector,
    etiket: String,
    modifier: Modifier = Modifier,
    onTikla: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTikla)
            .padding(vertical = Bosluk.M),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
    ) {
        Icon(imageVector = ikon, contentDescription = null, tint = YesilAna)
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MetinBirincil,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MetinUcuncul,
            modifier = Modifier.size(Olcu.Aralik),
        )
    }
}

/** Anahtarlı ayar satırı — gizlilik tercihleri. */
@Composable
fun AnahtarSatiri(
    ikon: ImageVector,
    etiket: String,
    acik: Boolean,
    onDegis: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Bosluk.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
    ) {
        Icon(imageVector = ikon, contentDescription = null, tint = YesilAna)
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MetinBirincil,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = acik,
            onCheckedChange = onDegis,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Beyaz,
                checkedTrackColor = YesilAna,
                uncheckedThumbColor = Beyaz,
                uncheckedTrackColor = Yesil200,
                uncheckedBorderColor = Yesil200,
            ),
        )
    }
}
