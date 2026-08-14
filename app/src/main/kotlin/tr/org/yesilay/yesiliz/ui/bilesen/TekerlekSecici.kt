package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50

/**
 * Tekerlekli değer seçici — ortadaki değer seçili, üstü/altı soluk.
 *
 * Odak seansında süre "Süreyi değiştir" butonuyla 15 → 25 → 45 → 15 diye
 * dönüyordu; aradaki bir değeri (örneğin 30 dk) seçmek mümkün değildi ve
 * istenen değere ulaşmak için butona kaç kez basılacağı belirsizdi.
 *
 * Kaydırma `rememberSnapFlingBehavior` ile hizalanıyor: parmak kalkınca
 * en yakın değere oturuyor, arada kalmıyor.
 *
 * @param degerler seçilebilecek değerler, artan sırada
 * @param secili şu an seçili değer; listede yoksa en yakınına oturur
 * @param etiketle değeri ekranda görünen metne çevirir
 */
@Composable
fun <T> TekerlekSecici(
    degerler: List<T>,
    secili: T,
    onSec: (T) -> Unit,
    modifier: Modifier = Modifier,
    gorunenSatir: Int = 5,
    satirYuksekligi: Dp = 44.dp,
    etiketle: (T) -> String = { it.toString() },
) {
    if (degerler.isEmpty()) return

    // Ortadaki yuvayı seçili kılmak için listenin başına ve sonuna boş
    // satırlar konuyor; yoksa ilk ve son değer asla ortaya gelemiyor.
    val kenarBosluk = gorunenSatir / 2
    val baslangic = degerler.indexOf(secili).coerceAtLeast(0)
    val listeDurumu = rememberLazyListState(initialFirstVisibleItemIndex = baslangic)

    val ortadakiIndeks by remember {
        derivedStateOf { listeDurumu.firstVisibleItemIndex.coerceIn(degerler.indices) }
    }

    // Kaydırma durunca seçim bildiriliyor. Her karede bildirmek, kaydırma
    // boyunca onlarca gereksiz güncelleme üretirdi.
    LaunchedEffect(listeDurumu, degerler) {
        snapshotFlow { listeDurumu.isScrollInProgress to ortadakiIndeks }
            .collect { (kayiyor, indeks) ->
                if (!kayiyor) {
                    degerler.getOrNull(indeks)?.let { if (it != secili) onSec(it) }
                }
            }
    }

    Box(modifier = modifier.height(satirYuksekligi * gorunenSatir)) {
        // Ortadaki yuvanın vurgusu — hangi satırın seçili olduğu, metin
        // solukluğundan bağımsız olarak da okunabilsin.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(satirYuksekligi)
                .clip(RoundedCornerShape(Yaricap.Cip))
                .background(Yesil50),
        )

        LazyColumn(
            state = listeDurumu,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listeDurumu),
            contentPadding = PaddingValues(vertical = satirYuksekligi * kenarBosluk),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(degerler) { i, deger ->
                val uzaklik = kotlin.math.abs(i - ortadakiIndeks)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(satirYuksekligi),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = etiketle(deger),
                        style = if (uzaklik == 0) {
                            MaterialTheme.typography.headlineLarge
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        fontWeight = if (uzaklik == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (uzaklik == 0) MetinBirincil else MetinUcuncul,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(
                            when (uzaklik) {
                                0 -> 1f
                                1 -> 0.6f
                                else -> 0.3f
                            },
                        ),
                    )
                }
            }
        }
    }
}
