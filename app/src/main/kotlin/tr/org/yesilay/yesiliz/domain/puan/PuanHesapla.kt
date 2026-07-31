package tr.org.yesilay.yesiliz.domain.puan

import kotlin.math.min

/**
 * Puan sabitleri — `docs/spec-puan.md §7`.
 *
 * Magic number yasağı (CLAUDE.md §7) gereği hepsi burada. Risk sabitleri
 * `core-risk/Sabitler.kt`'de; puan sistemi risk skorundan **tamamen bağımsız**
 * olduğu için ayrı yerde duruyor.
 */
object PuanSabitleri {
    /** Spor dakikası başına puan. */
    const val SPOR_PUANI_DK = 3.0

    /** Spor puanının günlük tavanı (30 dk × 3). */
    const val SPOR_TAVAN = 90.0

    /** Kaç adım = 1 puan. */
    const val ADIM_BOLEN = 200.0

    /** Adım puanının tavanı. */
    const val ADIM_TAVAN = 40.0

    /** Günlük tüketim hedefi tutturulunca. */
    const val HEDEF_PUANI = 25.0

    /** Gece temizse (00–06 arası 15 dk altı). */
    const val GECE_PUANI = 10.0

    /** Gece "temiz" sayılma eşiği, dakika. */
    const val GECE_ESIK_DK = 15.0

    /**
     * Günlük tavan — teorik max 201, uygulanan 150.
     *
     * Anti-gaming'in ana aracı: tek günde patlama yapıp lig sıralamasını
     * bozmayı engelliyor (`spec-puan.md §9.2`).
     */
    const val GUNLUK_TAVAN = 150.0

    /** Seri çarpanı: `1 + 0.1 × gün`, tavan 2.0. */
    const val SERI_ARTIS = 0.1
    const val SERI_TAVAN = 2.0

    /**
     * Kayıp koruması — haftada bir gün kaçırmak seriyi **sıfırlamaz, dondurur**
     * (`spec-puan.md §7.4`). Ceza üretmemek için: bir gün hasta olan kullanıcı
     * 40 günlük serisini kaybetmemeli.
     */
    const val KORUMA_PENCERESI_GUN = 7
}

/**
 * Günlük puan hesabı — **saf fonksiyon**, yan etki yok.
 *
 * `spec-puan.md §7.5`:
 * ```
 * Π_ham = 3 × spor_dk + min(40, adım/200) + görev puanları
 *       + 25 × hedef_oranı + 10 × gece_oranı
 * Π     = min(150, Π_ham × seri_çarpanı)
 * ```
 *
 * Hedef ve gece terimleri **oransal** (`debug.md` İ-4): hedefin bir dakika
 * üstünde kalmak 25 puanın tamamını götürmüyor artık.
 *
 * **Ceza yolu yok.** Hiçbir girdi kombinasyonu negatif puan üretemez
 * (CLAUDE.md §4): tüm terimler toplanıyor, çıkarma yok, tavanlar `min` ile.
 */
object PuanHesapla {

    /**
     * @param gorevPuani o gün tamamlanan görevlerden gelen ham puan
     * @param sporDk doğrulanmış egzersiz dakikası
     * @param adim o günkü adım
     * @param hedefOrani hedefe ne kadar yaklaşıldı, 0..1 — [hedefeYakinlik]
     * @param geceOrani gecenin ne kadar temiz olduğu, 0..1 — [geceTemizligi]
     * @param seriGun kaçıncı ardışık aktif gün (çarpan bundan)
     */
    fun gunlukPuan(
        gorevPuani: Int,
        sporDk: Double,
        adim: Double,
        hedefOrani: Double,
        geceOrani: Double,
        seriGun: Int,
    ): GunlukPuan {
        val spor = min(PuanSabitleri.SPOR_TAVAN, PuanSabitleri.SPOR_PUANI_DK * sporDk)
        val adimPuani = min(PuanSabitleri.ADIM_TAVAN, adim / PuanSabitleri.ADIM_BOLEN)
        val hedef = PuanSabitleri.HEDEF_PUANI * hedefOrani.coerceIn(0.0, 1.0)
        val gece = PuanSabitleri.GECE_PUANI * geceOrani.coerceIn(0.0, 1.0)

        val ham = gorevPuani + spor + adimPuani + hedef + gece
        val carpan = seriCarpani(seriGun)

        return GunlukPuan(
            ham = ham.toInt(),
            net = min(PuanSabitleri.GUNLUK_TAVAN, ham * carpan).toInt(),
            carpan = carpan,
            seriGun = seriGun,
        )
    }

    /**
     * Hedefe yakınlık, 0..1.
     *
     * Eskiden ikiliydi: hedefin bir dakika üstünde kalmak 25 puanın
     * tamamını götürüyordu. Hedefini 300 dakikadan 130'a indiren bir
     * kullanıcı, hedefi 120 olduğu için hiçbir şey kazanmıyor ve
     * uygulamanın çabayı görmediğini düşünüyordu.
     *
     * Hedefin altında ya da üstünde tam puan; aşıldıkça doğrusal iniyor ve
     * hedefin **iki katında** sıfırlanıyor. Ceza yok — en kötü ihtimalle
     * bu terim 0 (CLAUDE.md §4 "yalnızca kazanç").
     */
    fun hedefeYakinlik(toplamEkranDk: Double, hedefDk: Int): Double {
        if (hedefDk <= 0) return 0.0
        return ((2.0 * hedefDk - toplamEkranDk) / hedefDk).coerceIn(0.0, 1.0)
    }

    /**
     * Gecenin temizliği, 0..1.
     *
     * [hedefeYakinlik] ile aynı biçim: eşiğin altında tam puan, eşiğin iki
     * katında sıfır. Keskin bir eşik, 15 dakikayı 16 dakika yapan gecede
     * puanın tamamını götürüyordu.
     */
    fun geceTemizligi(geceKullanimDk: Double): Double {
        val esik = PuanSabitleri.GECE_ESIK_DK
        return ((2.0 * esik - geceKullanimDk) / esik).coerceIn(0.0, 1.0)
    }

    /** `min(2.0, 1 + 0.1 × seriGun)` — `spec-puan.md §7.4`. */
    fun seriCarpani(seriGun: Int): Double =
        min(PuanSabitleri.SERI_TAVAN, 1.0 + PuanSabitleri.SERI_ARTIS * seriGun)

    /**
     * Yeni seri değerini bulur.
     *
     * @param oncekiSeri en son aktif günün seri değeri
     * @param aradakiBosGun o günle bugün arasındaki boş gün sayısı
     * @return kesintisizse artan, tek gün kaçırıldıysa **donmuş**, daha
     *   fazlaysa 1'den başlayan seri
     */
    fun yeniSeri(oncekiSeri: Int, aradakiBosGun: Int): Int = when {
        oncekiSeri <= 0 -> 1
        aradakiBosGun == 0 -> oncekiSeri + 1
        // Kayıp koruması: bir gün kaçırma seriyi dondurur, sıfırlamaz.
        aradakiBosGun == 1 -> oncekiSeri
        else -> 1
    }
}

/**
 * Bir günün puan sonucu.
 *
 * @param ham tavan ve çarpan uygulanmamış toplam
 * @param net kullanıcıya yazılan — `min(150, ham × çarpan)`
 */
data class GunlukPuan(
    val ham: Int,
    val net: Int,
    val carpan: Double,
    val seriGun: Int,
)
