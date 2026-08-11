package tr.org.yesilay.yesiliz.risk

import kotlin.math.max

/**
 * §4.5 — ankete verilen ağırlık.
 *
 * Fikir: uygulama yeniyken elimizde davranış verisi yok, ankete güveniyoruz.
 * Veri biriktikçe anketin ağırlığı düşüyor, davranış devralıyor.
 * Anket eskidikçe de ayrıca sönüyor — 60 günlük anket hiçbir şey söylemez.
 */
object Alpha {

    /**
     * @param d davranış verisi gün sayısı (bkz. [dEfektif])
     * @param anketVar anket girilmiş mi; girilmemişse alpha daima 0
     * @param anketKacGunOnce `g` — anketten bu yana geçen gün
     */
    fun hesapla(d: Int, anketVar: Boolean, anketKacGunOnce: Int = 0): Double {
        if (!anketVar) return 0.0

        val alpha0 = when {
            d <= 0 -> 1.00
            d < Sabitler.ALPHA_ILK_ESIK ->
                1.00 - 0.5 * (d.toDouble() / Sabitler.ALPHA_ILK_ESIK)
            d < Sabitler.ALPHA_IKINCI_ESIK ->
                0.50 - 0.2 * ((d - Sabitler.ALPHA_ILK_ESIK).toDouble() /
                    (Sabitler.ALPHA_IKINCI_ESIK - Sabitler.ALPHA_ILK_ESIK))
            else -> Sabitler.ALPHA_TABAN
        }

        // Anket tazeliği: 60 günde doğrusal olarak sıfıra iner.
        val tazelik = max(0.0, 1.0 - anketKacGunOnce / Sabitler.ANKET_OMRU_GUN)
        return alpha0 * tazelik
    }

    /**
     * §4.5 — efektif gün sayısı.
     *
     * `queryUsageStats()` geriye dönük **hacim** verir ama `queryEvents()`
     * kısa saklandığı için **örüntü** vermez. Geçmiş yarım veridir, tam veri
     * gibi sayılmaz. iOS'te geçmiş sıfır → `d = 0` (§10).
     *
     * > **Referans Python'da karşılığı YOK.** `yesiliz_hesap.py` `kacinci_gun`
     * > parametresini doğrudan alıyor. Bu fonksiyonun kaynağı spec-risk.md
     * > §4.5; doğrulaması `SenaryoTest` içinde elle hesaplanmış beklentilerle,
     * > `test_vectors.json` üzerinden değil.
     */
    fun dEfektif(pencere: GecmisPencere): Int {
        val toplam = pencere.canliGun * Sabitler.CANLI_GUN_AGIRLIK +
            pencere.gecmisGun * Sabitler.GECMIS_GUN_AGIRLIK
        return toplam.toInt()
    }
}
