package tr.org.yesilay.yesiliz.ui.bilesen.veri

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yesil200
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Kalın halkalı dairesel gösterge.
 *
 * Hedef önerisi ekranındaki `285 dk` halkası ve risk detayındaki skor halkası
 * aynı bileşen. Boşluk saat 12'de başlıyor, saat yönünde doluyor; uçlar yuvarlak
 * (tasarımda çubuk ucu yuvarlatılmış).
 *
 * İçerik [icerik] slotundan geliyor — sayı ve alt yazı ekranın kendi metni.
 *
 * [renkFirca] verilirse dolu yay tek [renk] yerine bu fırçayla çizilir (risk
 * detayı ekranındaki turuncu→yeşil yay gibi) — verilmezse davranış eskisiyle
 * birebir aynı, mevcut kullanıcılar (Ana Sayfa, İlk sonuç) etkilenmez.
 */
@Composable
fun DaireselGosterge(
    oran: Float,
    modifier: Modifier = Modifier,
    cap: Dp = VarsayilanCap,
    kalinlik: Dp = Olcu.HalkaKalinligi,
    renk: Color = YesilAna,
    renkFirca: Brush? = null,
    zemin: Color = Yesil200,
    icerik: @Composable () -> Unit,
) {
    val hedef by animateFloatAsState(
        targetValue = oran.coerceIn(0f, 1f),
        label = "daire",
    )
    val yogunluk = LocalDensity.current

    Box(modifier = modifier.size(cap), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(cap)) {
            val kalem = with(yogunluk) { kalinlik.toPx() }
            val kutu = Size(size.width - kalem, size.height - kalem)
            val sol = Offset(kalem / 2f, kalem / 2f)

            drawArc(
                color = zemin,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = sol,
                size = kutu,
                style = Stroke(width = kalem, cap = StrokeCap.Round),
            )
            if (renkFirca != null) {
                drawArc(
                    brush = renkFirca,
                    startAngle = -90f,
                    sweepAngle = 360f * hedef,
                    useCenter = false,
                    topLeft = sol,
                    size = kutu,
                    style = Stroke(width = kalem, cap = StrokeCap.Round),
                )
            } else {
                drawArc(
                    color = renk,
                    startAngle = -90f,
                    sweepAngle = 360f * hedef,
                    useCenter = false,
                    topLeft = sol,
                    size = kutu,
                    style = Stroke(width = kalem, cap = StrokeCap.Round),
                )
            }
        }
        icerik()
    }
}

private val VarsayilanCap = 200.dp
