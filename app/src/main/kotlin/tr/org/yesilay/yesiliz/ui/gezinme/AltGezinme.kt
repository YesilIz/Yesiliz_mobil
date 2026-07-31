package tr.org.yesilay.yesiliz.ui.gezinme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
 *
 * **Yükseklik = tasarım yüksekliği + sistem çubuğu payı.** `enableEdgeToEdge`
 * açık olduğu için çubuk pencerenin en dibine, sistem gezinme çubuğunun
 * altına yerleşiyor. Çıplak `height(72.dp)` verilseydi `NavigationBar`'ın
 * kendi inset payı bu 72dp'nin İÇİNDEN düşülür, sekmelere 3 tuşlu gezinmede
 * 72−48 = 24dp kalırdı: ikon ve etiket ezilir, dokunma hedefi tıklanamayacak
 * kadar küçülürdü. Payı yüksekliğe ekleyince sekmeler yine 72dp alıyor.
 */
@Composable
fun AltGezinme(
    seciliRota: String?,
    onSekme: (Sekme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sistemPayi = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    NavigationBar(
        modifier = modifier.height(Olcu.AltBarYuksekligi + sistemPayi),
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
