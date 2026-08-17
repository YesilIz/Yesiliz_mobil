package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import tr.org.yesilay.yesiliz.ui.tema.Yesil500
import kotlin.math.hypot

/**
 * Ana sayfadaki ağacın arkasına çizilen manzara: uzakta yuvarlak tepeli
 * dağlar, önlerinde bir sırt, en önde ağacın üzerinde durduğu geniş oval
 * plato. [YaprakZemini] ile aynı sınıftan dekoratif katman — 3. parti
 * asset/kütüphane olmadan Canvas ile çiziliyor (CLAUDE.md §12 K5), renk
 * `ui/tema`dan geliyor (§7).
 *
 * Üç tasarım kararı:
 *
 * - **Tek ton, farklı opaklık.** Bütün katmanlar [Yesil500]; derinliği
 *   uzaktan yakına artan opaklık veriyor. Yeni renk sabiti gerekmiyor ve
 *   katmanlar birbirinden ayrışıyor ama palet tek kalıyor.
 * - **Zemin açık olmak zorunda.** İlk denemede manzara doygun marka
 *   yeşilinin üstüne çiziliyordu ve hiç okunmuyordu;
 *   [tr.org.yesilay.yesiliz.ui.tema.Gradyan.BaslikSonumu] artık başlığın
 *   altında hızla açık yeşile iniyor. Manzaranın görünürlüğü o gradyana bağlı.
 * - **Alt uç sönümleniyor** (`BlendMode.DstIn`): plato içeriğin başladığı
 *   yerde eriyor, kartların arkasında sert bir kenar kalmıyor. Üst uçta da
 *   aynısı — dağların tepesi başlığın yeşiline karışıyor.
 *
 * İçeriğin arkasına konur: `Box { OrmanZemini(Modifier.matchParentSize()); agac() }`.
 */
@Composable
fun OrmanZemini(
    modifier: Modifier = Modifier,
    renk: Color = Yesil500,
) {
    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        val w = size.width
        val h = size.height

        // --- UZAK DAĞLAR ---
        dagCiz(
            tepe = Offset(0.10f * w, 0.24f * h),
            tabanY = 0.86f * h,
            yariGenislik = 0.34f * w,
            renk = renk,
            alpha = 0.21f,
        )
        dagCiz(
            tepe = Offset(0.78f * w, 0.17f * h),
            tabanY = 0.86f * h,
            yariGenislik = 0.40f * w,
            renk = renk,
            alpha = 0.21f,
        )
        dagCiz(
            tepe = Offset(0.46f * w, 0.37f * h),
            tabanY = 0.86f * h,
            yariGenislik = 0.30f * w,
            renk = renk,
            alpha = 0.15f,
        )

        // --- ORTA SIRT --- ekranın iki yanından taşan alçak tepe
        drawOval(
            color = renk,
            topLeft = Offset(-0.55f * w, 0.50f * h),
            size = Size(1.5f * w, 0.95f * h),
            alpha = 0.18f,
        )
        drawOval(
            color = renk,
            topLeft = Offset(0.25f * w, 0.58f * h),
            size = Size(1.45f * w, 0.9f * h),
            alpha = 0.18f,
        )

        // --- ÖN PLATO --- ağacın üzerinde durduğu geniş oval.
        // İki iç içe oval: dıştaki platonun kendisi, içteki üst yüzeyi —
        // aradaki ince fark platoya kalınlık veriyor.
        drawOval(
            color = renk,
            topLeft = Offset(-0.30f * w, 0.70f * h),
            size = Size(1.60f * w, 0.62f * h),
            alpha = 0.34f,
        )
        drawOval(
            color = renk,
            topLeft = Offset(-0.18f * w, 0.74f * h),
            size = Size(1.36f * w, 0.52f * h),
            alpha = 0.16f,
        )

        // Uçların sönümü: alfa maskesi. Siyahın kendisi değil alfası önemli.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.09f to Color.Black,
                0.80f to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
}

/** Yuvarlak tepeli, tabanı düz dağ silueti. */
private fun DrawScope.dagCiz(
    tepe: Offset,
    tabanY: Float,
    yariGenislik: Float,
    renk: Color,
    alpha: Float,
) {
    val sol = Offset(tepe.x - yariGenislik, tabanY)
    val sag = Offset(tepe.x + yariGenislik, tabanY)
    val ucYaricap = yariGenislik * 0.55f

    val tepeSol = tepe.dogrultuda(sol, ucYaricap)
    val tepeSag = tepe.dogrultuda(sag, ucYaricap)

    val yol = Path().apply {
        moveTo(sol.x, sol.y)
        lineTo(tepeSol.x, tepeSol.y)
        quadraticTo(tepe.x, tepe.y, tepeSag.x, tepeSag.y)
        lineTo(sag.x, sag.y)
        close()
    }
    drawPath(yol, color = renk, alpha = alpha)
}

/** [hedef] yönünde [mesafe] kadar ilerlemiş nokta — köşe yuvarlatma için. */
private fun Offset.dogrultuda(hedef: Offset, mesafe: Float): Offset {
    val uzunluk = hypot(hedef.x - x, hedef.y - y)
    if (uzunluk == 0f) return this
    val oran = (mesafe / uzunluk).coerceAtMost(0.5f)
    return Offset(x + (hedef.x - x) * oran, y + (hedef.y - y) * oran)
}
