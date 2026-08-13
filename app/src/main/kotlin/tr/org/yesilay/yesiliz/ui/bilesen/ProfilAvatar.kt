package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu

/**
 * Kullanıcı profil avatarı seçenekleri.
 *
 * Kullanıcı varsayılan "Harf" avatarı veya sunulan tematik ikon avatarlarından
 * birini tercih edebilir.
 */
data class ProfilAvatarSecenegi(
    val id: String,
    val baslik: String,
    val ikon: ImageVector? = null,
    val zeminRenk: Color = YesilAna,
    val ikonRenk: Color = Beyaz,
)

val TUM_AVATARLAR = listOf(
    ProfilAvatarSecenegi(
        id = "HARF",
        baslik = "İsim Harfi",
        zeminRenk = YesilKoyu,
    ),
    ProfilAvatarSecenegi(
        id = "FIDAN",
        baslik = "Fidan",
        ikon = Icons.Filled.Eco,
        zeminRenk = YesilAna,
    ),
    ProfilAvatarSecenegi(
        id = "AGAC",
        baslik = "Ağaç",
        ikon = Icons.Filled.Park,
        zeminRenk = YesilKoyu,
    ),
    ProfilAvatarSecenegi(
        id = "ATES",
        baslik = "Ateş",
        ikon = Icons.Filled.LocalFireDepartment,
        zeminRenk = Color(0xFFE65100),
    ),
    ProfilAvatarSecenegi(
        id = "KALP",
        baslik = "Sağlık",
        ikon = Icons.Filled.MonitorHeart,
        zeminRenk = Color(0xFFC2185B),
    ),
    ProfilAvatarSecenegi(
        id = "SAMPION",
        baslik = "Şampiyon",
        ikon = Icons.Filled.EmojiEvents,
        zeminRenk = Color(0xFFF57F17),
    ),
    ProfilAvatarSecenegi(
        id = "MEDITASYON",
        baslik = "Mola",
        ikon = Icons.Filled.SelfImprovement,
        zeminRenk = Color(0xFF00796B),
    ),
    ProfilAvatarSecenegi(
        id = "GECELIK",
        baslik = "Gece",
        ikon = Icons.Filled.NightsStay,
        zeminRenk = Color(0xFF303F9F),
    ),
    ProfilAvatarSecenegi(
        id = "KOSUCU",
        baslik = "Yürüyüş",
        ikon = Icons.AutoMirrored.Filled.DirectionsWalk,
        zeminRenk = Color(0xFF2E7D32),
    ),
)

fun avatarBul(avatarId: String?): ProfilAvatarSecenegi? =
    TUM_AVATARLAR.firstOrNull { it.id.equals(avatarId, ignoreCase = true) }
