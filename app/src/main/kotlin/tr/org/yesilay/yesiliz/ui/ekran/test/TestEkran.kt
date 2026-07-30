package tr.org.yesilay.yesiliz.ui.ekran.test

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.domain.test.FarkindalikTesti
import tr.org.yesilay.yesiliz.domain.test.TestSecenegi
import tr.org.yesilay.yesiliz.domain.test.TestSonuc
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.IkincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.IlerlemeCubugu
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Ayrac
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Kendini Test Et — 5 soruluk kısa farkındalık testi.
 *
 * Teslim edilen tasarımda karşılığı yok; **04 · Anket** ekranının kart +
 * seçenek dili uyarlandı (aynı `Kart`, aynı ilerleme çubuğu, aynı buton).
 * Fark: 6'lı Likert yerine 3'lü seçim — bu ölçek değil, kısa bir
 * öz-gözlem (`domain/test/FarkindalikTesti`).
 */
@Composable
fun TestEkran(
    onGeri: () -> Unit,
    onBilgiYazisi: (String) -> Unit,
    onNefes: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TestViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    YesilBaslikliEkran(
        baslik = "Kendini test et",
        onGeri = onGeri,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
        ) {
            val sonuc = durum.sonuc
            if (sonuc == null) {
                Sorular(durum = durum, onYanit = viewModel::yanitla, onBitir = viewModel::bitir)
            } else {
                Sonuc(
                    sonuc = sonuc,
                    oncekiPuan = durum.oncekiPuan,
                    onBilgiYazisi = { onBilgiYazisi(sonuc.onerilenYaziId) },
                    onNefes = onNefes,
                    onTekrar = viewModel::bastanBasla,
                    onKapat = onGeri,
                )
            }
        }
    }
}

@Composable
private fun Sorular(
    durum: TestDurum,
    onYanit: (Int, TestSecenegi) -> Unit,
    onBitir: () -> Unit,
) {
    Yigin {
        Text(
            text = "Son bir haftayı düşün.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Text(
            text = "Bu kısa test bir tanı aracı değil. Amacı, kendi " +
                "gözlemlerini bir arada görmen.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        IlerlemeCubugu(oran = durum.ilerleme)

        FarkindalikTesti.SORULAR.forEachIndexed { indeks, soru ->
            Kart {
                Text(
                    text = soru.metin,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinBirincil,
                )
                Spacer(modifier = Modifier.height(Olcu.Aralik))
                Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
                    TestSecenegi.entries.forEach { secenek ->
                        SecenekDugmesi(
                            etiket = secenek.etiket,
                            secili = durum.yanitlar[indeks] == secenek,
                            onTikla = { onYanit(indeks, secenek) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        BirincilButon(
            metin = "Sonucu gör",
            onTikla = onBitir,
            etkin = durum.tamamMi,
        )
        if (!durum.tamamMi) {
            Text(
                text = "Beş sorunun tamamını işaretleyince sonuç çıkar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SecenekDugmesi(
    etiket: String,
    secili: Boolean,
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(Olcu.LikertDairesi)
            .clip(RoundedCornerShape(Yaricap.Cip))
            .background(if (secili) YesilAna else Yesil50)
            .border(
                width = Olcu.KartKenarligi,
                color = if (secili) YesilAna else Ayrac,
                shape = RoundedCornerShape(Yaricap.Cip),
            )
            .clickable(onClick = onTikla),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (secili) FontWeight.SemiBold else FontWeight.Normal,
            color = if (secili) Beyaz else MetinIkincil,
        )
    }
}

@Composable
private fun Sonuc(
    sonuc: TestSonuc,
    oncekiPuan: Int?,
    onBilgiYazisi: () -> Unit,
    onNefes: () -> Unit,
    onTekrar: () -> Unit,
    onKapat: () -> Unit,
) {
    Yigin {
        Text(
            text = sonuc.seviye.baslik,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Text(
            text = sonuc.ozet,
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )

        Kart {
            Text(
                text = "${sonuc.hamPuan} / ${FarkindalikTesti.AZAMI_PUAN}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
            Text(
                text = "işaretlediğin sıklık toplamı",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
            )
            // Karşılaştırma yalnızca daha önce çözülmüşse — ilk seferde
            // "geçen sefere göre" demek anlamsız.
            oncekiPuan?.let { onceki ->
                Spacer(modifier = Modifier.height(Bosluk.S))
                Text(
                    text = karsilastirma(sonuc.hamPuan, onceki),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinIkincil,
                )
            }
        }

        if (sonuc.ipuclari.isNotEmpty()) {
            Text(
                text = "Öne çıkanlar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
            sonuc.ipuclari.forEach { ipucu ->
                Kart {
                    Text(
                        text = ipucu,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MetinBirincil,
                    )
                }
            }
        }

        Kart(onTikla = onBilgiYazisi) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IkonRozeti(ikon = Icons.AutoMirrored.Filled.MenuBook)
                Spacer(modifier = Modifier.height(Olcu.Aralik))
                Column(modifier = Modifier.padding(start = Olcu.Aralik)) {
                    Text(
                        text = "Bu konuda okuyabilirsin",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MetinBirincil,
                    )
                    Text(
                        text = "Bilgi Bankası'ndan sana en yakın yazı",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinIkincil,
                    )
                }
            }
        }

        Kart(onTikla = onNefes) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IkonRozeti(ikon = Icons.Filled.SelfImprovement)
                Column(modifier = Modifier.padding(start = Olcu.Aralik)) {
                    Text(
                        text = "İki dakikan var mı?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MetinBirincil,
                    )
                    Text(
                        text = "Kutu nefesi ile kısa bir mola",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinIkincil,
                    )
                }
            }
        }

        BirincilButon(metin = "Bitir", onTikla = onKapat)
        IkincilButon(metin = "Tekrar çöz", onTikla = onTekrar)
        MetinBaglanti(
            metin = "Bu bir tanı değildir",
            onTikla = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * İki test arasındaki fark — **artış da azalış da yargısız** anlatılıyor.
 * "Kötüleşmişsin" demek yerine ne değiştiği söyleniyor (CLAUDE.md §4).
 */
private fun karsilastirma(simdiki: Int, onceki: Int): String = when {
    simdiki < onceki -> "Geçen sefere göre ${onceki - simdiki} puan daha az işaretledin."
    simdiki > onceki -> "Geçen sefere göre ${simdiki - onceki} puan daha fazla işaretledin."
    else -> "Geçen seferkiyle aynı."
}
