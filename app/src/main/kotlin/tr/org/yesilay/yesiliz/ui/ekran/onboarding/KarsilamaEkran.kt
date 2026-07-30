package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import tr.org.yesilay.yesiliz.R
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu

/**
 * Karşılama — `docs/tasarim-referans.md §2 / 01`.
 *
 * Fidan illüstrasyonu (evre 1): uygulama da kullanıcı da en baştan başlıyor.
 */
@Composable
fun KarsilamaEkran(
    onBasla: () -> Unit,
    onNasilCalisir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingIskelet(
        adim = 1,
        ilerleMetni = "Başla",
        onIlerle = onBasla,
        ikincilMetin = "Nasıl çalışır?",
        onIkincil = onNasilCalisir,
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.agac_evre1_cicek),
                contentDescription = null,
                modifier = Modifier.height(Olcu.AgacBoyu),
            )
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            Text(
                text = "Ekranla ilişkini\nyeniden kur.",
                style = MaterialTheme.typography.displayLarge,
                color = MetinBirincil,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Bosluk.M))
            Text(
                text = "Yeşilİz, telefon kullanımını anlamana ve daha sağlıklı " +
                    "bir dengeye ulaşmana yardım eder.",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
