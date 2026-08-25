package tr.org.yesilay.yesiliz.ui.ekran.hedef

/**
 * Günlük hedef ekranı — `docs/tasarim-referans.md §2 / 34`.
 *
 * Onboarding'deki hedef ekranlarından (06, 07) ayrı: onlar ilk kurulumda
 * bir kez çalışıyor ve öneri üretiyor, bu ise kullanıcının hedefini
 * istediği zaman değiştirdiği ayar ekranı.
 */
data class HedefDurum(
    /** Kaydedilmiş hedef — "Kaydet" basılana kadar değişmiyor. */
    val kayitliHedefDk: Int = 0,
    /** Ekranda seçili olan değer. */
    val secilenDk: Int = 0,
    /** Bugünkü ekran süresi; veri yoksa `null` ve satır gizli. */
    val bugunDk: Int? = null,
    val ebeveynAktifMi: Boolean = false,
    val kaydedildi: Boolean = false,
    /** Kaydedilmiş nudge eşiği (dakika) — hiç seçilmediyse varsayılan. */
    val kayitliNudgeDk: Int = 0,
    /** Ekranda seçili olan nudge eşiği. */
    val secilenNudgeDk: Int = 0,
    /**
     * Mola hatırlatıcı Profil'den açık mı — kapalıysa eşik seçilebiliyor ama
     * uygulanmıyor ve ekran bunu söylüyor (§12 K37: kapalı bir özelliğin
     * ayarını çalışıyormuş gibi göstermek).
     */
    val molaHatirlaticiAcik: Boolean = false,
    /** Kullanıcı kendi eşiğini kaydetmiş mi — false ise varsayılan geçerli. */
    val nudgeSecilmisMi: Boolean = false,
) {
    val hedefDegistiMi: Boolean get() = secilenDk != kayitliHedefDk

    val nudgeDegistiMi: Boolean get() = secilenNudgeDk != kayitliNudgeDk

    val degistiMi: Boolean get() = hedefDegistiMi || nudgeDegistiMi

    /** Halka oranı: bugünün kullanımının hedefe oranı. */
    val oran: Float
        get() = bugunDk?.let { (it.toFloat() / secilenDk.coerceAtLeast(1)).coerceIn(0f, 1f) } ?: 0f

    val yuzde: Int
        get() = bugunDk?.let { (it * 100 / secilenDk.coerceAtLeast(1)) } ?: 0
}

object HedefSinir {
    /**
     * Taban 120 dk — `docs/spec-puan.md §7.2`: "TBM önerisi, hedef bunun
     * altına inmez". Daha düşük bir hedef koymak, tutturulamayacak bir
     * çıta dikip her günü başarısızlıkla bitirmek olurdu.
     */
    const val EN_AZ = 120

    /** Üst sınır 6 saat — tasarımdaki hızlı seçim çipinin en büyüğü. */
    const val EN_COK = 360

    /** `−` / `+` adımı. */
    const val ADIM = 15

    fun kirp(dakika: Int): Int = dakika.coerceIn(EN_AZ, EN_COK)

    /** Tasarım 34'teki hızlı seçim çipleri: 3s · 4s · mevcut · 6s. */
    fun hizliSecim(mevcutHedef: Int): List<Int> =
        (listOf(180, 240, mevcutHedef, 360)).distinct().sorted()
}
