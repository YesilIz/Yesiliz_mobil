package tr.org.yesilay.yesiliz.ui.bilesen

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import tr.org.yesilay.yesiliz.domain.yedam.Yedam

/**
 * "115'i ara" düğmesi — üç ekranın paylaştığı **eylem**, yerleşim değil.
 *
 * Profil › YEDAM hakkında, YEDAM onayı ve Ebeveyn panelinin düzeni farklı;
 * ortaklaştırılan tek şey numara, buton metni ve çevirici açma davranışı
 * (5. tarama G5-009).
 *
 * `ACTION_DIAL` numarayı çevirici uygulamasına **yazıyor** ama aramayı
 * kendisi başlatmıyor. `ACTION_CALL` `CALL_PHONE` izni isterdi ve
 * kullanıcının onayı olmadan arama başlatmak bu ekranların işi değil.
 */
@Composable
fun YedamAraButonu(modifier: Modifier = Modifier) {
    val baglam = LocalContext.current
    BirincilButon(
        metin = Yedam.ARA_BUTONU,
        onTikla = {
            baglam.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(Yedam.TEL_URI)))
        },
        modifier = modifier,
    )
}
