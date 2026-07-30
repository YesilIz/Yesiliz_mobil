package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * SAS-SV maddesi — ifade + 6'lı Likert.
 *
 * **Ölçek değiştirilemez** (`docs/spec-anket.md §6.1`): 6 seçenek, ters madde
 * yok, uç etiketleri sabit. Tek kelime değişirse 29,5 kesme puanı geçersizleşir.
 *
 * @param secili 1..6 arası; 0 = henüz yanıtlanmadı (eksikle ilerlenemez)
 */
@Composable
fun LikertMaddesi(
    ifade: String,
    secili: Int,
    onSec: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Kart(modifier = modifier) {
        Text(
            text = ifade,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MetinBirincil,
        )

        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Bosluk.S),
        ) {
            (1..LIKERT_UST).forEach { deger ->
                val aktif = deger == secili
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(if (aktif) YesilAna else Yesil50)
                        .clickable { onSec(deger) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$deger",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (aktif) FontWeight.Bold else FontWeight.Normal,
                        color = if (aktif) Beyaz else MetinBirincil,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Bosluk.S))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                UcEtiketi("Kesinlikle\nkatılmıyorum", TextAlign.Start)
            }
            Column(modifier = Modifier.weight(1f)) {
                UcEtiketi("Kesinlikle\nkatılıyorum", TextAlign.End)
            }
        }
    }
}

@Composable
private fun UcEtiketi(metin: String, hiza: TextAlign) {
    Text(
        text = metin,
        style = MaterialTheme.typography.bodyMedium,
        color = MetinIkincil,
        textAlign = hiza,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Likert üst sınırı — `docs/spec-anket.md §6.3`. */
const val LIKERT_UST = 6
