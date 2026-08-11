package tr.org.yesilay.yesiliz.ui.ekran.yuruyus

/** Aktif yürüyüş ekranının durumu — `docs/tasarim-referans.md §4`. */
data class YuruyusDurum(
    val calisiyorMu: Boolean = false,
    val gecenSaniye: Int = 0,
    val kalanMetni: String = "",
    val oran: Float = 0f,
    val kazanilanPuan: Int = 0,
    val tamamlandiMi: Boolean = false,
    /** Oturum bittiğinde hedefe gerçekten ulaşıldı mı. */
    val hedefTamamlandiMi: Boolean = false,
    /** Health Connect'ten okunabilen adım — yoksa `null`, ekranda gizli. */
    val adim: Int? = null,
    /** Puan yazıldıysa tekrar yazılmaz. */
    val puanVerildiMi: Boolean = false,
)
