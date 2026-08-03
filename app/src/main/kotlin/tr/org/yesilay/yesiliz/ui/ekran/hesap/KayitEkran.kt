package tr.org.yesilay.yesiliz.ui.ekran.hesap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.domain.hesap.HesapAlani as Alan
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.SatirBaglanti
import tr.org.yesilay.yesiliz.domain.sozlesme.Sozlesmeler
import tr.org.yesilay.yesiliz.ui.bilesen.OnayKutusu
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.Hata
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu

/** Kayıt — `docs/tasarim-referans.md §2 / 15`. */
@Composable
fun KayitEkran(
    onBasarili: () -> Unit,
    onGiris: () -> Unit,
    onGeri: () -> Unit,
    onSozlesme: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HesapViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Beyaz)
            .statusBarsPadding()
            .navigationBarsPadding()
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

        Yigin {
            Text(
                text = "Hesap oluştur.",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )

            HesapAlani(
                etiket = "Ad Soyad",
                deger = durum.ad,
                onDegis = viewModel::adDegisti,
                hata = durum.hata(Alan.AD),
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
            HesapAlani(
                etiket = "Şifre tekrar",
                deger = durum.sifreTekrar,
                onDegis = viewModel::sifreTekrarDegisti,
                hata = durum.hata(Alan.SIFRE_TEKRAR),
                sifreMi = true,
                sifreGorunur = durum.sifreGorunur,
                klavyeTuru = KeyboardType.Password,
            )

            // İki metin artık gerçekten okunabiliyor. Onay ZORUNLU tutulan
            // bir metnin okunamaması, 18 yaş altı KVKK rejimi düşünülünce
            // ayrıca riskliydi (`debug.md` Y-6).
            OnayKutusu(
                metin = "Kullanım Koşulları'nı ve Gizlilik Politikası'nı okudum, kabul ediyorum.",
                secili = durum.kosullarOnaylandi,
                onDegis = viewModel::kosullariDegistir,
                baglantilar = mapOf(
                    "Kullanım Koşulları" to { onSozlesme(Sozlesmeler.KULLANIM_KOSULLARI) },
                    "Gizlilik Politikası" to { onSozlesme(Sozlesmeler.GIZLILIK_POLITIKASI) },
                ),
            )
            listOfNotNull(durum.hata(Alan.KOSULLAR), durum.hata(Alan.GENEL)).forEach { mesaj ->
                Text(
                    text = mesaj,
                    style = MaterialTheme.typography.labelMedium,
                    color = Hata,
                )
            }

            BirincilButon(
                metin = "Hesap oluştur",
                onTikla = { viewModel.kayitOl(onBasarili) },
                etkin = !durum.islemde,
                islemde = durum.islemde,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Zaten hesabın var mı? ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                )
                SatirBaglanti(metin = "Giriş yap", onTikla = onGiris)
            }
        }

        Spacer(modifier = Modifier.height(Olcu.BolumArasi))
    }
}
