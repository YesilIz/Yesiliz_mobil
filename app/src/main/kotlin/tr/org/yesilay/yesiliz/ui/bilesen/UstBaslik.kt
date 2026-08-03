package tr.org.yesilay.yesiliz.ui.bilesen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Yeşil ekran başlığı + üstüne binen içerik yüzeyi.
 *
 * Tasarımın imzası: `YesilAna` dolgulu başlık şeridi, altındaki içerik
 * yuvarlatılmış üst köşelerle (20dp) başlığın üzerine biniyor. İstatistik,
 * Lig, Profil, Ödüller ve Ebeveyn paneli bu iskeleti paylaşıyor.
 *
 * Alt gezinme çubuğu bu bileşenin dışında — `Scaffold` yönetiyor.
 */
@Composable
fun YesilBaslikliEkran(
    baslik: String,
    modifier: Modifier = Modifier,
    onGeri: (() -> Unit)? = null,
    icerikZemini: androidx.compose.ui.graphics.Color = Yesil50,
    /** Başlığın sağındaki metin eylemi — tasarım 35'teki "Tümünü oku". */
    sagEylem: (@Composable () -> Unit)? = null,
    icerik: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YesilAna),
    ) {
        Box(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Olcu.BaslikYuksekligi),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = baslik,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Beyaz,
                textAlign = TextAlign.Center,
                maxLines = 1,
                // Sığmayan başlık üç noktayla kesiliyor. Verilmediğinde
                // "Uygulama kategorileri" ekranda "Uygulama" olarak
                // kırpılıyor ve kırpıldığına dair hiçbir işaret olmuyordu
                // (`debug.md` O-6).
                overflow = TextOverflow.Ellipsis,
                // Pay yalnızca DOLU tarafa veriliyor. İki tarafa birden
                // 96dp koymak, 720px/280dpi bir ekranda başlığa ~65dp
                // bırakıyordu; sağ eylemi olmayan ekranlarda o payın
                // yarısı boşuna harcanıyordu.
                modifier = Modifier.padding(
                    start = if (onGeri != null) BaslikKenarPayi else Olcu.EkranKenari,
                    end = if (sagEylem != null) BaslikKenarPayi else Olcu.EkranKenari,
                ),
            )
            if (onGeri != null) {
                IconButton(
                    onClick = onGeri,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Geri",
                        tint = Beyaz,
                    )
                }
            }
            if (sagEylem != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = Olcu.EkranKenari)
                        // Genişlik SINIRLI. `fillMaxWidth` dayatan bir
                        // içerik (`MetinBaglanti`) verildiğinde eylem
                        // çubuğun tamamını kaplıyor, başlığın üstüne
                        // biniyor ve geri okuna basınca kendi eylemini
                        // çalıştırıyordu (`debug.md` Y-2).
                        .widthIn(max = SagEylemEnFazla),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    sagEylem()
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(
                topStart = Olcu.BaslikBindirmesi,
                topEnd = Olcu.BaslikBindirmesi,
            ),
            color = icerikZemini,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Pay `Surface`'in İÇİNDE: zemin rengi sistem gezinme
                    // çubuğunun arkasına kadar uzanmaya devam ediyor, yalnızca
                    // içerik yukarı çekiliyor. Alt çubuklu sekme ekranlarında
                    // pay `YesilizGezinme`'de tüketildiği için 0'a düşüyor.
                    .navigationBarsPadding()
                    .padding(horizontal = Olcu.EkranKenari),
                content = icerik,
            )
        }
    }
}

/** Geri oku ve sağ eylem için ayrılan yatay pay. */
private val BaslikKenarPayi = 96.dp

/**
 * Sağ eylemin alabileceği en fazla genişlik.
 *
 * [BaslikKenarPayi] ile aynı: eylem başlığa ayrılan alana taşmıyor.
 */
private val SagEylemEnFazla = 96.dp
