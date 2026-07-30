package tr.org.yesilay.yesiliz.ui.ekran.ana

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import tr.org.yesilay.yesiliz.R
import tr.org.yesilay.yesiliz.ui.binlik
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.KullaniciAvatari
import tr.org.yesilay.yesiliz.ui.bilesen.SallananYapraklar
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.sureMetni
import tr.org.yesilay.yesiliz.ui.tema.Ayrac
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.Gradyan
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.YesilAna

/**
 * Ana Sayfa — `docs/tasarim-referans.md §2 / 08`.
 *
 * Yeşil başlıktan ağaç illüstrasyonuna, oradan beyaz içeriğe geçen tek bir
 * dikey akış. Ağaç bölümü gradyanla beyaza söndüğü için illüstrasyonun
 * kendi zemini görünmüyor.
 */
@Composable
fun AnaEkran(
    onOdul: () -> Unit,
    onGorev: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnaViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Beyaz)
            .verticalScroll(rememberScrollState()),
    ) {
        Basliktan(durum)

        Yigin(
            modifier = Modifier.padding(
                start = Olcu.EkranKenari,
                end = Olcu.EkranKenari,
                bottom = Olcu.BolumArasi,
            ),
        ) {
            PuanKarti(puan = durum.puan, onTikla = onOdul)

            // Veri yoksa uydurma sayı göstermek yerine ne olduğunu söyle.
            // İzin verilmemişse sebep o; verilmişse henüz özet çıkmadı.
            if (durum.veriVarMi) {
                OzetSeridi(durum)
            } else {
                Kart {
                    BosDurum(
                        baslik = if (durum.izinVarMi) {
                            "Ölçmeye yeni başladık"
                        } else {
                            "Kullanım izni gerekiyor"
                        },
                        aciklama = if (durum.izinVarMi) {
                            "Bugünün özeti hazırlanıyor. Birkaç saat içinde ekran " +
                                "süren burada görünecek."
                        } else {
                            "Ekran süreni görebilmemiz için Ayarlar'dan kullanım " +
                                "erişimi vermen gerekiyor."
                        },
                    )
                }
            }
            RuhHaliKarti(deger = durum.ruhHali, onDegis = viewModel::ruhHaliDegisti)
            KesfetIkilisi()

            Text(
                text = "Bugünün görevleri",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
            )
            durum.gorevler.forEach { gorev ->
                GorevSatiri(gorev = gorev, onTikla = { onGorev(gorev.id) })
            }
        }
    }
}

/** Yeşil selamlama şeridi + altında beyaza sönen ağaç. */
@Composable
private fun Basliktan(durum: AnaDurum) {
    Column(modifier = Modifier.fillMaxWidth().background(YesilAna)) {
        Box(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Olcu.EkranKenari, vertical = Bosluk.M),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Bosluk.M),
        ) {
            KullaniciAvatari(
                isim = durum.isim,
                zemin = Beyaz.copy(alpha = YariSaydam),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = durum.selamlama,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Beyaz.copy(alpha = AzSaydam),
                )
                Text(
                    text = durum.isim,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Beyaz,
                )
            }
            Cip(
                metin = "${durum.serideGun} gün",
                ikon = Icons.Filled.LocalFireDepartment,
                zemin = Beyaz.copy(alpha = YariSaydam),
                renk = Beyaz,
            )
        }

        // Ağaç: yeşilden beyaza sönen zemin üstünde. İllüstrasyon açık zemin
        // için üretildi, doğrudan yeşile konursa kenarları kirli görünüyor.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AgacAlani)
                .background(Gradyan.BaslikSonumu),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(agacGorseli(durum.agacEvresi)),
                contentDescription = null,
                modifier = Modifier.height(AgacBoyu),
            )
            SallananYapraklar(modifier = Modifier.height(AgacBoyu).aspectRatio(1f))
        }
    }
}

/** Ağaç evresi (1..6) → drawable. Evre dışı değer en yakın uca kırpılır. */
private fun agacGorseli(evre: Int): Int = when (evre.coerceIn(1, 6)) {
    1 -> R.drawable.agac_evre1_cicek
    2 -> R.drawable.agac_evre2_narenciye
    3 -> R.drawable.agac_evre3_pembe
    4 -> R.drawable.agac_evre4_sari
    5 -> R.drawable.agac_evre5_erik
    else -> R.drawable.agac_evre6_elma
}

/** Gradyanlı puan kartı — dokununca Ödüller ekranı. */
@Composable
private fun PuanKarti(puan: Int, onTikla: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Kart))
            .background(Gradyan.PuanKarti)
            .clickable(onClick = onTikla)
            .padding(Olcu.KartIci),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Bosluk.M),
    ) {
        IkonRozeti(
            ikon = Icons.Filled.MonitorHeart,
            zemin = Beyaz.copy(alpha = YariSaydam),
            renk = Beyaz,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${puan.binlik()} puan",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Beyaz,
            )
            Text(
                text = "Ödüllerini görmek için dokun",
                style = MaterialTheme.typography.bodyMedium,
                color = Beyaz.copy(alpha = AzSaydam),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Beyaz,
        )
    }
}

/** Ekran · Hedef · Adım — üç sütun, aralarında ince ayraç. */
@Composable
private fun OzetSeridi(durum: AnaDurum) {
    Kart {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OzetSutunu(
                ikon = Icons.Filled.PhoneAndroid,
                deger = durum.ekranDakika.sureMetni(),
                etiket = "Ekran",
                modifier = Modifier.weight(1f),
            )
            DikeyAyrac()
            OzetSutunu(
                ikon = Icons.Filled.TrackChanges,
                deger = "${durum.hedefDakika} dk",
                etiket = "Hedef",
                modifier = Modifier.weight(1f),
            )
            DikeyAyrac()
            OzetSutunu(
                ikon = Icons.AutoMirrored.Filled.DirectionsRun,
                deger = durum.adim.binlik(),
                etiket = "Adım",
                modifier = Modifier.weight(1f),
            )
            if (durum.ebeveynAktifMi) {
                DikeyAyrac()
                OzetSutunu(
                    ikon = Icons.Filled.FamilyRestroom,
                    deger = "Aktif",
                    etiket = "Ebeveyn",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OzetSutunu(
    ikon: ImageVector,
    deger: String,
    etiket: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Bosluk.Xs),
    ) {
        Icon(imageVector = ikon, contentDescription = null, tint = YesilAna)
        Text(
            text = deger,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
    }
}

@Composable
private fun DikeyAyrac() {
    Box(
        modifier = Modifier
            .height(AyracBoyu)
            .width(Olcu.KartKenarligi)
            .background(Ayrac),
    )
}

/**
 * Ruh hali — kullanıcının kendi beyanı.
 *
 * Turuncu → yeşil gradyan. Bu **risk göstergesi değil**, bu yüzden
 * "risk kırmızıyla gösterilmez" kuralıyla çakışmıyor (CLAUDE.md §4);
 * turuncu uç da kırmızı değil.
 */
@Composable
private fun RuhHaliKarti(deger: Float, onDegis: (Float) -> Unit) {
    Kart {
        Text(
            text = "Bugün nasıl hissediyorsun?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Text(
            text = "Sürükle, cevabın her gün değişsin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Bosluk.M),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Olcu.IlerlemeYuksekligi)
                    .clip(RoundedCornerShape(Yaricap.Cip))
                    .background(Gradyan.RuhHali),
            )
            Slider(
                value = deger,
                onValueChange = onDegis,
                colors = SliderDefaults.colors(
                    // İz gradyanla çizildi, Slider'ın kendi izi saydam.
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = Beyaz,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Zorlanıyorum",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Harika",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinIkincil,
            )
        }
    }
}

/** Bilgi Bankası · Kendini Test Et — iki eşit kart. */
@Composable
private fun KesfetIkilisi() {
    Row(horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik)) {
        KesfetKarti(
            ikon = Icons.AutoMirrored.Filled.MenuBook,
            baslik = "Bilgi Bankası",
            aciklama = "Ekran bağımlılığı hakkında doğru bilgiler.",
            modifier = Modifier.weight(1f),
        )
        KesfetKarti(
            ikon = Icons.AutoMirrored.Filled.FactCheck,
            baslik = "Kendini Test Et",
            aciklama = "Farkındalığını artır.",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KesfetKarti(
    ikon: ImageVector,
    baslik: String,
    aciklama: String,
    modifier: Modifier = Modifier,
) {
    Kart(modifier = modifier) {
        IkonRozeti(ikon = ikon)
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = baslik,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Text(
            text = aciklama,
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
    }
}

/**
 * Görev satırı.
 *
 * Tamamlanan görev listeden düşmüyor, işaretleniyor — kaybolan görev
 * "yaptım mı yapmadım mı" belirsizliği üretiyor. Ceza yok, yalnızca kazanç.
 */
@Composable
private fun GorevSatiri(gorev: GorevOzeti, onTikla: () -> Unit) {
    Kart(onTikla = onTikla) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Bosluk.M),
        ) {
            IkonRozeti(ikon = gorev.ikon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gorev.baslik,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MetinBirincil,
                )
                Text(
                    text = gorev.aciklama,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinIkincil,
                )
            }
            Cip(metin = "+${gorev.puan}", renk = YesilAna)
        }
    }
}

private val AgacAlani: Dp = Olcu.AgacAlani
private val AgacBoyu: Dp = Olcu.AgacBoyu
private val AyracBoyu: Dp = Olcu.AyracBoyu

/** Yeşil zemin üstündeki beyaz dolguların opaklığı. */
private const val YariSaydam = 0.2f

/** Yeşil zemin üstündeki ikincil metin opaklığı. */
private const val AzSaydam = 0.85f
