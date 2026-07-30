package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import tr.org.yesilay.yesiliz.ui.tema.Yesil500

/** [YaprakZemini] içinde hangi alt köşe(ler)den fide/sarmaşık çıkacağı. */
enum class YaprakKenari { SolAlt, SagAlt, HerIkisi }

/**
 * Ekranın alt köşe(ler)inden yükselen, çok soluk (düşük opaklı) dekoratif
 * sarmaşık + yaprak deseni. Ödüller ekranı tasarımından (30 Tem 2026 güncel
 * teslim) alınan motif — içerik okumayı bozmayacak kadar soluk, marka
 * tonunda (yeşil), 3. parti asset/kütüphane olmadan Canvas ile çiziliyor
 * (K5 ilkesiyle tutarlı).
 *
 * İçeriğin ARKASINA konur: `Box { YaprakZemini(Modifier.matchParentSize());
 * asıl_icerik() }`.
 */
@Composable
fun YaprakZemini(
    modifier: Modifier = Modifier,
    renk: Color = Yesil500,
    alpha: Float = 0.11f,
    kenar: YaprakKenari = YaprakKenari.HerIkisi,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        if (kenar != YaprakKenari.SagAlt) {
            sarmasikCiz(
                kokX = w * 0.02f,
                kokY = h,
                ucX = w * 0.22f,
                ucY = h * 0.66f,
                ayna = false,
                renk = renk,
                alpha = alpha,
            )
        }
        if (kenar != YaprakKenari.SolAlt) {
            sarmasikCiz(
                kokX = w * 0.98f,
                kokY = h,
                ucX = w * 0.74f,
                ucY = h * 0.60f,
                ayna = true,
                renk = renk,
                alpha = alpha,
            )
        }
    }
}

private fun DrawScope.sarmasikCiz(
    kokX: Float,
    kokY: Float,
    ucX: Float,
    ucY: Float,
    ayna: Boolean,
    renk: Color,
    alpha: Float,
) {
    val kontrolX = kokX + (ucX - kokX) * 0.3f
    val govde = Path().apply {
        moveTo(kokX, kokY)
        quadraticTo(kontrolX, (kokY + ucY) / 2f, ucX, ucY)
    }
    drawPath(govde, color = renk, alpha = alpha * 0.4f, style = Stroke(width = 2f))

    val boyGenlik = kotlin.math.hypot((ucX - kokX).toDouble(), (ucY - kokY).toDouble()).toFloat()
    val yapraklar = listOf(
        Triple(0.30f, boyGenlik * 0.11f, -28f),
        Triple(0.58f, boyGenlik * 0.15f, 24f),
        Triple(0.82f, boyGenlik * 0.09f, -20f),
    )
    yapraklar.forEach { (t, boyut, aci) ->
        val x = kokX + (ucX - kokX) * t
        val y = kokY + (ucY - kokY) * t
        val yon = if (ayna) -1f else 1f
        withTransform({
            translate(x, y)
            rotate(aci * yon, pivot = Offset.Zero)
        }) {
            drawPath(yaprakYolu(boyut), color = renk, alpha = alpha)
        }
    }

    // birkaç dağınık nokta
    val noktalar = listOf(0.45f to 0.55f, 0.65f to 0.25f, 0.20f to 0.75f)
    noktalar.forEach { (nt, yanOran) ->
        val x = kokX + (ucX - kokX) * nt + (if (ayna) -1 else 1) * boyGenlik * yanOran * 0.3f
        val y = kokY + (ucY - kokY) * nt
        drawCircle(color = renk, radius = 5f, center = Offset(x, y), alpha = alpha * 0.8f)
    }
}

private fun yaprakYolu(boyut: Float): Path {
    val genislik = boyut
    val yukseklik = boyut * 1.4f
    return Path().apply {
        moveTo(0f, -yukseklik / 2f)
        quadraticTo(genislik / 2f, -yukseklik * 0.15f, 0f, yukseklik / 2f)
        quadraticTo(-genislik / 2f, -yukseklik * 0.15f, 0f, -yukseklik / 2f)
        close()
    }
}
