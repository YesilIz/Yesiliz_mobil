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
 *       + 25 (hedef tutturulduysa) + 10 (gece temizse)
 * Π     = min(150, Π_ham × seri_çarpanı)
 * ```
 *
 * **Ceza yolu yok.** Hiçbir girdi kombinasyonu negatif puan üretemez
 * (CLAUDE.md §4): tüm terimler toplanıyor, çıkarma yok, tavanlar `min` ile.
 */
object PuanHesapla {

    /**
     * @param gorevPuani o gün tamamlanan görevlerden gelen ham puan
     * @param sporDk doğrulanmış egzersiz dakikası
     * @param adim o günkü adım
     * @param hedefTutturuldu ekran süresi günlük hedefin altında mı
     * @param geceTemiz 00–06 arası kullanım [PuanSabitleri.GECE_ESIK_DK] altında mı
     * @param seriGun kaçıncı ardışık aktif gün (çarpan bundan)
     */
    fun gunlukPuan(
        gorevPuani: Int,
        sporDk: Double,
        adim: Double,
        hedefTutturuldu: Boolean,
        geceTemiz: Boolean,
        seriGun: Int,
    ): GunlukPuan {
        val spor = min(PuanSabitleri.SPOR_TAVAN, PuanSabitleri.SPOR_PUANI_DK * sporDk)
        val adimPuani = min(PuanSabitleri.ADIM_TAVAN, adim / PuanSabitleri.ADIM_BOLEN)
        val hedef = if (hedefTutturuldu) PuanSabitleri.HEDEF_PUANI else 0.0
        val gece = if (geceTemiz) PuanSabitleri.GECE_PUANI else 0.0

        val ham = gorevPuani + spor + adimPuani + hedef + gece
        val carpan = seriCarpani(seriGun)

        return GunlukPuan(
            ham = ham.toInt(),
            net = min(PuanSabitleri.GUNLUK_TAVAN, ham * carpan).toInt(),
            carpan = carpan,
            seriGun = seriGun,
        )
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
