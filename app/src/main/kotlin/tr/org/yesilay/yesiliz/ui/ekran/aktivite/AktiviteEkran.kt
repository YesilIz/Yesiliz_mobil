package tr.org.yesilay.yesiliz.ui.ekran.aktivite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.ui.puanEtiketi
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.suresiDk
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Aktiviteler — dördüncü sekme (`debug.md` İ-1).
 *
 * Ekrandan uzaklaşmanın somut yolları tek bir yerde. Ana ekrandaki günün
 * görevleri hâlâ duruyor; burası "şu an canım ne çekiyor" listesi, orası
 * "bugün ne öneriyoruz".
 *
 * Limiti dolan aktivite **gizlenmiyor**, işaretleniyor: yapılabilir
 * olduğunu görmek, yapılamaz olduğunu öğrenmekten daha az can sıkıcı ve
 * kullanıcı istediği kadar tekrar edebilir -- yalnızca puan tavana takılı.
 */
@Composable
fun AktiviteEkran(
    onAktivite: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AktiviteViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    YesilBaslikliEkran(baslik = "Aktiviteler", modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
        ) {
            Yigin {
                Text(
                    text = "Ekrandan uzaklaşmanın yolları",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Text(
                    text = if (durum.bugunKazanilan > 0) {
                        "Bugün aktivitelerden ${durum.bugunKazanilan} puan kazandın."
                    } else {
                        "Hangisine vaktin varsa. Hiçbiri zorunlu değil."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                )

                durum.kartlar.forEach { kart ->
                    AktiviteSatiri(kart = kart, onTikla = { onAktivite(kart.rota) })
                }
            }
        }
    }
}

@Composable
private fun AktiviteSatiri(kart: AktiviteKarti, onTikla: () -> Unit) {
    Kart(modifier = Modifier.clickable(onClick = onTikla)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonRozeti(
                ikon = kart.ikon,
                zemin = if (kart.limitDoldu) YesilNane else YesilNane,
                renk = YesilKoyu,
                sekil = CircleShape,
            )
            Spacer(modifier = Modifier.width(Olcu.Aralik))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = kart.gorev.baslik,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MetinBirincil,
                )
                Spacer(modifier = Modifier.height(Bosluk.Xs))
                Text(
                    text = kart.gorev.aciklama,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinIkincil,
                )
                Spacer(modifier = Modifier.height(Bosluk.S))
                Row {
                    kart.gorev.suresiDk()?.let {
                        Cip(metin = "$it dk")
                        Spacer(modifier = Modifier.width(Bosluk.S))
                    }
                    Cip(metin = kart.gorev.puan.puanEtiketi(), zemin = YesilNane, renk = YesilAna)
                }
            }
            Spacer(modifier = Modifier.width(Bosluk.S))
            if (kart.limitDoldu) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Bugünlük tamamlandı",
                    tint = YesilAna,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MetinUcuncul,
                )
            }
        }
    }
}
