package tr.org.yesilay.yesiliz.ui.ekran.lig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import tr.org.yesilay.yesiliz.ui.binlik
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.KullaniciAvatari
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Lig — `docs/tasarim-referans.md §2 / 10`.
 *
 * Ligde **düşme cezası puan silmiyor**; yalnızca bir alt gruba geçiliyor.
 * Kazanılan puan hiçbir koşulda geri alınmaz (CLAUDE.md §4).
 */
@Composable
fun LigEkran(
    modifier: Modifier = Modifier,
    viewModel: LigViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    // İçerik zemini beyaz: sıralama satırları `Yesil50` dolgulu, zemin de
    // `Yesil50` olsaydı satırlar kaybolurdu.
    YesilBaslikliEkran(baslik = "Lig", icerikZemini = Beyaz, modifier = modifier) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Bosluk.S),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = Olcu.BolumArasi,
            ),
        ) {
            item { LigBasligi(durum) }
            items(durum.siralama, key = { it.sira }) { satir ->
                LigSatiri(satir)
            }
        }
    }
}

@Composable
private fun LigBasligi(durum: LigDurum) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Olcu.KartIci),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(Olcu.AvatarBuyuk)
                .clip(CircleShape)
                .background(Beyaz)
                .border(Olcu.ButonKenarligi, YesilAna, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Park,
                contentDescription = null,
                tint = YesilAna,
                modifier = Modifier.size(Olcu.IkonRozeti),
            )
        }
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = durum.ligAdi,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Text(
            text = "${durum.kalanGun} gün kaldı",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinUcuncul,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Row(horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik)) {
            Cip(
                metin = "İlk ${durum.yukselen} yükselir",
                ikon = Icons.Filled.ArrowUpward,
                zemin = YesilNane,
            )
            Cip(
                metin = "Son ${durum.dusen} düşer",
                ikon = Icons.Filled.ArrowDownward,
                zemin = Yesil50,
                renk = MetinUcuncul,
            )
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))
    }
}

@Composable
private fun LigSatiri(satir: LigSatiriModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Buton))
            .background(if (satir.benMiyim) YesilNane else Yesil50)
            .then(
                if (satir.benMiyim) {
                    Modifier.border(
                        Olcu.ButonKenarligi,
                        YesilAna,
                        RoundedCornerShape(Yaricap.Buton),
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = Olcu.Aralik, vertical = Bosluk.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
    ) {
        Text(
            text = "${satir.sira}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MetinBirincil,
            textAlign = TextAlign.Center,
            modifier = Modifier.size(SiraGenisligi),
        )
        KullaniciAvatari(isim = satir.isim)
        Text(
            text = satir.isim,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (satir.benMiyim) FontWeight.Bold else FontWeight.Medium,
            color = MetinBirincil,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = satir.puan.binlik(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
    }
}

private val SiraGenisligi = Bosluk.L
