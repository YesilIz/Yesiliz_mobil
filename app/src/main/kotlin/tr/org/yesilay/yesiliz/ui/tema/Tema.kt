package tr.org.yesilay.yesiliz.ui.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Yeşilİz teması.
 *
 * **Sadece açık tema var.** DESIGN.md §2 "geniş beyaz alan esas" diyor ve
 * ağaç illüstrasyonları açık zemin için üretildi (`#edf7ef` → beyaz gradient).
 * Koyu tema ayrı bir illüstrasyon seti gerektirir — MVP kapsamı dışı.
 *
 * **Dinamik renk (Material You) KAPALI.** Kullanıcının duvar kâğıdından renk
 * türetmek Yeşilay marka kılavuzunu ihlal eder; kırmızı duvar kâğıdı kırmızı
 * bir arayüz üretirdi (CLAUDE.md §4).
 */
private val YesilizRenkSemasi = lightColorScheme(
    primary = YesilAna,
    onPrimary = Beyaz,
    primaryContainer = Yesil200,
    onPrimaryContainer = YesilKoyu,

    secondary = Yesil500,
    onSecondary = Beyaz,
    secondaryContainer = Yesil50,
    onSecondaryContainer = YesilKoyu,

    tertiary = YesilKoyu,
    onTertiary = Beyaz,

    // ekran zemini #edf7ef, kartlar beyazla öne çıkar
    background = Yesil50,
    onBackground = MetinBirincil,
    surface = Beyaz,
    onSurface = MetinBirincil,
    surfaceVariant = Yesil50,
    onSurfaceVariant = MetinIkincil,

    outline = Ayrac,
    outlineVariant = Ayrac,

    // KIRMIZI YOK — hata da koyu yeşil
    error = Hata,
    onError = Beyaz,
    errorContainer = HataZemin,
    onErrorContainer = YesilKoyu,
)

/**
 * Material 3'te karşılığı olmayan Yeşilİz'e özel renkler.
 *
 * Risk bandı renkleri `ColorScheme`'e sığmıyor (M3'ün böyle bir slotu yok),
 * ama ekran içinde hardcoded yazmak da §14 yasağı. Bu yüzden ayrı bir
 * CompositionLocal ile taşınıyorlar: `YesilizTema.risk.cok` gibi.
 */
data class RiskRenkleri(
    val az: androidx.compose.ui.graphics.Color = RiskAz,
    val orta: androidx.compose.ui.graphics.Color = RiskOrta,
    val cok: androidx.compose.ui.graphics.Color = RiskCok,
)

private val YerelRiskRenkleri = staticCompositionLocalOf { RiskRenkleri() }

object YesilizTema {
    val risk: RiskRenkleri
        @Composable @ReadOnlyComposable get() = YerelRiskRenkleri.current
}

@Composable
fun YesilizTemasi(content: @Composable () -> Unit) {
    CompositionLocalProvider(YerelRiskRenkleri provides RiskRenkleri()) {
        MaterialTheme(
            colorScheme = YesilizRenkSemasi,
            typography = YesilizTipografi,
            shapes = YesilizSekiller,
            content = content,
        )
    }
}
