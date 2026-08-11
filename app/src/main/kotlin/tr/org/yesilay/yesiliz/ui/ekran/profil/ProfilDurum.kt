package tr.org.yesilay.yesiliz.ui.ekran.profil

import tr.org.yesilay.yesiliz.data.tercih.BildirimSaatleri
import tr.org.yesilay.yesiliz.domain.sozlesme.SssMaddesi

/**
 * Profil ekranı durumu — `docs/tasarim-referans.md §2 / 12`.
 *
 * [yesilayPaylasimi], [ligdeGorun] ve [gunlukHedefDk] DataStore'da tutuluyor
 * (CLAUDE.md §6): tekil tercih değerleri Room tablosu istemiyor.
 */
data class ProfilDurum(
    val isim: String = "",
    val ligAdi: String = "",
    val yesilayPaylasimi: Boolean = false,
    val ligdeGorun: Boolean = true,
    val molaHatirlaticiAcik: Boolean = false,
    val gunlukHedefDk: Int = 0,
    val bildirimSaatleri: BildirimSaatleri = BildirimSaatleri(),
    /** Debug derlemesinde demo tohumu satırı gösterilsin mi. */
    val demoKullanilabilir: Boolean = false,
    /** Silme sürerken onay butonu pasif ve göstergeli olsun diye. */
    val siliniyor: Boolean = false,
    /**
     * Silme isteği tamamlanamadı; hiçbir veri silinmedi.
     *
     * Kullanıcının kimliği ve yerel verisi yeniden deneyebilmesi için
     * korunuyor; ekran bunu açıkça bildiriyor.
     */
    val sunucudaKaldiUyarisi: Boolean = false,
    /** Geçmiş dosyaları cihazda oluşturulamadı; kullanıcı yeniden deneyebilir. */
    val disaAktarmaHatasi: Boolean = false,
    /** Gerçek bir hesapla giriş yapıldı mı — misafirde "Çıkış yap" yok. */
    val hesapVarMi: Boolean = false,
    val eposta: String = "",
    /** Yardım alt sayfasındaki SSS — `assets/sozlesmeler.json`. */
    val sss: List<SssMaddesi> = emptyList(),
    /** Geliştirici ayarındaki sunucu adresi (`debug.md` O-14). */
    val sunucuTabani: String = "",
)
