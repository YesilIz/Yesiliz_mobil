package tr.org.yesilay.yesiliz.ui.ekran.profil.icerik

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import tr.org.yesilay.yesiliz.BuildConfig
import tr.org.yesilay.yesiliz.domain.sozlesme.Sozlesmeler
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.KartAyraci
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu

/** Alt sayfa 29 — Hakkında. Sürüm, kurulu APK'nın derleme bilgisidir. */
@Composable
internal fun HakkindaIcerik(onKapat: () -> Unit, onSozlesme: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            androidx.compose.material3.IconButton(onClick = onKapat) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Kapat",
                    tint = MetinBirincil,
                )
            }
        }

        IkonRozeti(
            ikon = Icons.Filled.Park,
            zemin = YesilKoyu,
            renk = Beyaz,
            olcu = Olcu.IkonRozeti,
            sekil = RoundedCornerShape(Yaricap.Cip),
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Yeşilİz",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Text(
            text = "Sürüm ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinUcuncul,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Yeşilİz, ekranla sağlıklı bir ilişki kurmana yardımcı olur.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        // Üç satır da chevron'lu ve tıklanabilir GÖRÜNÜYORDU ama `onTikla`
        // almıyordu; metinler hiçbir yerde yoktu (`debug.md` Y-6).
        val baglantilar = listOf(
            "Kullanım Koşulları" to Sozlesmeler.KULLANIM_KOSULLARI,
            "Gizlilik Politikası" to Sozlesmeler.GIZLILIK_POLITIKASI,
            "Açık kaynak lisansları" to Sozlesmeler.ACIK_KAYNAK,
        )
        baglantilar.forEachIndexed { i, (satir, id) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSozlesme(id) }
                    .padding(vertical = Bosluk.M),
            ) {
                Text(
                    text = satir,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MetinBirincil,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MetinUcuncul,
                )
            }
            if (i != baglantilar.lastIndex) KartAyraci()
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "© 2026 Yeşilİz",
            style = MaterialTheme.typography.bodySmall,
            color = MetinUcuncul,
        )
    }
}
