package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.YuzeyKirik

/**
 * Satır içi uyarı şeridi — bir eylemin **neden** yapılamadığını söyler.
 *
 * Kırmızı yok (CLAUDE.md §4): zemin nötr yüzey, metin ikincil ton. Hata
 * durumu da destekleyici dille anlatılıyor.
 *
 * `LigEkran`'daki `CevrimdisiNotu` / `GorunmuyorNotu` ile aynı görsel dilde
 * ama **kapatılabilir**: bunlar kalıcı durum notu, bu ise tek seferlik bir
 * denemenin sonucu.
 */
@Composable
fun UyariSeridi(
    metin: String,
    modifier: Modifier = Modifier,
    ikon: ImageVector = Icons.Outlined.Info,
    onKapat: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .background(YuzeyKirik)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = ikon, contentDescription = null, tint = MetinUcuncul)
            Spacer(modifier = Modifier.width(Bosluk.S))
            Text(
                text = metin,
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
            )
        }
        if (onKapat != null) {
            IconButton(onClick = onKapat) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Kapat",
                    tint = MetinUcuncul,
                )
            }
        }
    }
}
