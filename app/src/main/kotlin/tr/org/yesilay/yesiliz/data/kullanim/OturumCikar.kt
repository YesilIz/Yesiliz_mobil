package tr.org.yesilay.yesiliz.data.kullanim

import java.time.Duration
import java.time.Instant

/**
 * Ham sistem olaylarından ön plan oturumları çıkarır.
 *
 * **SAF FONKSİYON** — `UsageStatsManager` görmez, `Context` almaz.
 * Android tarafı ([KullanimOkuyucu]) sadece olayları bu biçime çevirip
 * buraya verir; asıl mantık burada ve emülatörsüz test edilebilir.
 *
 * ## Niçin `queryEvents`, `queryUsageStats` değil
 *
 * `queryUsageStats` günlük **toplam** verir — "Instagram bugün 90 dakika".
 * Örüntü metrikleri (gece oranı, 30dk+ kesintisiz blok, açılış sayısı)
 * bundan çıkarılamaz; ne zaman kullanıldığını bilmek gerekir. Bu yüzden
 * olay akışı okunup oturumlar elle kuruluyor.
 *
 * Bedeli: `queryEvents` geriye dönük kısa saklanıyor, kurulumdan önceki
 * günler için örüntü yok (spec-risk.md §4.5).
 */
object OturumCikar {

    /**
     * Bundan kısa oturumlar atılır.
     *
     * Uygulama geçişlerinde sistem bir dizi `RESUMED`/`PAUSED` üretiyor;
     * launcher'ın bir anlığına öne gelmesi gibi olaylar 100-200 ms sürüyor.
     * Bunlar "açılış" sayılırsa `acilis` örüntüsü şişer.
     */
    private val EN_KISA_OTURUM: Duration = Duration.ofSeconds(1)

    /**
     * Bir oturumun makul azami süresi.
     *
     * Ekran kapanma olayı kaçarsa (cihaz aniden kapandı, sistem olayı
     * düşürdü) oturum saatlerce açık kalır ve tek başına günü domine eder.
     * Bu süreyi aşan oturumlar buraya kırpılır.
     */
    private val EN_UZUN_OTURUM: Duration = Duration.ofHours(4)

    /**
     * @param olaylar zaman sırasına göre artan; sırasızsa burada sıralanır
     * @param pencereBas günün başlangıcı — daha önce açılmış oturum buradan sayılır
     * @param pencereBit günün sonu — hâlâ açık oturum burada kapatılır
     */
    fun cikar(
        olaylar: List<HamOlay>,
        pencereBas: Instant,
        pencereBit: Instant,
    ): List<HamOturum> {
        val sirali = olaylar.sortedBy { it.an }
        val oturumlar = mutableListOf<HamOturum>()

        var acikPaket: String? = null
        var acikBaslangic: Instant? = null

        fun kapat(an: Instant) {
            val paket = acikPaket ?: return
            val bas = acikBaslangic ?: return
            acikPaket = null
            acikBaslangic = null

            val bit = minOf(an, pencereBit)
            if (!bit.isAfter(bas)) return

            val kirpilmis = minOf(bit, bas.plus(EN_UZUN_OTURUM))
            if (Duration.between(bas, kirpilmis) >= EN_KISA_OTURUM) {
                oturumlar += HamOturum(paket, bas, kirpilmis)
            }
        }

        for (olay in sirali) {
            if (olay.an.isBefore(pencereBas) || olay.an.isAfter(pencereBit)) continue

            when (olay.tur) {
                OlayTuru.ON_PLANA_GELDI -> {
                    // Aynı paket zaten öndeyse yinelenen olayı yok say —
                    // aksi hâlde her Activity geçişi yeni oturum sayılırdı.
                    if (acikPaket == olay.paketAdi) continue
                    kapat(olay.an)
                    acikPaket = olay.paketAdi
                    acikBaslangic = olay.an
                }

                OlayTuru.ARKA_PLANA_GITTI -> {
                    if (acikPaket == olay.paketAdi) kapat(olay.an)
                }

                // Ekran kapandıysa hangi uygulama önde olursa olsun oturum biter.
                OlayTuru.EKRAN_KAPANDI -> kapat(olay.an)

                // Ekranın açılması tek başına kullanım değil; asıl oturumu
                // ardından gelen ON_PLANA_GELDI başlatır.
                OlayTuru.EKRAN_ACILDI -> Unit
            }
        }

        kapat(pencereBit)
        return oturumlar
    }
}

/** [OturumCikar]'ın anladığı olay türleri — sistem sabitlerinden bağımsız. */
enum class OlayTuru {
    ON_PLANA_GELDI,
    ARKA_PLANA_GITTI,
    EKRAN_ACILDI,
    EKRAN_KAPANDI,
}

/** Sistemden okunan ham olay. */
data class HamOlay(
    val paketAdi: String,
    val tur: OlayTuru,
    val an: Instant,
)

/** Çıkarılan ön plan oturumu — henüz kategorilendirilmemiş. */
data class HamOturum(
    val paketAdi: String,
    val baslangic: Instant,
    val bitis: Instant,
) {
    val sure: Duration get() = Duration.between(baslangic, bitis)
}
