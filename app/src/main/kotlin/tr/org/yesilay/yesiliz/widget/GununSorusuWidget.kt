package tr.org.yesilay.yesiliz.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.domain.gorev.UykuAraligi
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * "Bugünün sorusu" widget'ı — `docs/tasarim-referans.md §2 / 39`.
 *
 * **Etkileşimli:** seçeneğe dokununca uygulama açılmadan cevap kaydediliyor
 * ve [Gorev.UYKU_BEYANI] puanı veriliyor (3 puan, günlük limit 1).
 *
 * Cevap verildikten sonra widget seçenekleri göstermeye devam ediyor ama
 * seçili olanı işaretliyor — kullanıcı fikrini değiştirebilsin. İkinci
 * kayıt puan vermiyor; `PuanDeposu.gorevTamamla` günlük limiti zaten
 * uyguluyor.
 */
class GununSorusuWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val veri = WidgetVerisi.gununSorusu(context)
        provideContent { GlanceTheme { Icerik(veri) } }
    }

    @Composable
    private fun Icerik(veri: GununSorusuVerisi) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Beyaz))
                .cornerRadius(KartYaricapi)
                .padding(KartIci),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = GlanceModifier
                        .size(LogoBoyu)
                        .background(ColorProvider(YesilAna))
                        .cornerRadius(LogoYaricapi),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Y",
                        style = TextStyle(
                            color = ColorProvider(Beyaz),
                            fontSize = LogoYazi,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Spacer(modifier = GlanceModifier.width(Aralik))
                Text(
                    text = if (veri.cevaplandiMi) "Cevabın kaydedildi" else "Bugünün sorusu",
                    style = TextStyle(
                        color = ColorProvider(MetinBirincil),
                        fontSize = BaslikBoyu,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(modifier = GlanceModifier.height(Aralik))

            Text(
                text = if (veri.cevaplandiMi) {
                    "+${Gorev.UYKU_BEYANI.puan} puan kazandın"
                } else {
                    "Bugün kaç saat uyudun?"
                },
                style = TextStyle(
                    color = ColorProvider(if (veri.cevaplandiMi) MetinIkincil else MetinBirincil),
                    fontSize = SoruBoyu,
                    fontWeight = if (veri.cevaplandiMi) FontWeight.Normal else FontWeight.Bold,
                ),
            )

            Spacer(modifier = GlanceModifier.height(Aralik))

            // 2×2 ızgara. Glance'ta LazyVerticalGrid yok, iki Row yeterli.
            UykuAraligi.entries.chunked(SUTUN).forEach { satir ->
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    satir.forEachIndexed { i, aralik ->
                        Secenek(
                            aralik = aralik,
                            secili = veri.secilen == aralik,
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        if (i < satir.lastIndex) {
                            Spacer(modifier = GlanceModifier.width(SecenekAraligi))
                        }
                    }
                }
                Spacer(modifier = GlanceModifier.height(SecenekAraligi))
            }
        }
    }

    @Composable
    private fun Secenek(
        aralik: UykuAraligi,
        secili: Boolean,
        modifier: GlanceModifier = GlanceModifier,
    ) {
        Box(
            modifier = modifier
                .height(SecenekYuksekligi)
                .background(ColorProvider(if (secili) YesilAna else Yesil50))
                .cornerRadius(SecenekYaricapi)
                .clickable(uykuCevapla(aralik)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = aralik.etiket,
                style = TextStyle(
                    color = ColorProvider(if (secili) Beyaz else MetinIkincil),
                    fontSize = SecenekBoyu,
                    fontWeight = if (secili) FontWeight.Bold else FontWeight.Normal,
                ),
            )
        }
    }
}

class GununSorusuWidgetAlicisi : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GununSorusuWidget()
}

private const val SUTUN = 2

private val KartYaricapi = 20.dp
private val KartIci = 16.dp
private val LogoBoyu = 28.dp
private val LogoYaricapi = 8.dp
private val Aralik = 8.dp
private val SecenekAraligi = 8.dp
private val SecenekYuksekligi = 40.dp
private val SecenekYaricapi = 12.dp

private val BaslikBoyu = TextUnit(14f, TextUnitType.Sp)
private val SoruBoyu = TextUnit(16f, TextUnitType.Sp)
private val SecenekBoyu = TextUnit(14f, TextUnitType.Sp)
private val LogoYazi = TextUnit(14f, TextUnitType.Sp)
