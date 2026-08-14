package tr.org.yesilay.yesiliz.nudge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import tr.org.yesilay.yesiliz.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
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
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
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
    esikSaniye: Long,
    onDevamEt: () -> Unit,
    onNefesEvet: () -> Unit,
    onNefesHayir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var asama by remember { mutableStateOf(NudgeAsama.ANA_KART) }
    var gorunur by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        gorunur = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = Olcu.EkranKenari),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = gorunur,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialScale = 0.3f
            ) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Yaricap.Kart),
                color = Color.Transparent,
                border = BorderStroke(1.dp, Beyaz.copy(alpha = 0.6f)),
                shadowElevation = 24.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Beyaz.copy(alpha = 0.95f),
                                    Beyaz.copy(alpha = 0.85f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hilal),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .offset(y = (-50).dp)
                            .blur(radius = 12.dp)
                            .alpha(0.9f),
                        contentScale = ContentScale.Fit
                    )
                    Column(
                        modifier = Modifier.padding(Olcu.KartIci),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (asama) {
                            NudgeAsama.ANA_KART -> AnaKart(
                                esikSaniye = esikSaniye,
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
        }
    }
}

@Composable
private fun AnaKart(
    esikSaniye: Long,
    onMolaVer: () -> Unit,
    onDevamEt: () -> Unit,
) {
    val sureBilgisi = if (esikSaniye >= 60) {
        "${esikSaniye / 60} dakikadır buradasın"
    } else {
        "$esikSaniye saniyedir buradasın"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.sende_yesilaycisin),
            contentDescription = "Sen de Yeşilaycısın",
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(Bosluk.M))
        Text(
            text = sureBilgisi,
            style = MaterialTheme.typography.headlineMedium,
            color = MetinBirincil,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Bosluk.Xs))
        Text(
            text = "Kısa bir mola iyi gelebilir.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MetinIkincil,
            textAlign = TextAlign.Center
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.sende_yesilaycisin),
            contentDescription = "Sen de Yeşilaycısın",
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(Bosluk.M))
        Text(
            text = "Nefes egzersizi yapmak ister misin?",
            style = MaterialTheme.typography.headlineMedium,
            color = MetinBirincil,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        BirincilButon(metin = "Evet", onTikla = onEvet)
        MetinBaglanti(metin = "Hayır", onTikla = onHayir)
    }
}

