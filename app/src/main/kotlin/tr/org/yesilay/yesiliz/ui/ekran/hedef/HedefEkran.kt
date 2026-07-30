package tr.org.yesilay.yesiliz.ui.ekran.hedef

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.BolumBasligi
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.bilesen.veri.DaireselGosterge
import tr.org.yesilay.yesiliz.ui.sureMetni
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

/** Günlük hedef — `docs/tasarim-referans.md §2 / 34`. */
@Composable
fun HedefEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HedefViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    YesilBaslikliEkran(
        baslik = "Günlük hedef",
        onGeri = onGeri,
        icerikZemini = Beyaz,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
        ) {
            Yigin {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    DaireselGosterge(oran = durum.oran) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = durum.secilenDk.sureMetni(),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = MetinBirincil,
                            )
                            Text(
                                text = "günlük ekran hedefi",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MetinIkincil,
                            )
                        }
                    }
                }

                // Bugünün kullanımı yalnızca veri varsa — izin verilmemiş ya
                // da ilk gün olan kullanıcıya "%0 kullandın" demek yanlış.
                durum.bugunDk?.let { bugun ->
                    Text(
                        text = "Bugün ${bugun.sureMetni()} kullandın · %${durum.yuzde}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MetinIkincil,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                AyarSeridi(
                    deger = durum.secilenDk,
                    onAzalt = viewModel::azalt,
                    onArttir = viewModel::arttir,
                )

                BolumBasligi("Hızlı seçim")
                Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
                    HedefSinir.hizliSecim(durum.kayitliHedefDk).forEach { dakika ->
                        HizliCip(
                            dakika = dakika,
                            secili = dakika == durum.secilenDk,
                            onTikla = { viewModel.ayarla(dakika) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                BilgiSeridi(ebeveynAktifMi = durum.ebeveynAktifMi)

                BirincilButon(
                    metin = if (durum.kaydedildi) "Kaydedildi" else "Hedefi kaydet",
                    onTikla = viewModel::kaydet,
                    etkin = durum.degistiMi,
                )
            }
        }
    }
}

/** `−` (beyaz) · değer · `+` (YesilAna) — tasarım 34'teki şerit. */
@Composable
private fun AyarSeridi(deger: Int, onAzalt: () -> Unit, onArttir: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .background(Yesil50)
            .padding(Bosluk.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AyarDugmesi(
            ikon = Icons.Filled.Remove,
            aciklama = "Azalt",
            zemin = Beyaz,
            renk = MetinBirincil,
            etkin = deger > HedefSinir.EN_AZ,
            onTikla = onAzalt,
        )
        Text(
            text = uzunSure(deger),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        AyarDugmesi(
            ikon = Icons.Filled.Add,
            aciklama = "Arttır",
            zemin = YesilAna,
            renk = Beyaz,
            etkin = deger < HedefSinir.EN_COK,
            onTikla = onArttir,
        )
    }
}

@Composable
private fun AyarDugmesi(
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    aciklama: String,
    zemin: androidx.compose.ui.graphics.Color,
    renk: androidx.compose.ui.graphics.Color,
    etkin: Boolean,
    onTikla: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Olcu.IkonRozeti)
            .clip(CircleShape)
            .background(if (etkin) zemin else Ayrac)
            .clickable(enabled = etkin, onClick = onTikla),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ikon,
            contentDescription = aciklama,
            tint = if (etkin) renk else MetinUcuncul,
        )
    }
}

@Composable
private fun HizliCip(
    dakika: Int,
    secili: Boolean,
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(Olcu.LikertDairesi)
            .clip(RoundedCornerShape(Yaricap.Cip))
            .background(if (secili) Yesil50 else Beyaz)
            .border(
                width = Olcu.KartKenarligi,
                color = if (secili) YesilAna else Ayrac,
                shape = RoundedCornerShape(Yaricap.Cip),
            )
            .clickable(onClick = onTikla),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = dakika.sureMetni(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (secili) FontWeight.SemiBold else FontWeight.Normal,
            color = if (secili) YesilAna else MetinIkincil,
        )
    }
}

/**
 * Bilgi şeridi.
 *
 * Ebeveyn cümlesi **yalnızca eşleştirme varken** — eşleşmemiş kullanıcıya
 * "ebeveynine bildirilir" demek, olmayan bir gözetim hissi yaratırdı.
 */
@Composable
private fun BilgiSeridi(ebeveynAktifMi: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Buton))
            .background(Yesil50)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = YesilAna,
        )
        Spacer(modifier = Modifier.width(Olcu.Aralik))
        Text(
            text = if (ebeveynAktifMi) {
                "Hedefini aştığında ebeveynine bilgilendirme gönderilir. " +
                    "Değişiklikler yarından itibaren geçerli olur."
            } else {
                "Değişiklikler yarından itibaren geçerli olur."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MetinBirincil,
        )
    }
}

/** "4 saat 45 dakika" — şeritteki uzun biçim. */
private fun uzunSure(dakika: Int): String {
    val saat = dakika / 60
    val kalan = dakika % 60
    return when {
        saat == 0 -> "$kalan dakika"
        kalan == 0 -> "$saat saat"
        else -> "$saat saat $kalan dakika"
    }
}
