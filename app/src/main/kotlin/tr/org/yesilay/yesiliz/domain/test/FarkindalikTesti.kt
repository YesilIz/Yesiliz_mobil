package tr.org.yesilay.yesiliz.domain.test

/**
 * Kısa farkındalık testi — 5 soru, ana ekrandaki "Kendini Test Et" kartı.
 *
 * **SAS-SV DEĞİL.** Bu ayrımı korumak kritik:
 *
 * | | SAS-SV (`docs/spec-anket.md`) | Bu test |
 * |---|---|---|
 * | Madde | 10, değiştirilemez | 5, serbest |
 * | Kesme puanı | 29,5 (yayınlanmış) | **yok** |
 * | Risk motoru | `AnketGirdi` olarak girer | **girmez** |
 * | Amaç | ölçüm | farkındalık |
 *
 * SAS-SV'nin kesme puanı yayınlanmış bir çalışmaya dayanıyor; tek kelime
 * değişirse geçersizleşiyor. Bu yüzden onu kısaltmak yerine **ayrı** bir
 * araç yazıldı. Buradan çıkan puan hiçbir yerde bant üretmiyor, yalnızca
 * kullanıcıya kendi cevaplarını geri okuyor.
 *
 * Saf Kotlin — `app` modülünde ama Android'e dokunmuyor, emülatörsüz test
 * edilebiliyor (`OruntuCikar`, `NefesFazHesapla` ile aynı desen).
 */
enum class TestSecenegi(val puan: Int, val etiket: String) {
    NADIREN(0, "Nadiren"),
    BAZEN(1, "Bazen"),
    SIK(2, "Sık sık"),
}

/**
 * Soru metinleri **betimleyici ve gözlemlenebilir** — "kendini bağımlı
 * hissediyor musun" gibi kendi kendine teşhis ettiren bir soru yok
 * (CLAUDE.md §4).
 */
data class TestSorusu(
    val id: String,
    val metin: String,
    /** Sonuçta bu soru öne çıkarsa gösterilecek kısa ipucu. */
    val ipucu: String,
)

/** Sonucun üç düzeyi. Bant adı değil — risk bandıyla karıştırılmamalı. */
enum class TestSeviyesi(val baslik: String) {
    DUSUK("Alışkanlıkların dengeli görünüyor"),
    ORTA("Bazı anlarda telefon öne geçiyor"),
    YUKSEK("Telefon günün akışına sık karışıyor"),
}

data class TestSonuc(
    val hamPuan: Int,
    val seviye: TestSeviyesi,
    val ozet: String,
    /** En yüksek puanlı soruların ipuçları — en çok 2 tane. */
    val ipuclari: List<String>,
    /** Sonuç ekranından açılacak bilgi bankası yazısı. */
    val onerilenYaziId: String,
)

object FarkindalikTesti {

    val SORULAR = listOf(
        TestSorusu(
            id = "uyanir_uyanmaz",
            metin = "Sabah uyanır uyanmaz, daha yataktan kalkmadan telefonuna " +
                "bakıyor musun?",
            ipucu = "Güne telefonla başlamamak, ilk saatin tonunu değiştiriyor.",
        ),
        TestSorusu(
            id = "planladigindan_uzun",
            metin = "Telefonu eline aldığında, planladığından uzun süre " +
                "kullandığın oluyor mu?",
            ipucu = "Sonsuz kaydırma bilerek bitmiyor — molayı sen koyman gerekiyor.",
        ),
        TestSorusu(
            id = "yaninda_yokken",
            metin = "Telefonun yanında değilken huzursuz hissediyor musun?",
            ipucu = "Telefonsuz kısa aralıklar bu hissi zamanla azaltıyor.",
        ),
        TestSorusu(
            id = "araya_giriyor",
            metin = "Ders, iş ya da sohbet sırasında telefon araya giriyor mu?",
            ipucu = "Bildirimleri kısmak, bölünmenin en büyük kaynağını kapatıyor.",
        ),
        TestSorusu(
            id = "uykudan_once",
            metin = "Yatmadan önce düşündüğünden uzun süre ekranda kalıyor musun?",
            ipucu = "Uykudan önceki son yarım saat, uyku kalitesini en çok " +
                "etkileyen aralık.",
        ),
    )

    /** 5 soru × en çok 2 puan. */
    const val AZAMI_PUAN = 10

    private const val ORTA_ESIK = 4
    private const val YUKSEK_ESIK = 7
    private const val IPUCU_ADEDI = 2

    /**
     * Yanıtlar soru sırasıyla verilir. Eksik yanıt kabul edilmiyor —
     * ortalama ile doldurmak SAS-SV'de yasak, burada da aynı ilke geçerli:
     * cevaplanmamış soruyu tahmin etmek kullanıcının anlatısını bozar.
     */
    fun sonucHesapla(yanitlar: List<TestSecenegi>): TestSonuc {
        require(yanitlar.size == SORULAR.size) {
            "Test ${SORULAR.size} yanıt bekliyor, ${yanitlar.size} geldi"
        }

        val hamPuan = yanitlar.sumOf { it.puan }
        val seviye = when {
            hamPuan >= YUKSEK_ESIK -> TestSeviyesi.YUKSEK
            hamPuan >= ORTA_ESIK -> TestSeviyesi.ORTA
            else -> TestSeviyesi.DUSUK
        }

        val ipuclari = SORULAR.indices
            .filter { yanitlar[it] == TestSecenegi.SIK }
            .ifEmpty { SORULAR.indices.filter { yanitlar[it] == TestSecenegi.BAZEN } }
            .take(IPUCU_ADEDI)
            .map { SORULAR[it].ipucu }

        return TestSonuc(
            hamPuan = hamPuan,
            seviye = seviye,
            ozet = ozet(seviye),
            ipuclari = ipuclari,
            onerilenYaziId = onerilenYazi(yanitlar),
        )
    }

    /**
     * Özet cümleleri **teşhis değil gözlem**: "bağımlısın" ya da "riskli
     * gruptasın" geçmiyor, kullanıcının kendi işaretlediği şey geri okunuyor.
     */
    private fun ozet(seviye: TestSeviyesi): String = when (seviye) {
        TestSeviyesi.DUSUK ->
            "Verdiğin cevaplara göre telefon çoğu zaman senin belirlediğin " +
                "yerde duruyor. Bu dengeyi korumak da bir çaba — küçük " +
                "alışkanlıkların işe yarıyor."
        TestSeviyesi.ORTA ->
            "Cevapların, günün bazı anlarında telefonun öne geçtiğini " +
                "gösteriyor. Bunlar genelde tek tek küçük anlar; birlikte " +
                "bakınca fark ediliyor."
        TestSeviyesi.YUKSEK ->
            "Cevaplarına göre telefon günün akışına sık karışıyor. Bu bir " +
                "teşhis değil, senin kendi gözlemin — nereden başlanacağını " +
                "gösteriyor."
    }

    /**
     * Bilgi bankasındaki hangi yazının önerileceği, **en yüksek puanlı
     * soruya** bakılarak seçiliyor: genel bir yazı yerine kullanıcının kendi
     * işaretlediği örüntüye değen bir yazı gösteriliyor.
     */
    private fun onerilenYazi(yanitlar: List<TestSecenegi>): String {
        val enYuksek = yanitlar.withIndex().maxByOrNull { it.value.puan } ?: return "dijital-detoks"
        if (enYuksek.value == TestSecenegi.NADIREN) return "aliskanlik-kurmak"
        return when (SORULAR[enYuksek.index].id) {
            "uyanir_uyanmaz" -> "sabah-rutini"
            "planladigindan_uzun" -> "sonsuz-kaydirma"
            "yaninda_yokken" -> "nomofobi"
            "araya_giriyor" -> "bildirim-yorgunlugu"
            else -> "ekran-ve-uyku"
        }
    }
}
