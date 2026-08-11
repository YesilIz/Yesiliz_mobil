package tr.org.yesilay.yesiliz.ui.ekran.odul

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.IkincilButon
import tr.org.yesilay.yesiliz.ui.buyukHarf
import tr.org.yesilay.yesiliz.ui.tema.Ayrac
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu

/**
 * Kazanılmış dijital rozet için uygulama içi başarı kartı.
 *
 * Gerçek ödül/bağış sistemi kurulmadığı için bu kart kurum adına düzenlenmiş
 * belge, bağış makbuzu veya kupon gibi davranmaz.
 */
@Composable
fun BasariKartiIcerik(
    odulAdi: String,
    kullaniciAdi: String,
    onPaylas: () -> Unit,
    onKapat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Yaricap.Kart))
                .background(Beyaz)
                .border(
                    width = CerceveKalinligi,
                    color = YesilAna,
                    shape = RoundedCornerShape(Yaricap.Kart),
                )
                .padding(Olcu.KartIci),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(Olcu.IkonRozeti)
                    .clip(CircleShape)
                    .background(YesilKoyu),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Park, contentDescription = null, tint = Beyaz)
            }
            Spacer(modifier = Modifier.height(Bosluk.S))
            Text(
                text = "Yeşilİz",
                style = MaterialTheme.typography.labelMedium,
                color = MetinUcuncul,
            )

            Spacer(modifier = Modifier.height(Olcu.Aralik))
            Text(
                text = "Uygulama içi başarı kartı".buyukHarf(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = YesilAna,
            )
            Spacer(modifier = Modifier.height(Bosluk.S))
            Text(
                text = kullaniciAdi,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Bosluk.S))
            Text(
                text = "ekranla ilişkisini yeniden kurma yolunda attığı adımlarla " +
                    "“$odulAdi” uygulama içi başarısına ulaştı.",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
            )

        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        BirincilButon(metin = "Paylaş", onTikla = onPaylas)
        Spacer(modifier = Modifier.height(Bosluk.S))
        IkincilButon(metin = "Kapat", onTikla = onKapat)

        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = BASARI_KARTI_ACIKLAMASI,
            style = MaterialTheme.typography.labelMedium,
            color = MetinUcuncul,
            textAlign = TextAlign.Center,
        )
    }
}

internal const val BASARI_KARTI_ACIKLAMASI =
    "Bu kart yalnızca uygulama içi ilerlemeyi gösterir; kupon, bağış veya " +
        "resmî belge değildir."

private val CerceveKalinligi = Olcu.ButonKenarligi
