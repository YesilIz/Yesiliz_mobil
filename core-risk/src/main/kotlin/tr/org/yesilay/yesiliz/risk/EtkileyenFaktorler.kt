package tr.org.yesilay.yesiliz.risk

/**
 * Risk detayı ekranının "Etkileyen faktörler" bölümü (§12 sıradaki iş #2,
 * `docs/tasarim-referans.md §2` ekran 30). **SAF FONKSİYON.**
 *
 * `surukleyiciler` (en çok katkı yapan 3 KATEGORİ) ile karıştırılmaz —
 * bu 4 sabit, tasarımda adı geçen davranış boyutunu ölçer:
 * Gece kullanımı, Kesintisiz kullanım süresi, Sosyal medya oranı,
 * Fiziksel hareket. Eşikler [Sabitler.FAKTOR_ESIK_DUSUK] /
 * [Sabitler.FAKTOR_ESIK_ORTA] — GEÇİCİ, bkz. CLAUDE.md §12 A3.
 */
enum class FaktorSeviyesi(val etiket: String) {
    DUSUK("Düşük risk"),
    ORTA("Orta"),
    YUKSEK("Yüksek"),

    /**
     * Ölçüm yok — iddia da yok.
     *
     * "Fiziksel hareket" faktörü adım verisi hiç gelmediğinde `A = 0`
     * üretiyor, bu da `1 - A = 1,0`, yani **maksimum risk** demek oluyordu.
     * Uygulamayı yeni kuran her kullanıcı üstte "Düşük risk düzeyi / son 7
     * günlük kullanım örüntün sağlıklı görünüyor" yazarken hemen altında
     * "Fiziksel hareket → Yüksek" ve uçtan uca kırmızı bir çubuk görüyordu
     * (`debug.md` K-7).
     *
     * Veri yokluğunu maksimum risk olarak sunmak, uygulamanın başka her
     * yerinde uygulanan "veri yoksa boş durum göster, uydurma" ilkesinin
     * (`ui/bilesen/BosDurum.kt`) tersi. Sorun rengin değil, iddianın
     * yanlış olmasıydı.
     */
    BILINMIYOR("Veri yok"),
}

/**
 * @param ad görünen ad — "Gece kullanımı" gibi
 * @param seviye [FaktorSeviyesi]
 * @param oran 0-1 arası, çubuğun dolum oranı (risk katkısı — hareket için
 *   TERS çevrilmiş: çok hareket = düşük oran = kısa çubuk = düşük risk)
 */
data class EtkileyenFaktor(
    val ad: String,
    val seviye: FaktorSeviyesi,
    val oran: Double,
)

object EtkileyenFaktorler {

    fun hesapla(gun: GunlukGirdi): List<EtkileyenFaktor> {
        val gece = gun.oruntu["gece"] ?: 0.0
        val oturum = gun.oruntu["oturum"] ?: 0.0
        val sosyalOran = if (gun.toplamDk > 0.0) {
            (gun.kategoriDakika["social"] ?: 0.0) / gun.toplamDk
        } else {
            0.0
        }
        // Aktivite verisi hiç yoksa hareket faktörü ÖLÇÜLMEMİŞ sayılıyor.
        // Hem adım hem spor sıfırsa `A = 0` çıkıyor ve `1 - A` maksimum
        // riskle aynı değeri veriyor; ikisi ayırt edilemiyordu.
        val aktiviteOlculdu = gun.adim > 0.0 || gun.sporDk > 0.0
        val hareketOran = 1.0 - RiskHesapla.araUrunler(gun).A

        return listOf(
            EtkileyenFaktor("Gece kullanımı", seviyeye(gece), gece),
            EtkileyenFaktor("Kesintisiz kullanım süresi", seviyeye(oturum), oturum),
            EtkileyenFaktor("Sosyal medya oranı", seviyeye(sosyalOran), sosyalOran),
            EtkileyenFaktor(
                ad = "Fiziksel hareket",
                seviye = if (aktiviteOlculdu) seviyeye(hareketOran) else FaktorSeviyesi.BILINMIYOR,
                // Ölçüm yokken çubuk boş: dolu bir çubuk, olmayan bir
                // ölçümü varmış gibi gösterirdi.
                oran = if (aktiviteOlculdu) hareketOran else 0.0,
            ),
        )
    }

    private fun seviyeye(oran: Double): FaktorSeviyesi = when {
        oran < Sabitler.FAKTOR_ESIK_DUSUK -> FaktorSeviyesi.DUSUK
        oran < Sabitler.FAKTOR_ESIK_ORTA -> FaktorSeviyesi.ORTA
        else -> FaktorSeviyesi.YUKSEK
    }
}
