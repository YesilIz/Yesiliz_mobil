package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tr.org.yesilay.yesiliz.ui.bilesen.AdimGostergesi
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.Olcu

/** Onboarding toplam adım sayısı — tasarımda 4 çubuk. */
const val ONBOARDING_ADIM = 4

/**
 * Onboarding ekranlarının ortak iskeleti.
 *
 * Yapı her ekranda aynı (`docs/tasarim-referans.md §2 / 01–07`):
 * adım göstergesi · esneyen içerik · birincil buton · metin bağlantı.
 * İçerik dikey olarak ortalanıyor, butonlar hep dipte — 4 ekran arasında
 * buton zıplaması olmuyor.
 *
 * [adim] 0 verilirse gösterge çizilmez (ilk sonuç ve hedef ekranları).
 */
@Composable
fun OnboardingIskelet(
    adim: Int,
    ilerleMetni: String,
    onIlerle: () -> Unit,
    modifier: Modifier = Modifier,
    ilerleEtkin: Boolean = true,
    ikincilMetin: String? = null,
    onIkincil: (() -> Unit)? = null,
    ekButon: (@Composable ColumnScope.() -> Unit)? = null,
    icerik: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Beyaz)
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.systemBars)
            .padding(horizontal = Olcu.EkranKenari),
    ) {
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        if (adim > 0) {
            AdimGostergesi(adim = adim, toplam = ONBOARDING_ADIM)
        }

        // İçerik KAYDIRILABİLİR. Sabit `weight(1f)` ile sığmayan içerik
        // sessizce kırpılıyordu: ilk sonuç ekranında sürükleyiciler
        // kartının üçüncü satırı ("Fiziksel hareket") görünmüyor, alttaki
        // metin bağlantısı erişilebilirlik ağacında `[0,0][0,0]` olarak
        // çıkıyordu. Küçük ekran ya da büyük yazı tipi ölçeğinde diğer
        // adımlar da etkileniyordu (`debug.md` O-1).
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.BolumArasi),
            content = icerik,
        )

        BirincilButon(metin = ilerleMetni, onTikla = onIlerle, etkin = ilerleEtkin)

        // Çerçeveli ikinci eylem birincil butonun ALTINDA — tasarımda hedef
        // önerisi ekranı böyle (`docs/tasarim-referans.md §2 / 06`).
        if (ekButon != null) {
            Spacer(modifier = Modifier.height(Bosluk.S))
            ekButon()
        }

        if (ikincilMetin != null && onIkincil != null) {
            MetinBaglanti(metin = ikincilMetin, onTikla = onIkincil)
        } else {
            Spacer(modifier = Modifier.height(Bosluk.M))
        }
    }
}
