package tr.org.yesilay.yesiliz.ui.ekran.odak

/**
 * Yaşam döngüsü gözlemcisi eklenirken Android mevcut durumu gözlemciye
 * hemen iletir. Bu ilk `ON_RESUME`, kullanıcının ekrana geri döndüğü anlamına
 * gelmez. Yalnızca gerçek bir ayrılıştan sonraki dönüş odak seansını bozar.
 */
internal class OdakDonusTakibi {
    private var ekrandanAyrildi = false

    fun ayrildi() {
        ekrandanAyrildi = true
    }

    /** Gerçek bir ayrılıştan sonra dönüldüyse `true`. */
    fun dondu(): Boolean {
        if (!ekrandanAyrildi) return false
        ekrandanAyrildi = false
        return true
    }
}
