package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * "Nasıl çalışır?" — karşılama ekranındaki ikinci bağlantının hedefi.
 *
 * Bu ekran yoktu: `onBasla` ve `onNasilCalisir` **birebir aynı** hedefe
 * (`Rota.IZIN`) gidiyordu, yani bağlantı kullanıcıya bir şey vaat edip
 * hiçbir şey göstermiyordu (`debug.md` O-3).
 *
 * İçerik CLAUDE.md §3'teki çekirdek döngü — beş adım. Uygulamanın ne
 * YAPMADIĞI da yazılı: kilitleme ve ceza yok. İlk açılışta bunu söylemek,
 * kullanıcının izin vermeden önce neyi kabul ettiğini bilmesini sağlıyor.
 */
@Composable
fun NasilCalisirEkran(
    onDevam: () -> Unit,
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingIskelet(
        adim = 0,
        ilerleMetni = "Anladım, devam",
        onIlerle = onDevam,
        ikincilMetin = "Geri dön",
        onIkincil = onGeri,
        modifier = modifier,
    ) {
        Text(
            text = "Yeşilİz nasıl çalışır?",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Adim(
            ikon = Icons.Filled.Visibility,
            baslik = "İzler",
            aciklama = "Hangi uygulamayı ne kadar kullandığını ölçer. " +
                "Mesajlarını, fotoğraflarını ve konumunu okumaz.",
        )
        Adim(
            ikon = Icons.Filled.Insights,
            baslik = "Değerlendirir",
            aciklama = "Ölçümü telefonunun içinde bir skora çevirir. " +
                "Hesaplama cihazında yapılır, sunucuya gitmez.",
        )
        Adim(
            ikon = Icons.Filled.NotificationsActive,
            baslik = "Hatırlatır",
            aciklama = "Uzun bir oturumdan sonra birkaç saniyelik kısa bir " +
                "hatırlatma gösterir. Hiçbir uygulamayı kilitlemez.",
        )
        Adim(
            ikon = Icons.AutoMirrored.Filled.DirectionsWalk,
            baslik = "Ödüllendirir",
            aciklama = "Yürüyüş, nefes egzersizi ve hedefini tutturmak puan " +
                "kazandırır. Ceza puanı yoktur; hiçbir davranış puanını düşürmez.",
        )
        Adim(
            ikon = Icons.Filled.EmojiEvents,
            baslik = "Yalnız bırakmaz",
            aciklama = "İstersen arkadaşlarınla aynı ligde yarışırsın.",
        )
        Adim(
            ikon = Icons.Filled.SupportAgent,
            baslik = "Gerekirse köprü kurar",
            aciklama = "Zorlanıyorsan ve sen istersen, Yeşilay'ın ücretsiz " +
                "danışmanlık hattı YEDAM'a yönlendirir.",
        )
    }
}

@Composable
private fun Adim(ikon: ImageVector, baslik: String, aciklama: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
    ) {
        IkonRozeti(ikon = ikon, zemin = YesilNane, renk = YesilKoyu, sekil = CircleShape)
        Spacer(modifier = Modifier.width(Olcu.Aralik))
        Column {
            Text(
                text = baslik,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
            Spacer(modifier = Modifier.height(Bosluk.Xs))
            Text(
                text = aciklama,
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
            )
        }
    }
    Spacer(modifier = Modifier.height(Olcu.Aralik))
}
