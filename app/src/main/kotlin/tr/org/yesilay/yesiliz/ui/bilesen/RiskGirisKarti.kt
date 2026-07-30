package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.Gradyan
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap

/**
 * "Risk analizi: Orta düzey ›" kartı — risk detayına (ekran 30) giriş.
 *
 * İki ekranda birebir aynı görünüyor (09 · İstatistik ve 31 · Uygulama
 * kategorileri), o yüzden ekran içinde tekrarlanmak yerine buraya çıkarıldı.
 *
 * @param bant `null` ise kart hiç çizilmiyor — henüz `risk_sonuc` yok.
 */
@Composable
fun RiskGirisKarti(bant: Bant?, onTikla: () -> Unit, modifier: Modifier = Modifier) {
    if (bant == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .background(Gradyan.PuanKarti)
            .clickable(onClick = onTikla)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Bosluk.M),
    ) {
        IkonRozeti(
            ikon = Icons.Filled.WarningAmber,
            zemin = Beyaz.copy(alpha = RozetSaydamligi),
            renk = Beyaz,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Risk analizi: ${bant.kisaEtiket()} düzey",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Beyaz,
            )
            Text(
                text = "Sonuçları ve önerilen eylemleri gör",
                style = MaterialTheme.typography.bodySmall,
                color = Beyaz.copy(alpha = AltMetinSaydamligi),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Beyaz,
        )
    }
}

/**
 * Ekranda gösterilen kısa bant adı.
 *
 * `Bant.etiket` ("ORTA RİSKLİ") teşhis gibi duruyor — arayüzde
 * "Orta düzey" deniyor (CLAUDE.md §4).
 */
fun Bant.kisaEtiket(): String = when (this) {
    Bant.AZ -> "Düşük"
    Bant.ORTA -> "Orta"
    Bant.COK -> "Yüksek"
}

private const val RozetSaydamligi = 0.15f
private const val AltMetinSaydamligi = 0.8f
