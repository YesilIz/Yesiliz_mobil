package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.HapSekli
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yesil200
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Yatay ilerleme çubuğu — hap uçlu.
 *
 * M3'ün `LinearProgressIndicator`'ı 2026 sürümünde uçlara "stop indicator"
 * noktası ve segmentler arası boşluk koyuyor; tasarımda ikisi de yok.
 * Bu yüzden elle çiziliyor.
 */
@Composable
fun IlerlemeCubugu(
    oran: Float,
    modifier: Modifier = Modifier,
    renk: Color = YesilAna,
    zemin: Color = Yesil200,
    yukseklik: Dp = Olcu.IlerlemeYuksekligi,
) {
    val hedef by animateFloatAsState(
        targetValue = oran.coerceIn(0f, 1f),
        label = "ilerleme",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(yukseklik)
            .clip(HapSekli)
            .background(zemin),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(hedef)
                .fillMaxHeight()
                .clip(HapSekli)
                .background(renk),
        )
    }
}

/**
 * Onboarding adım göstergesi — [toplam] ayrı çubuk, ilk [adim] tanesi dolu.
 * Altında "Adım 1 / 4" metni.
 */
@Composable
fun AdimGostergesi(
    adim: Int,
    toplam: Int,
    modifier: Modifier = Modifier,
    altYazi: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Bosluk.S),
        ) {
            repeat(toplam) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Olcu.AdimCubugu)
                        .clip(HapSekli)
                        .background(if (i < adim) YesilAna else Yesil200),
                )
            }
        }
        if (altYazi) {
            Text(
                text = "Adım $adim / $toplam",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinUcuncul,
                modifier = Modifier.padding(top = Bosluk.S),
            )
        }
    }
}
