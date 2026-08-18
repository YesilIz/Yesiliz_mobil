package tr.org.yesilay.yesiliz.ui.ekran.profil.icerik

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import tr.org.yesilay.yesiliz.domain.yedam.Yedam
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.YedamAraButonu
import tr.org.yesilay.yesiliz.ui.ekran.profil.AltSayfaBasligi
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Alt sayfa 27 — YEDAM hakkında.
 *
 * **Sadece bilgilendirme.** Yönlendirme/rıza akışı ayrı bir ekran
 * (`ui/ekran/yedam/`), yalnızca yüksek bantta ve kullanıcı onayıyla açılıyor
 * (`docs/tasarim-referans.md §2 / 27` notu).
 */
@Composable
internal fun YedamHakkindaIcerik(onKapat: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
    ) {
        AltSayfaBasligi("YEDAM hakkında", onKapat)
        Spacer(modifier = Modifier.height(Bosluk.S))

        IkonRozeti(
            ikon = Icons.Filled.SupportAgent,
            zemin = YesilKoyu,
            renk = Beyaz,
            sekil = RoundedCornerShape(Yaricap.Cip),
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Text(
            text = "YEDAM (Yeşilay Danışmanlık Merkezi), bağımlılık alanında " +
                "ücretsiz danışmanlık veriyor. Başvurmak için bir tanı almış " +
                "olman gerekmiyor.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Yaricap.Buton))
                .background(YesilNane)
                .padding(Olcu.KartIci),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.SupportAgent,
                contentDescription = null,
                tint = YesilAna,
            )
            Spacer(modifier = Modifier.width(Olcu.Aralik))
            Text(
                text = "${Yedam.NUMARA} — ücretsiz ve gizli YEDAM Hattı",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MetinBirincil,
            )
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Text(
            text = "Görüşmeler gizlilik ilkesiyle yürütülür.",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
        Text(
            text = "Görüşmeyi YEDAM uzmanları yürütür.",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
        Text(
            text = Yedam.CALISMA_SAATLERI,
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        YedamAraButonu()
    }
}
