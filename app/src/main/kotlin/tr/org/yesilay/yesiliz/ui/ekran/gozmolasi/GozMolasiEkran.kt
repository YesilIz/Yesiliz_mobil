package tr.org.yesilay.yesiliz.ui.ekran.gozmolasi

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.ui.EkranAcikTut
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.veri.DaireselGosterge
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * 20-20-20 göz molası — `debug.md` İ-1.
 *
 * Ekran açık tutuluyor ([EkranAcikTut]): kullanıcı ekrana bakmıyor ama
 * sayaç dolduğunda geri dönecek; ekranın uykuya geçmesi sayacın
 * bittiğini fark etmesini engellerdi. Nefes ve yürüyüş ekranlarıyla aynı
 * gerekçe.
 */
@Composable
fun GozMolasiEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GozMolasiViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    EkranAcikTut(aktif = durum.basladi && !durum.ilerleme.tamamlandiMi)

    YesilBaslikliEkran(baslik = "Göz molası", onGeri = onGeri, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Büyük yazı tipi ölçeğinde ve küçük ekranda içerik
                // sığmıyor; kaydırmasız bırakmak O-1 ile aynı hata olurdu.
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = when {
                    durum.ilerleme.tamamlandiMi -> "Gözlerin dinlendi"
                    durum.basladi -> "Uzağa bak"
                    else -> "20-20-20 kuralı"
                },
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Bosluk.S))
            Text(
                text = when {
                    durum.ilerleme.tamamlandiMi ->
                        "Bir sonraki molayı 20 dakika sonra vermeyi deneyebilirsin."
                    durum.basladi ->
                        "Ekrandan yaklaşık 6 metre uzaktaki bir şeye bak. " +
                            "Sayaç dolunca haber vereceğiz."
                    else ->
                        "Her 20 dakikada bir, 20 saniye boyunca yaklaşık " +
                            "6 metre uzağa bakmak göz yorgunluğunu azaltıyor."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))

            DaireselGosterge(oran = durum.ilerleme.oran) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${durum.ilerleme.kalanSaniye}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MetinBirincil,
                    )
                    Text(
                        text = "saniye",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinIkincil,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            when {
                durum.ilerleme.tamamlandiMi -> {
                    Cip(
                        metin = if (durum.puanYazildi) {
                            "+${Gorev.GOZ_MOLASI.puan} puan"
                        } else {
                            "Bugünün göz molası puanları dolmuş"
                        },
                        zemin = YesilNane,
                    )
                    Spacer(modifier = Modifier.height(Olcu.Aralik))
                    BirincilButon(metin = "Bitir", onTikla = onGeri)
                    Spacer(modifier = Modifier.height(Bosluk.S))
                    MetinBaglanti(metin = "Bir kez daha", onTikla = viewModel::bastanBaslat)
                }

                durum.basladi -> MetinBaglanti(metin = "Vazgeç", onTikla = onGeri)
                else -> BirincilButon(metin = "Başlat", onTikla = viewModel::basla)
            }
        }
    }
}
