package tr.org.yesilay.yesiliz.ui.ekran.hesap

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import tr.org.yesilay.yesiliz.ui.tema.Ayrac
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.Hata
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Hesap ekranlarının ortak alanı — `Yesil50` zeminli, etiketi üstte.
 *
 * **Hata rengi yeşil** (`Renk.Hata` = `YesilKoyu`). Kırmızı yalnızca iki
 * istisnada kullanılıyor (CLAUDE.md §12 K10) ve form hatası onlardan biri
 * değil. Material3'ün varsayılan error paleti bu yüzden ezilmek zorunda.
 */
@Composable
fun HesapAlani(
    etiket: String,
    deger: String,
    onDegis: (String) -> Unit,
    modifier: Modifier = Modifier,
    hata: String? = null,
    sifreMi: Boolean = false,
    sifreGorunur: Boolean = false,
    onGorunurluk: (() -> Unit)? = null,
    klavyeTuru: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Bosluk.Xs))
        OutlinedTextField(
            value = deger,
            onValueChange = onDegis,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = hata != null,
            shape = RoundedCornerShape(Yaricap.Girdi),
            visualTransformation = if (sifreMi && !sifreGorunur) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = klavyeTuru),
            trailingIcon = if (sifreMi && onGorunurluk != null) {
                {
                    IconButton(onClick = onGorunurluk) {
                        Icon(
                            imageVector = if (sifreGorunur) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (sifreGorunur) "Şifreyi gizle" else "Şifreyi göster",
                            tint = MetinUcuncul,
                        )
                    }
                }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Yesil50,
                unfocusedContainerColor = Yesil50,
                errorContainerColor = Yesil50,
                focusedBorderColor = YesilAna,
                unfocusedBorderColor = Ayrac,
                // Kırmızı yok: hata kenarlığı da koyu yeşil.
                errorBorderColor = Hata,
                errorTextColor = MetinBirincil,
                errorCursorColor = YesilAna,
            ),
        )
        if (hata != null) {
            Spacer(modifier = Modifier.height(Bosluk.Xs))
            Text(
                text = hata,
                style = MaterialTheme.typography.labelMedium,
                color = Hata,
            )
        }
    }
}

/** "veya" ayracı — tasarım 14, 15. */
@Composable
fun VeyaAyraci(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Ayrac)
        Text(
            text = "veya",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
            modifier = Modifier.padding(horizontal = Olcu.Aralik),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Ayrac)
    }
}
