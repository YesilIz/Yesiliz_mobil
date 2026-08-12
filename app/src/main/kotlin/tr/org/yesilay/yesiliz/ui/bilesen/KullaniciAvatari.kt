package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import tr.org.yesilay.yesiliz.ui.buyukHarf
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.avatarRengi

/**
 * Baş harf avatarı.
 *
 * Foto yüklenmiyor — profil fotoğrafı toplamak veri politikasına yük bindirir
 * ve tasarımda da yok. Renk isimden deterministik türetiliyor
 * (`ui/tema/KategoriRenk.kt → avatarRengi`).
 *
 * `buyukHarf()` şart: "irem".first().uppercase() ROOT'ta "I" verir,
 * Türkçede "İ" olmalı (CLAUDE.md §9).
 */
@Composable
fun KullaniciAvatari(
    isim: String,
    modifier: Modifier = Modifier,
    olcu: Dp = Olcu.AvatarKucuk,
    zemin: Color? = null,
    metinRengi: Color = Beyaz,
    /**
     * Verilirse avatar tıklanabilir olur.
     *
     * Ana ekranın sol üstündeki avatar süslemeydi; Profil'e götürmüyordu
     * (`debug.md` O-17). Lig satırlarındaki avatarlar tıklanabilir
     * olmamaya devam ediyor.
     */
    onTikla: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(olcu)
            .clip(CircleShape)
            .background(zemin ?: avatarRengi(isim))
            .then(if (onTikla != null) Modifier.clickable(onClick = onTikla) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = isim.take(1).buyukHarf(),
            style = if (olcu >= Olcu.AvatarBuyuk) {
                MaterialTheme.typography.displayLarge
            } else {
                MaterialTheme.typography.headlineMedium
            },
            fontWeight = FontWeight.SemiBold,
            color = metinRengi,
        )
    }
}
