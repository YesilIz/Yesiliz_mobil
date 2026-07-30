package tr.org.yesilay.yesiliz.ui.bilesen.veri

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import tr.org.yesilay.yesiliz.ui.tema.Ayrac
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yesil200
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/** Bir çubuk: etiket + değer. [deger] eksen birimindeki ham değer. */
data class Cubuk(val etiket: String, val deger: Float)

/**
 * Haftalık çubuk grafik — İstatistik ve Ebeveyn panelindeki grafik.
 *
 * 3. parti chart kütüphanesi yok (CLAUDE.md §12 K5): tasarımın kesikli
 * ızgarası, yuvarlatılmış çubuk uçları ve sağdaki eksen etiketleri hazır
 * kütüphanelerde ya yok ya da kendi paletini dayatıyor.
 *
 * @param vurgulanan vurgulanacak çubuğun indeksi (bugün / en iyi gün); -1 yoksa
 * @param eksenEtiketleri sağ eksende gösterilecek 3 etiket (üst → alt)
 */
@Composable
fun CubukGrafik(
    cubuklar: List<Cubuk>,
    modifier: Modifier = Modifier,
    vurgulanan: Int = -1,
    renk: Color = YesilAna,
    soluk: Color = Yesil200,
    eksenEtiketleri: List<String> = emptyList(),
) {
    if (cubuklar.isEmpty()) return
    val enBuyuk = cubuklar.maxOf { it.deger }.coerceAtLeast(1f)
    val yogunluk = LocalDensity.current

    Row(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Olcu.GrafikYuksekligi),
            ) {
                // kesikli ızgara — 0, orta, tavan
                val kesik = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                listOf(0f, 0.5f, 1f).forEach { o ->
                    val y = size.height * (1f - o)
                    drawLine(
                        color = Ayrac,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                        pathEffect = kesik,
                    )
                }

                val yuva = size.width / cubuklar.size
                val genislik = yuva * 0.46f
                val yaricap = with(yogunluk) { Olcu.CubukYaricapi.toPx() }

                cubuklar.forEachIndexed { i, cubuk ->
                    val yukseklik = size.height * (cubuk.deger / enBuyuk)
                    val x = yuva * i + (yuva - genislik) / 2f
                    drawRoundRect(
                        color = if (i == vurgulanan) renk else soluk,
                        topLeft = Offset(x, size.height - yukseklik),
                        size = Size(genislik, yukseklik),
                        cornerRadius = CornerRadius(yaricap, yaricap),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Bosluk.S),
            ) {
                cubuklar.forEachIndexed { i, cubuk ->
                    Text(
                        text = cubuk.etiket,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (i == vurgulanan) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (i == vurgulanan) MetinBirincil else MetinUcuncul,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (eksenEtiketleri.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(start = Bosluk.S)
                    .width(Olcu.GrafikEkseni)
                    .height(Olcu.GrafikYuksekligi),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                eksenEtiketleri.forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinUcuncul,
                    )
                }
            }
        }
    }
}
