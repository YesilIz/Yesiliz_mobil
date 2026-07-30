package tr.org.yesilay.yesiliz.ui.ekran.bilgi

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.domain.bilgi.BilgiYazisi
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Bilgi Bankası — liste.
 *
 * Teslim edilen tasarımda karşılığı yok; **12 · Profil** ekranının kart-liste
 * dili ve `YesilBaslikliEkran` çerçevesi kullanıldı. İçerik
 * `assets/bilgi_bankasi.json`, çevrimdışı çalışıyor.
 */
@Composable
fun BilgiEkran(
    onGeri: () -> Unit,
    onYazi: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BilgiViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    YesilBaslikliEkran(baslik = "Bilgi Bankası", onGeri = onGeri, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
        ) {
            Yigin {
                Text(
                    text = "Ekranla ilişkin hakkında, korkutmadan.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Text(
                    text = "Kısa yazılar. Hepsi çevrimdışı okunabilir.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Bosluk.S),
                ) {
                    durum.kategoriler.forEach { kategori ->
                        val secili = durum.seciliKategori == kategori
                        Cip(
                            metin = kategori,
                            zemin = if (secili) YesilAna else tr.org.yesilay.yesiliz.ui.tema.Yesil50,
                            renk = if (secili) Beyaz else MetinBirincil,
                            onTikla = { viewModel.kategoriSecildi(kategori) },
                        )
                    }
                }

                if (!durum.yukleniyor && durum.gorunenYazilar.isEmpty()) {
                    BosDurum(
                        baslik = "Bu kategoride yazı yok",
                        aciklama = "Çipe tekrar dokunarak tüm yazılara dönebilirsin.",
                    )
                }

                durum.gorunenYazilar.forEach { yazi ->
                    YaziSatiri(yazi = yazi, onTikla = { onYazi(yazi.id) })
                }
            }
        }
    }
}

@Composable
private fun YaziSatiri(yazi: BilgiYazisi, onTikla: () -> Unit) {
    Kart(onTikla = onTikla) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = yazi.baslik,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Spacer(modifier = Modifier.height(Bosluk.Xs))
                Text(
                    text = yazi.ozet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinIkincil,
                )
                Spacer(modifier = Modifier.height(Bosluk.S))
                Text(
                    text = "${yazi.kategori} · ${yazi.okumaDk} dk okuma",
                    style = MaterialTheme.typography.labelMedium,
                    color = MetinUcuncul,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MetinUcuncul,
            )
        }
    }
}
