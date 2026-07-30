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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import tr.org.yesilay.yesiliz.domain.gorev.DogrulamaTuru
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.ui.binlik
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
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
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Ana Sayfa — `docs/tasarim-referans.md §2 / 08`.
 *
 * Yeşil başlıktan ağaç illüstrasyonuna, oradan beyaz içeriğe geçen tek bir
 * dikey akış. Ağaç bölümü gradyanla beyaza söndüğü için illüstrasyonun
 * kendi zemini görünmüyor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnaEkran(
    onOdul: () -> Unit,
    onNefesBaslat: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnaViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    var acikGorev by remember { mutableStateOf<Gorev?>(null) }
    val sayfaDurumu = rememberModalBottomSheetState()

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
                GorevSatiri(gorev = gorev, onTikla = { acikGorev = Gorev.idden(gorev.id) })
            }
        }
    }

    acikGorev?.let { gorev ->
        val ozet = durum.gorevler.firstOrNull { it.id == gorev.id }
        ModalBottomSheet(
            onDismissRequest = { acikGorev = null },
            sheetState = sayfaDurumu,
            containerColor = Beyaz,
        ) {
            GorevDetayi(
                gorev = gorev,
                ikon = ozet?.ikon ?: Icons.Filled.TrackChanges,
                onNefesBaslat = {
                    acikGorev = null
                    onNefesBaslat()
                },
                onTamamla = {
                    viewModel.gorevTiklandi(gorev.id)
                    acikGorev = null
                },
                onKapat = { acikGorev = null },
            )
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
            if (gorev.tamamlandi) {
                Cip(metin = "Tamamlandı", ikon = Icons.Filled.CheckCircle, zemin = YesilNane, renk = YesilAna)
            } else {
                Cip(metin = "+${gorev.puan}", renk = YesilAna)
            }
        }
    }
}

/**
 * Görev detayı alt sayfası — `docs/tasarim-referans.md §4` (Stitch yerleşim
 * referansı `g_rev_detay/`, resmi teslim yok). Tüm görevler için ortak,
 * tek eylemi görev türüne göre değişiyor.
 */
@Composable
private fun GorevDetayi(
    gorev: Gorev,
    ikon: ImageVector,
    onNefesBaslat: () -> Unit,
    onTamamla: () -> Unit,
    onKapat: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IkonRozeti(ikon = ikon)
            Spacer(modifier = Modifier.weight(1f))
            Cip(metin = "${gorev.puan} puan", ikon = Icons.Filled.Eco, zemin = YesilNane, renk = YesilAna)
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = gorev.baslik,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = gorev.detayAciklamasi(),
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
            gorev.suresiDk()?.let { Cip(metin = "$it dk") }
            Cip(
                metin = if (gorev.dogrulama == DogrulamaTuru.OTOMATIK) "Doğrulanmış" else "Kendi beyanın",
            )
            Cip(metin = "Bugün ${gorev.gunlukLimit} kez")
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        when (gorev) {
            Gorev.NEFES -> BirincilButon(metin = "Nefes egzersizine başla", onTikla = onNefesBaslat)
            // Aktif yürüyüş (canlı sayaç/adım) ekranı henüz yok — CLAUDE.md §11.
            // Puan o ekran gelince başlangıçta değil, bitişte verilecek.
            Gorev.YURUYUS -> BirincilButon(metin = "Yakında", onTikla = {}, etkin = false)
            else -> BirincilButon(metin = "Tamamlandı olarak işaretle", onTikla = onTamamla)
        }
    }
}

/** Görev detayı sayfasının uzun açıklaması — kart satırındaki kısa metinden ayrı. */
private fun Gorev.detayAciklamasi(): String = when (this) {
    Gorev.YURUYUS -> "Kısa bir yürüyüş ekrandan uzaklaşmanın en kolay yolu. " +
        "Telefonunu cebinde tut, biz saymaya devam edeceğiz."
    else -> aciklama
}

/** Süre çipi için dakika değeri — süresi olmayan görevlerde null (çip gösterilmez). */
private fun Gorev.suresiDk(): Int? = when (this) {
    Gorev.NEFES -> 2
    Gorev.DIJITAL_MOLA -> 15
    Gorev.YURUYUS -> 15
    Gorev.RUH_HALI, Gorev.UYKU_BEYANI, Gorev.GECE_TEMIZ, Gorev.TUKETIM_HEDEFI -> null
}

private val AgacAlani: Dp = Olcu.AgacAlani
private val AgacBoyu: Dp = Olcu.AgacBoyu
private val AyracBoyu: Dp = Olcu.AyracBoyu

/** Yeşil zemin üstündeki beyaz dolguların opaklığı. */
private const val YariSaydam = 0.2f

/** Yeşil zemin üstündeki ikincil metin opaklığı. */
private const val AzSaydam = 0.85f
