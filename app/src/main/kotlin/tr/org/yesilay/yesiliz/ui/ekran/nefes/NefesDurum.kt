package tr.org.yesilay.yesiliz.ui.ekran.nefes

/**
 * Nefes ekranının durumu — `docs/tasarim-referans.md §2 / 16`.
 *
 * [fazSaniye] varsayılan 4 — tasarımın tek somut rakamı ("4 saniye
 * boyunca..."). Tempo seçici [fazSaniye]'yi 4/5/6 arasında döngüler.
 */
data class NefesDurum(
    val ilerleme: NefesIlerleme = NefesIlerleme(
        fazIndeksi = 0,
        fazIciKalanSaniye = 4,
        toplamKalanSaniye = VARSAYILAN_SURE_SN,
        tamamlandiMi = false,
    ),
    val fazSaniye: Int = 4,
    /**
     * Seans süresi, saniye — tempodan **ayrı** kontrol.
     *
     * Ekranda tek bir kontrol vardı ve o tempoyu (4/5/6 sn nefes)
     * değiştiriyordu; kullanıcı bunu seans süresi sandı, etiketleme de
     * buna davetiye çıkarıyordu (`debug.md` İ-2).
     */
    val seansSaniye: Int = VARSAYILAN_SURE_SN,
    val oynatiliyor: Boolean = true,
    /**
     * Seans bitti — tebrik durumu gösteriliyor.
     *
     * Eskiden sayaç `0:00 kaldı`'ya ulaşıp öylece duruyordu: faz dairesi
     * hâlâ "Tut" yazıyor, buton "Oynat"a dönüyordu. Puan aslında
     * yazılıyordu ama kullanıcı bunu ancak geri gidip ana ekrana bakarsa
     * anlıyordu (`debug.md` Y-8).
     */
    val tamamlandi: Boolean = false,
    /** Bu seansta gerçekten puan yazıldı mı — günlük limit dolmuş olabilir. */
    val puanYazildi: Boolean = false,
)

/** Seans süresi seçenekleri, dakika. */
val SEANS_SECENEKLERI = listOf(1, 2, 5)

/** `Gorev.NEFES.aciklama` = "2 dakikalık kutu nefesi" ile birebir. */
const val VARSAYILAN_SURE_SN = 120
