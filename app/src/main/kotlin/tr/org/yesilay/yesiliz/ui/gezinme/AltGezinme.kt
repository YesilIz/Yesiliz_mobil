package tr.org.yesilay.yesiliz.ui.gezinme

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Alt gezinme çubuğu — 4 sekme.
 *
 * Tasarımda seçili sekmenin ikonu ve etiketi `YesilAna`, gösterge kapsülü
 * (M3'ün varsayılan `indicator` pilulu) YOK. Bu yüzden `indicatorColor`
 * saydam veriliyor.
 */
@Composable
fun AltGezinme(
    seciliRota: String?,
    onSekme: (Sekme) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.height(Olcu.AltBarYuksekligi),
        containerColor = Beyaz,
        tonalElevation = Olcu.Sifir,
    ) {
        Sekme.entries.forEach { sekme ->
            val secili = seciliRota == sekme.rota
            NavigationBarItem(
                selected = secili,
                onClick = { onSekme(sekme) },
                icon = {
                    Icon(
                        imageVector = if (secili) sekme.ikonDolu else sekme.ikonCizgi,
                        contentDescription = sekme.etiket,
                    )
                },
                label = {
                    Text(text = sekme.etiket, style = MaterialTheme.typography.labelMedium)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = YesilAna,
                    selectedTextColor = YesilAna,
                    unselectedIconColor = MetinIkincil,
                    unselectedTextColor = MetinIkincil,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
