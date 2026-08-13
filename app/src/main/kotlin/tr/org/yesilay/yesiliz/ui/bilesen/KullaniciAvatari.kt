package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
 * Kullanıcı avatarı bileşeni.
 *
 * Kullanıcı tarafından tercih edilmiş bir [avatarId] varsa ilgili tematik ikonu,
 * yoksa isimden türetilen baş harfi dairesel bir zemin üstünde çizer.
 */
@Composable
fun KullaniciAvatari(
    isim: String,
    modifier: Modifier = Modifier,
    avatarId: String? = null,
    olcu: Dp = Olcu.AvatarKucuk,
    zemin: Color? = null,
    metinRengi: Color = Beyaz,
    onTikla: (() -> Unit)? = null,
) {
    val secenek = avatarBul(avatarId)
    val seciliZemin = zemin ?: secenek?.zeminRenk ?: avatarRengi(isim)

    Box(
        modifier = modifier
            .size(olcu)
            .clip(CircleShape)
            .background(seciliZemin)
            .then(if (onTikla != null) Modifier.clickable(onClick = onTikla) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val ikon = secenek?.ikon
        if (ikon != null) {
            Icon(
                imageVector = ikon,
                contentDescription = secenek.baslik,
                tint = metinRengi,
                modifier = Modifier.size(olcu * 0.55f),
            )
        } else {
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
}
