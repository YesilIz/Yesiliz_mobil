package tr.org.yesilay.yesiliz.ui.ekran.hesap

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.domain.hesap.HesapAlani as Alan
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Şifremi unuttum — `docs/tasarim-referans.md §2 / 38`.
 *
 * **Gerçek bir e-posta gönderilmiyor** (sunucu Faz 2). Ekran bunu
 * kullanıcıya açıkça söylüyor; "bağlantı gönderildi" deyip hiçbir şey
 * yapmamak, kullanıcıyı gelen kutusunda beklemeye bırakırdı.
 */
@Composable
fun SifreEkran(
    onGeri: () -> Unit,
    onGiris: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HesapViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Beyaz)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Olcu.EkranKenari),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Olcu.EkranKenari),
        ) {
            IconButton(onClick = onGeri) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Geri",
                    tint = MetinBirincil,
                )
            }
        }

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(Olcu.AvatarBuyuk)
                    .clip(CircleShape)
                    .background(Yesil50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (durum.sifirlamaGonderildi) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.Lock
                    },
                    contentDescription = null,
                    tint = YesilAna,
                    modifier = Modifier.size(Olcu.IkonRozeti),
                )
            }
        }

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))

        Yigin {
            Text(
                text = if (durum.sifirlamaGonderildi) {
                    "İsteğin alındı"
                } else {
                    "Şifreni mi unuttun?"
                },
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            if (durum.sifirlamaGonderildi) {
                Text(
                    text = "Şifre sıfırlama e-postası, hesap sistemi devreye " +
                        "girdiğinde gönderilecek. Şimdilik misafir olarak " +
                        "devam edebilirsin; verilerin telefonunda duruyor.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                    textAlign = TextAlign.Center,
                )
                BirincilButon(metin = "Giriş ekranına dön", onTikla = onGiris)
            } else {
                Text(
                    text = "E-posta adresini gir, sana sıfırlama bağlantısı gönderelim.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                    textAlign = TextAlign.Center,
                )

                HesapAlani(
                    etiket = "E-posta",
                    deger = durum.eposta,
                    onDegis = viewModel::epostaDegisti,
                    hata = durum.hata(Alan.EPOSTA),
                    klavyeTuru = KeyboardType.Email,
                )

                BirincilButon(
                    metin = "Sıfırlama bağlantısı gönder",
                    onTikla = viewModel::sifreSifirla,
                    etkin = !durum.islemde,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Şifreni hatırladın mı? ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MetinIkincil,
                    )
                    MetinBaglanti(metin = "Giriş yap", onTikla = onGiris)
                }

                Text(
                    text = "Hesap sistemi henüz devrede değil; e-posta gönderimi " +
                        "sunucu bağlantısıyla birlikte gelecek.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MetinUcuncul,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))
    }
}
