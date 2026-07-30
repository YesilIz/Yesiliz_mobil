package tr.org.yesilay.yesiliz.data.kullanim

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/**
 * `PACKAGE_USAGE_STATS` izninin durumu.
 *
 * Bu izin normal çalışma zamanı izni **değil** — `requestPermissions()` ile
 * istenemez. Kullanıcı Ayarlar → Kullanım erişimi ekranından elle vermek
 * zorunda. Bu yüzden onboarding'de (DESIGN.md §5.2) niçin gerektiği
 * anlatılıp kullanıcı o ekrana yönlendiriliyor.
 *
 * Verilip verilmediği `AppOpsManager` üzerinden okunuyor; `checkSelfPermission`
 * bu izinde çalışmaz, daima `DENIED` döner.
 */
object KullanimIzni {

    /** İzin verilmiş mi? */
    fun verildiMi(baglam: Context): Boolean {
        val appOps = baglam.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val sonuc = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            baglam.packageName,
        )
        return sonuc == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Kullanım erişimi ayar ekranını açan intent.
     *
     * Doğrudan uygulamanın satırına gitmenin garantili yolu yok — üreticiler
     * bu ekranı farklı yerlere koyuyor. Genel listeye götürülüp kullanıcıya
     * "listeden Yeşilİz'i seç" denir.
     */
    fun ayarIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Ayar ekranı bu cihazda açılabiliyor mu?
     *
     * Bazı üretici ROM'larında ve Android TV / Wear gibi türevlerde bu
     * ekran yok. Yönlendirmeden önce kontrol edilmeli, yoksa uygulama
     * `ActivityNotFoundException` ile çöker.
     */
    fun ayarEkraniVarMi(baglam: Context): Boolean =
        ayarIntent().resolveActivity(baglam.packageManager) != null
}
