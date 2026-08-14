package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import tr.org.yesilay.yesiliz.ui.tema.Beyaz

/**
 * Yuvarlak oklu yenile düğmesi.
 *
 * Ana Sayfa, İstatistik ve Lig verisi Room akışlarından geliyor ve arka plan
 * işi gecelik koşuyor; kullanıcı gün içinde bir şey değiştiğinde ekranın
 * güncellenmesi için uygulamadan çıkıp girmek zorunda kalıyordu. Bu düğme
 * tazelemeyi elle tetikliyor.
 *
 * [donuyor] iken ikon sürekli dönüyor: iş kuyruğa alındığında hiçbir görsel
 * geri bildirim olmazsa kullanıcı dokunuşun işe yarayıp yaramadığını
 * bilemiyor ve üst üste basıyor.
 */
@Composable
fun YenileDugmesi(
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
    donuyor: Boolean = false,
    renk: Color = Beyaz,
) {
    val gecis = rememberInfiniteTransition(label = "yenile")
    val aci by gecis.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DONUS_SURESI_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "aci",
    )

    IconButton(onClick = onTikla, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Yenile",
            tint = renk,
            modifier = Modifier.rotate(if (donuyor) aci else 0f),
        )
    }
}

private const val DONUS_SURESI_MS = 900
