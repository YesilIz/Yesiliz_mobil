package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Hareket izni — karşılama akışının 3. adımı.
 *
 * Adım verisi eskiden **yalnızca** Profil › "Adım ve hareket izni"
 * satırından isteniyordu; karşılama akışında sorulmadığı için varsayılan
 * kullanıcı hiç izin vermiyor, aktivite sönümlemesi (`D`) hep 1,0 kalıyor
 * ve risk skoru aktiviteyi hiç görmüyordu.
 *
 * Burada yalnızca **donanım sayacı** izni (`ACTIVITY_RECOGNITION`)
 * isteniyor. Health Connect bilinçli olarak dışarıda: kurulu olmayabiliyor,
 * kendi izin ekranını açıyor ve Play Store yönlendirmesi gerekebiliyor —
 * karşılamanın ortasında kullanıcıyı uygulamadan çıkarmak akışı kırıyor.
 * Health Connect'i bağlamak isteyen Profil › Sağlık'tan bağlıyor; ana
 * ekrandaki hatırlatıcı da oraya götürüyor.
 *
 * **Atlanabilir.** İzin vermeyen kullanıcı uygulamanın geri kalanını
 * eksiksiz kullanmaya devam ediyor; yalnızca adım kartları boş kalıyor
 * (CLAUDE.md §3: hafif dokunuş, zorlama yok).
 */
@Composable
fun HareketIzinEkran(
    onDevam: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // API 28 ve altında `ACTIVITY_RECOGNITION` çalışma zamanı izni değil;
    // istemeye çalışmak sistem diyaloğu açmadan reddedilmiş sayılıyor.
    val izinGerekli = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    val baslatici = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Sonuç ne olursa olsun akış ilerliyor: izin reddi bir hata değil,
        // kullanıcının kararı. Ana ekrandaki hatırlatıcı daha sonra
        // yeniden sunuyor.
        onDevam()
    }

    OnboardingIskelet(
        adim = 3,
        ilerleMetni = "İzin ver",
        onIlerle = {
            if (izinGerekli) {
                baslatici.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                onDevam()
            }
        },
        ikincilMetin = "Şimdi değil",
        onIkincil = onDevam,
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(Olcu.AvatarBuyuk)
                    .clip(CircleShape)
                    .background(Yesil50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = YesilAna,
                    modifier = Modifier.size(Olcu.IkonRozeti),
                )
            }
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            Text(
                text = "Hareketini de\nsayabilmemiz için",
                style = MaterialTheme.typography.displayLarge,
                color = MetinBirincil,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Bosluk.M))
            Text(
                text = "Adım sayın, ekran süresinin yanında dengeyi görmemizi " +
                    "sağlıyor. Telefonun kendi adım sayacını okuyoruz; " +
                    "nerede olduğunu değil, kaç adım attığını.",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Yaricap.Buton))
                    .background(YesilNane)
                    .padding(Olcu.Aralik),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = YesilKoyu)
                Text(
                    text = "Konum ya da GPS istemiyoruz.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinBirincil,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
