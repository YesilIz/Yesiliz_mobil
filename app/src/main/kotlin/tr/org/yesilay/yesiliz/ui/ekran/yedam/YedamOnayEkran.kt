package tr.org.yesilay.yesiliz.ui.ekran.yedam

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane
import tr.org.yesilay.yesiliz.domain.yedam.Yedam
import tr.org.yesilay.yesiliz.ui.bilesen.YedamAraButonu

/**
 * Yüksek riskte gösterilen YEDAM köprüsü.
 *
 * Bu sürümün YEDAM'a veri ya da geri arama talebi gönderen bir entegrasyonu
 * yoktur. Kullanıcıya kaydedilmemiş bir onay veya kurulmamış bir bağlantı
 * gösterilmez; yalnızca 115 numarası çeviricide açılır.
 */
@Composable
fun YedamOnayEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    YesilBaslikliEkran(
        baslik = "YEDAM desteği",
        onGeri = onGeri,
        icerikZemini = Beyaz,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
        ) {
            Yigin {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(DaireBoyu)
                            .clip(CircleShape)
                            .background(Yesil50),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SupportAgent,
                            contentDescription = null,
                            tint = YesilKoyu,
                            modifier = Modifier.size(Olcu.IkonRozeti),
                        )
                    }
                }

                Text(
                    text = "Bir uzmanla konuşmak ister misin?",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Text(
                    text = "YEDAM (Yeşilay Danışmanlık Merkezi), bağımlılık " +
                        "alanında ücretsiz ve gizli danışmanlık sunar. 115'i " +
                        "arayarak bir uzmanla görüşebilir veya randevu alabilirsin.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                )

                BilgiSeridi(
                    metin = "Bu ekrandan YEDAM'a hiçbir veri veya iletişim talebi " +
                        "gönderilmez. ${Yedam.NUMARA}'e dokunduğunda yalnızca " +
                        "telefon çeviricisi açılır; aramayı sen başlatırsın.",
                )

                Text(
                    text = Yedam.CALISMA_SAATLERI,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinIkincil,
                )

                YedamAraButonu()
                MetinBaglanti(
                    metin = "Şimdi değil",
                    onTikla = onGeri,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BilgiSeridi(metin: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Buton))
            .background(YesilNane)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = YesilAna)
        Spacer(modifier = Modifier.width(Olcu.Aralik))
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyMedium,
            color = MetinBirincil,
        )
    }
}

private val DaireBoyu = Olcu.AvatarBuyuk
