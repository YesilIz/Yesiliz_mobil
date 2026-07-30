package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import tr.org.yesilay.yesiliz.ui.bilesen.IkincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.IlerlemeCubugu
import tr.org.yesilay.yesiliz.ui.bilesen.veri.DaireselGosterge
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu

/**
 * Hedef önerisi — `docs/tasarim-referans.md §2 / 06`.
 *
 * İlk hafta hedefi mevcut ortalamanın yalnızca **15 dk** altında. Büyük
 * sıçrama isteyen hedef tutturulamıyor, tutturulamayan hedef uygulamayı
 * sildiriyor. 4 haftalık merdiven kullanıcıya nereye gittiğini gösteriyor.
 */
@Composable
fun HedefEkran(
    durum: OnboardingDurum,
    onBasla: () -> Unit,
    onKendiHedefi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingIskelet(
        adim = 0,
        ilerleMetni = "Hadi başlayalım",
        onIlerle = onBasla,
        modifier = modifier,
        ekButon = {
            IkincilButon(metin = "Kendi hedefini belirle", onTikla = onKendiHedefi)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Küçük adımlarla\nbaşlıyoruz.",
                style = MaterialTheme.typography.displayLarge,
                color = MetinBirincil,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Bosluk.M))
            Text(
                text = "Şu anki ortalaman ${durum.mevcutOrtalama} dakika. İlk hafta " +
                    "hedefin sadece ${durum.mevcutOrtalama - durum.hedefDakika} dakika " +
                    "daha az. Her hafta biraz daha ineceğiz.",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(1f))

            DaireselGosterge(oran = HEDEF_HALKA_ORANI) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${durum.hedefDakika} dk",
                        style = MaterialTheme.typography.displayLarge,
                        color = MetinBirincil,
                    )
                    Text(
                        text = "günlük hedefin",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MetinIkincil,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            HaftaMerdiveni(durum.hedefDakika)
        }
    }
}

/** 4 haftalık iniş: 285 → 240 → 180 → 120. */
@Composable
private fun HaftaMerdiveni(ilkHedef: Int) {
    val haftalar = listOf(ilkHedef, 240, 180, 120)

    Column(modifier = Modifier.fillMaxWidth()) {
        IlerlemeCubugu(oran = 1f / haftalar.size)
        Spacer(modifier = Modifier.height(Bosluk.S))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            haftalar.forEachIndexed { i, dakika ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$dakika",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MetinBirincil,
                    )
                    Text(
                        text = "${i + 1}. Hafta",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinUcuncul,
                    )
                }
            }
        }
    }
}

/**
 * Halkanın dolu kısmı — tasarımda tam daire değil, küçük bir boşluk var.
 * "Hedefe ulaşılmadı" demiyor; halka hedefin kendisini temsil ediyor.
 */
private const val HEDEF_HALKA_ORANI = 0.94f
