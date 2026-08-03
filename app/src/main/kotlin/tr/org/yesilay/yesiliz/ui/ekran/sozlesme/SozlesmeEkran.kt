package tr.org.yesilay.yesiliz.ui.ekran.sozlesme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.kalinIsle
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.YuzeyKirik

/**
 * Sözleşme metni — Kullanım Koşulları, Gizlilik Politikası, açık kaynak.
 *
 * Üçü tek ekranı paylaşıyor: yapıları aynı (başlık + paragraflar) ve
 * içerik `assets/sozlesmeler.json`'dan geliyor. Bilgi Bankası yazı ekranıyla
 * aynı desen.
 *
 * Bu ekranın var olması Y-6'nın çekirdeği: kayıt ekranı bu metinlerin
 * onaylanmasını **zorunlu** tutuyordu ama okumanın hiçbir yolu yoktu.
 */
@Composable
fun SozlesmeEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SozlesmeViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    val sozlesme = durum.sozlesme

    YesilBaslikliEkran(
        baslik = sozlesme?.baslik ?: "Sözleşme",
        onGeri = onGeri,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
        ) {
            if (sozlesme == null) {
                if (!durum.yukleniyor) {
                    BosDurum(
                        baslik = "Metin bulunamadı",
                        aciklama = "Profil › Hakkında'dan tekrar deneyebilirsin.",
                    )
                }
                return@Column
            }

            Yigin {
                Text(
                    text = "Son güncelleme: ${sozlesme.guncelleme}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinUcuncul,
                )

                // Metnin hukuki incelemeden geçmediği ekranın BAŞINDA
                // yazıyor. Sonuna koymak, okumadan kabul eden kullanıcının
                // hiç görmemesi demek olurdu.
                if (sozlesme.uyari.isNotBlank()) {
                    Text(
                        text = sozlesme.uyari,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinIkincil,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Yaricap.Kart))
                            .background(YuzeyKirik)
                            .padding(Olcu.KartIci),
                    )
                }

                sozlesme.govde.forEach { paragraf ->
                    val basliksa = paragraf.startsWith("**") && paragraf.endsWith("**")
                    Text(
                        text = kalinIsle(paragraf),
                        style = if (basliksa) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        fontWeight = if (basliksa) FontWeight.Bold else null,
                        color = if (basliksa) MetinBirincil else MetinIkincil,
                    )
                }
            }
        }
    }
}
