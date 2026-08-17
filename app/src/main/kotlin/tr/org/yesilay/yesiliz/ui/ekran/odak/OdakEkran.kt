package tr.org.yesilay.yesiliz.ui.ekran.odak

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.TekerlekSecici
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilNane
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import tr.org.yesilay.yesiliz.ui.EkranDonusTakibi
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.IkincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.veri.DaireselGosterge
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu

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
        val donusTakibi = EkranDonusTakibi()
        val gozlemci = LifecycleEventObserver { _, olay ->
            when (olay) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                -> donusTakibi.ayrildi()

                Lifecycle.Event.ON_RESUME -> {
                    if (donusTakibi.dondu()) viewModel.ekranAcildi()
                }

                else -> Unit
            }
        }
        // Gözlemci seans başladıktan sonra takılıyor. Lifecycle, yeni
        // gözlemciyi mevcut RESUMED durumuna eşitlemek için ilk ON_RESUME'u
        // hemen gönderebilir; EkranDonusTakibi bunu gerçek dönüşten ayırır.
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
                    onSureSec = viewModel::sureSecildi,
                    onBasla = viewModel::basla,
                )
            }
        }
    }
}

@Composable
private fun Hazirlik(sureDk: Int, onSureSec: (Int) -> Unit, onBasla: () -> Unit) {
    var tekerlekAcik by remember { mutableStateOf(false) }

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

    // Hızlı çipler duruyor: kullanıcıların çoğu bu üç süreden birini
    // seçiyor ve tekerleği açmaya gerek kalmıyor.
    Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
        OdakHesapla.SURELER.forEach { dakika ->
            Cip(
                metin = "$dakika dk",
                onTikla = { onSureSec(dakika) },
                zemin = if (dakika == sureDk) YesilAna else YesilNane,
                renk = if (dakika == sureDk) Beyaz else YesilAna,
            )
        }
    }
    Spacer(modifier = Modifier.height(Bosluk.S))
    IkincilButon(
        metin = if (tekerlekAcik) "Tamam" else "Kendim seçeyim",
        onTikla = { tekerlekAcik = !tekerlekAcik },
    )

    if (tekerlekAcik) {
        Spacer(modifier = Modifier.height(Bosluk.S))
        TekerlekSecici(
            degerler = OdakHesapla.TEKERLEK_SURELERI,
            secili = sureDk,
            onSec = onSureSec,
            etiketle = { "$it dk" },
            modifier = Modifier.fillMaxWidth(TEKERLEK_GENISLIK_ORANI),
        )
    }

    Spacer(modifier = Modifier.height(Olcu.BolumArasi))
    BirincilButon(metin = "Başlat", onTikla = onBasla)
}

/** Tekerlek ekranın tamamını kaplamasın — orta sütun yeterli. */
private const val TEKERLEK_GENISLIK_ORANI = 0.6f

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
