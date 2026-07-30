package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tr.org.yesilay.yesiliz.ui.tema.Yesil500
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

private data class YaprakKonumu(
    val xOran: Float,
    val yOran: Float,
    val boyut: Dp,
    val periyotMs: Int,
    val fazMs: Int,
    val genlikDerece: Float,
)

// Ağaç görsellerinin gövde/tepe kompozisyonuna göre kabaca kanopi alanına
// düşen sabit konumlar — evre görseli değişse de tepe her zaman üstte,
// gövde altta ortalandığı için aynı oranlar tüm evrelerde işe yarıyor.
private val VARSAYILAN_YAPRAKLAR = listOf(
    YaprakKonumu(0.22f, 0.18f, 14.dp, periyotMs = 2600, fazMs = 0, genlikDerece = 6f),
    YaprakKonumu(0.74f, 0.24f, 12.dp, periyotMs = 3100, fazMs = 400, genlikDerece = 5f),
    YaprakKonumu(0.36f, 0.42f, 10.dp, periyotMs = 2200, fazMs = 900, genlikDerece = 7f),
    YaprakKonumu(0.64f, 0.48f, 13.dp, periyotMs = 2900, fazMs = 1300, genlikDerece = 5f),
)

/**
 * Ağaç illüstrasyonunun üstüne bindirilen, hafifçe sallanan birkaç yaprak.
 *
 * Kaynak illüstrasyonlar düz raster (katmansız) olduğu için ağacın kendi
 * yapraklarını tek tek oynatmak mümkün değil — bunun yerine üstüne aynı
 * yeşil tonlarda küçük, bağımsız yapraklar bindirilip her biri farklı
 * periyot/faz ile ileri-geri döndürülüyor (K5: 3. parti animasyon/grafik
 * kütüphanesi yok, saf Compose).
 *
 * [modifier] ağaç görseliyle aynı boyuttaki kutuya verilmeli (örn. aynı
 * `Box` içinde `Image`'ın üstüne).
 */
@Composable
fun SallananYapraklar(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val genislik = maxWidth
        val yukseklik = maxHeight
        VARSAYILAN_YAPRAKLAR.forEachIndexed { i, yaprak ->
            TekYaprak(
                yaprak = yaprak,
                koyuTon = i % 2 == 0,
                modifier = Modifier.offset(
                    x = genislik * yaprak.xOran - yaprak.boyut / 2,
                    y = yukseklik * yaprak.yOran - yaprak.boyut / 2,
                ),
            )
        }
    }
}

@Composable
private fun TekYaprak(yaprak: YaprakKonumu, koyuTon: Boolean, modifier: Modifier = Modifier) {
    val gecis = rememberInfiniteTransition(label = "yaprak")
    val aci by gecis.animateFloat(
        initialValue = -yaprak.genlikDerece,
        targetValue = yaprak.genlikDerece,
        animationSpec = infiniteRepeatable(
            animation = tween(yaprak.periyotMs, delayMillis = yaprak.fazMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aci",
    )

    Canvas(
        modifier = modifier
            .size(yaprak.boyut, yaprak.boyut * 1.3f)
            .graphicsLayer {
                rotationZ = aci
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
    ) {
        val yol = Path().apply {
            moveTo(size.width / 2f, 0f)
            quadraticTo(size.width, size.height * 0.35f, size.width / 2f, size.height)
            quadraticTo(0f, size.height * 0.35f, size.width / 2f, 0f)
            close()
        }
        drawPath(yol, color = if (koyuTon) YesilAna else Yesil500)
    }
}
