package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import tr.org.yesilay.yesiliz.ui.bilesen.BolumBasligi
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Yaş ve cinsiyet — `docs/tasarim-referans.md §2 / 03`.
 *
 * Yaş **gerekli**: 18 altı ebeveyn onayı zorunlu, KVKK ayrı rejim
 * (CLAUDE.md §4). Cinsiyet öneri kişiselleştirmesi için; "Belirtmek
 * istemiyorum" gerçek bir seçenek, ilerlemeyi engellemiyor.
 */
@Composable
fun ProfilKurulumEkran(
    durum: OnboardingDurum,
    onYas: (Int) -> Unit,
    onCinsiyet: (Cinsiyet) -> Unit,
    onDevam: () -> Unit,
    onAtla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingIskelet(
        adim = 3,
        ilerleMetni = "Devam",
        onIlerle = onDevam,
        ilerleEtkin = durum.cinsiyet != null,
        ikincilMetin = "Şimdi değil",
        onIkincil = onAtla,
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Seni biraz\ntanıyalım.",
            style = MaterialTheme.typography.displayLarge,
            color = MetinBirincil,
        )

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))

        BolumBasligi("Yaşın")
        Spacer(modifier = Modifier.height(Bosluk.S))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Yaricap.Buton))
                .background(Yesil50)
                .padding(Bosluk.S),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${durum.yas}",
                style = MaterialTheme.typography.displayLarge,
                color = MetinBirincil,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Olcu.Aralik),
            )
            YasButonu(Icons.Filled.Remove, Beyaz, MetinBirincil) { onYas(-1) }
            Spacer(modifier = Modifier.size(Bosluk.S))
            YasButonu(Icons.Filled.Add, YesilAna, Beyaz) { onYas(+1) }
        }

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))

        BolumBasligi("Cinsiyetin")
        Spacer(modifier = Modifier.height(Bosluk.S))
        // `IntrinsicSize.Min` + `fillMaxHeight`: "Belirtmek istemiyorum"
        // iki satıra düşünce kapsayıcı yükseliyor ama seçili pil tek satır
        // yüksekliğinde kalıyordu; seçim hizasız görünüyordu
        // (`debug.md` O-9).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(Yaricap.Buton))
                .background(Yesil50)
                .padding(Bosluk.Xs),
        ) {
            Cinsiyet.entries.forEach { secenek ->
                val aktif = secenek == durum.cinsiyet
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(Yaricap.Cip))
                        .background(if (aktif) YesilAna else Color.Transparent)
                        .clickable { onCinsiyet(secenek) }
                        .padding(vertical = Bosluk.M, horizontal = Bosluk.Xs),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = secenek.etiket,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (aktif) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (aktif) Beyaz else MetinIkincil,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "Bu bilgiler önerileri sana uygun hale getirmek için.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun YasButonu(
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    zemin: Color,
    renk: Color,
    onTikla: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Olcu.ButonYuksekligi)
            .clip(RoundedCornerShape(Yaricap.Cip))
            .background(zemin)
            .clickable(onClick = onTikla),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = ikon, contentDescription = null, tint = renk)
    }
}
