package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import tr.org.yesilay.yesiliz.ui.buyukHarf
import tr.org.yesilay.yesiliz.ui.tema.Ayrac
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.Golge
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Beyaz kart — tasarımdaki tüm içerik blokları bunun içinde.
 *
 * 20dp yarıçap, çok yumuşak gölge, ince `Ayrac` kenarlık. Gölge tek başına
 * `Yesil50` zemin üzerinde yeterince ayırmıyor; tasarımda kenarlık da var.
 */
@Composable
fun Kart(
    modifier: Modifier = Modifier,
    icBosluk: Dp = Olcu.KartIci,
    onTikla: (() -> Unit)? = null,
    icerik: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Yaricap.Kart),
        color = Beyaz,
        border = BorderStroke(Olcu.KartKenarligi, Ayrac),
        shadowElevation = Golge.Kart,
    ) {
        Column(
            modifier = Modifier
                .then(if (onTikla != null) Modifier.clickable(onClick = onTikla) else Modifier)
                .padding(icBosluk),
            content = icerik,
        )
    }
}

/**
 * Bölüm başlığı — `HEDEFLERİM`, `VERİLERİM`, `ORTALAMA`.
 * Tasarımda küçük, büyük harf, gri.
 *
 * `buyukHarf()` zorunlu: çıplak `uppercase()` "HEDEFLERIM" üretir (CLAUDE.md §9).
 */
@Composable
fun BolumBasligi(metin: String, modifier: Modifier = Modifier) {
    Text(
        text = metin.buyukHarf(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MetinIkincil,
        modifier = modifier,
    )
}

/**
 * İkon rozeti — liste satırlarının solundaki `YesilNane` yuvarlatılmış kare.
 * Ödül basamağı, görev satırı ve puan geçmişi hep bunu kullanıyor.
 *
 * [fircaZemin] verilirse (kazanılmış ödül rozeti) düz renk yerine gradyan.
 */
@Composable
fun IkonRozeti(
    ikon: ImageVector,
    modifier: Modifier = Modifier,
    zemin: Color = YesilNane,
    renk: Color = YesilAna,
    fircaZemin: Brush? = null,
    olcu: Dp = Olcu.IkonRozeti,
    sekil: androidx.compose.ui.graphics.Shape = RoundedCornerShape(Yaricap.Cip),
) {
    Box(
        modifier = modifier
            .size(olcu)
            .clip(sekil)
            .then(
                if (fircaZemin != null) Modifier.background(fircaZemin)
                else Modifier.background(zemin),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = ikon, contentDescription = null, tint = renk)
    }
}

/** Kart içi satır ayracı. */
@Composable
fun KartAyraci(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = Bosluk.S),
        color = Ayrac,
    )
}

/**
 * Seçilebilir kart — ikon rozeti + başlık + açıklama + sağda seçim
 * göstergesi (radyo veya onay ikonu). Ebeveyn paneli "cihaz rolü" seçimi
 * (ekran 20) ve "Geçmişi indir" PDF seçeneği (ekran 24) bunu kullanıyor.
 *
 * [onayIkonuMu] `true` ise sağda `CheckCircle` (tek seçenek, işaretli/değil),
 * `false` ise `RadioButton` (birden çok seçenekten biri).
 */
@Composable
fun SecenekKarti(
    ikon: ImageVector,
    baslik: String,
    aciklama: String,
    secili: Boolean,
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
    onayIkonuMu: Boolean = false,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .clickable(onClick = onTikla),
        shape = RoundedCornerShape(Yaricap.Kart),
        color = if (secili) Yesil50 else Beyaz,
        border = BorderStroke(
            if (secili) Olcu.ButonKenarligi else Olcu.KartKenarligi,
            if (secili) YesilAna else Ayrac,
        ),
    ) {
        Row(
            modifier = Modifier.padding(Olcu.KartIci),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Bosluk.M),
        ) {
            IkonRozeti(ikon = ikon, zemin = if (secili) Beyaz else YesilNane)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = baslik,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = aciklama,
                    style = MaterialTheme.typography.bodySmall,
                    color = MetinUcuncul,
                )
            }
            if (onayIkonuMu) {
                if (secili) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = YesilAna,
                    )
                }
            } else {
                RadioButton(
                    selected = secili,
                    onClick = onTikla,
                    colors = RadioButtonDefaults.colors(selectedColor = YesilAna),
                )
            }
        }
    }
}

/** Dikey yığın — ekranların ortak boşluk ritmi (16dp). */
@Composable
fun Yigin(
    modifier: Modifier = Modifier,
    aralik: Dp = Olcu.Aralik,
    icerik: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(aralik),
        content = icerik,
    )
}
