package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import tr.org.yesilay.yesiliz.R
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.KartAyraci
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu

/**
 * İlk değerlendirme sonucu — `docs/tasarim-referans.md §2 / 05`.
 *
 * **Teşhis yok.** Bant adı ekranda yazmıyor; cümle "şu an dikkat edilmesi
 * gereken bir düzeyde" (CLAUDE.md §4). Alt metin de bunu açıkça söylüyor:
 * "Bu bir teşhis değil."
 *
 * Anket tek başına yeterli sinyal değil — davranış verisi henüz yok
 * (`alpha` eğrisi, `docs/spec-risk.md`). Bu yüzden ekran "önümüzdeki günlerde
 * ölçüp plan çıkaracağız" diyor, kesin bir skor göstermiyor.
 */
@Composable
fun IlkSonucEkran(
    onPlanimiGor: () -> Unit,
    onAtla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingIskelet(
        adim = 0,
        ilerleMetni = "Planımı gör",
        onIlerle = onPlanimiGor,
        ikincilMetin = "Şimdi değil",
        onIkincil = onAtla,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Cip(metin = "Başlangıç değerlendirmen hazır")
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            Image(
                painter = painterResource(R.drawable.agac_evre2_narenciye),
                contentDescription = null,
                modifier = Modifier.height(Olcu.AgacBoyu),
            )
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            Text(
                text = "Telefon kullanımın şu an dikkat edilmesi gereken bir düzeyde.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Bosluk.M))
            Text(
                text = "Bu bir teşhis değil. Önümüzdeki günlerde gerçek kullanımını " +
                    "ölçüp sana özel bir plan çıkaracağız.",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))

        Kart(icBosluk = Olcu.Aralik) {
            SurukleyiciSatiri(Icons.Filled.NightsStay, "Gece kullanımı")
            KartAyraci()
            SurukleyiciSatiri(Icons.Filled.Schedule, "Kesintisiz süre")
            KartAyraci()
            SurukleyiciSatiri(Icons.Filled.Bolt, "Fiziksel hareket")
        }
    }
}

/**
 * Ölçülecek sürükleyiciler.
 *
 * Her risk hesabı `surukleyiciler` döndürmek zorunda (CLAUDE.md §7);
 * kullanıcı daha ilk ekranda neye bakacağımızı görüyor.
 */
@Composable
private fun SurukleyiciSatiri(ikon: ImageVector, etiket: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Olcu.ButonYuksekligi),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
    ) {
        IkonRozeti(ikon = ikon)
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MetinBirincil,
        )
    }
}
