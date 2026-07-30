package tr.org.yesilay.yesiliz.ui.ekran.lig

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.PermContactCalendar
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.domain.lig.OnerilenKisi
import tr.org.yesilay.yesiliz.ui.bilesen.BolumBasligi
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.KullaniciAvatari
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Ayrac
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.Gradyan
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/** Arkadaş ekle — `docs/tasarim-referans.md §2 / 36`. */
@Composable
fun ArkadasEkleEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArkadasEkleViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    val context = LocalContext.current

    YesilBaslikliEkran(
        baslik = "Arkadaş ekle",
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
                TanitimKarti()

                DavetKoduSeridi(
                    kod = durum.davetKodu,
                    kopyalandi = durum.kopyalandi,
                    onKopyala = {
                        panoyaKopyala(context, durum.davetKodu)
                        viewModel.kopyalandiIsaretle()
                    },
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
                    EylemKutusu(
                        ikon = Icons.Filled.MailOutline,
                        etiket = "Mesajla paylaş",
                        modifier = Modifier.weight(1f),
                        onTikla = { davetPaylas(context, durum.davetKodu) },
                    )
                    EylemKutusu(
                        ikon = Icons.Filled.Share,
                        etiket = "Bağlantı paylaş",
                        modifier = Modifier.weight(1f),
                        onTikla = { davetPaylas(context, durum.davetKodu) },
                    )
                    // Rehber verisi HİÇ toplanmıyor (CLAUDE.md §4). Tasarımda
                    // bu buton var ama izin istemek, toplamayacağımız bir
                    // veriye erişim istemek olurdu — devre dışı ve sebebi
                    // hemen altında yazıyor.
                    EylemKutusu(
                        ikon = Icons.Filled.PermContactCalendar,
                        etiket = "Kişilerden bul",
                        modifier = Modifier.weight(1f),
                        etkin = false,
                        onTikla = {},
                    )
                }
                Text(
                    text = "Rehberine erişmiyoruz; kişi listesi hiçbir zaman " +
                        "okunmuyor. Davet kodunu paylaşman yeterli.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MetinUcuncul,
                )

                BolumBasligi("Önerilen kişiler")
                durum.onerilenler.forEach { kisi ->
                    OnerilenSatiri(
                        kisi = kisi,
                        onEkle = { viewModel.davetGonder(kisi.isim) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TanitimKarti() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .background(Gradyan.PuanKarti)
            .padding(Olcu.KartIci),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(Olcu.IkonRozeti)
                .clip(RoundedCornerShape(Yaricap.Cip))
                .background(Beyaz.copy(alpha = RozetSaydamligi)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.Groups, contentDescription = null, tint = Beyaz)
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "Birlikte daha güçlüsünüz",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Beyaz,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Arkadaşlarınla aynı ligde yarış, birbirinizi motive edin.",
            style = MaterialTheme.typography.bodyLarge,
            color = Beyaz.copy(alpha = AltMetinSaydamligi),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DavetKoduSeridi(kod: String, kopyalandi: Boolean, onKopyala: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .background(Yesil50)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Davet kodun",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
            )
            Text(
                text = kod,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
        }
        Cip(
            metin = if (kopyalandi) "Kopyalandı" else "Kopyala",
            zemin = YesilAna,
            renk = Beyaz,
            onTikla = onKopyala,
        )
    }
}

@Composable
private fun EylemKutusu(
    ikon: ImageVector,
    etiket: String,
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
    etkin: Boolean = true,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Yaricap.Buton))
            .border(
                width = Olcu.KartKenarligi,
                color = Ayrac,
                shape = RoundedCornerShape(Yaricap.Buton),
            )
            .clickable(enabled = etkin, onClick = onTikla)
            .padding(vertical = Olcu.Aralik, horizontal = Bosluk.S),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = ikon,
            contentDescription = null,
            tint = if (etkin) YesilAna else MetinUcuncul,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodyMedium,
            color = if (etkin) MetinBirincil else MetinUcuncul,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OnerilenSatiri(kisi: OnerilenKisi, onEkle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Bosluk.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KullaniciAvatari(isim = kisi.isim, zemin = Yesil50, metinRengi = MetinBirincil)
        Spacer(modifier = Modifier.width(Olcu.Aralik))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = kisi.isim,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MetinBirincil,
            )
            Text(
                text = kisi.altMetin,
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
            )
        }
        if (kisi.davetGonderildiMi) {
            Cip(metin = "Davet gönderildi", zemin = Yesil50, renk = MetinIkincil)
        } else {
            Cip(metin = "Ekle", zemin = Beyaz, renk = YesilAna, onTikla = onEkle)
        }
    }
}

private fun panoyaKopyala(baglam: Context, kod: String) {
    val pano = baglam.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    pano.setPrimaryClip(ClipData.newPlainText("Yeşilİz davet kodu", kod))
}

/** Android'in kendi paylaş sayfası — uygulama içi bir paylaşım listesi yok. */
private fun davetPaylas(baglam: Context, kod: String) {
    val niyet = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "Yeşilİz'de birlikte yarışalım. Davet kodum: $kod",
        )
    }
    baglam.startActivity(Intent.createChooser(niyet, "Daveti paylaş"))
}

private const val RozetSaydamligi = 0.15f
private const val AltMetinSaydamligi = 0.8f
