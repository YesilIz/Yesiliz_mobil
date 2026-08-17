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
     * Ana sayfadaki ağaç alanı — yeşil başlıktan açık yeşile sönüyor.
     * Ağaç illüstrasyonu açık zemin için üretildiği için geçiş şart.
     *
     * Marka yeşili ilk %12'de **korunuyor**: başlığın metinleri beyaz ve
     * başlık zemini ancak kaydırınca opaklaşıyor (`AnaEkran`), yani üst şerit
     * koyu kalmazsa beyaz metin okunmaz oluyor. Onun altında hızla açılıyor —
     * arkadaki manzara ([tr.org.yesilay.yesiliz.ui.bilesen.OrmanZemini])
     * doygun yeşilin üstünde hiç okunmuyordu.
     */
    val BaslikSonumu: Brush = Brush.verticalGradient(
        0.00f to YesilAna,
        0.12f to YesilAna,
        0.30f to YesilNane,
        1.00f to Yesil50,
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

    /** Nefes egzersizi — dolu iç daire, sol üstte hafif parlaklık. */
    val NefesDairesi: Brush = Brush.radialGradient(listOf(PuanKartiSon, YesilAna))
}
