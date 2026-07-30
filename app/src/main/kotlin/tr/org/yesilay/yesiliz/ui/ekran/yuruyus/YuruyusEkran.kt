package tr.org.yesilay.yesiliz.ui.ekran.yuruyus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.ui.EkranAcikTut
import tr.org.yesilay.yesiliz.ui.binlik
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.veri.DaireselGosterge
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yesil50

/**
 * Aktif yürüyüş — `docs/tasarim-referans.md §4` (resmi teslim yok,
 * `stitch/aktif_y_r_y_d_zeltilmi/` **yalnızca yerleşim** referansı).
 *
 * Nefes ekranıyla (16) aynı tam ekran dilinde: `Yesil50` zemin, üstte kapat,
 * merkezde halka, altta kontroller. İki ekranın aynı görünmesi bilinçli —
 * ikisi de "bir süre telefonu bırak" ekranı.
 */
@Composable
fun YuruyusEkran(
    onKapat: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: YuruyusViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    // Sayaç yalnızca ekran açıkken ilerliyor; ekran kapanırsa duruyor.
    EkranAcikTut(aktif = durum.calisiyorMu)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Yesil50)
            .statusBarsPadding()
            .padding(horizontal = Olcu.EkranKenari),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKapat) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Kapat",
                    tint = MetinBirincil,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Cip(metin = "${durum.kalanMetni} kaldı")
        }

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))

        Text(
            text = "YÜRÜYÜŞ",
            style = MaterialTheme.typography.labelMedium,
            color = MetinUcuncul,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "${YuruyusHesapla.HEDEF_DAKIKA} dakika, acele yok",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            DaireselGosterge(oran = durum.oran) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = MetinBirincil,
                    )
                    Text(
                        text = YuruyusHesapla.sayacMetni(durum.gecenSaniye),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MetinBirincil,
                    )
                    // Adım yalnızca Health Connect izniyle görünüyor.
                    durum.adim?.let { adim ->
                        Text(
                            text = "${adim.binlik()} adım",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetinIkincil,
                        )
                    }
                }
            }
        }

        if (durum.tamamlandiMi) {
            Kart {
                Text(
                    text = "Yürüyüşü tamamladın",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Text(
                    text = "+${durum.kazanilanPuan} puan hesabına eklendi.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                )
            }
            Spacer(modifier = Modifier.height(Olcu.Aralik))
            BirincilButon(metin = "Bitir", onTikla = onKapat)
        } else {
            Text(
                text = "Sayaç ekran açıkken ilerliyor. Telefonu cebine koyarsan " +
                    "duruyor — bu ekranı açık tutabilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinUcuncul,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Olcu.Aralik))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Bosluk.M, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::bastanBaslat) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Baştan başla",
                        tint = MetinIkincil,
                    )
                }
                BirincilButon(
                    metin = if (durum.calisiyorMu) "Duraklat" else "Başlat",
                    onTikla = viewModel::baslatDuraklat,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (durum.calisiyorMu) {
                        Icons.Filled.Pause
                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = null,
                    tint = MetinIkincil,
                )
            }
        }

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))
    }
}
