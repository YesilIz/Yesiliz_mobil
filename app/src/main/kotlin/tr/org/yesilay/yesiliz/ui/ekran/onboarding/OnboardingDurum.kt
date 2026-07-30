package tr.org.yesilay.yesiliz.ui.ekran.onboarding

/** Cinsiyet — "Belirtmek istemiyorum" gerçek bir seçenek, boş değer değil. */
enum class Cinsiyet(val etiket: String) {
    KADIN("Kadın"),
    ERKEK("Erkek"),
    BELIRTMEDI("Belirtmek istemiyorum"),
}

/**
 * Onboarding akışının tamamının durumu — tek ViewModel, 7 ekran.
 *
 * Ekran başına ayrı ViewModel yapmak yaş/anket/hedef değerlerini ekranlar
 * arasında elle taşımayı gerektirirdi; akış tek oturumda tamamlanan
 * doğrusal bir zincir olduğu için tek durum daha basit.
 */
data class OnboardingDurum(
    val yas: Int = VARSAYILAN_YAS,
    val cinsiyet: Cinsiyet? = null,
    val yanitlar: List<Int> = List(SasSv.MADDELER.size) { 0 },
    val mevcutOrtalama: Int = 300,
    val hedefDakika: Int = 285,
    /** UsageStats izni verildi mi — `IzinEkran`'ın `ON_RESUME` kontrolüyle güncellenir. */
    val izinVerildiMi: Boolean = false,
) {
    /** 10 madde zorunlu — eksikle ilerlenemez (`docs/spec-anket.md §6.4`). */
    val anketTamam: Boolean get() = yanitlar.all { it > 0 }

    /** SAS-SV ham puanı, 10–60. Ters madde yok, düz toplam. */
    val hamPuan: Int get() = yanitlar.sum()

    /** 18 yaş altı → ebeveyn onayı zorunlu (CLAUDE.md §4). */
    val ebeveynOnayiGerekir: Boolean get() = yas < ERGIN_YAS
}

/**
 * SAS-SV maddeleri — `docs/spec-anket.md §6.2`.
 *
 * **DEĞİŞTİRİLEMEZ.** Metin, sayı, sıra sabit; tek kelime değişirse 29,5
 * kesme puanı geçersizleşir. Bu yüzden burada sabit, DataStore'dan ya da
 * sunucudan gelmiyor.
 */
object SasSv {
    val MADDELER: List<String> = listOf(
        "Akıllı telefon kullanımından dolayı planladığım işleri yetiştiremem.",
        "Akıllı telefon kullanımından dolayı, sınıfta ödev yaparken veya ders " +
            "dinlerken konsantre olmakta zorlanırım.",
        "Akıllı telefon kullanırken el bileklerimde veya ensemde ağrı hissederim.",
        "Akıllı telefon olmadan yapamam.",
        "Akıllı telefonum elimde olmadığında sabırsız ve huysuz hissederim.",
        "Kullanmasam bile akıllı telefonum hep aklımdadır.",
        "Günlük yaşantım çok etkilenmiş olsa bile akıllı telefonumu kullanmayı " +
            "asla bırakmam.",
        "Twitter veya Facebook'taki diğer insanlar arasındaki konuşmaları " +
            "kaçırmamak için sürekli olarak akıllı telefonumu kontrol ederim.",
        "Akıllı telefonumu düşündüğümden daha uzun süre kullanırım.",
        "Etrafımdaki insanlar akıllı telefonumu çok fazla kullandığımı söyler.",
    )

    /** Sayfa başına 2–3 madde; 10 maddeyi tek ekranda gösterme (§6.4). */
    const val SAYFA_BOYU = 2
}

private const val VARSAYILAN_YAS = 17
private const val ERGIN_YAS = 18
