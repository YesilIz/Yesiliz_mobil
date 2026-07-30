package tr.org.yesilay.yesiliz.ui.tema

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tr.org.yesilay.yesiliz.R

/**
 * DESIGN.md §1 tipografi skalası.
 *
 * Font kararı **Inter** (CLAUDE.md §12 K4). Kurumsal font Averta ticari lisans
 * gerektiriyor; lisans gelirse SADECE bu dosya değişir — `Inter` yerine
 * `Averta` font ailesi tanımlanır, ölçek aynı kalır.
 *
 * Inter SIL Open Font License 1.1 ile dağıtılıyor, uygulama içinde
 * paketlenmesi serbest.
 */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/**
 * DESIGN.md skalası → Material 3 karşılıkları.
 *
 * | DESIGN.md | Boyut | Ağırlık   | Kullanım                        | M3 slotu      |
 * |-----------|-------|-----------|---------------------------------|---------------|
 * | Display   | 32sp  | Bold      | ana ekran skoru, büyük sayılar  | displayLarge  |
 * | H1        | 24sp  | Semibold  | ekran başlığı                   | headlineLarge |
 * | H2        | 20sp  | Semibold  | bölüm başlığı                   | headlineMedium|
 * | Body      | 16sp  | Regular   | gövde metni                     | bodyLarge     |
 * | Caption   | 14sp  | Regular   | yardımcı metin                  | bodyMedium    |
 * | Label     | 12sp  | Medium    | etiket, chip                    | labelMedium   |
 *
 * Satır yükseklikleri Stitch tasarım sisteminden alındı (1.25–1.5 arası),
 * "generous line height" ilkesine uygun.
 */
val YesilizTipografi = Typography(
    displayLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        // DESIGN.md: -0.02em · 32sp × -0.02 = -0.64sp
        letterSpacing = (-0.64).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        // DESIGN.md: 0.01em · 12sp × 0.01 = 0.12sp
        letterSpacing = 0.12.sp,
    ),
)
