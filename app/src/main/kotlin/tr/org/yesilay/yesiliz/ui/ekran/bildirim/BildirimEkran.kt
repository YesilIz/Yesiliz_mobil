package tr.org.yesilay.yesiliz.ui.ekran.bildirim

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.ui.bilesen.BolumBasligi
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Ayrac
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/** Bildirimler — `docs/tasarim-referans.md §2 / 35`. */
@Composable
fun BildirimEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BildirimViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    YesilBaslikliEkran(
        baslik = "Bildirimler",
        onGeri = onGeri,
        icerikZemini = Beyaz,
        sagEylem = {
            // Okunmamış yoksa eylem de yok — tıklanınca hiçbir şey olmayan
            // bir bağlantı göstermek, düzeltmeye çalıştığımız sorunun aynısı.
            if (durum.okunmamisVarMi) {
                MetinBaglanti(metin = "Tümünü oku", onTikla = viewModel::tumunuOku)
            }
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
        ) {
            if (!durum.yukleniyor && durum.gruplar.isEmpty()) {
                BosDurum(
                    baslik = "Henüz bildirim yok",
                    aciklama = "Görev tamamladıkça, hedefini tutturdukça ve ligde " +
                        "sıran değiştikçe buraya düşecek.",
                )
                return@Column
            }

            durum.gruplar.forEach { grup ->
                BolumBasligi(metin = grup.grup.baslik)
                Spacer(modifier = Modifier.height(Bosluk.S))
                Yigin(aralik = Bosluk.S) {
                    grup.satirlar.forEach { satir -> BildirimSatiri(satir) }
                }
                Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            }
        }
    }
}

@Composable
private fun BildirimSatiri(satir: BildirimSatiriModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .background(if (satir.okundu) Beyaz else Yesil50)
            .then(
                if (satir.okundu) {
                    Modifier.border(
                        width = Olcu.KartKenarligi,
                        color = Ayrac,
                        shape = RoundedCornerShape(Yaricap.Kart),
                    )
                } else {
                    Modifier
                },
            )
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.Top,
    ) {
        IkonRozeti(ikon = satir.ikon, zemin = Beyaz)
        Spacer(modifier = Modifier.width(Olcu.Aralik))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = satir.baslik,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
            Spacer(modifier = Modifier.height(Bosluk.Xs))
            Text(
                text = satir.govde,
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
            )
            Spacer(modifier = Modifier.height(Bosluk.Xs))
            Text(
                text = satir.zamanEtiketi,
                style = MaterialTheme.typography.labelMedium,
                color = MetinUcuncul,
            )
        }
        if (!satir.okundu) {
            Box(
                modifier = Modifier
                    .size(NoktaBoyu)
                    .clip(RoundedCornerShape(NoktaBoyu / 2))
                    .background(YesilAna),
            )
        }
    }
}

private val NoktaBoyu: Dp = Bosluk.S
