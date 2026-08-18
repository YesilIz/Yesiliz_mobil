package tr.org.yesilay.yesiliz.ui.ekran.profil.icerik

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import tr.org.yesilay.yesiliz.domain.sozlesme.SssMaddesi
import tr.org.yesilay.yesiliz.ui.bilesen.KartAyraci
import tr.org.yesilay.yesiliz.ui.ekran.profil.AltSayfaBasligi
import tr.org.yesilay.yesiliz.ui.kalinIsle
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/*
 * Profil'in saf **içerik** alt sayfaları ayrı dosyalarda (5. tarama G5-024).
 *
 * `ProfilEkran.kt` 1.616 satırdı ve bir ayar satırını değiştirmek için o
 * dosyayı açmak gerekiyordu; alt sayfaların bir kısmı da Profil'in kendi
 * işiyle ilgisizdi. Burada yalnızca `ProfilViewModel`'e HİÇ dokunmayan
 * içerik sayfaları var — CLAUDE.md §12 K11 bozulmuyor: ayrı ViewModel
 * açılmadı, bunlar hâlâ Profil'in alt sayfaları. Veri taşıyan alt sayfalar
 * (Profili Düzenle, Verilerimi sil, Sunucu adresi…) `ProfilEkran.kt`'te kaldı.
 */
/** Alt sayfa 28 — Yardım. Statik SSS, backend gerektirmiyor. */
@Composable
internal fun YardimIcerik(onKapat: () -> Unit, sorular: List<SssMaddesi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
    ) {
        AltSayfaBasligi("Yardım", onKapat)
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Sık sorulan sorular ve destek yolları.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        // Dört soru chevron'lu ve tıklanabilir görünüyordu ama HİÇBİRİNİN
        // cevabı yazılmamıştı (`debug.md` Y-6). Cevaplar artık
        // `assets/sozlesmeler.json`'da; satıra dokununca açılıyor.
        var acikSoru by remember { mutableStateOf<String?>(null) }

        if (sorular.isEmpty()) {
            Text(
                text = "Sorular yükleniyor…",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinUcuncul,
            )
        }
        sorular.forEachIndexed { i, madde ->
            val acik = acikSoru == madde.soru
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { acikSoru = if (acik) null else madde.soru }
                        .padding(vertical = Bosluk.M),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = madde.soru,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MetinBirincil,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (acik) {
                            Icons.Filled.KeyboardArrowDown
                        } else {
                            Icons.AutoMirrored.Filled.KeyboardArrowRight
                        },
                        contentDescription = null,
                        tint = MetinUcuncul,
                    )
                }
                if (acik) {
                    madde.cevap.forEach { paragraf ->
                        Text(
                            text = kalinIsle(paragraf),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetinIkincil,
                            modifier = Modifier.padding(bottom = Bosluk.S),
                        )
                    }
                }
            }
            if (i != sorular.lastIndex) KartAyraci()
        }
    }
}
