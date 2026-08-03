package tr.org.yesilay.yesiliz.ui.ekran.aktivite

import androidx.compose.ui.graphics.vector.ImageVector
import tr.org.yesilay.yesiliz.domain.gorev.Gorev

/**
 * Aktiviteler sekmesi — `debug.md` İ-1.
 *
 * Dördüncü sekme artık Profil değil Aktiviteler; Profil Ana Sayfa
 * başlığındaki avatardan açılıyor (`Sekme.AKTIVITE` kdoc'u).
 *
 * Altı aktivitenin hepsi §4 mutlak kurallarına uygun: kilitleme yok,
 * ceza yok, içerik okuma yok, serbest metin yok.
 */
data class AktiviteDurum(
    val kartlar: List<AktiviteKarti> = emptyList(),
    /** Bugün kazanılan aktivite puanı — tekrar açmaya değer mi bilgisi. */
    val bugunKazanilan: Int = 0,
    val yukleniyor: Boolean = true,
)

data class AktiviteKarti(
    val gorev: Gorev,
    val ikon: ImageVector,
    val rota: String,
    /** Bugün kaç kez yapıldı / günlük limit. */
    val bugunYapilan: Int,
) {
    val limitDoldu: Boolean get() = bugunYapilan >= gorev.gunlukLimit
}
