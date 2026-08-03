package tr.org.yesilay.yesiliz.ui.ekran.bilgi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.ui.kalinIsle
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu

/** Bilgi Bankası — tek yazı. */
@Composable
fun BilgiYaziEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BilgiYaziViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    val yazi = durum.yazi

    YesilBaslikliEkran(
        baslik = yazi?.kategori ?: "Bilgi Bankası",
        onGeri = onGeri,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
        ) {
            if (yazi == null) {
                if (!durum.yukleniyor) {
                    BosDurum(
                        baslik = "Yazı bulunamadı",
                        aciklama = "Bilgi Bankası listesinden tekrar deneyebilirsin.",
                    )
                }
                return@Column
            }

            Yigin {
                Text(
                    text = yazi.baslik,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Cip(metin = "${yazi.okumaDk} dk okuma")
                yazi.govde.forEach { paragraf ->
                    Text(
                        text = kalinIsle(paragraf),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MetinIkincil,
                    )
                }
            }
        }
    }
}
