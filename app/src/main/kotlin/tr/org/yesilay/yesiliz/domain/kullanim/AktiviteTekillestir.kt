package tr.org.yesilay.yesiliz.domain.kullanim

import java.time.Duration
import java.time.Instant

/**
 * Uygulama yürüyüşü ile Health Connect kaydının aynı aktivite olup
 * olmadığını belirler ve örtüşenleri tek sayar (CLAUDE.md §12 K31).
 *
 * **SAF FONKSİYON** — Android'e bağımlı değil, `core-risk`'teki desenin
 * aynısı: girdi → çıktı, yan etki yok, emülatörsüz test edilebilir.
 *
 * `AktiviteSaglayici` bunu Health Connect'ten gelen egzersiz oturumları
 * ile `aktivite_oturumu` tablosundaki uygulama yürüyüşlerini birleştirmek
 * için çağırıyor; sonuç `gunluk_ozet.sporDk`'ya, oradan da aktivite
 * sönümlemesine (`RiskHesapla` §3, tavan %15) giriyor. Şişirilen her
 * dakika riski haksız yere aşağı çeker — bu yüzden toplama değil,
 * birleştirme.
 */
object AktiviteTekillestir {

    /**
     * Çakışma oranı eşiği — iki kaydın zaman aralığı, KISA olanın süresine
     * göre bu oranın altında örtüşüyorsa ayrı aktivite sayılır.
     *
     * Merkezi ve tek yerde tutuluyor: tolerans kodun farklı yerlerine
     * ayrı ayrı yazılırsa iki taraf sessizce ayrışır (CLAUDE.md §12 K31).
     */
    const val CAKISMA_ORANI_ESIGI = 0.5

    /**
     * Toplam örtüşmesiz süre (dakika). Boş liste `0.0` döner.
     *
     * Kural sırası:
     * 1. Aynı [AktiviteOturumu.disKimlik] → tek kayıt (aynı Health Connect
     *    kaydının tekrar senkronize edilmesi, CLAUDE.md §12 K31 son madde).
     * 2. Zaman aralığı, KISA kaydın süresinin en az [CAKISMA_ORANI_ESIGI]
     *    kadarıyla örtüşüyorsa → aynı aktivite kabul edilir, süreler
     *    TOPLANMAZ (ikisinin birleşimi/union'ı tek kayıt gibi sayılır).
     * 3. Örtüşmeyen ya da eşiğin altında örtüşen kayıtlar → ayrı sayılır.
     *
     * Sıralı tarama + son birleşik aralıkla karşılaştırma kullanıyor
     * (klasik interval-merge); günde ikiden fazla örtüşen kayıt olağan
     * senaryoda beklenmiyor, o yüzden bu yeterli.
     */
    fun birlestirilmisSureDk(oturumlar: List<AktiviteOturumu>): Double {
        if (oturumlar.isEmpty()) return 0.0

        val disKimlikTekil = oturumlar
            .groupBy { it.disKimlik }
            .flatMap { (disKimlik, grup) -> if (disKimlik == null) grup else listOf(grup.first()) }

        val sirali = disKimlikTekil.sortedBy { it.baslangic }
        val birlesikAraliklar = mutableListOf<Pair<Instant, Instant>>()
        for (oturum in sirali) {
            val son = birlesikAraliklar.lastOrNull()
            val bu = oturum.baslangic to oturum.bitis
            if (son != null && ayniAktiviteMi(son, bu)) {
                birlesikAraliklar[birlesikAraliklar.lastIndex] = son.first to maxOf(son.second, bu.second)
            } else {
                birlesikAraliklar.add(bu)
            }
        }
        return birlesikAraliklar.sumOf { (bas, bit) -> Duration.between(bas, bit).toMillis() / 60_000.0 }
    }

    private fun ayniAktiviteMi(a: Pair<Instant, Instant>, b: Pair<Instant, Instant>): Boolean {
        val kesisimBas = maxOf(a.first, b.first)
        val kesisimBit = minOf(a.second, b.second)
        if (!kesisimBit.isAfter(kesisimBas)) return false

        val kesisimSure = Duration.between(kesisimBas, kesisimBit)
        val kisaSure = minOf(Duration.between(a.first, a.second), Duration.between(b.first, b.second))
        if (kisaSure.isZero) return false

        return kesisimSure.toMillis().toDouble() / kisaSure.toMillis() >= CAKISMA_ORANI_ESIGI
    }
}
