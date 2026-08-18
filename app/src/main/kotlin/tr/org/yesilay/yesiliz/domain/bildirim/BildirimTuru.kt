package tr.org.yesilay.yesiliz.domain.bildirim

/**
 * Uygulamanın kullanıcıya söylediği şeylerin türleri (tasarım 35).
 *
 * **Hepsi olumlu ya da nötr.** "Hedefini aştın", "serin kırıldı" gibi bir tür
 * bilinçli olarak yok: CLAUDE.md §4 ceza ve korkutma yasağı bildirim diline
 * de uygulanıyor. Kullanıcının kötü giden bir gününü ona bildirimle
 * hatırlatmak, uygulamayı sildiren türden bir davranış.
 */
enum class BildirimTuru(val anahtar: String) {
    /** Görev tamamlandı, puan kazanıldı. */
    PUAN("puan"),

    /** Günlük ekran hedefinin altında kalındı. */
    HEDEF("hedef"),

    /** Seri ilerledi (3, 7, 14, 30, 60. gün). */
    SERI("seri"),

    /** Mola hatırlatıcı tetiklendi. */
    MOLA("mola"),

    /** Günlük hatırlatma (bildirim saatleri ayarı). */
    HATIRLATMA("hatirlatma"),

    /**
     * Eski sürümün sahte “gönderildi” kaydını okuyabilmek için korunur.
     * Yeni kayıt üretilmez; 7→8 geçişi var olanları temizler.
     */
    EBEVEYN("ebeveyn"),
    ;

    companion object {
        fun anahtardan(anahtar: String): BildirimTuru? =
            entries.firstOrNull { it.anahtar == anahtar }
    }
}
