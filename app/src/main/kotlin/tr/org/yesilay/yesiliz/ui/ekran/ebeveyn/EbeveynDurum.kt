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
 * gerçek panel (ekran 13). Eşleşme durumu `Tercihler`'den, paneldeki
 * özet ise sunucudan gelir. Sunucuya ulaşılamadığında gerçekmiş gibi
 * örnek çocuk/veri gösterilmez; yeniden deneme durumu sunulur.
 */
data class EbeveynDurum(
    val eslesmisMi: Boolean = false,
    val cihazRolu: String? = null,
    // --- sihirbaz ---
    val adim: EslestirmeAdimi = EslestirmeAdimi.ROL_SECIMI,
    val secilenRol: CihazRolu = CihazRolu.EBEVEYN,
    val uretilenKod: String = "",
    val kodYukleniyor: Boolean = false,
    val kodSunucudanMi: Boolean = false,
    /** Sunucudan gelen eşleştirme hatası; `null` iken hata yok. */
    val eslestirmeHatasi: String? = null,
    val eslesmeKaldiriliyor: Boolean = false,
    val eslesmeKaldirmaHatasi: String? = null,
    // --- gerçek çocuk paneli (ekran 13) ---
    val ozetYukleniyor: Boolean = true,
    val ozetHatasi: String? = null,
    val cocukAdi: String = "",
    val cocukYasi: Int? = null,
    val ekranSuresiOrtalama: String = "",
    val tarihAraligi: String = "Son 7 gün",
    val ekranCubuklari: List<Cubuk> = emptyList(),
    val ekranVurgu: Int = -1,
    val bant: String = "Henüz hesaplanmadı",
    val bantMesaji: String = "Yeterli veri oluştuğunda burada görünür.",
    val bildirimler: List<EbeveynBildirimi> = emptyList(),
)

data class EbeveynBildirimi(val baslik: String, val zaman: String)
