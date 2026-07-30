package tr.org.yesilay.yesiliz.ui.ekran.profil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import tr.org.yesilay.yesiliz.nudge.NudgeIzni
import tr.org.yesilay.yesiliz.ui.bilesen.AnahtarSatiri
import tr.org.yesilay.yesiliz.ui.bilesen.AyarSatiri
import tr.org.yesilay.yesiliz.ui.bilesen.BolumBasligi
import tr.org.yesilay.yesiliz.ui.bilesen.Cip
import tr.org.yesilay.yesiliz.ui.bilesen.IkonRozeti
import tr.org.yesilay.yesiliz.ui.bilesen.Kart
import tr.org.yesilay.yesiliz.ui.bilesen.KartAyraci
import tr.org.yesilay.yesiliz.ui.bilesen.KullaniciAvatari
import tr.org.yesilay.yesiliz.ui.bilesen.MetinBaglanti
import tr.org.yesilay.yesiliz.ui.bilesen.BirincilButon
import tr.org.yesilay.yesiliz.ui.bilesen.OnayKutusu
import tr.org.yesilay.yesiliz.ui.bilesen.SecenekKarti
import tr.org.yesilay.yesiliz.ui.bilesen.TehlikeliButon
import tr.org.yesilay.yesiliz.ui.bilesen.YesilBaslikliEkran
import tr.org.yesilay.yesiliz.ui.tema.Beyaz
import tr.org.yesilay.yesiliz.ui.tema.Bosluk
import tr.org.yesilay.yesiliz.ui.tema.KirmiziIstisna
import tr.org.yesilay.yesiliz.ui.tema.KirmiziIstisnaZemin
import tr.org.yesilay.yesiliz.ui.tema.MetinBirincil
import tr.org.yesilay.yesiliz.ui.tema.MetinIkincil
import tr.org.yesilay.yesiliz.ui.tema.MetinUcuncul
import tr.org.yesilay.yesiliz.ui.tema.Olcu
import tr.org.yesilay.yesiliz.ui.tema.Yaricap
import tr.org.yesilay.yesiliz.ui.tema.YesilAna
import tr.org.yesilay.yesiliz.ui.tema.YesilKoyu
import tr.org.yesilay.yesiliz.ui.tema.YesilNane

/**
 * Profil ve ayarlar — `docs/tasarim-referans.md §2 / 12`.
 *
 * Üç bölüm: HEDEFLERİM · VERİLERİM · GİZLİLİK, artı DESTEK. "Verilerimi sil"
 * ve "Geçmişi indir" KVKK gereği görünür yerde duruyor, ayarların dibine
 * gömülmüyor.
 *
 * Alt sayfalar (23-29, `docs/tasarim-referans.md §2`) Ödüller ekranındaki
 * kalıpla aynı: ayrı bir NavHost rotası değil, yerel `remember` state +
 * `ModalBottomSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilEkran(
    onEbeveyn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfilViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var toplananVerilerAcik by remember { mutableStateOf(false) }
    var gecmisiIndirAcik by remember { mutableStateOf(false) }
    var verileriSilAcik by remember { mutableStateOf(false) }
    var hesabiSilAcik by remember { mutableStateOf(false) }
    var yardimAcik by remember { mutableStateOf(false) }
    var hakkindaAcik by remember { mutableStateOf(false) }
    val sayfaDurumu = rememberModalBottomSheetState()

    YesilBaslikliEkran(baslik = "Profil", modifier = modifier) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(Olcu.KartIci))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                KullaniciAvatari(
                    isim = durum.isim,
                    olcu = Olcu.AvatarBuyuk,
                    zemin = YesilKoyu,
                )
                Spacer(modifier = Modifier.height(Bosluk.S))
                Text(
                    text = durum.isim,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MetinBirincil,
                )
                Spacer(modifier = Modifier.height(Bosluk.S))
                Cip(metin = durum.ligAdi, zemin = YesilNane)
            }

            Spacer(modifier = Modifier.height(Olcu.BolumArasi))

            BolumBasligi("Hedeflerim")
            Spacer(modifier = Modifier.height(Bosluk.S))
            Kart(icBosluk = Olcu.Aralik) {
                AyarSatiri(Icons.Filled.TrackChanges, "Günlük hedef") {}
                KartAyraci()
                AyarSatiri(Icons.Filled.Apps, "Uygulama kategorileri") {}
                KartAyraci()
                AyarSatiri(Icons.Filled.Notifications, "Bildirim saatleri") {}
            }

            Spacer(modifier = Modifier.height(Olcu.BolumArasi))

            BolumBasligi("Verilerim")
            Spacer(modifier = Modifier.height(Bosluk.S))
            Kart(icBosluk = Olcu.Aralik) {
                AyarSatiri(
                    Icons.AutoMirrored.Filled.Notes,
                    "Toplanan veriler",
                    onTikla = { toplananVerilerAcik = true },
                )
                KartAyraci()
                AyarSatiri(
                    Icons.Filled.Download,
                    "Geçmişi indir",
                    onTikla = { gecmisiIndirAcik = true },
                )
                KartAyraci()
                AyarSatiri(
                    Icons.Filled.Delete,
                    "Verilerimi sil",
                    onTikla = { verileriSilAcik = true },
                )
                KartAyraci()
                AyarSatiri(
                    Icons.Filled.DeleteForever,
                    "Hesabımı sil",
                    onTikla = { hesabiSilAcik = true },
                )
            }

            Spacer(modifier = Modifier.height(Olcu.BolumArasi))

            BolumBasligi("Gizlilik")
            Spacer(modifier = Modifier.height(Bosluk.S))
            Kart(icBosluk = Olcu.Aralik) {
                AnahtarSatiri(
                    ikon = Icons.Filled.Lock,
                    etiket = "Yeşilay ile paylaşım",
                    acik = durum.yesilayPaylasimi,
                    onDegis = viewModel::yesilayPaylasimi,
                )
                KartAyraci()
                AnahtarSatiri(
                    ikon = Icons.Filled.EmojiEvents,
                    etiket = "Ligde görün",
                    acik = durum.ligdeGorun,
                    onDegis = viewModel::ligdeGorun,
                )
                KartAyraci()
                AnahtarSatiri(
                    ikon = Icons.Filled.SelfImprovement,
                    etiket = "Mola hatırlatıcı",
                    acik = durum.molaHatirlaticiAcik,
                    onDegis = { acik ->
                        viewModel.molaHatirlaticiAyarla(acik)
                        if (acik && !NudgeIzni.verildiMi(context) && NudgeIzni.ayarEkraniVarMi(context)) {
                            context.startActivity(NudgeIzni.ayarIntent(context))
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(Olcu.BolumArasi))

            Kart(icBosluk = Olcu.Aralik) {
                AyarSatiri(Icons.Filled.FamilyRestroom, "Ebeveyn paneli", onTikla = onEbeveyn)
            }

            Spacer(modifier = Modifier.height(Olcu.BolumArasi))

            BolumBasligi("Destek")
            Spacer(modifier = Modifier.height(Bosluk.S))
            Kart(icBosluk = Olcu.Aralik) {
                AyarSatiri(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    "Yardım",
                    onTikla = { yardimAcik = true },
                )
                KartAyraci()
                AyarSatiri(
                    Icons.Filled.Info,
                    "Hakkında",
                    onTikla = { hakkindaAcik = true },
                )
            }

            // Yalnızca debug derlemesinde görünür — release'de `DemoVeri`
            // no-op ve `demoKullanilabilir` false.
            if (durum.demoKullanilabilir) {
                Spacer(modifier = Modifier.height(Olcu.BolumArasi))
                BolumBasligi("Geliştirici")
                Spacer(modifier = Modifier.height(Bosluk.S))
                Kart(icBosluk = Olcu.Aralik) {
                    AyarSatiri(
                        Icons.Filled.Science,
                        "Örnek veri oluştur",
                        onTikla = viewModel::demoTohumla,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
        }
    }

    if (toplananVerilerAcik) {
        ModalBottomSheet(
            onDismissRequest = { toplananVerilerAcik = false },
            sheetState = sayfaDurumu,
            containerColor = Beyaz,
        ) {
            ToplananVerilerIcerik(onKapat = { toplananVerilerAcik = false })
        }
    }

    if (gecmisiIndirAcik) {
        ModalBottomSheet(
            onDismissRequest = { gecmisiIndirAcik = false },
            sheetState = sayfaDurumu,
            containerColor = Beyaz,
        ) {
            GecmisiIndirIcerik(onKapat = { gecmisiIndirAcik = false })
        }
    }

    if (verileriSilAcik) {
        ModalBottomSheet(
            onDismissRequest = { verileriSilAcik = false },
            sheetState = sayfaDurumu,
            containerColor = Beyaz,
        ) {
            VerileriSilIcerik(
                onOnayla = {
                    viewModel.verileriSil()
                    verileriSilAcik = false
                },
                onVazgec = { verileriSilAcik = false },
            )
        }
    }

    if (hesabiSilAcik) {
        ModalBottomSheet(
            onDismissRequest = { hesabiSilAcik = false },
            sheetState = sayfaDurumu,
            containerColor = Beyaz,
        ) {
            HesabiSilIcerik(
                onOnayla = {
                    viewModel.verileriSil()
                    hesabiSilAcik = false
                },
                onVazgec = { hesabiSilAcik = false },
            )
        }
    }

    if (yardimAcik) {
        ModalBottomSheet(
            onDismissRequest = { yardimAcik = false },
            sheetState = sayfaDurumu,
            containerColor = Beyaz,
        ) {
            YardimIcerik(onKapat = { yardimAcik = false })
        }
    }

    if (hakkindaAcik) {
        ModalBottomSheet(
            onDismissRequest = { hakkindaAcik = false },
            sheetState = sayfaDurumu,
            containerColor = Beyaz,
        ) {
            HakkindaIcerik(onKapat = { hakkindaAcik = false })
        }
    }
}

/** Alt sayfa başlığı — "X + kapat" düzeni, tüm alt sayfalar bunu paylaşıyor. */
@Composable
private fun AltSayfaBasligi(baslik: String, onKapat: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = baslik,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
            modifier = Modifier.weight(1f),
        )
        androidx.compose.material3.IconButton(onClick = onKapat) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Kapat",
                tint = MetinBirincil,
            )
        }
    }
}

/** Alt sayfa 23 — Toplanan veriler. Yalnızca özet/kategori, içerik yok. */
@Composable
private fun ToplananVerilerIcerik(onKapat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
    ) {
        AltSayfaBasligi("Toplanan veriler", onKapat)
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Deneyimini kişiselleştirmek için yalnızca aşağıdaki verileri topluyoruz.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        val satirlar = listOf(
            Triple(Icons.Filled.PhoneAndroid, "Uygulama kullanım süresi", "Kategori bazında, uygulama isimleri olmadan"),
            Triple(Icons.AutoMirrored.Filled.DirectionsWalk, "Adım sayısı", "Telefonundaki hareket sensöründen"),
            Triple(Icons.AutoMirrored.Filled.Assignment, "Anket ve kendini test et cevapları", "Yalnızca sana özel geri bildirim için"),
            Triple(Icons.Filled.LocalFireDepartment, "Günlük giriş serisi ve puanlar", "İlerlemeni ve ödüllerini göstermek için"),
            Triple(Icons.Filled.Person, "Ad ve yaş grubu", "Profilini kişiselleştirmek için"),
        )
        satirlar.forEachIndexed { i, (ikon, baslik, aciklama) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Bosluk.S),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Olcu.Aralik),
            ) {
                IkonRozeti(ikon = ikon, zemin = YesilNane, renk = YesilAna)
                Column {
                    Text(
                        text = baslik,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MetinBirincil,
                    )
                    Text(
                        text = aciklama,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinUcuncul,
                    )
                }
            }
            if (i != satirlar.lastIndex) KartAyraci()
        }

        Spacer(modifier = Modifier.height(Bosluk.S))
        GizlilikNotu("Uygulama içeriklerine (mesaj, arama, fotoğraf) asla erişilmez.")
    }
}

/** Alt sayfa 24 — Geçmişi indir. PDF üretimi Faz 2, burada yalnızca akış. */
@Composable
private fun GecmisiIndirIcerik(onKapat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
    ) {
        AltSayfaBasligi("Geçmişi indir", onKapat)
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Tüm ekran süresi, hedef ve puan geçmişini bir dosya olarak al.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        SecenekKarti(
            ikon = Icons.Filled.Description,
            baslik = "PDF raporu",
            aciklama = "Okunabilir özet, grafiklerle",
            secili = true,
            onayIkonuMu = true,
            onTikla = {},
        )

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        GizlilikNotu(
            "Dosya hazırlanınca kayıtlı e-posta adresine gönderilecek.",
            ikon = androidx.compose.material.icons.Icons.Filled.Sync,
        )

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        BirincilButon(metin = "İndirmeyi başlat", onTikla = { /* PDF üretimi Faz 2 — backend gerekiyor */ })
    }
}

/** Alt sayfa 25 — Verilerimi kalıcı sil. CLAUDE.md §12 K10: KirmiziIstisna. */
@Composable
private fun VerileriSilIcerik(onOnayla: () -> Unit, onVazgec: () -> Unit) {
    var onayli by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IkonRozeti(
            ikon = Icons.Filled.DeleteForever,
            zemin = KirmiziIstisnaZemin,
            renk = KirmiziIstisna,
            olcu = Olcu.AvatarBuyuk,
            sekil = androidx.compose.foundation.shape.CircleShape,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "Verilerini kalıcı olarak sil?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Bu işlem geri alınamaz. Sildiğinde aşağıdakiler kaybolur:",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Yaricap.Buton))
                .background(color = KirmiziIstisnaZemin)
                .padding(Olcu.Aralik),
        ) {
            listOf(
                "Ekran süresi ve hareket geçmişin",
                "Puanların, rozetlerin ve ödül ilerlemen",
                "Ebeveyn cihazıyla eşleştirmen",
            ).forEach { madde ->
                MaddeSatiri(madde)
            }
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        OnayKutusu(
            metin = "Verilerimin kalıcı olarak silineceğini anlıyorum.",
            secili = onayli,
            onDegis = { onayli = it },
            tehlikeliMi = true,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        TehlikeliButon(metin = "Verilerimi kalıcı olarak sil", onTikla = onOnayla, etkin = onayli)
        MetinBaglanti(metin = "Vazgeç", onTikla = onVazgec)
    }
}

/**
 * Alt sayfa 26 — Hesabımı kalıcı sil. Gerçek hesap sistemi Faz 2'ye kadar
 * "hesabı sil" ile "verileri sil" AYNI işlemi yapıyor ([ProfilViewModel.verileriSil]).
 */
@Composable
private fun HesabiSilIcerik(onOnayla: () -> Unit, onVazgec: () -> Unit) {
    var girilenMetin by remember { mutableStateOf("") }
    val onayli = girilenMetin == "SİL"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IkonRozeti(
            ikon = Icons.Filled.Cancel,
            zemin = KirmiziIstisnaZemin,
            renk = KirmiziIstisna,
            olcu = Olcu.AvatarBuyuk,
            sekil = androidx.compose.foundation.shape.CircleShape,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "Hesabını kalıcı olarak sil?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Bu işlem geri alınamaz. Hesabını sildiğinde:",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Yaricap.Buton))
                .background(color = KirmiziIstisnaZemin)
                .padding(Olcu.Aralik),
        ) {
            listOf(
                "Profilin ve tüm verilerin silinir",
                "Puanların, rozetlerin ve ödüllerin kaybolur",
                "Ebeveyn cihazıyla eşleştirmen sona erer",
                "Bu hesapla bir daha giriş yapamazsın",
            ).forEach { madde -> MaddeSatiri(madde) }
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "Onaylamak için \"SİL\" yaz",
            style = MaterialTheme.typography.labelLarge,
            color = MetinIkincil,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        OutlinedTextField(
            value = girilenMetin,
            onValueChange = { girilenMetin = it },
            placeholder = { Text("SİL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        TehlikeliButon(metin = "Hesabımı kalıcı olarak sil", onTikla = onOnayla, etkin = onayli)
        MetinBaglanti(metin = "Vazgeç", onTikla = onVazgec)
    }
}

/** Alt sayfa 28 — Yardım. Statik SSS, backend gerektirmiyor. */
@Composable
private fun YardimIcerik(onKapat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
    ) {
        AltSayfaBasligi("Yardım", onKapat)
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Sık sorulan sorular ve destek yolları.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        val sorular = listOf(
            "Puanlarım nasıl hesaplanıyor?",
            "Ebeveyn paneli eşleştirmesini nasıl kaldırırım?",
            "Hedefimi nasıl değiştiririm?",
            "Verilerim kimlerle paylaşılıyor?",
        )
        sorular.forEachIndexed { i, soru ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Bosluk.M),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = soru,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MetinBirincil,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MetinUcuncul,
                )
            }
            if (i != sorular.lastIndex) KartAyraci()
        }
    }
}

/** Alt sayfa 29 — Hakkında. Sürüm sabit — `BuildConfig.VERSION_NAME` Faz 2. */
@Composable
private fun HakkindaIcerik(onKapat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            androidx.compose.material3.IconButton(onClick = onKapat) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Kapat",
                    tint = MetinBirincil,
                )
            }
        }

        IkonRozeti(
            ikon = Icons.Filled.Park,
            zemin = YesilKoyu,
            renk = Beyaz,
            olcu = Olcu.IkonRozeti,
            sekil = RoundedCornerShape(Yaricap.Cip),
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Yeşilİz",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Text(
            text = "Sürüm 2.4.1",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinUcuncul,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "Yeşilİz, ekranla sağlıklı bir ilişki kurmana yardımcı olur.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        listOf("Kullanım şartları", "Gizlilik politikası", "Açık kaynak lisansları").forEachIndexed { i, satir ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Bosluk.M),
            ) {
                Text(
                    text = satir,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MetinBirincil,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MetinUcuncul,
                )
            }
            if (i != 2) KartAyraci()
        }

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "© 2026 Yeşilİz",
            style = MaterialTheme.typography.bodySmall,
            color = MetinUcuncul,
        )
    }
}

@Composable
private fun MaddeSatiri(metin: String) {
    Row(modifier = Modifier.padding(vertical = Bosluk.Xs)) {
        Text(text = "•  ", color = KirmiziIstisna)
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyMedium,
            color = MetinBirincil,
        )
    }
}

@Composable
private fun GizlilikNotu(
    metin: String,
    ikon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Lock,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Buton))
            .background(color = YesilNane)
            .padding(Olcu.Aralik),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Bosluk.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = ikon, contentDescription = null, tint = YesilAna)
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyMedium,
            color = MetinBirincil,
        )
    }
}
