package tr.org.yesilay.yesiliz.ui.ekran.saglik

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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.IkincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Health Connect izin akışı — teslim edilen tasarımda karşılığı **yok**;
 * **02 · İzin** ekranının yerleşimi birebir uyarlandı (aynı daire ikon,
 * aynı bilgi şeridi, aynı buton çifti).
 *
 * İzin verilmezse uygulama çalışmaya devam ediyor: `SaglikOkuyucu` boş
 * aktivite döndürüyor ve risk hesabında `D = 1.0` oluyor (sönümleme yok).
 * Bu ekranda da bu açıkça yazıyor — kullanıcı "vermezsem bozulur mu"
 * diye tahmin etmek zorunda kalmasın.
 */
@Composable
fun SaglikIzinEkran(
    onGeri: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SaglikIzinViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val izinIstegi = saglikIzinIstegi(viewModel.gerekliIzinler) { viewModel.yenile() }

    YesilBaslikliEkran(
        baslik = "Adım ve hareket",
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
                    Box(
                        modifier = Modifier
                            .size(DaireBoyu)
                            .clip(CircleShape)
                            .background(Yesil50),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (durum.izinVarMi) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.AutoMirrored.Filled.DirectionsWalk
                            },
                            contentDescription = null,
                            tint = YesilAna,
                            modifier = Modifier.size(Olcu.IkonRozeti),
                        )
                    }
                }

                Text(
                    text = if (durum.izinVarMi) {
                        "Adım verine erişebiliyoruz"
                    } else {
                        "Hareketini de sayabilmemiz için"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Text(
                    text = "Adım sayın ve egzersiz süren, ekran süresinin yanına " +
                        "konduğunda tabloyu tamamlıyor. Gerçek hayatta geçen " +
                        "zaman, risk skorunu bir miktar yumuşatıyor.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MetinIkincil,
                )

                BilgiSeridi(
                    metin = "Yalnızca adım sayısı ve egzersiz süresi okunur. " +
                        "Kalp atışı, uyku, kilo ve konum hiç istenmez.",
                )

                when {
                    durum.izinVarMi -> {
                        Kart {
                            Text(
                                text = "İzin verildi",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MetinBirincil,
                            )
                            Text(
                                text = "Adım verin İstatistik ekranında ve yürüyüş " +
                                    "sayacında görünüyor.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MetinIkincil,
                            )
                        }
                        IkincilButon(metin = "Geri dön", onTikla = onGeri)
                    }

                    // Health Connect kurulu değil: izin isteyecek bir muhatap
                    // yok, Play Store'a yönlendirmek tek anlamlı adım.
                    !durum.kullanilabilir -> {
                        Kart {
                            Text(
                                text = "Health Connect kurulu değil",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MetinBirincil,
                            )
                            Text(
                                text = "Adım verisi Android'in Health Connect " +
                                    "uygulaması üzerinden okunuyor.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MetinIkincil,
                            )
                        }
                        BirincilButon(
                            metin = "Health Connect'i kur",
                            onTikla = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_URL)),
                                )
                            },
                        )
                    }

                    else -> {
                        BirincilButon(
                            metin = "İzin ver",
                            onTikla = { izinIstegi() },
                        )
                    }
                }

                Text(
                    text = "İzin vermezsen de uygulama çalışır — adım verisi " +
                        "olmadan risk hesabı hareketi hiç düşmez, o kadar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinIkincil,
                )
            }
        }
    }
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

private val DaireBoyu = Olcu.AvatarBuyuk
private const val PLAY_URL =
    "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"
