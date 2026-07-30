package tr.org.yesilay.yesiliz.nudge

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * `SYSTEM_ALERT_WINDOW` izninin durumu.
 *
 * `PACKAGE_USAGE_STATS` gibi normal çalışma zamanı izni **değil** —
 * `requestPermissions()` ile istenemez, kullanıcı Ayarlar → "diğer
 * uygulamaların üzerinde göster" ekranından elle vermek zorunda
 * (`data/kullanim/KullanimIzni`'nin aynısı, farklı izin).
 */
object NudgeIzni {

    fun verildiMi(baglam: Context): Boolean = Settings.canDrawOverlays(baglam)

    /** Doğrudan uygulamanın satırına giden intent — bu izinde garantili. */
    fun ayarIntent(baglam: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${baglam.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Bazı üretici ROM'larında bu ekran farklı davranabiliyor; yönlendirmeden önce kontrol. */
    fun ayarEkraniVarMi(baglam: Context): Boolean =
        ayarIntent(baglam).resolveActivity(baglam.packageManager) != null
}
