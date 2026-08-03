package tr.org.yesilay.yesiliz.ui.ekran.ekransiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.SecenekKarti
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * "Ekransız ne yaptın?" — `debug.md` İ-1.
 *
 * Kart seçimi; **serbest metin yok** (CLAUDE.md §4). Seçim hiçbir yere
 * yazılmıyor, yalnızca görev tamamlanıyor: amaç veri toplamak değil,
 * kullanıcının ekran dışında geçirdiği zamanı kendisine görünür kılmak.
 * Ekranda da böyle yazıyor.
 */
@Composable
fun EkransizEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EkransizViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    YesilBaslikliEkran(baslik = "Ekransız", onGeri = onGeri, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
        ) {
            Yigin {
                Text(
                    text = if (durum.kaydedildi) "Kaydedildi" else "Bugün ekransız ne yaptın?",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Text(
                    text = if (durum.kaydedildi) {
                        "Küçük şeyler de sayılıyor. Yarın tekrar görüşürüz."
                    } else {
                        "Birini seç. Ne seçtiğini kaydetmiyoruz — bu soru " +
                            "senin için, bizim için değil."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                )

                if (durum.kaydedildi) {
                    Cip(
                        metin = if (durum.puanYazildi) {
                            "+${Gorev.EKRANSIZ.puan} puan"
                        } else {
                            "Bugün zaten kaydettin"
                        },
                        zemin = YesilNane,
                    )
                    BirincilButon(metin = "Kapat", onTikla = onGeri)
                    return@Yigin
                }

                EkransizSecenek.entries.chunked(SUTUN).forEach { satir ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik)) {
                        satir.forEach { secenek ->
                            SecenekKarti(
                                ikon = secenek.ikon,
                                baslik = secenek.etiket,
                                aciklama = "",
                                secili = durum.secilen == secenek,
                                onTikla = { viewModel.sec(secenek) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Tek kalan kartın satırı doldurmasını engelliyor.
                        repeat(SUTUN - satir.size) {
                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Text(
                    text = "Bu seçim cihazından çıkmıyor ve saklanmıyor.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MetinUcuncul,
                )
                BirincilButon(
                    metin = "Kaydet",
                    onTikla = viewModel::kaydet,
                    etkin = durum.secilen != null,
                )
            }
        }
    }
}

private const val SUTUN = 2
