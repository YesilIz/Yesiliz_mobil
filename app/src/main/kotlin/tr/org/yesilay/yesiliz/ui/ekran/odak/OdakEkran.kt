package tr.org.yesilay.yesiliz.ui.ekran.odak

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.veri.DaireselGosterge
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Odak seansı — `debug.md` İ-1.
 *
 * `Gorev.DIJITAL_MOLA`'nın gerçekten doğrulanmış hâli: kullanıcı "yaptım"
 * demiyor, telefonu bırakması gerekiyor. Ekran seans sırasında ön plana
 * dönerse seans bozuluyor.
 *
 * **Ceza yok** (CLAUDE.md §4): bozulan seans puan vermiyor, var olan
 * puanı silmiyor. Ekranda da böyle anlatılıyor — "başaramadın" değil,
 * "bu sefer olmadı, tekrar deneyebilirsin".
 */
@Composable
fun OdakEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OdakViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    // Seans sürerken ekrana dönülmesi = telefonun açılması.
    val sahip = LocalLifecycleOwner.current
    DisposableEffect(sahip, durum.basladi) {
        val gozlemci = LifecycleEventObserver { _, olay ->
            if (olay == Lifecycle.Event.ON_RESUME) viewModel.ekranAcildi()
        }
        // İlk kompozisyonda ON_RESUME zaten gelmiş oluyor; gözlemci
        // yalnızca seans BAŞLADIKTAN sonra takılıyor.
        if (durum.basladi) sahip.lifecycle.addObserver(gozlemci)
        onDispose { sahip.lifecycle.removeObserver(gozlemci) }
    }

    YesilBaslikliEkran(baslik = "Odak seansı", onGeri = onGeri, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Büyük yazı tipi ölçeğinde ve küçük ekranda içerik
                // sığmıyor; kaydırmasız bırakmak O-1 ile aynı hata olurdu.
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                durum.bitti -> Sonuc(durum = durum, onTekrar = viewModel::vazgec, onGeri = onGeri)
                durum.basladi -> Suruyor(durum = durum, onVazgec = viewModel::vazgec)
                else -> Hazirlik(
                    sureDk = durum.sureDk,
                    onSureDegistir = viewModel::sureDegistir,
                    onBasla = viewModel::basla,
                )
            }
        }
    }
}

@Composable
private fun Hazirlik(sureDk: Int, onSureDegistir: () -> Unit, onBasla: () -> Unit) {
    Text(
        text = "Telefonu bırak",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MetinBirincil,
    )
    Spacer(modifier = Modifier.height(Bosluk.S))
    Text(
        text = "Süreyi seç ve başlat. Ekranı açmadan süre dolarsa " +
            "${Gorev.ODAK.puan} puan kazanırsın. Açarsan puan yok — " +
            "ama kaybettiğin bir şey de olmaz.",
        style = MaterialTheme.typography.bodyLarge,
        color = MetinIkincil,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(Olcu.BolumArasi))
    DaireselGosterge(oran = 0f) {
        HalkaIcerik(ana = "$sureDk", alt = "dakika")
    }
    Spacer(modifier = Modifier.height(Olcu.BolumArasi))
    IkincilButon(metin = "Süreyi değiştir", onTikla = onSureDegistir)
    Spacer(modifier = Modifier.height(Bosluk.S))
    BirincilButon(metin = "Başlat", onTikla = onBasla)
}

@Composable
private fun Suruyor(durum: OdakDurum, onVazgec: () -> Unit) {
    Text(
        text = if (durum.ekranAcildi) "Seans bozuldu" else "Odaklanıyorsun",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MetinBirincil,
    )
    Spacer(modifier = Modifier.height(Bosluk.S))
    Text(
        text = if (durum.ekranAcildi) {
            "Telefon açıldı, bu seans puan vermeyecek. Sayaç yine de " +
                "sürüyor; istersen sonuna kadar götürebilirsin."
        } else {
            "Telefonu bırakabilirsin. Süre ekran kapalıyken de işliyor."
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MetinIkincil,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(Olcu.BolumArasi))
    DaireselGosterge(oran = durum.ilerleme.oran) {
        HalkaIcerik(ana = OdakHesapla.kalanMetni(durum.ilerleme.kalanSaniye), alt = "kaldı")
    }
    Spacer(modifier = Modifier.height(Olcu.BolumArasi))
    MetinBaglanti(metin = "Seansı bırak", onTikla = onVazgec)
}

@Composable
private fun Sonuc(durum: OdakDurum, onTekrar: () -> Unit, onGeri: () -> Unit) {
    val basarili = OdakHesapla.basariliMi(true, durum.ekranAcildi)
    Text(
        text = if (basarili) "Seansı tamamladın" else "Bu sefer olmadı",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MetinBirincil,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(Bosluk.S))
    Text(
        text = if (basarili) {
            "${durum.sureDk} dakika boyunca telefonu bırakabildin."
        } else {
            "Seans sırasında telefon açıldı. Puan yazılmadı ama hiçbir " +
                "şey de kaybetmedin; istediğin zaman tekrar deneyebilirsin."
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MetinIkincil,
        textAlign = TextAlign.Center,
    )
    if (basarili) {
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Cip(
            metin = if (durum.puanYazildi) {
                "+${Gorev.ODAK.puan} puan"
            } else {
                "Bugünün odak puanı zaten alınmış"
            },
            zemin = YesilNane,
        )
    }
    Spacer(modifier = Modifier.height(Olcu.BolumArasi))
    BirincilButon(metin = "Bitir", onTikla = onGeri)
    Spacer(modifier = Modifier.height(Bosluk.S))
    MetinBaglanti(metin = "Yeni seans", onTikla = onTekrar)
}

/** Halkanın ortasındaki iki satır — üç ekranda ortak. */
@Composable
private fun HalkaIcerik(ana: String, alt: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = ana,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Text(
            text = alt,
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
    }
}
