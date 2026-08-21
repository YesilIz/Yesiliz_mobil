package tr.org.yesilay.yesiliz.ui.ekran.hesap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tr.org.yesilay.yesiliz.ui.bilesen.IkincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Hesap askıya alındığında görünen engelleyici ekran.
 *
 * **Niçin var:** sunucu askıdaki hesaba `403` dönüyor. Bu yanıt olduğu gibi
 * bırakılsaydı depolar onu "sunucuya ulaşılamıyor"a indirgerdi ve kullanıcı
 * uygulamanın bozulduğunu sanardı — sebebini hiç öğrenemeden. Yapamadığını
 * söylemek, sessizce çalışmamaktan iyidir (CLAUDE.md §4, §12 K37).
 *
 * **Ton:** betimleyici, suçlayıcı değil. Ekran ne olduğunu ve ne
 * yapılabileceğini söylüyor; emir kipi ve korkutma yok (§4). Kırmızı da
 * yok — K10'un iki istisnası bu ekran değil.
 *
 * Gerekçe sunucudan geliyor ve **uydurulmuyor**: boşsa o cümle hiç
 * çizilmiyor, "sebep belirtilmemiş" gibi bir dolgu yazılmıyor.
 *
 * **Destek butonu yok** — uygulamada tanımlı bir destek adresi ya da
 * telefonu bulunmuyor. Olmayan bir kanala düğme koymak, tam da §4'ün
 * yasakladığı şey. Yerine gerçekten işe yarayan tek eylem var: yeniden
 * dene. Askı panelden kaldırıldıysa ilk başarılı yanıt bu ekranı
 * kapatıyor ve kullanıcı uygulamayı yeniden başlatmak zorunda kalmıyor.
 */
@Composable
fun AskidaEkran(
    sebep: String,
    onTekrarDene: () -> Unit,
    modifier: Modifier = Modifier,
    deneniyor: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Beyaz)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Olcu.EkranKenari),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Yesil50, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = YesilAna,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(Modifier.height(Bosluk.L))

            Text(
                text = "Hesabın şu anda kullanıma kapalı",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MetinBirincil,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Bosluk.S))

            Text(
                text = "Verilerin silinmedi. Hesabın yeniden açıldığında " +
                    "geçmişin, puanların ve ağacın olduğu gibi seni bekliyor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
            )

            if (sebep.isNotBlank()) {
                Spacer(Modifier.height(Bosluk.L))
                Kart(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Belirtilen sebep",
                        style = MaterialTheme.typography.labelMedium,
                        color = MetinIkincil,
                    )
                    Spacer(Modifier.height(Bosluk.Xs))
                    Text(
                        text = sebep,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinBirincil,
                    )
                }
            }

            Spacer(Modifier.height(Bosluk.L))

            IkincilButon(
                metin = if (deneniyor) "Kontrol ediliyor…" else "Yeniden dene",
                onTikla = onTekrarDene,
                etkin = !deneniyor,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Bosluk.S))

            Text(
                text = "Hesabın yeniden açıldıysa bu ekran kendiliğinden kapanır.",
                style = MaterialTheme.typography.bodySmall,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
            )
        }
    }
}


/**
 * Sunucu bakımdayken görünen bilgilendirme (M9).
 *
 * [AskidaEkran] ile aynı desende ama **farklı bir şey söylüyor**: askı
 * hesaba özel ve kalıcı, bakım herkese ve geçici. İkisini aynı ekrana
 * indirgemek, kullanıcıya hesabının kapandığını sandırırdı.
 *
 * **Engelleyici değil.** Askı erişimi gerçekten kapatıyor; bakımda ise
 * uygulamanın büyük kısmı çalışmaya devam ediyor (ölçüm, görevler,
 * aktiviteler hepsi cihazda). Bu yüzden ekran kapatılabiliyor: kullanıcıyı
 * yapabileceği şeylerden alıkoymanın gerekçesi yok.
 */
@Composable
fun BakimSeridi(
    mesaj: String,
    onKapat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Kart(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Build,
                contentDescription = null,
                tint = YesilAna,
            )
            Spacer(Modifier.width(Bosluk.S))
            Text(
                text = "Sunucuda bakım var",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MetinBirincil,
            )
        }
        Spacer(Modifier.height(Bosluk.S))
        Text(
            // Mesaj **sunucudan** geliyor: bakımın ne kadar süreceğini ya
            // da neyi etkilediğini cihaz bilemez, uydurmuyor.
            text = mesaj,
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
        Spacer(Modifier.height(Bosluk.S))
        IkincilButon(metin = "Anladım", onTikla = onKapat)
    }
}
