package tr.org.yesilay.yesiliz.ui.ekran.ebeveyn

import tr.org.yesilay.yesiliz.ui.bilesen.veri.Cubuk

/** Cihaz rolü seçimi — `Tercihler.ebeveynCihazRolu`'nda aynı string saklanıyor. */
enum class CihazRolu(val deger: String, val baslik: String, val aciklama: String) {
    EBEVEYN("ebeveyn", "Bu cihaz ebeveyn cihazı", "Çocuğunun ekran süresini buradan takip edeceksin"),
    COCUK("cocuk", "Bu cihaz çocuk cihazı", "Bu, Yeşilİz'ı kullandığın kendi cihazın"),
}

/** İlk kurulum sihirbazının hangi adımında olduğumuz (ekran 20 → 21 → 22). */
enum class EslestirmeAdimi { ROL_SECIMI, KOD, BASARILI }

/**
 * Ebeveyn paneli durumu — `docs/tasarim-referans.md §2 / 13, 20-22, 27`.
 *
 * [eslesmisMi] false iken sihirbaz (ekran 20-22) gösteriliyor, true iken
 * gerçek panel (ekran 13). Çocuğun verisi MVP'de **mock** — gerçek
 * `EbeveynBaglantiEntity` yok (CLAUDE.md §6, Faz 2'ye ertelendi). Eşleşme
 * durumu `Tercihler`'den geliyor, çocuk verisi sabit örnek.
 */
data class EbeveynDurum(
    val eslesmisMi: Boolean = false,
    val cihazRolu: String? = null,
    // --- sihirbaz ---
    val adim: EslestirmeAdimi = EslestirmeAdimi.ROL_SECIMI,
    val secilenRol: CihazRolu = CihazRolu.EBEVEYN,
    val uretilenKod: String = "",
    /** Sunucudan gelen eşleştirme hatası; `null` iken hata yok. */
    val eslestirmeHatasi: String? = null,
    // --- mock çocuk paneli (ekran 13) ---
    val cocukAdi: String = "Elif",
    val cocukYasi: Int = 14,
    val ekranSuresiOrtalama: String = "3s 40dk",
    val tarihAraligi: String = "22–28 Tem, 2026",
    val ekranCubuklari: List<Cubuk> = emptyList(),
    val ekranVurgu: Int = -1,
    val bant: String = "ORTA",
    val bantMesaji: String = "Geçen haftaya göre iyileşiyor",
    val bildirimler: List<EbeveynBildirimi> = emptyList(),
)

data class EbeveynBildirimi(val baslik: String, val zaman: String)
