package tr.org.yesilay.yesiliz.domain.kullanim

import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import tr.org.yesilay.yesiliz.risk.Sabitler
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

/**
 * Ham kullanım olaylarından günlük özet ve örüntü değerleri çıkarır.
 *
 * **SAF FONKSİYON** — Context yok, veritabanı yok, saat okuma yok.
 * Zaman dilimi dışarıdan verilir ki test belirlenimci olsun.
 *
 * ---
 * ## Çıkarım tanımları — DİKKAT
 *
 * spec-risk.md §4.2 altı örüntünün **katsayısını** veriyor ama
 * **nasıl ölçüleceğini** söylemiyor. Aşağıdaki tanımların üçü doğrudan
 * spesifikasyondan, üçü bu dosyada yapılmış **geçici** yorumlar:
 *
 * | Örüntü | Kaynak | Tanım |
 * |---|---|---|
 * | `gece` | ✅ spec | 00:00–06:00 arası dakika / toplam dakika |
 * | `oturum` | ⚠️ yorum | 30dk+ kesintisiz tüketim bloklarındaki dakika / toplam tüketim dakikası |
 * | `acilis` | ✅ spec | (açılış / uyanık saat) / 6, tavan 1.0 |
 * | `sabah` | ⚠️ yorum | uyanıştan sonraki ilk oturumun süresi / 30 dk, tavan 1.0 |
 * | `uyku_oncesi` | ⚠️ yorum | uykudan önceki 30 dk içindeki kullanım / 30 |
 * | `yer_degis` | ⚠️ yorum | çok günlü — [YerDegisHesapla] |
 *
 * ⚠️ işaretliler **onaylanmadı.** Risk skorunu doğrudan etkiliyorlar:
 * `oturum` + `sabah` + `uyku_oncesi` + `yer_degis` katsayıları toplamı 1,3;
 * örüntü çarpanı P'nin azami 2,2'lik aralığının %59'u. Yanlış ölçülürse
 * skor sistematik olarak kayar. Pilot veriyle doğrulanmalı.
 */
object OruntuCikar {

    /** Gece penceresi — spec-risk.md §4.2. */
    private val GECE_BASLANGIC: LocalTime = LocalTime.MIDNIGHT
    private val GECE_BITIS: LocalTime = LocalTime.of(6, 0)

    /**
     * İki tüketim olayı arasındaki bu süreden kısa boşluklar **blok bozmaz**.
     * Instagram'dan TikTok'a geçmek tıkınmayı kesmez; 2 dakika içinde geri
     * dönülüyorsa aynı oturum sayılır.
     *
     * `internal` — `nudge/Tetikleyici`'nin canlı blok takibi aynı toleransı
     * kullanıyor, ayrı bir sabit olarak kopyalanmıyor.
     */
    internal val BLOK_TOLERANSI: Duration = Duration.ofMinutes(2)

    /** "Kesintisiz blok" eşiği — spec-risk.md §4.2 `oturum` maddesi. */
    private val BLOK_ESIGI: Duration = Duration.ofMinutes(30)

    /** Uyku sayılması için gereken en kısa boşluk. */
    private val UYKU_ESIGI: Duration = Duration.ofHours(4)

    /**
     * Boşluğun uyku sayılması için **orta noktasının** düşmesi gereken aralık.
     *
     * Yalnızca "en uzun boşluk" demek yetmiyor: telefonu hiç açmadan geçen
     * bir iş günü 8-9 saatlik boşluk üretir ve yanlışlıkla uyku sanılır.
     * Bu hata testte yakalandı.
     */
    private val UYKU_ORTASI_BAS: LocalTime = LocalTime.of(22, 0)
    private val UYKU_ORTASI_BIT: LocalTime = LocalTime.of(8, 0)

    /** `sabah` ve `uyku_oncesi` için doygunluk süresi. */
    private const val SABAH_PENCERE_DK = 30.0

    /** `uyku_oncesi` penceresi. */
    private const val UYKU_ONCESI_PENCERE_DK = 30.0

    /** Uyku bulunamazsa varsayılan uyanık süre. */
    private const val VARSAYILAN_UYANIK_SAAT = 16.0

    /** `acilis` = 1.0 kabul edilen saatlik açılış sayısı. */
    private const val ACILIS_TAVAN = 6.0

    /**
     * @param olaylar bir güne ait olaylar; sırasız gelebilir
     * @param tarih özeti çıkarılan yerel gün
     * @param zon kullanıcının zaman dilimi
     */
    fun cikar(
        olaylar: List<KullanimOlay>,
        tarih: LocalDate,
        zon: ZoneId,
    ): GunlukCikarim {
        val sirali = olaylar
            .filter { it.kategori.hesabaGirer }
            .sortedBy { it.baslangic }

        if (sirali.isEmpty()) {
            return GunlukCikarim(
                kategoriDakika = emptyMap(),
                oruntu = emptyMap(),
                toplamDk = 0.0,
                geceKullanimDk = 0.0,
                acilisSayisi = 0,
                uykuPenceresi = null,
            )
        }

        val kategoriDakika = LinkedHashMap<String, Double>()
        for (olay in sirali) {
            val anahtar = olay.kategori.anahtar
            kategoriDakika[anahtar] = (kategoriDakika[anahtar] ?: 0.0) + olay.dakika
        }

        val toplamDk = sirali.sumOf { it.dakika }
        val geceDk = sirali.sumOf { geceKesisimi(it, tarih, zon) }
        val uyku = uykuBul(sirali, zon)
        val uyanikSaat = uyku?.let { max(1.0, 24.0 - it.saat) } ?: VARSAYILAN_UYANIK_SAAT

        val oruntu = mapOf(
            "gece" to if (toplamDk > 0) (geceDk / toplamDk).coerceIn(0.0, 1.0) else 0.0,
            "oturum" to oturumOrani(sirali),
            "acilis" to min(1.0, (sirali.size / uyanikSaat) / ACILIS_TAVAN),
            "sabah" to sabahOrani(sirali, uyku),
            "uyku_oncesi" to uykuOncesiOrani(sirali, uyku),
            // yer_degis çok günlü — YerDegisHesapla ayrıca ekliyor
            "yer_degis" to 0.0,
        )

        return GunlukCikarim(
            kategoriDakika = kategoriDakika,
            oruntu = oruntu,
            toplamDk = toplamDk,
            geceKullanimDk = geceDk,
            acilisSayisi = sirali.size,
            uykuPenceresi = uyku,
        )
    }

    /** Olayın [tarih] gününün 00:00–06:00 penceresiyle kesişen dakikası. */
    private fun geceKesisimi(olay: KullanimOlay, tarih: LocalDate, zon: ZoneId): Double {
        val pencereBas = tarih.atTime(GECE_BASLANGIC).atZone(zon).toInstant()
        val pencereBit = tarih.atTime(GECE_BITIS).atZone(zon).toInstant()
        val bas = maxOf(olay.baslangic, pencereBas)
        val bit = minOf(olay.bitis, pencereBit)
        return if (bit.isAfter(bas)) Duration.between(bas, bit).toMillis() / 60_000.0 else 0.0
    }

    /**
     * Tüketim olaylarını [BLOK_TOLERANSI] boşluğuna kadar birleştirir,
     * [BLOK_ESIGI]'ni aşan blokların dakikasını toplam tüketim dakikasına böler.
     *
     * §4.6 md.2: bu metrik **sadece tüketim kategorilerinde** anlamlı.
     * 2 saatlik CapCut kurgusu derin çalışmadır; 2 saatlik TikTok tıkınmadır.
     */
    private fun oturumOrani(sirali: List<KullanimOlay>): Double {
        val tuketim = sirali.filter { it.kategori.anahtar in Sabitler.TUKETIM }
        if (tuketim.isEmpty()) return 0.0

        val toplamTuketimDk = tuketim.sumOf { it.dakika }
        if (toplamTuketimDk <= 0.0) return 0.0

        var blokBas = tuketim.first().baslangic
        var blokBit = tuketim.first().bitis
        var blokIciDk = 0.0

        fun blogunuKapat() {
            val blokSuresi = Duration.between(blokBas, blokBit)
            if (blokSuresi >= BLOK_ESIGI) {
                blokIciDk += blokSuresi.toMillis() / 60_000.0
            }
        }

        for (olay in tuketim.drop(1)) {
            val bosluk = Duration.between(blokBit, olay.baslangic)
            if (bosluk <= BLOK_TOLERANSI) {
                blokBit = maxOf(blokBit, olay.bitis)
            } else {
                blogunuKapat()
                blokBas = olay.baslangic
                blokBit = olay.bitis
            }
        }
        blogunuKapat()

        return (blokIciDk / toplamTuketimDk).coerceIn(0.0, 1.0)
    }

    /**
     * En uzun boşluk hem [UYKU_ESIGI]'ni aşıyor **hem de** orta noktası gece
     * saatlerine düşüyorsa uyku sayılır.
     *
     * Gece koşulu şart: telefonu hiç açmadan geçen bir iş günü de 8 saatlik
     * boşluk üretir. Onu uyku sanmak `sabah` ve `uyku_oncesi` örüntülerini
     * tamamen yanlış yerden ölçmeye yol açar.
     */
    private fun uykuBul(sirali: List<KullanimOlay>, zon: ZoneId): UykuPenceresi? {
        var enUzun: UykuPenceresi? = null
        var enUzunSure = Duration.ZERO

        for ((onceki, sonraki) in sirali.zipWithNext()) {
            val bosluk = Duration.between(onceki.bitis, sonraki.baslangic)
            if (bosluk > enUzunSure) {
                enUzunSure = bosluk
                enUzun = UykuPenceresi(onceki.bitis, sonraki.baslangic)
            }
        }
        if (enUzun == null || enUzunSure < UYKU_ESIGI) return null

        val orta = enUzun.baslangic.plusMillis(enUzunSure.toMillis() / 2)
        return if (geceyeDusuyorMu(orta, zon)) enUzun else null
    }

    /** [an] yerel saatiyle 22:00–08:00 arasına düşüyor mu (gece yarısını aşan aralık). */
    private fun geceyeDusuyorMu(an: Instant, zon: ZoneId): Boolean {
        val saat = an.atZone(zon).toLocalTime()
        return saat >= UYKU_ORTASI_BAS || saat < UYKU_ORTASI_BIT
    }

    /**
     * Uyanıştan sonraki **ilk oturumun uzunluğu**.
     * [SABAH_PENCERE_DK] ve üstü → 1.0
     *
     * > Not: "uyanıştan ilk kullanıma kadar geçen gecikme" ölçülemiyor —
     * > uyanma anını zaten telefonun ilk açılışından çıkarsıyoruz, gecikme
     * > tanımı gereği daima sıfır olurdu. Bunun yerine ilk oturumun süresi
     * > ölçülüyor: "gözünü açar açmaz 25 dakika kaydırmak" ile "alarmı
     * > kapatıp bırakmak" arasındaki farkı bu yakalıyor.
     * > Gerçek uyanma saati Health Connect uyku verisiyle gelirse
     * > tanım gecikmeye çevrilebilir.
     */
    private fun sabahOrani(sirali: List<KullanimOlay>, uyku: UykuPenceresi?): Double {
        if (uyku == null) return 0.0
        val ilkOturum = sirali.firstOrNull { !it.baslangic.isBefore(uyku.bitis) }
            ?: return 0.0
        return min(1.0, ilkOturum.dakika / SABAH_PENCERE_DK)
    }

    /** Uykuya dalmadan önceki [UYKU_ONCESI_PENCERE_DK] dakikadaki kullanım oranı. */
    private fun uykuOncesiOrani(sirali: List<KullanimOlay>, uyku: UykuPenceresi?): Double {
        if (uyku == null) return 0.0
        val pencereBas = uyku.baslangic.minusSeconds((UYKU_ONCESI_PENCERE_DK * 60).toLong())
        val kullanimDk = sirali.sumOf { olay ->
            val bas = maxOf(olay.baslangic, pencereBas)
            val bit = minOf(olay.bitis, uyku.baslangic)
            if (bit.isAfter(bas)) Duration.between(bas, bit).toMillis() / 60_000.0 else 0.0
        }
        return (kullanimDk / UYKU_ONCESI_PENCERE_DK).coerceIn(0.0, 1.0)
    }
}

/**
 * `yer_degis` (katsayı 0,4) — "ekran artarken aktivite düşüyor mu".
 *
 * Tek günden çıkarılamaz: bir *eğilim* ölçüyor. Son 7 günün ilk ve ikinci
 * yarısı karşılaştırılıyor.
 *
 * ⚠️ **Bu tanım geçici** — spec-risk.md §4.2 metriği adlandırıyor ama
 * ölçüm biçimini vermiyor. Pilot veriyle doğrulanmalı.
 */
object YerDegisHesapla {

    /**
     * @param ekranDk günlük toplam ekran dakikası, eskiden yeniye
     * @param adim günlük adım sayısı, aynı sırada ve aynı uzunlukta
     * @return 0..1 — 1.0 "ekran belirgin arttı, hareket belirgin düştü"
     */
    fun hesapla(ekranDk: List<Double>, adim: List<Double>): Double {
        require(ekranDk.size == adim.size) { "Listeler aynı uzunlukta olmalı" }
        if (ekranDk.size < 4) return 0.0

        val orta = ekranDk.size / 2
        val ekranOnce = ekranDk.take(orta).average()
        val ekranSonra = ekranDk.drop(orta).average()
        val adimOnce = adim.take(orta).average()
        val adimSonra = adim.drop(orta).average()

        if (ekranOnce <= 0.0 || adimOnce <= 0.0) return 0.0

        val ekranArtisi = (ekranSonra - ekranOnce) / ekranOnce
        val adimDususu = (adimOnce - adimSonra) / adimOnce

        // İkisi de olumsuz yöndeyse sinyal var; biri bile değilse 0.
        if (ekranArtisi <= 0.0 || adimDususu <= 0.0) return 0.0

        // %25 artış + %25 düşüş → 1.0 kabul ediliyor.
        return min(1.0, (ekranArtisi + adimDususu) / 0.5)
    }
}
