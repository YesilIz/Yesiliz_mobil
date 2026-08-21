package tr.org.yesilay.yesiliz.ui.ekran.yedam

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.IkincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.KartAyraci
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu

/**
 * "Verilerine kim baktı" ve rıza geri çekme (M5, M6).
 *
 * Rıza metninin iki sözünün ekrandaki karşılığı. Kayıt listesi rızadan
 * **bağımsız**: rızasını geri çeken kullanıcı da geçmişte kimin baktığını
 * görebiliyor — kaydın varlık sebebi bu, rızayla birlikte kaybolamaz.
 *
 * Geri çekme onayı **kırmızısız** (§4). Kırmızının iki istisnası (K10)
 * risk analizi etiketi ve hesap/veri kalıcı silme onayı; bu ekran onlardan
 * biri değil ve zaten geri alınabilir bir işlem — kullanıcı yeniden
 * başvurabiliyor.
 */
@Composable
fun YedamGizlilikEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    gorunum: YedamGizlilikViewModel = hiltViewModel(),
) {
    val durum by gorunum.durum.collectAsState()
    var onayIstendi by remember { mutableStateOf(false) }

    YesilBaslikliEkran(
        baslik = "YEDAM ve verilerin",
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
                Text(
                    text = "Verilerine kim baktı",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MetinBirincil,
                )
                Text(
                    text = "Bir YEDAM danışmanı bilgilerine her baktığında " +
                        "buraya bir kayıt düşer. Danışmanın adı gösterilmiyor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinIkincil,
                )

                when {
                    durum.yukleniyor -> Text(
                        text = "Yükleniyor…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinIkincil,
                    )

                    // "Kimse bakmamış" ile "bilmiyoruz" aynı şey değil.
                    durum.cevrimdisi -> BosDurum(
                        baslik = "Kayıtlar alınamadı",
                        aciklama = "Sunucuya ulaşılamadı. Bağlantını kontrol " +
                            "edip tekrar deneyebilirsin.",
                        ikon = Icons.Filled.Visibility,
                        eylem = { IkincilButon("Tekrar dene", gorunum::tazele) },
                    )

                    durum.kayitlar.isEmpty() -> BosDurum(
                        baslik = "Henüz kimse bakmadı",
                        aciklama = "Verilerine bir danışman baktığında burada " +
                            "tarih ve saatiyle görürsün.",
                        ikon = Icons.Filled.Visibility,
                    )

                    else -> Kart {
                        durum.kayitlar.forEachIndexed { indeks, kayit ->
                            if (indeks > 0) KartAyraci()
                            Text(
                                text = kayit.tarih,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MetinBirincil,
                            )
                            Text(
                                text = "Görülen: ${kayit.kapsamMetni}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MetinIkincil,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Bosluk.M))

                if (durum.rizaVar) {
                    Text(
                        text = "YEDAM rızan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MetinBirincil,
                    )
                    Text(
                        text = "Geri çektiğin anda danışmanın erişimi kesilir " +
                            "ve risk ile anket bilgilerin sunucudan silinir. " +
                            "Başvurunun kaydı, görüşme kayıtları ve sana kimin " +
                            "baktığını gösteren kayıt kalır — bunlar bir " +
                            "danışmanlık ilişkisinin kaydı.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinIkincil,
                    )

                    if (onayIstendi) {
                        Text(
                            text = "Emin misin? İstersen daha sonra yeniden " +
                                "başvurabilirsin.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MetinBirincil,
                        )
                        IkincilButon(
                            metin = "Evet, rızamı geri çek",
                            onTikla = {
                                onayIstendi = false
                                gorunum.geriCek()
                            },
                            etkin = !durum.geriCekiliyor,
                        )
                        IkincilButon("Vazgeç", { onayIstendi = false })
                    } else {
                        IkincilButon(
                            metin = "YEDAM rızamı geri çek",
                            onTikla = { onayIstendi = true },
                            etkin = !durum.geriCekiliyor,
                        )
                    }
                }

                if (durum.geriCekildi) {
                    Text(
                        text = "Rızan geri çekildi ve bilgilerin silindi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinBirincil,
                    )
                }
                if (durum.geriCekilemedi) {
                    Text(
                        text = "Sunucuya ulaşılamadı, rızan geri çekilemedi. " +
                            "Bağlantını kontrol edip tekrar dene — bilgilerin " +
                            "hâlâ paylaşılıyor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinBirincil,
                    )
                }
            }
        }
    }
}
