package tr.org.yesilay.yesiliz.ui.ekran.ekransiz

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * "Ekransız ne yaptın?" — kart seçimi.
 *
 * **Serbest metin YOK** (CLAUDE.md §4: serbest metin chatbot ilk sürüm
 * kapsamı dışı, `docs/spec-veri.md`). Kullanıcı hazır kartlardan seçiyor;
 * `ruh_hali` ile aynı desen: tek dokunuş, kaydediliyor, yorumlanmıyor.
 *
 * Seçim **saklanmıyor**. Amaç veri toplamak değil, kullanıcının ekran
 * dışında geçirdiği zamanı kendisine görünür kılmak; yalnızca görev
 * tamamlandı olarak işaretleniyor. Toplamayacağımız bir veriyi sormak
 * için kart göstermek yeterli.
 */
enum class EkransizSecenek(val etiket: String, val ikon: ImageVector) {
    YURUYUS("Yürüdüm", Icons.AutoMirrored.Filled.DirectionsWalk),
    SPOR("Spor yaptım", Icons.Filled.SportsSoccer),
    KITAP("Kitap okudum", Icons.AutoMirrored.Filled.MenuBook),
    SOHBET("Sohbet ettim", Icons.Filled.Groups),
    YEMEK("Yemek yaptım", Icons.Filled.Restaurant),
    URETTIM("Bir şey ürettim", Icons.Filled.Brush),
    DOGA("Dışarıda vakit geçirdim", Icons.Filled.Yard),
    DINLENDIM("Sadece dinlendim", Icons.Filled.SelfImprovement),
}

data class EkransizDurum(
    val secilen: EkransizSecenek? = null,
    val kaydedildi: Boolean = false,
    val puanYazildi: Boolean = false,
)
