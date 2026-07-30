package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import tr.org.yesilay.yesiliz.ui.bilesen.BolumBasligi
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil200
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Kendi hedefini belirle — `docs/tasarim-referans.md §2 / 07`.
 *
 * Slider'ın üst sınırı mevcut ortalama: hedefin bugünkünden yüksek olması
 * anlamsız. Alt sınır 60 dk — sıfır hedef gerçekçi değil ve tutturulamayan
 * hedef vazgeçiriyor.
 */
@Composable
fun HedefOzelEkran(
    durum: OnboardingDurum,
    onHedef: (Int) -> Unit,
    onHizli: (Int) -> Unit,
    onKaydet: () -> Unit,
    onVazgec: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingIskelet(
        adim = 0,
        ilerleMetni = "Hedefi kaydet",
        onIlerle = onKaydet,
        ikincilMetin = "Vazgeç",
        onIkincil = onVazgec,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Kendi hedefini belirle.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Bosluk.S))
            Text(
                text = "Şu anki ortalaman ${durum.mevcutOrtalama} dakika. " +
                    "Kendine uygun bir günlük hedef seç.",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Olcu.BolumArasi))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${durum.hedefDakika}",
                    style = MaterialTheme.typography.displayLarge,
                    color = MetinBirincil,
                )
                Text(
                    text = " dk",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                    modifier = Modifier.padding(bottom = Bosluk.S),
                )
            }
            Text(
                text = "yeni günlük hedefin",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
            )
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Slider(
            value = durum.hedefDakika.toFloat(),
            onValueChange = { onHedef(it.toInt()) },
            valueRange = OnboardingViewModel.HEDEF_ALT.toFloat()..durum.mevcutOrtalama.toFloat(),
            colors = SliderDefaults.colors(
                activeTrackColor = YesilAna,
                inactiveTrackColor = Yesil200,
                thumbColor = Beyaz,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${OnboardingViewModel.HEDEF_ALT} dk",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinUcuncul,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${durum.mevcutOrtalama} dk",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinUcuncul,
            )
        }

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))

        BolumBasligi("Hızlı seçim")
        Spacer(modifier = Modifier.height(Bosluk.S))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
        ) {
            HIZLI_DUSUSLER.forEach { dusus ->
                val aktif = durum.hedefDakika == durum.mevcutOrtalama - dusus
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Yaricap.Buton))
                        .background(if (aktif) YesilAna else Yesil50)
                        .clickable { onHizli(dusus) }
                        .padding(vertical = Olcu.Aralik),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "−$dusus dk",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (aktif) Beyaz else MetinBirincil,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "Hedefini istediğin zaman değiştirebilirsin.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

/** Tasarımdaki hızlı seçim çipleri. */
private val HIZLI_DUSUSLER = listOf(15, 30, 45)
