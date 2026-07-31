package tr.org.yesilay.yesiliz.ui.ekran.anket

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IlerlemeCubugu
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.LikertMaddesi
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.bilesen.veri.Cubuk
import tr.org.yesilay.yesiliz.ui.bilesen.veri.CubukGrafik
import tr.org.yesilay.yesiliz.ui.ekran.onboarding.SasSv
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import java.time.LocalDate

/**
 * SAS-SV yeniden değerlendirmesi — ekran **04**'ün maddeleri, onboarding
 * iskeleti olmadan.
 *
 * `AnketEkran` yeniden kullanılamadı çünkü o `OnboardingIskelet`'e bağlı
 * ("Adım 4 / 4" göstergesi, onboarding butonları). Değişmeyen kısım —
 * [SasSv.MADDELER] ve [LikertMaddesi] — aynen paylaşılıyor; madde metni
 * tek bir yerde duruyor.
 */
@Composable
fun AnketYenileEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnketYenileViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    // Geri tuşu önce sayfayı geri alsın, ekrandan çıkmasın.
    BackHandler(enabled = !durum.bitti && durum.sayfa > 0) { viewModel.geriSayfa() }

    YesilBaslikliEkran(
        baslik = "Değerlendirme",
        onGeri = { if (durum.bitti || !viewModel.geriSayfa()) onGeri() },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
        ) {
            if (durum.bitti) {
                Sonuc(durum = durum, onKapat = onGeri)
            } else {
                Maddeler(
                    durum = durum,
                    onYanit = viewModel::yanitla,
                    onIlerle = { if (durum.sonSayfaMi) viewModel.bitir() else viewModel.ilerle() },
                )
            }
        }
    }
}

@Composable
private fun Maddeler(
    durum: AnketYenileDurum,
    onYanit: (Int, Int) -> Unit,
    onIlerle: () -> Unit,
) {
    Yigin {
        Text(
            text = "Aşağıdaki ifadelere\nne kadar katılıyorsun?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        durum.oncekiTarih?.let { tarih ->
            Text(
                text = "Son değerlendirmen ${tarihMetni(tarih)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinUcuncul,
            )
        }
        IlerlemeCubugu(oran = durum.ilerleme)

        Column(verticalArrangement = Arrangement.spacedBy(Olcu.Aralik)) {
            (durum.ilkMadde until durum.sonMadde).forEach { madde ->
                LikertMaddesi(
                    ifade = SasSv.MADDELER[madde],
                    secili = durum.yanitlar[madde],
                    onSec = { onYanit(madde, it) },
                )
            }
        }

        BirincilButon(
            metin = if (durum.sonSayfaMi) "Değerlendirmeyi bitir" else "Devam",
            onTikla = onIlerle,
            etkin = durum.sayfaTamamMi,
        )
    }
}

@Composable
private fun Sonuc(durum: AnketYenileDurum, onKapat: () -> Unit) {
    Yigin {
        Text(
            text = "Değerlendirmen kaydedildi.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        // Bant adı ya da kesme puanı GÖSTERİLMİYOR: 29,5 bir tarama eşiği,
        // tanı eşiği değil (docs/spec-anket.md §6.4). Kullanıcıya "eşiğin
        // üstündesin" demek teşhis diline kayardı.
        Text(
            text = "Bu sonuç risk hesabına katıldı ve ölçümün tazelendi. " +
                "Değişimi İstatistik sekmesinden takip edebilirsin.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )

        Kart {
            Text(
                text = "${durum.hamPuan}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
            Text(
                text = "ham puan (10–60 arası)",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
            )
            durum.oncekiPuan?.let { onceki ->
                Spacer(modifier = Modifier.height(Olcu.Aralik))
                Cip(metin = karsilastirma(durum.hamPuan, onceki))
            }
        }

        // Trend yalnızca en az iki ölçüm varken anlamlı.
        if (durum.gecmis.size >= 2) {
            Kart {
                Text(
                    text = "Değerlendirme geçmişin",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Spacer(modifier = Modifier.height(Olcu.Aralik))
                CubukGrafik(
                    cubuklar = durum.gecmis.map { (tarih, puan) ->
                        Cubuk(etiket = kisaTarih(tarih), deger = puan.toFloat())
                    },
                    vurgulanan = durum.gecmis.lastIndex,
                )
            }
        }

        // Y-7'nin asıl UX kusuru: kullanıcı ham puanı 60'tan 10'a
        // düşürdüğünde bile ekranda "orta" görmeye devam ediyor ve anketi
        // doldurmanın hiçbir işe yaramadığı sonucuna varıyordu. Yanıt
        // kaydedildi ve hesaba girdi; hareket etmeyen şey GÖSTERİLEN bant
        // ve bunun sebebi tasarım gereği (histerezis, `Sabitler.GUN_ASAGI`).
        // Söylenmediği sürece bu, bozukluk gibi görünüyor.
        Kart {
            Text(
                text = "Yeni değerlendirmen kaydedildi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
            Spacer(modifier = Modifier.height(Bosluk.Xs))
            Text(
                text = "Ana ekranda gördüğün risk seviyesi tek bir ölçüme değil " +
                    "son günlerin eğilimine bakıyor, bu yüzden birkaç gün " +
                    "içinde güncellenecek. Seviye yükselirken daha hızlı, " +
                    "inerken daha temkinli davranıyor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
            )
        }

        BirincilButon(metin = "Kapat", onTikla = onKapat)
        // Sorumluluk reddi metni -- tıklanabilir DEĞİL. `MetinBaglanti`
        // `TextButton` olduğu için dalga efekti veriyor ve dokununca hiçbir
        // şey yapmıyordu (`debug.md` O-4).
        Text(
            text = "Bu bir tanı değildir",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinUcuncul,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Ham puan düşüşü **iyi haber**, ama abartılı kutlama da yok. */
private fun karsilastirma(simdiki: Int, onceki: Double): String {
    val fark = simdiki - onceki
    return when {
        fark < 0 -> "Öncekine göre ${(-fark).toInt()} puan daha düşük"
        fark > 0 -> "Öncekine göre ${fark.toInt()} puan daha yüksek"
        else -> "Öncekiyle aynı"
    }
}

private fun tarihMetni(tarih: LocalDate): String =
    "${tarih.dayOfMonth} ${AYLAR[tarih.monthValue - 1]} ${tarih.year}"

private fun kisaTarih(tarih: LocalDate): String =
    "${tarih.dayOfMonth} ${AYLAR[tarih.monthValue - 1].take(3)}"

private val AYLAR = listOf(
    "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık",
)
