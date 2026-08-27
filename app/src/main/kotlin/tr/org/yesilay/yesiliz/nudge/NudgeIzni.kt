package tr.org.yesilay.yesiliz.nudge

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService

/**
 * Mola hatırlatıcısının çalışması için gereken iki sistem izni.
 *
 * İkisi de normal çalışma zamanı izni **değil** — `requestPermissions()` ile
 * istenemez, kullanıcı sistem Ayarları'ndan elle vermek zorunda
 * (`data/kullanim/KullanimIzni`'nin aynısı, farklı izinler).
 *
 * ## İzin ne zaman isteniyor
 *
 * **Yalnızca kullanıcı mola hatırlatıcıyı AÇTIĞINDA** (`HedefEkran`).
 *
 * Eskiden `MainActivity.onResume()` her dönüşte üç adımlı bir kontrol
 * koşturuyordu: eksik izin varsa sistem ekranını kendiliğinden açıyor,
 * üçü de verildiyse tercihi `true`'ya çekiyordu. `molaHatirlaticiAcikMi`
 * üç durumlu olmadığı için ("kapattım" ile "hiç dokunmadım" aynı `false`)
 * bu, **anahtarı kapatan kullanıcının kararını her açılışta eziyordu** —
 * özelliği kapatmanın hiçbir yolu kalmıyordu. Ayrıca bilinçli reddedeni
 * her açılışta sistem ekranına göndermek CLAUDE.md §3'ün "zorlama yok"
 * ilkesine ve §12 K45/K53'e aykırıydı.
 */
object NudgeIzni {

    fun verildiMi(baglam: Context): Boolean = Settings.canDrawOverlays(baglam)

    /** Doğrudan uygulamanın satırına giden intent — bu izinde garantili. */
    private fun ayarIntent(baglam: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${baglam.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Samsung ve diğer OEM ROM'ları foreground service'i bile arka planda
     * öldürebiliyor; muafiyet olmadan nudge servisi güvenilir çalışmıyor.
     */
    private fun pilMuafiyetiVarMi(baglam: Context): Boolean =
        baglam.getSystemService<PowerManager>()
            ?.isIgnoringBatteryOptimizations(baglam.packageName) == true

    private fun pilMuafiyetiIntenti(baglam: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${baglam.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Sırada hangi izin ekranı var — hepsi verildiyse `null`.
     *
     * **Bir çağrıda en fazla bir ekran.** Kullanıcıyı art arda iki sistem
     * ekranına göndermek, ikincisini görmeden ilkinden geri dönmesine yol
     * açıyor. Anahtarı bir daha açmak sıradakini getiriyor.
     *
     * Çözümlenemeyen bir intent `null` dönüyor: var olmayan bir ekrana
     * `startActivity` çağırmak `ActivityNotFoundException` ile çöker.
     */
    fun eksikIzinIntenti(baglam: Context): Intent? {
        val intent = when (
            siradakiIzin(
                overlayVar = verildiMi(baglam),
                pilMuafiyetiVar = pilMuafiyetiVarMi(baglam),
            )
        ) {
            EksikNudgeIzni.OVERLAY -> ayarIntent(baglam)
            EksikNudgeIzni.PIL_MUAFIYETI -> pilMuafiyetiIntenti(baglam)
            null -> return null
        }
        return intent.takeIf { it.resolveActivity(baglam.packageManager) != null }
    }
}

/** [NudgeIzni.eksikIzinIntenti]'nin kullanıcıdan isteyebileceği izinler. */
enum class EksikNudgeIzni { OVERLAY, PIL_MUAFIYETI }

/**
 * Hangi izin sırada — **saf fonksiyon**, emülatörsüz test edilebilir
 * (`OnPlanTespit` / `NefesFazHesapla` ile aynı desen, CLAUDE.md §6).
 *
 * Overlay önce geliyor: o izin olmadan popup hiç çizilemiyor, yani
 * özelliğin kendisi yok. Pil muafiyeti ise güvenilirliği artırıyor —
 * eksikken özellik çalışıyor, yalnızca OEM'in servisi öldürme riski var.
 */
internal fun siradakiIzin(overlayVar: Boolean, pilMuafiyetiVar: Boolean): EksikNudgeIzni? = when {
    !overlayVar -> EksikNudgeIzni.OVERLAY
    !pilMuafiyetiVar -> EksikNudgeIzni.PIL_MUAFIYETI
    else -> null
}
