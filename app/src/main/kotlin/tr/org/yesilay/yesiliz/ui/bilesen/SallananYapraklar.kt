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
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import kotlin.math.sin

private data class YaprakParcacigi(
    val id: Int,
    val baslangicX: Float, // Ağaç kanopi merkezi X (0.45f .. 0.55f)
    val baslangicY: Float, // Ağaç kanopi merkezi Y (0.22f .. 0.38f)
    val hedefX: Float,     // Dışarı saçılma hedef X (0.05f .. 0.95f)
    val hedefY: Float,     // Dışarı saçılma hedef Y (0.02f .. 0.85f)
    val boyutDp: Dp,
    val sureMs: Int,
    val gecikmeMs: Int,
    val ruzgarSalinimi: Float,
    val donusAci: Float,
    val renkTipi: Int,
)

/**
 * Ağaç etrafında merkezden sürekli dışarı doğru saçılan, süzülen ve uzaklaştıkça sönerek kaybolan yaprak parçacıkları.
 */
private val SACILAN_YAPRAKLAR = listOf(
    // Sol üst saçılma
    YaprakParcacigi(1, 0.48f, 0.30f, 0.08f, 0.08f, 15.dp, sureMs = 3400, gecikmeMs = 0, ruzgarSalinimi = 0.06f, donusAci = 160f, renkTipi = 0),
    // Sağ üst saçılma
    YaprakParcacigi(2, 0.52f, 0.28f, 0.92f, 0.06f, 13.dp, sureMs = 3800, gecikmeMs = 400, ruzgarSalinimi = -0.07f, donusAci = -180f, renkTipi = 1),
    // Sol orta dışa saçılma
    YaprakParcacigi(3, 0.46f, 0.35f, 0.02f, 0.38f, 16.dp, sureMs = 3100, gecikmeMs = 900, ruzgarSalinimi = 0.05f, donusAci = 220f, renkTipi = 2),
    // Sağ orta dışa saçılma
    YaprakParcacigi(4, 0.54f, 0.34f, 0.98f, 0.42f, 14.dp, sureMs = 3600, gecikmeMs = 250, ruzgarSalinimi = -0.06f, donusAci = -190f, renkTipi = 0),
    // Sol alt rüzgarla süzülme
    YaprakParcacigi(5, 0.47f, 0.38f, 0.12f, 0.78f, 12.dp, sureMs = 4200, gecikmeMs = 1500, ruzgarSalinimi = 0.08f, donusAci = 280f, renkTipi = 1),
    // Sağ alt rüzgarla süzülme
    YaprakParcacigi(6, 0.53f, 0.36f, 0.88f, 0.74f, 15.dp, sureMs = 3900, gecikmeMs = 700, ruzgarSalinimi = -0.07f, donusAci = -250f, renkTipi = 2),
    // Tepeye doğru yukarı saçılma
    YaprakParcacigi(7, 0.50f, 0.26f, 0.50f, 0.01f, 11.dp, sureMs = 3200, gecikmeMs = 1200, ruzgarSalinimi = 0.04f, donusAci = 110f, renkTipi = 0),
    // Sol alt köşe süzülme
    YaprakParcacigi(8, 0.45f, 0.32f, 0.18f, 0.65f, 14.dp, sureMs = 3700, gecikmeMs = 2000, ruzgarSalinimi = -0.05f, donusAci = -140f, renkTipi = 1),
    // Sağ alt köşe süzülme
    YaprakParcacigi(9, 0.55f, 0.33f, 0.82f, 0.68f, 13.dp, sureMs = 3500, gecikmeMs = 1700, ruzgarSalinimi = 0.06f, donusAci = 200f, renkTipi = 0),
    // Yavaş ve narin mikro yapraklar
    YaprakParcacigi(10, 0.49f, 0.34f, 0.25f, 0.88f, 10.dp, sureMs = 4500, gecikmeMs = 2400, ruzgarSalinimi = -0.09f, donusAci = 320f, renkTipi = 2),
    YaprakParcacigi(11, 0.51f, 0.31f, 0.75f, 0.85f, 11.dp, sureMs = 4100, gecikmeMs = 2800, ruzgarSalinimi = 0.07f, donusAci = -220f, renkTipi = 1),
    YaprakParcacigi(12, 0.50f, 0.28f, 0.45f, 0.92f, 12.dp, sureMs = 4000, gecikmeMs = 1100, ruzgarSalinimi = -0.05f, donusAci = 170f, renkTipi = 0),
)

/**
 * Ağaç etrafında merkezden sürekli etrafa saçılan ve
 * ağaçtan uzaklaştıkça opaklığı sönerek kaybolan animasyonlu yapraklar.
 *
 * [modifier] ağaç görseliyle aynı boyuttaki `Box` bileşenine verilmeli.
 */
@Composable
fun SallananYapraklar(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val genislik = maxWidth
        val yukseklik = maxHeight
        SACILAN_YAPRAKLAR.forEach { parcacik ->
            TekSacilanYaprak(
                parcacik = parcacik,
                genislik = genislik,
                yukseklik = yukseklik,
            )
        }
    }
}

@Composable
private fun TekSacilanYaprak(
    parcacik: YaprakParcacigi,
    genislik: Dp,
    yukseklik: Dp,
) {
    val gecis = rememberInfiniteTransition(label = "sacilanYaprak_${parcacik.id}")
    val ilerleme by gecis.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = parcacik.sureMs,
                delayMillis = parcacik.gecikmeMs,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ilerleme_${parcacik.id}",
    )

    // Ağaçtan uzaklaştıkça kaybolma (Fade-Out) hesabı:
    // Başlangıçta (0..0.15) yumuşak belirir, 0.15..0.50 tam görünür,
    // 0.50'den sonra ağaçtan uzaklaştıkça opaklığı (alpha) 0'a söner.
    val matlik = when {
        ilerleme < 0.15f -> ilerleme / 0.15f
        ilerleme < 0.50f -> 1.0f
        else -> (1.0f - ilerleme) / 0.50f
    }.coerceIn(0f, 1f)

    // Rüzgar salınımı eklenmiş dinamik konum
    val ruzgarOffset = sin(ilerleme * Math.PI * 2).toFloat() * parcacik.ruzgarSalinimi
    val xOran = lerp(parcacik.baslangicX, parcacik.hedefX, ilerleme) + ruzgarOffset
    val yOran = lerp(parcacik.baslangicY, parcacik.hedefY, ilerleme)

    val xKonum = genislik * xOran - parcacik.boyutDp / 2
    val yKonum = yukseklik * yOran - parcacik.boyutDp / 2
    val aci = parcacik.donusAci * ilerleme

    val renk = when (parcacik.renkTipi) {
        0 -> YesilAna
        1 -> Yesil500
        else -> YesilKoyu
    }

    Canvas(
        modifier = Modifier
            .offset(x = xKonum, y = yKonum)
            .size(parcacik.boyutDp, parcacik.boyutDp * 1.3f)
            .graphicsLayer {
                rotationZ = aci
                alpha = matlik
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            },
    ) {
        val yol = Path().apply {
            moveTo(size.width / 2f, 0f)
            quadraticTo(size.width, size.height * 0.35f, size.width / 2f, size.height)
            quadraticTo(0f, size.height * 0.35f, size.width / 2f, 0f)
            close()
        }
        drawPath(yol, color = renk)
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
