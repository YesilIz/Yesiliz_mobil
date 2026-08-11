package tr.org.yesilay.yesiliz.risk

/**
 * §5 — skoru banda çevirir. **Dört mekanizma, sıra korunur:**
 * ```
 * 1. eşikten aday bant
 * 2. güvenlik tabanı uygula   (aday YÜKSELEBİLİR)
 * 3. histerezis               (aday kabul edilir mi?)
 * 4. trend                    (bandı değiştirmez, MESAJI değiştirir)
 * ```
 */
object Bantlama {

    /** §5.1 — `0 ── 40 ── 65 ── 100`. */
    fun esiktenBant(skor: Double): Bant = when {
        skor < Sabitler.BANT_AZ_UST -> Bant.AZ
        skor < Sabitler.BANT_ORTA_UST -> Bant.ORTA
        else -> Bant.COK
    }

    /**
     * §5.3 — skor ne derse desin devreye giren mutlak tabanlar.
     *
     * Kronik kullanıcıyı yakalamak için gerekli: bu kişilerde gün gün
     * değişim sinyali yoktur, skor sabit kalır ve eşik onları kaçırır.
     *
     * Son [Sabitler.GUVENLIK_GUN] güne bakar; daha az gün varsa taban yok.
     */
    fun guvenlikTabani(gunluk: List<GunlukOzet>): GuvenlikSonuc {
        if (gunluk.size < Sabitler.GUVENLIK_GUN) return GuvenlikSonuc(null, null)
        val son = gunluk.takeLast(Sabitler.GUVENLIK_GUN)
        val n = Sabitler.GUVENLIK_GUN

        return when {
            son.all { it.tuketimDk > Sabitler.GUVENLIK_TUKETIM_COK } ->
                GuvenlikSonuc(Bant.COK, "$n gün üst üste 8 saat+ tüketim")
            son.all { it.tuketimDk > Sabitler.GUVENLIK_TUKETIM_ORTA } ->
                GuvenlikSonuc(Bant.ORTA, "$n gün üst üste 6 saat+ tüketim")
            son.all { it.geceDk > Sabitler.GUVENLIK_GECE_ORTA } ->
                GuvenlikSonuc(Bant.ORTA, "$n gün üst üste 90 dk+ gece kullanımı")
            else -> GuvenlikSonuc(null, null)
        }
    }

    /**
     * §5.5 — son 7 gün ortalaması vs önceki 7 gün.
     * 14 günden az geçmişte karar verilmez.
     */
    fun trendBul(skorGecmisi: List<Double>): Trend {
        val pencere = Sabitler.PENCERE_GUN
        if (skorGecmisi.size < 2 * pencere) return Trend.BELIRSIZ
        val son = skorGecmisi.takeLast(pencere).average()
        val onceki = skorGecmisi.dropLast(pencere).takeLast(pencere).average()
        val fark = son - onceki
        return when {
            fark > Sabitler.TREND_ESIK -> Trend.KOTULESIYOR
            fark < -Sabitler.TREND_ESIK -> Trend.IYILESIYOR
            else -> Trend.SABIT
        }
    }

    /**
     * Bir günü işler: eşik → güvenlik → histerezis → trend.
     *
     * [takipci] **durum taşır** — çağrılar arasında saklanmalı, her gün
     * yeniden oluşturulmamalı. Bu, motorun tek yan etkili parçası;
     * bilinçli olarak ayrı bir sınıfta tutuldu ki hesap fonksiyonları
     * saf kalsın (CLAUDE.md §7).
     */
    fun gunuIsle(
        skor: Double,
        gunlukVeri: List<GunlukOzet>,
        skorGecmisi: List<Double>,
        takipci: BantTakipci,
    ): GunSonuc {
        var aday = esiktenBant(skor)
        val guvenlik = guvenlikTabani(gunlukVeri)

        // Taban yalnızca YUKARI çeker; aday zaten yüksekse kural uygulanmadı sayılır.
        val kural = if (guvenlik.taban != null && guvenlik.taban.sira > aday.sira) {
            aday = guvenlik.taban
            guvenlik.kural
        } else {
            null
        }

        return GunSonuc(
            skor = skor,
            adayBant = aday,
            gosterilenBant = takipci.guncelle(aday),
            guvenlikKurali = kural,
            bekleyen = takipci.durum(),
            trend = trendBul(skorGecmisi),
        )
    }
}

/**
 * §5.4 histerezis — bant değişimi için ardışık gün şartı.
 *
 * Asimetrik: yukarı [Sabitler.GUN_YUKARI] gün, aşağı [Sabitler.GUN_ASAGI] gün.
 * Uyarmakta hızlı, rahatlamakta yavaş.
 *
 * > **BİLİNEN TUZAK — bozma.** Sayaç yalnızca YÖN değişirse sıfırlanır.
 * > Kullanıcı iyileşmeye devam edip daha da düşük banda aday olursa
 * > (ÇOK'tan inerken aday ORTA iken AZ'a derinleşirse) sayaç
 * > SIFIRLANMAMALI. Aksi hâlde "daha çok iyileştiğin için baştan başla"
 * > cezası doğar. Bu hata bir kez yapıldı.
 * > Regresyon vektörü: `histerezis.TUZAK_ayni_yon_sayac_sifirlanmaz`.
 */
class BantTakipci {
    var mevcutBant: Bant? = null
        private set

    private var bekleyenBant: Bant? = null
    private var bekleyenGun: Int = 0

    fun guncelle(aday: Bant): Bant {
        val mevcut = mevcutBant
        if (mevcut == null) {
            mevcutBant = aday
            return aday
        }

        if (aday == mevcut) {
            bekleyenBant = null
            bekleyenGun = 0
            return mevcut
        }

        val adayYukari = aday.sira > mevcut.sira
        val bekleyen = bekleyenBant
        if (bekleyen == null) {
            bekleyenBant = aday
            bekleyenGun = 1
        } else {
            val bekleyenYukari = bekleyen.sira > mevcut.sira
            if (adayYukari == bekleyenYukari) {
                // Aynı yön: sayaç DEVAM eder — ORTA→AZ derinleşmesi cezalanmaz.
                bekleyenBant = aday
                bekleyenGun += 1
            } else {
                // Yön değişti: baştan.
                bekleyenBant = aday
                bekleyenGun = 1
            }
        }

        val gereken = if (adayYukari) Sabitler.GUN_YUKARI else Sabitler.GUN_ASAGI
        if (bekleyenGun >= gereken) {
            mevcutBant = aday
            bekleyenBant = null
            bekleyenGun = 0
        }
        return mevcutBant!!
    }

    /** Bekleyen aday varsa "ÇOK RİSKLİ adayı: 2/3 gün" biçiminde döner. */
    fun durum(): String? {
        val bekleyen = bekleyenBant ?: return null
        val mevcut = mevcutBant ?: return null
        val yukari = bekleyen.sira > mevcut.sira
        val gereken = if (yukari) Sabitler.GUN_YUKARI else Sabitler.GUN_ASAGI
        return "${bekleyen.etiket} adayı: $bekleyenGun/$gereken gün"
    }
}
