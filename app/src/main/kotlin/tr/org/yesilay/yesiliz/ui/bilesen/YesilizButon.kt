package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.KirmiziIstisna
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Birincil eylem — tam genişlik, 56dp, `YesilAna` dolgu.
 *
 * Tasarımın her ekranında en fazla **bir** tane var; altında ikincil eylem
 * çerçeveli buton ya da metin bağlantı olarak duruyor
 * (`docs/tasarim-referans.md §1`).
 */
@Composable
fun BirincilButon(
    metin: String,
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
    etkin: Boolean = true,
) {
    Button(
        onClick = onTikla,
        enabled = etkin,
        modifier = modifier
            .fillMaxWidth()
            .height(Olcu.ButonYuksekligi),
        shape = RoundedCornerShape(Yaricap.Buton),
        colors = ButtonDefaults.buttonColors(
            containerColor = YesilAna,
            contentColor = Beyaz,
        ),
    ) {
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * İkincil eylem — çerçeveli, dolgusuz.
 * Tasarımda yalnızca hedef önerisi ekranındaki "Kendi hedefini belirle"de var.
 */
@Composable
fun IkincilButon(
    metin: String,
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
    etkin: Boolean = true,
) {
    OutlinedButton(
        onClick = onTikla,
        enabled = etkin,
        modifier = modifier
            .fillMaxWidth()
            .height(Olcu.ButonYuksekligi),
        shape = RoundedCornerShape(Yaricap.Buton),
        border = BorderStroke(Olcu.ButonKenarligi, YesilAna),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = YesilAna),
    ) {
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Geri alınamaz işlem onayı — "Verilerimi/Hesabımı kalıcı sil" (ekran 25/26).
 * `KirmiziIstisna` — CLAUDE.md §12 K10'daki iki istisnadan biri, başka
 * hiçbir yerde kullanılmaz.
 */
@Composable
fun TehlikeliButon(
    metin: String,
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
    etkin: Boolean = true,
) {
    Button(
        onClick = onTikla,
        enabled = etkin,
        modifier = modifier
            .fillMaxWidth()
            .height(Olcu.ButonYuksekligi),
        shape = RoundedCornerShape(Yaricap.Buton),
        colors = ButtonDefaults.buttonColors(
            containerColor = KirmiziIstisna,
            contentColor = Beyaz,
        ),
    ) {
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * "Şimdi değil", "Vazgeç", "Nasıl çalışır?" — birincil butonun altındaki
 * kaçış yolu. Tasarımda gri, altı çizgisiz.
 */
@Composable
fun MetinBaglanti(
    metin: String,
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onTikla,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(contentColor = MetinIkincil),
    ) {
        Text(text = metin, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Çerçevesiz, yeşil metinli ikincil eylem — nudge overlay'deki "Devam et".
 *
 * [MetinBaglanti]'den farkı renk (gri değil `YesilAna`): "Devam et" bir
 * kaçış değil, eşit ağırlıklı bir seçim ("mola ver" kadar geçerli bir
 * karar, küçümsenen bir "vazgeç" değil).
 */
@Composable
fun DuzYesilButon(
    metin: String,
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onTikla,
        modifier = modifier
            .fillMaxWidth()
            .height(Olcu.ButonYuksekligi),
        shape = RoundedCornerShape(Yaricap.Buton),
        colors = ButtonDefaults.textButtonColors(contentColor = YesilAna),
    ) {
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
