package tr.org.yesilay.yesiliz.ui.ekran.ana

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import tr.org.yesilay.yesiliz.ui.tema.Golge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import tr.org.yesilay.yesiliz.R
import tr.org.yesilay.yesiliz.domain.gorev.DogrulamaTuru
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.domain.gorev.RuhHaliPenceresi
import tr.org.yesilay.yesiliz.ui.binlik
import tr.org.yesilay.yesiliz.ui.puanEtiketi
import tr.org.yesilay.yesiliz.ui.suresiDk
import tr.org.yesilay.yesiliz.ui.bilesen.AltSayfa
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.BosDurum
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.KullaniciAvatari
import tr.org.yesilay.yesiliz.ui.bilesen.YenileDugmesi
import tr.org.yesilay.yesiliz.ui.bilesen.OrmanZemini
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
    onProfil: () -> Unit,
    onIstatistik: () -> Unit,
    onKullanimIzni: () -> Unit,
    onSaglikIzni: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnaViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    var acikGorev by remember { mutableStateOf<Gorev?>(null) }
    val kaydirmaDurumu = rememberScrollState()

    // PACKAGE_USAGE_STATS normal izin diyaloğu olmadığı için Ayarlar'dan
    // dönüşte callback gelmez. Kart/buton aynı anda doğru duruma geçsin.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val gozlemci = LifecycleEventObserver { _, olay ->
            if (olay == Lifecycle.Event.ON_RESUME) viewModel.kullanimIzniniTazele()
        }
        lifecycleOwner.lifecycle.addObserver(gozlemci)
        onDispose { lifecycleOwner.lifecycle.removeObserver(gozlemci) }
    }

    // Kaydırma miktarına göre ağacın ölçeklenmesi (1.0f -> 1.35f) ve parallax hareketi
    val kaydirmaOrani = (kaydirmaDurumu.value / 600f).coerceIn(0f, 1f)
    val agacOlcegi = 1.0f + (kaydirmaOrani * 0.35f)
    val agacKaymaY = kaydirmaDurumu.value * 0.35f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Gradyan.BaslikSonumu),
    ) {
        // 1. ARKA PLAN: Sabit / Parallax Ağaç Katmanı (En üstten başlayan yumuşak gradyen)
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Box(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(modifier = Modifier.height(68.dp))

            // Büyük Hero Ağaç Alanı (Kaydırdıkça Ölçeklenen ve Büyüyen Ağaç)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Ağacın arkasındaki düz yeşil alan derinliksiz duruyordu.
                // Siluet ağaçtan daha yavaş kayıyor — parallax'ın uzak katmanı.
                OrmanZemini(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { translationY = agacKaymaY * 0.4f },
                )

                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = agacOlcegi
                        scaleY = agacOlcegi
                        translationY = agacKaymaY
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(agacGorseli(durum.agacEvresi)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(340.dp),
                    )
                    SallananYapraklar(modifier = Modifier.size(340.dp))
                }
            }
        }

        // 2. KAYDIRILABİLİR İÇERİK KATMANI (Ağacın üstüne kayan yapı)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(kaydirmaDurumu),
        ) {
            // İlk açılışta ağacın ve saksısının tamamen görünmesini sağlayan üst pay
            Spacer(modifier = Modifier.height(460.dp))

            // Puan Kartı ve Özet Şeridi (İlk açılışta ekranda görünen puan ve ekran süresi)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Olcu.EkranKenari),
                verticalArrangement = Arrangement.spacedBy(Bosluk.M),
            ) {
                PuanKarti(puan = durum.puan, onTikla = onOdul)

                // Veri yoksa uydurma sayı göstermek yerine ne olduğunu söyle.
                // İzin verilmemişse sebep o; verilmişse henüz özet çıkmadı.
                // İzin yoksa talimat yetmiyor — ayarları açan bir eylem şart
                // (DBG-073), yoksa kullanıcı çıkışsız bir kartta kalıyor.
                if (durum.veriVarMi) {
                    OzetSeridi(durum)
                } else if (!durum.izinVarMi) {
                    Kart {
                        BosDurum(
                            baslik = "Kullanım izni gerekiyor",
                            aciklama = "Ekran süreni görebilmemiz için Ayarlar'dan kullanım " +
                                "erişimi vermen gerekiyor.",
                            eylem = {
                                BirincilButon(
                                    metin = "İzin ayarlarını aç",
                                    onTikla = onKullanimIzni,
                                )
                            },
                        )
                    }
                } else {
                    Kart {
                        BosDurum(
                            baslik = "Ölçmeye yeni başladık",
                            aciklama = "Bugünün özeti hazırlanıyor. Birkaç saat içinde ekran " +
                                "süren burada görünecek.",
                        )
                    }
                }

                // Karşılama akışında izin atlanabiliyor; atlayan kullanıcı
                // eksiğini ancak Profil'in derinliklerinde bulabiliyordu.
                // Kart kapatılabilir — bilinçli reddedeni sürekli dürtmek
                // §3'ün "zorlama yok" ilkesine aykırı.
                //
                // Kullanım izni ZATEN yukarıdaki boş durum kartında
                // anlatılıyor; ikisini birden göstermek aynı şeyi iki kez
                // söylemek olurdu. Hatırlatıcı yalnızca o kartın kapsamadığı
                // eksikleri sunuyor.
                val ustKartKullanimIznini = !durum.veriVarMi && !durum.izinVarMi
                durum.eksikIzinler
                    .filterNot { ustKartKullanimIznini && it == EksikIzin.KULLANIM }
                    .firstOrNull()?.let { eksik ->
                    IzinHatirlatmaKarti(
                        eksik = eksik,
                        onIzinVer = if (eksik == EksikIzin.KULLANIM) onKullanimIzni else onSaglikIzni,
                        onGizle = viewModel::izinHatirlatmasiniGizle,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Bosluk.L))

            // Alttaki Öğeler (Aşağı kaydırdıkça ağacın ve üst kartların üstüne kayan beyaz içerik kartı)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = Yaricap.AltSayfa, topEnd = Yaricap.AltSayfa),
                color = Beyaz,
                shadowElevation = Golge.Kart,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Olcu.EkranKenari,
                            end = Olcu.EkranKenari,
                            top = Olcu.BolumArasi,
                            bottom = Olcu.BolumArasi,
                        ),
                    verticalArrangement = Arrangement.spacedBy(Bosluk.M),
                ) {
                    if (durum.degerlendirmeYenilensin) {
                        DegerlendirmeKarti(
                            onYenile = onDegerlendirmeYenile,
                            onErtele = viewModel::degerlendirmeErtele,
                        )
                    }

                    RuhHaliKarti(
                        deger = durum.ruhHali,
                        kaydedildi = durum.ruhHaliKaydedildi,
                        pencere = durum.ruhHaliPenceresi,
                        doldurulan = durum.ruhHaliDoldurulan,
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
        }

        // 3. ÜST BAŞLIK KATMANI (En üstte duran ve tıklamaları doğrudan alan
        // etkileşimli başlık)
        //
        // Zemin kaydırmayla opaklaşıyor. Başlık şeffaftı ve metinleri beyazdı;
        // aşağı kaydırınca 2. katmandaki beyaz `Surface` altına giriyor ve
        // beyaz üstüne beyaz kalıyordu — satır görünmez oluyor ama tıklamayı
        // almaya devam ediyordu.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(YesilAna.copy(alpha = kaydirmaOrani)),
        ) {
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
                    avatarId = durum.avatarId,
                    zemin = Beyaz.copy(alpha = YariSaydam),
                    onTikla = onProfil,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onProfil),
                ) {
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
                // Seri 0 iken çip bir eksikliği duyuruyor, bilgi vermiyor —
                // üstelik K40'a göre 0 çoğu zaman "gün henüz ölçülmedi"
                // demek. İlk seri gününde belirmesi küçük bir ödül anı
                // yaratıyor (5. tarama G5-032).
                if (durum.serideGun > 0) {
                    Cip(
                        onTikla = onIstatistik,
                        metin = durum.seriEtiketi,
                        ikon = Icons.Filled.LocalFireDepartment,
                        zemin = Beyaz.copy(alpha = YariSaydam),
                        renk = Beyaz,
                    )
                }
                YenileDugmesi(
                    onTikla = viewModel::tazele,
                    donuyor = durum.tazeleniyor,
                )
                ZilIkonu(okunmamis = durum.okunmamisBildirim, onTikla = onBildirimler)
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

/**
 * Başlıktaki zil — okunmamış varsa üstünde küçük beyaz nokta.
 *
 * Teslim edilen 08 tasarımında zil yok; bildirim listesine (35) ulaşacak
 * bir giriş noktası da yok. Serinin sağına, aynı hizaya konuldu — başlığın
 * dengesi bozulmuyor ve rozet sayı yerine **nokta**: "3 okunmamış bildirim"
 * demek uygulamayı açtırmaya çalışan bir dil, uygulamanın amacıyla çelişir.
 */
/**
 * Eksik izin hatırlatıcısı.
 *
 * Ekranda zaten kurulu olan `Kart` + `BosDurum` deseni kullanılıyor
 * (kullanım izni kartıyla aynı dil); yeni bir uyarı bileşeni yazmak iki
 * farklı görünüm üretirdi.
 */
@Composable
private fun IzinHatirlatmaKarti(
    eksik: EksikIzin,
    onIzinVer: () -> Unit,
    onGizle: () -> Unit,
) {
    Kart {
        BosDurum(
            baslik = eksik.baslik,
            aciklama = eksik.aciklama,
            eylem = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BirincilButon(metin = "İzin ver", onTikla = onIzinVer)
                    Spacer(modifier = Modifier.height(Bosluk.S))
                    Text(
                        text = "Şimdi değil",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinIkincil,
                        modifier = Modifier.clickable(onClick = onGizle),
                    )
                }
            },
        )
    }
}

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

/** Ağaç evresi (1..9) → drawable. Evre dışı değer en yakın uca kırpılır. */
private fun agacGorseli(evre: Int): Int = when (evre.coerceIn(1, 9)) {
    1 -> R.drawable.downscaled_tree_phase_1
    2 -> R.drawable.downscaled_tree_phase_2
    3 -> R.drawable.downscaled_tree_phase_3
    4 -> R.drawable.downscaled_tree_phase_4
    5 -> R.drawable.downscaled_tree_phase_5
    6 -> R.drawable.downscaled_tree_phase_6
    7 -> R.drawable.downscaled_tree_phase_7
    8 -> R.drawable.downscaled_tree_phase_8
    else -> R.drawable.downscaled_tree_phase_9
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
    pencere: String,
    doldurulan: Int,
    onDegis: (Float) -> Unit,
    onBirakildi: () -> Unit,
) {
    Kart {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$pencere: nasıl hissediyorsun?",
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
        // Gün üç pencereye bölündü (sabah/öğlen/akşam). "Bugün 2/3" demek,
        // kaydın niye tekrar istendiğini ve puanın nereden geldiğini
        // açıklıyor — eskiden tek kayıt vardı ve kullanıcı gün içinde
        // tekrar sürüklediğinde neden puan almadığını bilmiyordu.
        Text(
            text = if (kaydedildi) {
                "Bu pencere kaydedildi · bugün $doldurulan/" +
                    "${RuhHaliPenceresi.GUNLUK_PENCERE_SAYISI}. " +
                    "Fikrin değişirse tekrar sürükleyebilirsin."
            } else {
                "Sürükle · bugün $doldurulan/" +
                    "${RuhHaliPenceresi.GUNLUK_PENCERE_SAYISI} kayıt"
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
    // `IntrinsicSize.Min` + `fillMaxHeight`: iki kart farklı yükseklikteydi.
    // Sağdaki kısa kalıyor, başlığı iki satıra kırılınca alt metniyle
    // çakışıyordu (`debug.md` O-10).
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Olcu.Aralik),
    ) {
        KesfetKarti(
            ikon = Icons.AutoMirrored.Filled.MenuBook,
            baslik = "Bilgi Bankası",
            aciklama = "Ekran bağımlılığı hakkında doğru bilgiler.",
            onTikla = onBilgiBankasi,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        KesfetKarti(
            ikon = Icons.AutoMirrored.Filled.FactCheck,
            baslik = "Kendini Test Et",
            aciklama = "Farkındalığını artır.",
            onTikla = onKendiniTest,
            modifier = Modifier.weight(1f).fillMaxHeight(),
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
                Cip(
                    metin = "Tamamlandı",
                    ikon = Icons.Filled.CheckCircle,
                    zemin = YesilNane,
                    renk = YesilAna,
                )
            } else if (gorev.gunlukLimit > 1 && gorev.yapilan > 0) {
                // Kısmen yapılmış çok pencereli görev (ruh hali: 3 pencere).
                // "Tamamlandı" da "+3p" de yanlış olurdu; kaçının kaldığını
                // söylemek doğru olan.
                Cip(
                    metin = "${gorev.yapilan}/${gorev.gunlukLimit}",
                    zemin = YesilNane,
                    renk = YesilAna,
                )
            } else {
                Cip(metin = gorev.puan.puanEtiketi(), renk = YesilAna)
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
