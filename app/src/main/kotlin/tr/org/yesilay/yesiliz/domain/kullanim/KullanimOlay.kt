package tr.org.yesilay.yesiliz.domain.kullanim

import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import java.time.Duration
import java.time.Instant

/**
 * Bir uygulamanın tek ön plan oturumu — domain karşılığı.
 *
 * Room entity'si doğrudan kullanılmaz (CLAUDE.md §7 yasağı); bu model
 * `Kategori` enum'unu taşır, entity ise dize.
 */
data class KullanimOlay(
    val paketAdi: String,
    val kategori: Kategori,
    val baslangic: Instant,
    val bitis: Instant,
) {
    val sure: Duration get() = Duration.between(baslangic, bitis)
    val dakika: Double get() = sure.toMillis() / 60_000.0

    init {
        require(!bitis.isBefore(baslangic)) {
            "Bitiş başlangıçtan önce olamaz: $paketAdi $baslangic → $bitis"
        }
    }
}

/**
 * Bir günün ham olaylarından çıkarılan her şey.
 *
 * [kategoriDakika] ve [oruntu] doğrudan core-risk'in
 * `GunlukGirdi` girdisi olarak kullanılır.
 */
data class GunlukCikarim(
    /** Kategori anahtarı → dakika. `sistem` paketleri zaten düşülmüş. */
    val kategoriDakika: Map<String, Double>,
    /** Örüntü adı → 0..1 normalize değer (spec-risk.md §4.2). */
    val oruntu: Map<String, Double>,
    val toplamDk: Double,
    val geceKullanimDk: Double,
    val acilisSayisi: Int,
    /** Uyku olarak yorumlanan boşluk; bulunamadıysa null. */
    val uykuPenceresi: UykuPenceresi?,
    /**
     * Gece kullanımı gerçekten **ölçülebildi mi**.
     *
     * Olay bazlı yolda (`OruntuCikar`) her oturumun saati bilindiği için
     * her zaman `true`. Agrega yolunda (Android yalnızca paket başına toplam
     * süre veriyor, saat bilgisi yok) `false` — orada `geceKullanimDk = 0`
     * "gece hiç kullanılmadı" değil "gece ölçülemedi" demek. Ayrımı
     * kaybedersek `GECE_TEMIZ` görevi ölçülemeyen her güne puan yazar.
     */
    val geceOlculduMu: Boolean = true,
)

/**
 * Kullanım verisinden *çıkarsanan* uyku aralığı.
 *
 * Gerçek uyku takibi değil — telefanun uzun süre hiç kullanılmadığı en uzun
 * boşluk. `sabah` ve `uyku_oncesi` örüntüleri buna dayanıyor.
 */
data class UykuPenceresi(
    val baslangic: Instant,
    val bitis: Instant,
) {
    val saat: Double get() = Duration.between(baslangic, bitis).toMillis() / 3_600_000.0
}
