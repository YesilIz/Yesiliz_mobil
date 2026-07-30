package tr.org.yesilay.yesiliz.nudge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.DuzYesilButon
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Nudge overlay — `stitch_markdown_design_system/mola_hat_rlat_c/code.html`
 * yerleşim referansı, tonlar/tipografi bu dosyadaki gerçek tokenlardan.
 *
 * Zeminde **karartma yok** — CLAUDE.md §4 "uygulama kilitleme yasak, sadece
 * nudge" ilkesiyle tutarlı; beyaz yarı saydam + blur (blur, servisin
 * pencere katmanında API 31+'ta ekleniyor, bu Composable yalnızca zemin
 * rengini çiziyor).
 *
 * İki alt ekran tek pencerede: "5 dk mola ver" tıklanınca aynı kart nefes
 * egzersizi sorusuna geçiyor — ayrı bir overlay penceresi açılmıyor.
 */
@Composable
fun NudgeOverlay(
    esikDakika: Long,
    onDevamEt: () -> Unit,
    onNefesEvet: () -> Unit,
    onNefesHayir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var asama by remember { mutableStateOf(NudgeAsama.ANA_KART) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Beyaz.copy(alpha = 0.4f))
            .padding(horizontal = Olcu.EkranKenari),
        contentAlignment = Alignment.TopCenter,
    ) {
        Kart(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = UST_BOSLUK),
        ) {
            when (asama) {
                NudgeAsama.ANA_KART -> AnaKart(
                    esikDakika = esikDakika,
                    onMolaVer = { asama = NudgeAsama.MOLA_SORUSU },
                    onDevamEt = onDevamEt,
                )
                NudgeAsama.MOLA_SORUSU -> MolaSorusu(
                    onEvet = onNefesEvet,
                    onHayir = onNefesHayir,
                )
            }
        }
    }
}

@Composable
private fun AnaKart(
    esikDakika: Long,
    onMolaVer: () -> Unit,
    onDevamEt: () -> Unit,
) {
    Column {
        IkonRozeti(
            ikon = Icons.Filled.EnergySavingsLeaf,
            zemin = YesilNane,
            renk = YesilAna,
            olcu = 48.dp,
            sekil = CircleShape,
        )
        Spacer(modifier = Modifier.height(Bosluk.M))
        Text(
            text = "$esikDakika dakikadır buradasın",
            style = MaterialTheme.typography.headlineMedium,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Bosluk.Xs))
        Text(
            text = "Kısa bir mola iyi gelebilir.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        BirincilButon(metin = "5 dk mola ver", onTikla = onMolaVer)
        DuzYesilButon(metin = "Devam et", onTikla = onDevamEt)
    }
}

@Composable
private fun MolaSorusu(
    onEvet: () -> Unit,
    onHayir: () -> Unit,
) {
    Column {
        IkonRozeti(
            ikon = Icons.Filled.EnergySavingsLeaf,
            zemin = YesilNane,
            renk = YesilAna,
            olcu = 48.dp,
            sekil = CircleShape,
        )
        Spacer(modifier = Modifier.height(Bosluk.M))
        Text(
            text = "Nefes egzersizi yapmak ister misin?",
            style = MaterialTheme.typography.headlineMedium,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        BirincilButon(metin = "Evet", onTikla = onEvet)
        MetinBaglanti(metin = "Hayır", onTikla = onHayir)
    }
}

/**
 * Kartın ekranın üst üçte birine yerleşmesi — tasarımda tam bir token yok
 * (`docs/tasarim-referans.md §4`: bu ekran için yalnızca yerleşim referansı
 * var), Stitch mockup'ının `pt-[221px]`'ine yakın bir sabit.
 */
private val UST_BOSLUK = 96.dp
