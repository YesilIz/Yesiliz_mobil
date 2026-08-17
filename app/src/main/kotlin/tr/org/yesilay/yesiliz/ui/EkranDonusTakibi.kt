package tr.org.yesilay.yesiliz.ui

/**
 * Yaşam döngüsü gözlemcisi eklenirken Android mevcut durumu gözlemciye
 * hemen iletir. Bu ilk `ON_RESUME`, kullanıcının ekrana geri döndüğü anlamına
 * gelmez. Yalnızca gerçek bir ayrılıştan sonraki dönüş sayılır.
 *
 * Odak seansı için yazılmıştı (`OdakEkran`, CLAUDE.md §9 "Zaten `RESUMED`
 * sahibe `LifecycleObserver` eklemek"); nefes ekranı da aynı ayrımı istediği
 * için `ui/`'ya taşındı. Davranışı taşınırken **değişmedi**.
 */
internal class EkranDonusTakibi {
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
