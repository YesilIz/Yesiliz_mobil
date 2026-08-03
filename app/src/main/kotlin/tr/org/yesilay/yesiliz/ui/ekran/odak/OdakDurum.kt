package tr.org.yesilay.yesiliz.ui.ekran.odak

/**
 * Odak seansı durumu — `debug.md` İ-1.
 *
 * `DIJITAL_MOLA`'nın gerçekten doğrulanmış hâli: kullanıcı "yaptım"
 * demiyor, ekranın kapalı kaldığı süre ölçülüyor.
 */
data class OdakDurum(
    /** Seçilen seans uzunluğu, dakika. */
    val sureDk: Int = OdakHesapla.SURELER[1],
    val basladi: Boolean = false,
    val ilerleme: OdakIlerleme = OdakIlerleme(0, 0f, false),
    /**
     * Seans sırasında ekran açıldı mı.
     *
     * Görevin adı "telefonu bırak"; tek bir uyandırma bile onu bozuyor.
     * **Ceza yok** — bozulan seans yalnızca puan vermiyor.
     */
    val ekranAcildi: Boolean = false,
    val bitti: Boolean = false,
    val puanYazildi: Boolean = false,
)
