package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import tr.org.yesilay.yesiliz.ui.bilesen.LikertMaddesi
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu

/**
 * SAS-SV anketi — `docs/tasarim-referans.md §2 / 04`.
 *
 * Sayfa başına 2 madde (`docs/spec-anket.md §6.4`): 10 maddeyi tek ekranda
 * göstermek yanıt kalitesini düşürüyor. Sayfa dolmadan "Devam" pasif —
 * eksik yanıt ortalamayla doldurulmuyor.
 */
@Composable
fun AnketEkran(
    durum: OnboardingDurum,
    onYanit: (madde: Int, deger: Int) -> Unit,
    onBitti: () -> Unit,
    onAtla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sayfa by remember { mutableIntStateOf(0) }
    val ilk = sayfa * SasSv.SAYFA_BOYU
    val son = (ilk + SasSv.SAYFA_BOYU).coerceAtMost(SasSv.MADDELER.size)
    val sonSayfa = son >= SasSv.MADDELER.size
    val sayfaTamam = (ilk until son).all { durum.yanitlar[it] > 0 }

    OnboardingIskelet(
        adim = 4,
        ilerleMetni = "Devam",
        onIlerle = { if (sonSayfa) onBitti() else sayfa++ },
        ilerleEtkin = sayfaTamam,
        ikincilMetin = "Şimdi değil",
        onIkincil = onAtla,
        modifier = modifier,
    ) {
        Text(
            text = "Aşağıdaki ifadelere\nne kadar katılıyorsun?",
            style = MaterialTheme.typography.headlineLarge,
            color = MetinBirincil,
        )

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Olcu.Aralik),
        ) {
            (ilk until son).forEach { madde ->
                LikertMaddesi(
                    ifade = SasSv.MADDELER[madde],
                    secili = durum.yanitlar[madde],
                    onSec = { onYanit(madde, it) },
                )
            }
        }
    }
}
