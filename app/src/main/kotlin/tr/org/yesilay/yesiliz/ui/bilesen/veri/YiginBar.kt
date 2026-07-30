package tr.org.yesilay.yesiliz.ui.bilesen.veri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.HapSekli
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu

/** Yığın barın bir dilimi. [oran] 0..1, toplamları 1 olmalı. */
data class Dilim(val etiket: String, val oran: Float, val renk: Color)

/**
 * Kategori dağılımı — tek satırlık yığın bar + lejant.
 *
 * Pasta grafik değil: tasarım tek çizgi seçmiş, küçük ekranda 6 dilimlik
 * pastadan çok daha okunaklı ve dikey yer yemiyor.
 */
@Composable
fun YiginBar(
    dilimler: List<Dilim>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Olcu.YiginYuksekligi)
            .clip(HapSekli),
    ) {
        dilimler.filter { it.oran > 0f }.forEach { dilim ->
            Box(
                modifier = Modifier
                    .weight(dilim.oran)
                    .fillMaxHeight()
                    .background(dilim.renk),
            )
        }
    }
}

/** Yığın barın lejantı — nokta + etiket. Tasarımda ilk iki kategori görünür. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YiginLejant(
    dilimler: List<Dilim>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
        verticalArrangement = Arrangement.spacedBy(Bosluk.S),
    ) {
        dilimler.forEach { dilim ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Bosluk.S),
            ) {
                Box(
                    modifier = Modifier
                        .size(Bosluk.S)
                        .clip(CircleShape)
                        .background(dilim.renk),
                )
                Text(
                    text = dilim.etiket,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinBirincil,
                )
            }
        }
    }
}
