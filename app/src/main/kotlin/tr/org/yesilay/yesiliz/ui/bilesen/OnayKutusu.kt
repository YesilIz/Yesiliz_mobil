package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tr.org.yesilay.yesiliz.ui.tema.KirmiziIstisna
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Onay kutusu + metin — "Verilerimin kalıcı olarak silineceğini anlıyorum."
 * (ekran 25) gibi açık rıza gerektiren tekil onaylarda kullanılıyor.
 *
 * Proje genelinde başka bir yerde `Checkbox` yok; bu yalnızca geri
 * alınamaz işlem onayları için var, [tehlikeliMi] `true` ise işaretliyken
 * `KirmiziIstisna` (CLAUDE.md §12 K10).
 */
@Composable
fun OnayKutusu(
    metin: String,
    secili: Boolean,
    onDegis: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tehlikeliMi: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDegis(!secili) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = secili,
            onCheckedChange = onDegis,
            colors = CheckboxDefaults.colors(
                checkedColor = if (tehlikeliMi) KirmiziIstisna else YesilAna,
            ),
        )
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyMedium,
            color = MetinBirincil,
        )
    }
}
