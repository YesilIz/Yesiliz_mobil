package tr.org.yesilay.yesiliz.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Ekran süresi widget'ı — `docs/tasarim-referans.md §2 / 40`.
 *
 * Küçük kare: halka + süre + "ekran süresi". Dokununca uygulamayı açıyor.
 *
 * **Halka Glance'ta gerçek bir yay değil.** Glance kendi Composable ağacını
 * kullanıyor ve `Canvas` sunmuyor; dairesel yay çizmek ancak `Bitmap`
 * üretip `Image` olarak koymakla mümkün. Widget her güncellemede bitmap
 * üretmek yerine, tasarımın halkasını **kalın çerçeveli yuvarlak kutu** ile
 * temsil ediyor: doluluk oranı zeminin renk yoğunluğuyla veriliyor.
 * Bilgi aynı, maliyet çok daha düşük.
 */
class EkranSuresiWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val veri = WidgetVerisi.ekranSuresi(context)
        provideContent { GlanceTheme { Icerik(veri) } }
    }

    @Composable
    private fun Icerik(veri: EkranSuresiVerisi) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Beyaz))
                .cornerRadius(KartYaricapi)
                .padding(KartIci)
                .clickable(uygulamayiAc()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(HalkaBoyu)
                    .background(ColorProvider(if (veri.veriVarMi) YesilAna else Yesil50))
                    .cornerRadius(HalkaBoyu / 2),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(HalkaBoyu - HalkaKalinligi * 2)
                        .background(ColorProvider(Beyaz))
                        .cornerRadius((HalkaBoyu - HalkaKalinligi * 2) / 2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (veri.veriVarMi) "%${(veri.oran * 100).toInt()}" else "–",
                        style = TextStyle(
                            color = ColorProvider(YesilAna),
                            fontSize = OranBoyu,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(Aralik))

            Text(
                text = if (veri.veriVarMi) veri.metin() else "Veri yok",
                style = TextStyle(
                    color = ColorProvider(MetinBirincil),
                    fontSize = DegerBoyu,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = "ekran süresi",
                style = TextStyle(
                    color = ColorProvider(MetinIkincil),
                    fontSize = EtiketBoyu,
                ),
            )
        }
    }
}

class EkranSuresiWidgetAlicisi : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EkranSuresiWidget()
}

private val KartYaricapi = 20.dp
private val KartIci = 12.dp
private val HalkaBoyu = 64.dp
private val HalkaKalinligi = 7.dp
private val Aralik = 8.dp
private val DegerBoyu = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)
private val EtiketBoyu = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)
private val OranBoyu = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)
