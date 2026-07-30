package tr.org.yesilay.yesiliz.nudge

/**
 * Overlay penceresinin kendi iç durumu — bir NavHost ekranı değil, bu yüzden
 * `<Ad>Ekran/ViewModel/Durum` üçlüsü değil, `NudgeOverlayService`'in kendi
 * `ComposeView`'ına verdiği küçük bir durum.
 */
enum class NudgeAsama {
    /** "45 dakikadır buradasın" + "5 dk mola ver" / "Devam et". */
    ANA_KART,

    /** "5 dk mola ver" sonrası — "Nefes egzersizi yapmak ister misin?". */
    MOLA_SORUSU,
}

data class NudgeDurum(
    val asama: NudgeAsama = NudgeAsama.ANA_KART,
    val esikDakika: Long = NudgeSabitleri.ESIK.toMinutes(),
)
