package tr.org.yesilay.yesiliz.ui.ekran.gecekapanisi

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.OnayKutusu
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Gece kapanışı — `debug.md` İ-1.
 *
 * Dört adımlık kısa bir ritüel. Adımlar **emir kipiyle yazılmıyor**
 * (Yeşilay marka kılavuzu, CLAUDE.md §4): her maddenin altında niçin
 * yardımcı olduğu var, "yap" denmiyor.
 *
 * `GECE_TEMIZ` görevinden ayrı puanlanıyor: biri beyan (ritüeli yaptım),
 * öteki ölçüm (gece gerçekten telefonsuz geçti). İkisini birleştirmek,
 * ölçülmüş bir şeyi beyana bağlamak olurdu.
 */
@Composable
fun GeceKapanisiEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GeceKapanisiViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    YesilBaslikliEkran(baslik = "Gece kapanışı", onGeri = onGeri, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Olcu.EkranKenari),
        ) {
            Yigin {
                Text(
                    text = if (durum.tamamlandi) "Günü kapattın" else "Günü kapat",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Text(
                    text = if (durum.tamamlandi) {
                        "İyi geceler. Yarın buradayız."
                    } else {
                        "Dört küçük adım. Hepsi bir dakikadan kısa sürüyor."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                )

                ADIMLAR.forEachIndexed { indeks, (baslik, aciklama) ->
                    Kart {
                        OnayKutusu(
                            metin = baslik,
                            secili = indeks in durum.isaretliler,
                            onDegis = { viewModel.adimDegis(indeks) },
                        )
                        Spacer(modifier = Modifier.height(Bosluk.Xs))
                        Text(
                            text = aciklama,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetinIkincil,
                        )
                    }
                }

                if (durum.tamamlandi) {
                    Cip(
                        metin = if (durum.puanYazildi) {
                            "+${Gorev.GECE_KAPANISI.puan} puan"
                        } else {
                            "Bugünün gece kapanışı zaten yapılmış"
                        },
                        zemin = YesilNane,
                    )
                    BirincilButon(metin = "Kapat", onTikla = onGeri)
                } else {
                    BirincilButon(
                        metin = "Tamamladım",
                        onTikla = viewModel::tamamla,
                        etkin = durum.hepsiIsaretli,
                    )
                }
            }
        }
    }
}
