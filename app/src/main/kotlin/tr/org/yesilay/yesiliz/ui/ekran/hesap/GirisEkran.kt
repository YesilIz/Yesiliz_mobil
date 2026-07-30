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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Icon
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
import tr.org.yesilay.yesiliz.ui.bilesen.IkincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.SatirBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu

/**
 * Giriş — `docs/tasarim-referans.md §2 / 14`.
 *
 * "Google ile devam et" tasarımda var ama **uygulanmadı**: gerçek bir
 * OAuth akışı sunucu ve Play Console yapılandırması istiyor, ikisi de
 * Faz 2. Çalışmayan bir buton koymaktansa hiç koymamak doğru — bu ekranın
 * yerine geçen "Misafir olarak devam et" zaten çalışıyor.
 */
@Composable
fun GirisEkran(
    onBasarili: () -> Unit,
    onKayit: () -> Unit,
    onSifremiUnuttum: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HesapViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Beyaz)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(Olcu.EkranKenari),
    ) {
        Spacer(modifier = Modifier.height(Olcu.BolumArasi))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(Olcu.IkonRozeti)
                        .clip(RoundedCornerShape(Yaricap.Cip))
                        .background(YesilKoyu),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Park, contentDescription = null, tint = Beyaz)
                }
                Spacer(modifier = Modifier.height(Bosluk.S))
                Text(
                    text = "Yeşilİz",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
            }
        }

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))

        Yigin {
            Text(
                text = "Hoş geldin.",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )

            HesapAlani(
                etiket = "E-posta",
                deger = durum.eposta,
                onDegis = viewModel::epostaDegisti,
                hata = durum.hata(Alan.EPOSTA),
                klavyeTuru = KeyboardType.Email,
            )
            HesapAlani(
                etiket = "Şifre",
                deger = durum.sifre,
                onDegis = viewModel::sifreDegisti,
                hata = durum.hata(Alan.SIFRE),
                sifreMi = true,
                sifreGorunur = durum.sifreGorunur,
                onGorunurluk = viewModel::sifreGorunurlugu,
                klavyeTuru = KeyboardType.Password,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                SatirBaglanti(metin = "Şifremi unuttum", onTikla = onSifremiUnuttum)
            }

            BirincilButon(
                metin = "Giriş yap",
                onTikla = { viewModel.girisYap(onBasarili) },
                etkin = !durum.islemde,
            )

            VeyaAyraci()

            IkincilButon(
                metin = "Misafir olarak devam et",
                onTikla = { viewModel.misafirDevam(onBasarili) },
            )

            // Sunucu yokken hesap yalnızca adı saklıyor; bunu gizlemek
            // kullanıcıya olmayan bir güvence vermek olurdu.
            Text(
                text = "Şu an hesaplar yalnızca bu cihazda tutuluyor; " +
                    "verilerin telefonundan çıkmıyor.",
                style = MaterialTheme.typography.labelMedium,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Hesabın yok mu? ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                )
                SatirBaglanti(metin = "Kayıt ol", onTikla = onKayit)
            }
        }

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))
    }
}
