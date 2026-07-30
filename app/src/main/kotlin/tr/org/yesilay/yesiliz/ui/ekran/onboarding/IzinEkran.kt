package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * UsageStats izni — `docs/tasarim-referans.md §2 / 02`.
 *
 * Metin **ne toplanmadığını** da söylüyor: "İçeriklerine asla bakmayız."
 * İzin ekranında bunu yazmak, ayarlardaki gizlilik metnine gömmekten
 * fazlasını yapıyor — kullanıcı kararı verirken görüyor (CLAUDE.md §4).
 */
@Composable
fun IzinEkran(
    onIzinVer: () -> Unit,
    onAtla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingIskelet(
        adim = 2,
        ilerleMetni = "İzin ver",
        onIlerle = onIzinVer,
        ikincilMetin = "Şimdi değil",
        onIkincil = onAtla,
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(Olcu.AvatarBuyuk)
                    .clip(CircleShape)
                    .background(Yesil50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = YesilAna,
                    modifier = Modifier.size(Olcu.IkonRozeti),
                )
            }
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            Text(
                text = "Ekran kullanımını\ngörebilmemiz için",
                style = MaterialTheme.typography.displayLarge,
                color = MetinBirincil,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Bosluk.M))
            Text(
                text = "Hangi uygulamada ne kadar zaman geçirdiğini görebilmemiz " +
                    "gerekiyor. İçeriklerine asla bakmayız — sadece süre ve kategori.",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Yaricap.Buton))
                    .background(YesilNane)
                    .padding(Olcu.Aralik),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = YesilKoyu)
                Text(
                    text = "Verilerin telefonunda kalır.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinBirincil,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
