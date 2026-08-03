package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import tr.org.yesilay.yesiliz.ui.tema.KirmiziIstisna
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Onay kutusu + metin — "Verilerimin kalıcı olarak silineceğini anlıyorum."
 * (ekran 25) gibi açık rıza gerektiren tekil onaylarda kullanılıyor.
 *
 * Proje genelinde başka bir yerde `Checkbox` yok; bu yalnızca geri
 * alınamaz işlem onayları için var, [tehlikeliMi] `true` ise işaretliyken
 * `KirmiziIstisna` (CLAUDE.md §12 K10).
 */
@Composable
fun OnayKutusu(
    metin: String,
    secili: Boolean,
    onDegis: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tehlikeliMi: Boolean = false,
    /**
     * Metin içindeki tıklanabilir parçalar — `görünen metin` → geri çağrı.
     *
     * Kayıt ekranı "Kullanım Koşulları'nı ve Gizlilik Politikası'nı okudum"
     * onayını **zorunlu** tutuyordu ama metinler düz `String`'di ve okumanın
     * hiçbir yolu yoktu (`debug.md` Y-6). Bağlantılı parça verildiğinde
     * satırın tamamını işaretleme davranışı kalkıyor — yoksa bağlantıya
     * dokunmak kutuyu da işaretlerdi.
     */
    baglantilar: Map<String, () -> Unit> = emptyMap(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (baglantilar.isEmpty()) Modifier.clickable { onDegis(!secili) } else Modifier,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = secili,
            onCheckedChange = onDegis,
            colors = CheckboxDefaults.colors(
                checkedColor = if (tehlikeliMi) KirmiziIstisna else YesilAna,
            ),
        )
        if (baglantilar.isEmpty()) {
            Text(
                text = metin,
                style = MaterialTheme.typography.bodyMedium,
                color = MetinBirincil,
            )
        } else {
            BaglantiliMetin(metin = metin, baglantilar = baglantilar)
        }
    }
}

/**
 * İçinde tıklanabilir parçalar olan metin.
 *
 * `LinkAnnotation.Clickable` kullanıyor: Compose'un `ClickableText`'i
 * kullanımdan kaldırıldı ve erişilebilirlik ağacında bağlantıyı ayrı bir
 * öge olarak bildirmiyordu.
 */
@Composable
private fun BaglantiliMetin(metin: String, baglantilar: Map<String, () -> Unit>) {
    val stil = SpanStyle(color = YesilAna, textDecoration = TextDecoration.Underline)

    val yazi = buildAnnotatedString {
        var kalan = metin
        while (kalan.isNotEmpty()) {
            // En erken başlayan bağlantıyı bul; sıraya değil konuma göre.
            val sonraki = baglantilar
                .mapNotNull { (etiket, eylem) ->
                    kalan.indexOf(etiket).takeIf { it >= 0 }?.let { Triple(it, etiket, eylem) }
                }
                .minByOrNull { it.first }

            if (sonraki == null) {
                append(kalan)
                break
            }

            val (konum, etiket, eylem) = sonraki
            append(kalan.substring(0, konum))
            withLink(LinkAnnotation.Clickable(tag = etiket, styles = TextLinkStyles(stil)) { eylem() }) {
                append(etiket)
            }
            kalan = kalan.substring(konum + etiket.length)
        }
    }

    Text(
        text = yazi,
        style = MaterialTheme.typography.bodyMedium,
        color = MetinBirincil,
    )
}
