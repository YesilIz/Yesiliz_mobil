package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu

/**
 * "Henüz veri yok" durumu.
 *
 * Gerçek kullanıcının 1. günü bu ekranı görüyor: izin yeni verildi, gecelik
 * worker henüz koşmadı. Uydurma sayı göstermek yerine ne olduğunu söylemek
 * doğru — CLAUDE.md §10 zaten `d_efektif = 0` iken motorun `S ≈ 0`
 * ürettiğini not ediyor, yani boş grafik teknik gerçeğin karşılığı.
 *
 * Ton **betimleyici ve suçlayıcı değil**: "veri toplamadın" değil,
 * "birkaç gün ölçelim".
 */
@Composable
fun BosDurum(
    baslik: String,
    aciklama: String,
    modifier: Modifier = Modifier,
    ikon: ImageVector = Icons.Filled.HourglassEmpty,
    eylem: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Olcu.BolumArasi),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Bosluk.S),
    ) {
        IkonRozeti(ikon = ikon, olcu = Olcu.AvatarBuyuk, sekil = CircleShape)
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = baslik,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
            textAlign = TextAlign.Center,
        )
        Text(
            text = aciklama,
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Olcu.KartIci),
        )
        if (eylem != null) {
            Spacer(modifier = Modifier.height(Bosluk.S))
            eylem()
        }
    }
}
