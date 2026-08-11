package tr.org.yesilay.yesiliz.risk

/**
 * Risk motorunun TÜM sabitleri — spec-risk.md §4.1 §4.2 §4.3 §5.
 *
 * **Kalibrasyon sonrası SADECE bu dosya değişir** (CLAUDE.md §7).
 * Hiçbir sayı başka dosyaya gömülmez.
 *
 * Referans: `docs/referans/yesiliz_hesap.py`. Değer değiştirirsen
 * `python3 tests/vektor_uret.py` ile vektörleri yeniden üret, yoksa
 * `VektorTest` kırmızıya döner.
 */
object Sabitler {

    // ---------------------------------------------------------------
    // §4.1 Kategori ağırlıkları — "bu kategoride 1 dakika ne kadar riskli?"
    // ---------------------------------------------------------------
    // GEÇİCİ değerler, literatür tahmini. Veri gelince çoklu regresyonun
    // standartlaştırılmış beta katsayılarıyla değişecek:
    //     w_c = max(0, β_c) / max|β_j|
    val KATEGORI_AGIRLIK: Map<String, Double> = mapOf(
        "social" to 1.0,
        "entertainment" to 1.0,
        "games" to 0.8,
        "communication" to 0.5,
        "productivity" to 0.3,
        "education" to 0.2,
        "health" to 0.1,
        "other" to 0.5,
    )

    /** Tabloda bulunamayan kategorinin ağırlığı. */
    const val VARSAYILAN_AGIRLIK = 0.5

    /**
     * Tüketim kategorileri — örüntü çarpanı ve aktivite sönümlemesi
     * **SADECE** bunlara uygulanır (§4.6 md.1).
     */
    val TUKETIM: Set<String> = setOf("social", "entertainment", "games")

    // ---------------------------------------------------------------
    // §4.2 Örüntü katsayıları — hepsi 0-1 normalize girdi bekler
    // ---------------------------------------------------------------
    val ORUNTU_KATSAYI: Map<String, Double> = mapOf(
        "gece" to 0.6,        // 00:00-06:00 kullanım oranı
        "oturum" to 0.4,      // 30dk+ kesintisiz TÜKETİM blokları
        "acilis" to 0.3,      // saat başına açılış (6/saat = 1.0)
        "sabah" to 0.2,       // uyanınca hemen telefona sarılma
        "uyku_oncesi" to 0.3, // yatmadan önceki 30 dk
        "yer_degis" to 0.4,   // ekran artarken aktivite düşüyor mu
    )

    // ---------------------------------------------------------------
    // §4.3 Referans sabitleri
    // ---------------------------------------------------------------
    /**
     * Davranış skoru referansı: S = 300 → R_b tam 50.
     *
     * **300, 120 DEĞİL.** TBM'in 120 dakikası *hedeftir*, eşik değil.
     * 120'yi eşik alırsak ortalama kullanıcı (4-5 saat) bile "çok riskli"
     * çıkar ve ölçüm ayırt ediciliğini kaybeder.
     */
    const val S_REF = 300.0

    /** SAS-SV Türkçe ergen formu kesme puanı → R_s tam 50. */
    const val SASSV_KESME = 29.5

    /** SAS-SV ham puan aralığı (10 madde × 1-6 Likert). */
    const val SASSV_MIN = 10.0
    const val SASSV_MAKS = 60.0

    /** Aktivite sönümleme tavanı — spor riski en fazla %15 azaltır. */
    const val MAKS_SONUMLEME = 0.15

    /** R_b eğrisinin eğimi. 0.693 ≈ ln(2), S_REF'te tam yarıya getirir. */
    const val EGIM = 0.693

    /** Aktivite bileşenleri: adım hedefi ve spor dakikası hedefi. */
    const val ADIM_HEDEF = 8000.0
    const val SPOR_HEDEF = 30.0
    const val ADIM_PAYI = 0.4
    const val SPOR_PAYI = 0.6

    // ---------------------------------------------------------------
    // §4.5 alpha eğrisi
    // ---------------------------------------------------------------
    const val ALPHA_ILK_ESIK = 7      // d < 7 birinci kol
    const val ALPHA_IKINCI_ESIK = 28  // d < 28 ikinci kol
    const val ALPHA_TABAN = 0.30      // d >= 28
    const val ANKET_OMRU_GUN = 60.0   // g = 60 → anket ağırlığı sıfır

    /** §4.5 geçmiş gün kalite ağırlığı — canlı gün 1.0, geçmiş gün 0.6. */
    const val CANLI_GUN_AGIRLIK = 1.0
    const val GECMIS_GUN_AGIRLIK = 0.6

    // ---------------------------------------------------------------
    // §5 Bantlama
    // ---------------------------------------------------------------
    const val BANT_AZ_UST = 40.0
    const val BANT_ORTA_UST = 65.0

    /** Kayan pencere — karar tek günle değil 7 günlük ortalamayla verilir. */
    const val PENCERE_GUN = 7

    /** §5.3 güvenlik tabanı: kaç gün üst üste bakılır. */
    const val GUVENLIK_GUN = 5
    const val GUVENLIK_TUKETIM_COK = 480.0
    const val GUVENLIK_TUKETIM_ORTA = 360.0
    const val GUVENLIK_GECE_ORTA = 90.0

    /** §5.4 histerezis — asimetrik: uyarmakta hızlı, rahatlamakta yavaş. */
    const val GUN_YUKARI = 3
    const val GUN_ASAGI = 7

    /** §5.5 trend eşiği — iki haftalık ortalama farkı. */
    const val TREND_ESIK = 5.0

    // ---------------------------------------------------------------
    // Etkileyen faktörler — risk detayı ekranındaki 4 faktör bandı (§12 A3).
    // ---------------------------------------------------------------
    // GEÇİCİ eşikler, pilot verisiyle kalibre edilecek (docs/spec-risk.md
    // §4.2'deki örüntü ölçüm tanımları gibi bunlar da A3 kapsamında).
    /** oran < bu → DÜŞÜK */
    const val FAKTOR_ESIK_DUSUK = 0.33

    /** oran < bu → ORTA, üstü YÜKSEK */
    const val FAKTOR_ESIK_ORTA = 0.66

    /**
     * Örüntü oranlarından seviye çıkarmak için gereken **asgari ölçüm**.
     *
     * Örüntüler (gece, oturum, sosyal oran) günün toplamına bölünüyor.
     * Toplam birkaç dakikayken bölme uç değerler üretiyor: uygulamayı yeni
     * kuran kullanıcı 34 saniyelik kullanımının tamamı gece penceresine
     * düştüğü için `gece = 1,0` alıyor ve risk analizi ekranında
     * "Gece kullanımı → Yüksek" görüyor.
     *
     * Bu, `debug.md` K-7'nin aynısı: veri yokluğunu maksimum risk olarak
     * sunmak. Orada hareket faktörü içindi, burada kullanım oranları için.
     * Eşiğin altında faktörler [FaktorSeviyesi.BILINMIYOR] dönüyor.
     *
     * 15 dakika **geçici** — A3 kalibrasyonuyla birlikte gözden geçirilecek
     * (CLAUDE.md §12 A3).
     */
    const val FAKTOR_ASGARI_DK = 15.0
}
