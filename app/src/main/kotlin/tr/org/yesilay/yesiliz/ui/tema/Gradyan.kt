package tr.org.yesilay.yesiliz.ui.tema

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * Teslim edilen tasarımdaki gradyanlar (`docs/tasarim-referans.md §2`).
 *
 * `Brush.linearGradient` başlangıç/bitiş verilmezse çizim alanının tamamına
 * yayılır — bu yüzden üst düzey `val` olarak tutulabiliyorlar, boyuta bağlı
 * değiller. Diyagonal olanlar [Offset.Infinite] ile sol üst → sağ alt.
 */
object Gradyan {

    /** Ana sayfa / İstatistik / Lig / Profil yeşil başlığı. */
    val Baslik: Brush = Brush.verticalGradient(
        listOf(YesilAna, YesilAna),
    )

    /**
     * Ana sayfadaki ağaç alanı — yeşil başlıktan beyaza sönüyor.
     * Ağaç illüstrasyonu açık zemin için üretildiği için geçiş şart.
     */
    val BaslikSonumu: Brush = Brush.verticalGradient(
        listOf(YesilAna, Beyaz),
    )

    /** Puan kartı — sol üst koyu yeşil, sağ alt açık yeşil. */
    val PuanKarti: Brush = Brush.linearGradient(
        colors = listOf(PuanKartiBas, PuanKartiSon),
        start = Offset.Zero,
        end = Offset.Infinite,
    )

    /** Ödül basamağındaki kazanılmış rozet dairesi. */
    val OdulRozeti: Brush = Brush.linearGradient(
        colors = listOf(PuanKartiBas, PuanKartiSon),
        start = Offset.Zero,
        end = Offset.Infinite,
    )

    /**
     * Ruh hali slider'ı: zorlanıyorum → harika.
     *
     * Turuncu uç **kırmızı değil** ve risk göstermiyor; kullanıcının kendi
     * beyanı. CLAUDE.md §4'teki kırmızı yasağı risk bandı içindir.
     */
    val RuhHali: Brush = Brush.horizontalGradient(
        listOf(RuhHaliBas, RuhHaliOrta, RuhHaliSon),
    )

    /**
     * Risk detayı ekranındaki dairesel gösterge yayı — turuncudan yeşile.
     * Ruh hali gradyanıyla aynı mantık: turuncu risk göstermiyor, yalnızca
     * "dikkat" tonu (CLAUDE.md §4 kırmızı yasağı bunu kapsamaz).
     */
    val RiskHalkasi: Brush = Brush.sweepGradient(listOf(RuhHaliBas, YesilAna))
}
