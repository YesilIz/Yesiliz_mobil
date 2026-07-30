package tr.org.yesilay.yesiliz.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Ekranı açık tutar — yalnızca [aktif] iken.
 *
 * Aktif yürüyüş ve nefes gibi "sayaç işliyor, kullanıcı ekrana bakmıyor
 * olabilir" ekranları için. `FLAG_KEEP_SCREEN_ON` bileşen ekrandan
 * ayrıldığında **mutlaka** kaldırılıyor; kaldırılmazsa bayrak pencerede
 * kalır ve uygulamanın geri kalanı boyunca ekran hiç sönmez.
 *
 * `WAKE_LOCK` izni istemiyor — bu bayrak izin gerektirmiyor ve yalnızca
 * pencere görünürken etkili.
 */
@Composable
fun EkranAcikTut(aktif: Boolean) {
    val baglam = LocalContext.current
    DisposableEffect(aktif) {
        val pencere = (baglam as? Activity)?.window
        if (aktif) {
            pencere?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            pencere?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            pencere?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
