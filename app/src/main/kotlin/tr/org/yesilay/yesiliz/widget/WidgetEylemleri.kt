package tr.org.yesilay.yesiliz.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException
import tr.org.yesilay.yesiliz.MainActivity
import tr.org.yesilay.yesiliz.domain.gorev.UykuAraligi

/**
 * Widget dokunma eylemleri.
 *
 * `actionStartActivity` yerine elle `Intent`: `MainActivity` tek Activity ve
 * `SINGLE_TOP` bayrağıyla açılıyor ki widget'a her dokunuşta yeni bir örnek
 * yığılmasın.
 */
internal fun uygulamayiAc(): Action = actionRunCallback<UygulamaAcEylemi>()

class UygulamaAcEylemi : ActionCallback {
    override suspend fun onAction(
        baglam: Context,
        glanceId: GlanceId,
        parametreler: ActionParameters,
    ) {
        baglam.startActivity(
            Intent(baglam, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
    }
}

/** "Bugünün sorusu" widget'ındaki seçeneklerden birine dokunma. */
internal val UykuAnahtari = ActionParameters.Key<String>("uyku_araligi")

internal fun uykuCevapla(aralik: UykuAraligi): Action =
    actionRunCallback<UykuCevapEylemi>(actionParametersOf(UykuAnahtari to aralik.anahtar))

/**
 * Uyku beyanını **uygulamayı açmadan** kaydeder.
 *
 * Etkileşimli widget'ın bütün değeri bu: kullanıcı tek dokunuşla cevap
 * veriyor, görev puanı yazılıyor ve widget kendini güncelliyor.
 */
class UykuCevapEylemi : ActionCallback {
    override suspend fun onAction(
        baglam: Context,
        glanceId: GlanceId,
        parametreler: ActionParameters,
    ) {
        val anahtar = parametreler[UykuAnahtari] ?: return
        val aralik = UykuAraligi.anahtardan(anahtar) ?: return

        WidgetVerisi.uykuCevapla(baglam, aralik)
        GununSorusuWidget().updateAll(baglam)
    }
}

/**
 * Tüm widget'ları tazeler.
 *
 * Gece worker'ı özet yazdıktan sonra ve görev tamamlandığında çağrılıyor;
 * widget'ın kendi periyodik güncellemesini beklemek, kullanıcıya saatlerce
 * eski sayı göstermek demek.
 *
 * Widget hiç eklenmemişse `updateAll` sessizce hiçbir şey yapmıyor.
 */
suspend fun widgetlariTazele(baglam: Context) {
    try {
        EkranSuresiWidget().updateAll(baglam)
        GununSorusuWidget().updateAll(baglam)
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: Exception) {
        Log.w("YesilizWidget", "Widget'lar tazelenemedi", hata)
    }
}
