package tr.org.yesilay.yesiliz.ui.ekran.yedam

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.IkincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.OnayKutusu
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * YEDAM yönlendirme onayı — köprü/rıza ekranı.
 *
 * **Ekran 27 "YEDAM hakkında" ile karıştırılmaz.** O yalnızca bilgilendirme
 * ve Profil'den her zaman açılabiliyor; bu ise risk yüksekken risk detay
 * ekranındaki önerilen eylemlerden biri olarak beliriyor ve **paylaşım
 * rızası** istiyor.
 *
 * Üç kural bu ekranın tamamını belirliyor (CLAUDE.md §4, `docs/spec-veri.md`):
 *  1. Otomatik yönlendirme **yok** — kullanıcı gelmeden ekran açılmıyor.
 *  2. Onay kutusu işaretlenmeden hiçbir şey paylaşılmıyor.
 *  3. Paylaşılacak bilgiler tek tek yazılı; "ve benzeri" yok.
 *
 * Teslim edilen tasarımda karşılığı yok; `stitch/yedam_onay/` yerleşimi
 * gerçek tokenlarla uyarlandı (`docs/tasarim-referans.md §4`).
 */
@Composable
fun YedamOnayEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var onaylandi by remember { mutableStateOf(false) }
    var gonderildi by remember { mutableStateOf(false) }

    YesilBaslikliEkran(
        baslik = "YEDAM'a yönlendirme",
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
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(DaireBoyu)
                            .clip(CircleShape)
                            .background(if (gonderildi) YesilNane else Yesil50),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (gonderildi) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Filled.SupportAgent
                            },
                            contentDescription = null,
                            tint = if (gonderildi) YesilAna else YesilKoyu,
                            modifier = Modifier.size(Olcu.IkonRozeti),
                        )
                    }
                }

                if (gonderildi) {
                    Gonderildi(onGeri = onGeri)
                    return@Yigin
                }

                Text(
                    text = "Bir uzmanla konuşmak ister misin?",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Text(
                    text = "YEDAM (Yeşilay Danışmanlık Merkezi) ücretsiz danışmanlık " +
                        "veriyor. İstersen son dönem özetini seninle birlikte " +
                        "değerlendirmeleri için paylaşabiliriz.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                )

                Kart {
                    Text(
                        text = "Paylaşılacak bilgiler",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MetinBirincil,
                    )
                    Spacer(modifier = Modifier.height(Bosluk.S))
                    PAYLASILANLAR.forEach { madde ->
                        Row(modifier = Modifier.padding(vertical = Bosluk.Xs)) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyLarge,
                                color = YesilAna,
                            )
                            Spacer(modifier = Modifier.width(Bosluk.S))
                            Text(
                                text = madde,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MetinIkincil,
                            )
                        }
                    }
                }

                BilgiSeridi(
                    metin = "Paylaşılmayanlar: uygulama adları, mesaj/arama " +
                        "içeriği, konum, rehber. Bu veriler zaten hiç toplanmıyor.",
                )

                OnayKutusu(
                    metin = "Yukarıdaki özetin YEDAM ile paylaşılmasını onaylıyorum.",
                    secili = onaylandi,
                    onDegis = { onaylandi = it },
                )

                BirincilButon(
                    metin = "Onayla ve devam et",
                    onTikla = { gonderildi = true },
                    etkin = onaylandi,
                )
                IkincilButon(
                    metin = "115'i kendim arayayım",
                    onTikla = {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:115")),
                        )
                    },
                )
                MetinBaglanti(
                    metin = "Şimdi değil",
                    onTikla = onGeri,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Bu onayı sen istedin; uygulama kendiliğinden " +
                        "kimseyle iletişime geçmez. Onayını istediğin zaman " +
                        "Profil'den geri alabilirsin.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MetinUcuncul,
                )
            }
        }
    }
}

@Composable
private fun Gonderildi(onGeri: () -> Unit) {
    Text(
        text = "Onayın kaydedildi",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MetinBirincil,
    )
    // MVP'de gerçek bir gönderim yok: YEDAM entegrasyonu backend işi (Faz 2)
    // ve uydurma bir "gönderildi" mesajı vermek yanlış olurdu. Kullanıcıya
    // ne olduğu ve şimdi ne yapabileceği doğrudan söyleniyor.
    Text(
        text = "Bağlantı kurulduğunda seninle iletişime geçilecek. Beklemek " +
            "istemezsen 115'i şimdi arayabilirsin — hat 7/24 açık, ücretsiz " +
            "ve gizli.",
        style = MaterialTheme.typography.bodyLarge,
        color = MetinIkincil,
    )
    Spacer(modifier = Modifier.height(Olcu.Aralik))
    BirincilButon(metin = "Kapat", onTikla = onGeri)
}

@Composable
private fun BilgiSeridi(metin: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Buton))
            .background(YesilNane)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = YesilAna)
        Spacer(modifier = Modifier.width(Olcu.Aralik))
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyMedium,
            color = MetinBirincil,
        )
    }
}

/**
 * Paylaşılacak bilgiler — **tek tek sayılı**.
 *
 * "Kullanım özeti ve benzeri veriler" gibi bir ifade, rızayı belirsiz
 * kılardı. Liste kısa ve somut tutuluyor.
 */
private val PAYLASILANLAR = listOf(
    "Son 4 haftanın günlük ekran süresi ortalaması",
    "Risk düzeyi ve onu etkileyen dört faktör",
    "Değerlendirme (ATBÖ-KV) ham puanın",
    "Yaş aralığın",
)

private val DaireBoyu = Olcu.AvatarBuyuk
