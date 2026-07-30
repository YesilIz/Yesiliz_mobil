package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import tr.org.yesilay.yesiliz.ui.tema.Ayrac
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

private const val HANE = 6

/**
 * 6 haneli eşleştirme kodu — hem üretilen kodu **göstermek** (ebeveyn
 * paneli ilk kurulum, ekran 21 üst satır) hem kullanıcıdan **almak**
 * (aynı ekranın alt satırı) için kullanılıyor.
 *
 * Ebeveyn eşleştirmesi MVP'de mock (`Tercihler`, gerçek sunucu doğrulaması
 * yok — bkz. `EbeveynViewModel`), bu yüzden burada da doğrulama değil
 * yalnızca 6 haneli girişi toplama sorumluluğu var.
 */
@Composable
fun KodGoster(kod: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
        repeat(HANE) { i ->
            KodKutusuTekil(
                karakter = kod.getOrNull(i)?.toString() ?: "",
                aktif = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun KodGir(
    deger: TextFieldValue,
    onDegisti: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = deger,
        onValueChange = { yeni ->
            if (yeni.text.length <= HANE && yeni.text.all(Char::isDigit)) onDegisti(yeni)
        },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(color = androidx.compose.ui.graphics.Color.Transparent),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
                repeat(HANE) { i ->
                    KodKutusuTekil(
                        karakter = deger.text.getOrNull(i)?.toString() ?: "",
                        aktif = i == deger.text.length,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

@Composable
private fun KodKutusuTekil(karakter: String, aktif: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(Olcu.ButonYuksekligi)
            .clip(RoundedCornerShape(Yaricap.Cip)),
        color = if (karakter.isNotEmpty()) Yesil50 else Yesil50.copy(alpha = 0.4f),
        border = if (aktif) BorderStroke(Olcu.ButonKenarligi, YesilAna) else BorderStroke(Olcu.KartKenarligi, Ayrac),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = karakter,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
        }
    }
}
