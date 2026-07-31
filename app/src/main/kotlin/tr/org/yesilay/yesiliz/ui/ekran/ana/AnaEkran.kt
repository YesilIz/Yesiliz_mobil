package tr.org.yesilay.yesiliz.ui.ekran.ana

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import tr.org.yesilay.yesiliz.ui.bilesen.AltSayfa
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.KullaniciAvatari
import tr.org.yesilay.yesiliz.ui.bilesen.SallananYapraklar
import tr.org.yesilay.yesiliz.ui.bilesen.Yigin
import tr.org.yesilay.yesiliz.ui.sureMetni
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
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
    onBilgiBankasi: () -> Unit,
    onKendiniTest: () -> Unit,
    onBildirimler: () -> Unit,
    onYuruyusBaslat: () -> Unit,
    onDegerlendirmeYenile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnaViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    var acikGorev by remember { mutableStateOf<Gorev?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Beyaz)
            .verticalScroll(rememberScrollState()),
    ) {
        Basliktan(durum, onBildirimler = onBildirimler)

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
            if (durum.degerlendirmeYenilensin) {
                DegerlendirmeKarti(
                    onYenile = onDegerlendirmeYenile,
                    onErtele = viewModel::degerlendirmeErtele,
                )
            }

            RuhHaliKarti(
                deger = durum.ruhHali,
                kaydedildi = durum.ruhHaliKaydedildi,
                onDegis = viewModel::ruhHaliDegisti,
                onBirakildi = viewModel::ruhHaliBirakildi,
            )
            KesfetIkilisi(onBilgiBankasi = onBilgiBankasi, onKendiniTest = onKendiniTest)

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
        AltSayfa(onKapat = { acikGorev = null }) {
            GorevDetayi(
                gorev = gorev,
                ikon = ozet?.ikon ?: Icons.Filled.TrackChanges,
                onNefesBaslat = {
                    acikGorev = null
                    onNefesBaslat()
                },
                onYuruyusBaslat = {
                    acikGorev = null
                    onYuruyusBaslat()
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
private fun Basliktan(durum: AnaDurum, onBildirimler: () -> Unit) {
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
            ZilIkonu(okunmamis = durum.okunmamisBildirim, onTikla = onBildirimler)
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

/**
 * Başlıktaki zil — okunmamış varsa üstünde küçük beyaz nokta.
 *
 * Teslim edilen 08 tasarımında zil yok; bildirim listesine (35) ulaşacak
 * bir giriş noktası da yok. Serinin sağına, aynı hizaya konuldu — başlığın
 * dengesi bozulmuyor ve rozet sayı yerine **nokta**: "3 okunmamış bildirim"
 * demek uygulamayı açtırmaya çalışan bir dil, uygulamanın amacıyla çelişir.
 */
@Composable
private fun ZilIkonu(okunmamis: Int, onTikla: () -> Unit) {
    Box(
        modifier = Modifier
            .size(Olcu.IkonRozeti)
            .clip(RoundedCornerShape(Yaricap.Cip))
            .background(Beyaz.copy(alpha = YariSaydam))
            .clickable(onClick = onTikla),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "Bildirimler",
            tint = Beyaz,
        )
        if (okunmamis > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Bosluk.Xs)
                    .size(NoktaBoyu)
                    .clip(RoundedCornerShape(NoktaBoyu / 2))
                    .background(Beyaz),
            )
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuhHaliKarti(
    deger: Float,
    kaydedildi: Boolean,
    onDegis: (Float) -> Unit,
    onBirakildi: () -> Unit,
) {
    Kart {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Bugün nasıl hissediyorsun?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MetinBirincil,
                modifier = Modifier.weight(1f),
            )
            if (kaydedildi) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Bugün kaydedildi",
                    tint = YesilAna,
                )
            }
        }
        Text(
            text = if (kaydedildi) {
                "Kaydedildi. Gün içinde fikrin değişirse tekrar sürükleyebilirsin."
            } else {
                "Sürükle, cevabın her gün değişsin."
            },
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
                // Parmak kalkınca yazılıyor — sürükleme boyunca her karede
                // Room'a yazmanın anlamı yok.
                onValueChangeFinished = onBirakildi,
                colors = SliderDefaults.colors(
                    // İz gradyanla çizildi, Slider'ın kendi izi saydam.
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = Beyaz,
                ),
                // Varsayılan tutamak düz beyaz; kart zemini de beyaz olduğu
                // için gradyan çubuğun dışında kalan kısmı **görünmüyordu**.
                // Tasarım 07'deki "beyaz dolgu, yeşil kenarlık" tutamağı
                // burada da kullanılıyor.
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(TutamakBoyu)
                            .clip(CircleShape)
                            .background(Beyaz)
                            .border(TutamakKenari, YesilAna, CircleShape),
                    )
                },
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
private fun KesfetIkilisi(onBilgiBankasi: () -> Unit, onKendiniTest: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik)) {
        KesfetKarti(
            ikon = Icons.AutoMirrored.Filled.MenuBook,
            baslik = "Bilgi Bankası",
            aciklama = "Ekran bağımlılığı hakkında doğru bilgiler.",
            onTikla = onBilgiBankasi,
            modifier = Modifier.weight(1f),
        )
        KesfetKarti(
            ikon = Icons.AutoMirrored.Filled.FactCheck,
            baslik = "Kendini Test Et",
            aciklama = "Farkındalığını artır.",
            onTikla = onKendiniTest,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KesfetKarti(
    ikon: ImageVector,
    baslik: String,
    aciklama: String,
    onTikla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Kart(modifier = modifier, onTikla = onTikla) {
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
    onYuruyusBaslat: () -> Unit,
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
                metin = when (gorev.dogrulama) {
                    DogrulamaTuru.OTOMATIK -> "Otomatik"
                    DogrulamaTuru.AKTIVITE -> "Uygulama içinde"
                    DogrulamaTuru.BEYAN -> "Kendi beyanın"
                },
            )
            Cip(metin = "Bugün ${gorev.gunlukLimit} kez")
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        // OTOMATIK görevlerde buton YOK. Bu iki görev puanını zaten günlük
        // özetten alıyor; "Tamamlandı olarak işaretle" hem aynı gün için
        // ikinci kez puan yazıyor hem de kaydı "otomatik" olarak
        // damgalayarak denetim izini bozuyordu (`debug.md` K-9).
        when {
            gorev.dogrulama == DogrulamaTuru.OTOMATIK -> OtomatikGorevNotu(gorev)
            gorev == Gorev.NEFES ->
                BirincilButon(metin = "Nefes egzersizine başla", onTikla = onNefesBaslat)
            // Puan başlangıçta değil, yürüyüş bitince veriliyor — o yüzden
            // buton "tamamla" değil "başlat".
            gorev == Gorev.YURUYUS ->
                BirincilButon(metin = "Yürüyüşü başlat", onTikla = onYuruyusBaslat)
            else -> BirincilButon(metin = "Tamamlandı olarak işaretle", onTikla = onTamamla)
        }
    }
}

/**
 * Otomatik doğrulanan görevin durum notu — butonun yerine geçiyor.
 *
 * "Yapacak bir şey yok" demiyor; ne ölçüldüğünü ve puanın ne zaman
 * yazılacağını söylüyor. Sessiz bir boşluk bırakmak, kullanıcının görevi
 * kaydedemediğini sanmasına yol açardı.
 */
@Composable
private fun OtomatikGorevNotu(gorev: Gorev) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Buton))
            .background(color = Yesil50)
            .padding(Olcu.Aralik),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = YesilKoyu,
        )
        Spacer(modifier = Modifier.width(Olcu.Aralik))
        Text(
            text = when (gorev) {
                Gorev.TUKETIM_HEDEFI ->
                    "Bu görevi işaretlemene gerek yok. Ekran süren gün " +
                        "sonunda hedefinle karşılaştırılıyor; hedefine ne " +
                        "kadar yaklaşırsan o kadar puan yazılıyor."

                Gorev.GECE_TEMIZ ->
                    "Bu görevi işaretlemene gerek yok. 00–06 arası kullanımın " +
                        "gün sonunda ölçülüyor ve puanı kendiliğinden yazılıyor."

                else -> "Bu görev kullanımından otomatik doğrulanıyor."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
    }
}

/**
 * 4 hafta dolduğunda beliren değerlendirme hatırlatması
 * (`docs/spec-anket.md §6.4`).
 *
 * Dil dikkatli: "test olma vaktin geldi" değil, "yenileyebilirsin". Anketin
 * amacı ölçümü tazelemek; kullanıcıya ödev gibi sunulmuyor.
 */
@Composable
private fun DegerlendirmeKarti(onYenile: () -> Unit, onErtele: () -> Unit) {
    Kart {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonRozeti(ikon = Icons.AutoMirrored.Filled.FactCheck)
            Spacer(modifier = Modifier.width(Olcu.Aralik))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Değerlendirmeni yenileyebilirsin",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Text(
                    text = "Son değerlendirmenin üzerinden dört hafta geçti.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinIkincil,
                )
            }
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
            Cip(metin = "Şimdi yap", zemin = YesilAna, renk = Beyaz, onTikla = onYenile)
            Cip(metin = "Sonra", onTikla = onErtele)
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

/** Zil rozeti — sayı değil nokta. */
private val NoktaBoyu: Dp = Bosluk.S

/** Ruh hali tutamağı — beyaz dolgu, yeşil kenarlık (tasarım 07 dili). */
private val TutamakBoyu: Dp = Bosluk.L
private val TutamakKenari: Dp = Olcu.ButonKenarligi

/** Yeşil zemin üstündeki beyaz dolguların opaklığı. */
private const val YariSaydam = 0.2f

/** Yeşil zemin üstündeki ikincil metin opaklığı. */
private const val AzSaydam = 0.85f
