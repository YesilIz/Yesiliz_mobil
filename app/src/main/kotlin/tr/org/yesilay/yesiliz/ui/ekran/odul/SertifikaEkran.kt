package tr.org.yesilay.yesiliz.ui.ekran.odul

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu

/**
 * Kazanılmış ödülün sertifikası — tasarım 19'daki "Sertifikayı görüntüle"
 * butonunun hedefi.
 *
 * **Cihazda üretiliyor.** Sertifika numarası ödül adı + tarihten
 * deterministik türetiliyor; sunucudan gelen bir belge yok çünkü gerçek
 * ödül/bağış sistemi henüz kurulmadı (§12 A4 açık kararı: ödül finansmanı
 * teknik değil, sponsorluk kararı). Bu ekran o karar verildiğinde
 * sunucudan gelen gerçek sertifikayı gösterecek biçime hazır — yalnızca
 * [sertifikaNo] kaynağı değişecek.
 */
@Composable
fun SertifikaIcerik(
    odulAdi: String,
    kullaniciAdi: String,
    bagisTarihi: String,
    sertifikaNo: String,
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
                text = "Türkiye Yeşilay Cemiyeti",
                style = MaterialTheme.typography.labelMedium,
                color = MetinUcuncul,
            )

            Spacer(modifier = Modifier.height(Olcu.Aralik))
            Text(
                text = "Teşekkür belgesi".buyukHarf(),
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
                    "“$odulAdi” ödülünü kazanmıştır.",
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Yaricap.Buton))
                    .background(Yesil50)
                    .padding(Olcu.Aralik),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Tarih",
                        style = MaterialTheme.typography.labelMedium,
                        color = MetinUcuncul,
                    )
                    Text(
                        text = bagisTarihi,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MetinBirincil,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Belge no",
                        style = MaterialTheme.typography.labelMedium,
                        color = MetinUcuncul,
                    )
                    Text(
                        text = sertifikaNo,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MetinBirincil,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        BirincilButon(metin = "Paylaş", onTikla = onPaylas)
        Spacer(modifier = Modifier.height(Bosluk.S))
        IkincilButon(metin = "Kapat", onTikla = onKapat)

        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Bu belge uygulamada üretilmiştir; resmî bir bağış makbuzu " +
                "değildir.",
            style = MaterialTheme.typography.labelMedium,
            color = MetinUcuncul,
            textAlign = TextAlign.Center,
        )
    }
}

private val CerceveKalinligi = Olcu.ButonKenarligi
