package tr.org.yesilay.yesiliz.ui.ekran.nefes

/**
 * Nefes seansının **ön planda geçen** süresi — SAF SINIF.
 *
 * `NefesFazHesapla` ile aynı gerekçe (CLAUDE.md §6): ekran mantığının saf
 * bir alt-hesaba ayrılabilen kısmı ayrılıyor ki emülatörsüz sınanabilsin.
 *
 * ## Niçin duraklatma, "bozuldu" değil
 *
 * Odak seansı ayrılışı seansı **bozulmuş** sayıyor (`OdakHesapla.basariliMi`)
 * çünkü görevin tamamı telefonun kapalı kalmasına dayanıyor. Nefes 2
 * dakikalık ve kullanıcı ekrana bakıyor (K21); gelen bir aramayla seansı
 * tamamen kaybettirmek §4'ün ceza yasağına yaklaşırdı. Burada ayrılış
 * sayacı **donduruyor**, dönüş kaldığı yerden devam ettiriyor.
 *
 * Değişmeyen şey şu: arka planda geçen saniye tamamlanmaya **sayılmaz**.
 * Sayılınca kullanıcı ekranı açıp HOME'a basarak 5 puan alabiliyordu
 * (4. tarama T4-010) — yapılmamış bir aktiviteye puan yazmak §4'ün
 * "yapılmayan işi yapıldı gösterme" maddesiyle aynı aile.
 */
internal class NefesSayac {

    var gecenSaniye: Int = 0
        private set

    private var onPlanda: Boolean = true
    private var kullaniciDuraklatti: Boolean = false

    /** Sayaç şu an ilerlemeli mi — ekranın "oynatılıyor" durumu da bu. */
    val calisiyorMu: Boolean get() = onPlanda && !kullaniciDuraklatti

    /**
     * Bir saniye ilerlet.
     *
     * @return saniye gerçekten sayıldıysa `true`; duraklamış sayaçta `false`
     */
    fun tik(): Boolean {
        if (!calisiyorMu) return false
        gecenSaniye++
        return true
    }

    fun tamamlandiMi(seansSaniye: Int): Boolean = gecenSaniye >= seansSaniye

    /** Uygulama arka plana düştü — sayaç donuyor. */
    fun arkaPlanaGitti() {
        onPlanda = false
    }

    /**
     * Kullanıcı ekrana döndü.
     *
     * Elle duraklatılmış bir seansı **kendiliğinden başlatmıyor**: dönüş,
     * kullanıcının duraklat kararını ezmemeli.
     */
    fun onPlanaDondu() {
        onPlanda = true
    }

    fun duraklat() {
        kullaniciDuraklatti = true
    }

    fun devamEt() {
        kullaniciDuraklatti = false
    }

    /** Baştan başlat / seans süresi değişti. */
    fun sifirla() {
        gecenSaniye = 0
        kullaniciDuraklatti = false
    }
}
