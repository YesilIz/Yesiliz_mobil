package tr.org.yesilay.yesiliz.ui.ekran.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tr.org.yesilay.yesiliz.ui.bilesen.AdimGostergesi
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.SatirBaglanti
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
    /**
     * İçeriği iskelet mi kaydırsın.
     *
     * Kendi kaydırmasını yöneten ekranlar (`AnketEkran` gibi) `false`
     * vermek ZORUNDA: iç içe iki dikey kaydırma, içtekinin sonsuz
     * yükseklik kısıtıyla ölçülmesi demek ve Compose bunu
     * `IllegalStateException` ile reddediyor -- ekran açılmıyor, uygulama
     * çöküyor.
     */
    kaydirilabilir: Boolean = true,
    icerik: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Beyaz)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Olcu.EkranKenari),
        // `SpaceBetween` + `fill = false`: butonlar dipte kalırken içerik
        // yalnızca ihtiyacı kadar yer alıyor.
        //
        // `weight(1f)` tek başına (fill = true) içeriğe kalan alanın
        // TAMAMINI veriyor ve alt eylem bloğuna yer kalmıyordu: birincil
        // buton sığıyor ama ikincil bağlantı 70px yerine 9px'e iniyordu.
        // Ekranda görünüyor, dokunulamıyordu -- erişilebilirlik ağacında
        // `[0,0][0,0]`. "Nasıl çalışır?" ve dört ekrandaki "Şimdi değil"
        // bu yüzden tıklanamıyordu.
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Spacer(modifier = Modifier.height(Olcu.Aralik))
            if (adim > 0) {
                AdimGostergesi(adim = adim, toplam = ONBOARDING_ADIM)
            }
        }

        // İçerik KAYDIRILABİLİR. Sabit `weight(1f)` ile sığmayan içerik
        // sessizce kırpılıyordu: ilk sonuç ekranında sürükleyiciler
        // kartının üçüncü satırı ("Fiziksel hareket") görünmüyor, alttaki
        // metin bağlantısı erişilebilirlik ağacında `[0,0][0,0]` olarak
        // çıkıyordu. Küçük ekran ya da büyük yazı tipi ölçeğinde diğer
        // adımlar da etkileniyordu (`debug.md` O-1).
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .then(
                    if (kaydirilabilir) {
                        Modifier.verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = Olcu.BolumArasi),
            content = icerik,
        )

        // Alt eylem bloğu AYRI bir Column'da ve `wrapContentHeight` ile:
        // düz sıralamada Compose ikincil bağlantıyı kalan alana sıkıştırıp
        // 9 piksele indiriyordu. Ekranda görünüyor ama dokunma hedefi yok;
        // erişilebilirlik ağacında `[0,0][0,0]` çıkıyordu. "Nasıl çalışır?"
        // ve dört ekrandaki "Şimdi değil" bu yüzden tıklanamıyordu.
        // `wrapContentHeight(unbounded = true)` DENENDİ VE GERİ ALINDI:
        // sorunu çözmediği gibi alt ağaçtaki yükseklik kısıtını sonsuza
        // çeviriyor ve içinde `verticalScroll` olan her onboarding ekranı
        // "Vertically scrollable component was measured with an infinity
        // maximum height constraints" ile ÇÖKÜYOR. Anket ekranı bu yüzden
        // açılmıyordu.
        Column {
            BirincilButon(metin = ilerleMetni, onTikla = onIlerle, etkin = ilerleEtkin)

            // Çerçeveli ikinci eylem birincil butonun ALTINDA — tasarımda
            // hedef önerisi ekranı böyle (`docs/tasarim-referans.md §2 / 06`).
            if (ekButon != null) {
                Spacer(modifier = Modifier.height(Bosluk.S))
                ekButon()
            }

            if (ikincilMetin != null && onIkincil != null) {
                // `MetinBaglanti` DEĞİL: o bir `TextButton` ve bu yerleşimde
                // kalan alana göre ölçülüp 70px yerine 9px'e iniyordu --
                // ekranda görünüyor ama dokunulamıyordu. Düz `Text` +
                // `clickable` sabit dolgusuyla her zaman dokunulabilir bir
                // hedef veriyor.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Olcu.ButonYuksekligi)
                        .clickable(onClick = onIkincil),
                    contentAlignment = Alignment.Center,
                ) {
                    SatirBaglanti(metin = ikincilMetin, onTikla = onIkincil)
                }
            } else {
                Spacer(modifier = Modifier.height(Bosluk.M))
            }
        }
    }
}
