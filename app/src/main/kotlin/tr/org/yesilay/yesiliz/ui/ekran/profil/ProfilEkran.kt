package tr.org.yesilay.yesiliz.ui.ekran.profil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import tr.org.yesilay.yesiliz.data.tercih.BildirimSaatleri
import tr.org.yesilay.yesiliz.nudge.NudgeIzni
import tr.org.yesilay.yesiliz.ui.bilesen.AltSayfa
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
import tr.org.yesilay.yesiliz.ui.tema.Yesil50
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
    onGunlukHedef: () -> Unit,
    onKategoriler: () -> Unit,
    onAnketYenile: () -> Unit,
    onSaglikIzni: () -> Unit,
    onSilindi: () -> Unit,
    onHesapOlustur: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfilViewModel = hiltViewModel(),
) {
    val durum by viewModel.durum.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Silme bitince hesap akışına dönülüyor: `Tercihler.sifirla()` onboarding
    // bayrağını da sıfırladı, kullanıcıyı silinmiş verinin ekranlarında
    // bırakmak boş grafikler ve "0 puan" göstermek olurdu.
    LaunchedEffect(Unit) {
        viewModel.silindi.collect { onSilindi() }
    }

    var toplananVerilerAcik by remember { mutableStateOf(false) }
    var gecmisiIndirAcik by remember { mutableStateOf(false) }
    var verileriSilAcik by remember { mutableStateOf(false) }
    var hesabiSilAcik by remember { mutableStateOf(false) }
    var yardimAcik by remember { mutableStateOf(false) }
    var hakkindaAcik by remember { mutableStateOf(false) }
    var yedamAcik by remember { mutableStateOf(false) }
    var bildirimSaatleriAcik by remember { mutableStateOf(false) }
    var cikisOnayi by remember { mutableStateOf(false) }
    val bildirimIzni = bildirimIzniIstegi()

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
                AyarSatiri(Icons.Filled.TrackChanges, "Günlük hedef", onTikla = onGunlukHedef)
                KartAyraci()
                AyarSatiri(Icons.Filled.Apps, "Uygulama kategorileri", onTikla = onKategoriler)
                KartAyraci()
                AyarSatiri(
                    Icons.Filled.Notifications,
                    "Bildirim saatleri",
                    onTikla = { bildirimSaatleriAcik = true },
                )
                KartAyraci()
                AyarSatiri(
                    Icons.AutoMirrored.Filled.FactCheck,
                    "Değerlendirmeyi yenile",
                    onTikla = onAnketYenile,
                )
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
                KartAyraci()
                AyarSatiri(
                    Icons.Filled.MonitorHeart,
                    "Adım ve hareket izni",
                    onTikla = onSaglikIzni,
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

            // Hesap bölümü.
            //
            // `HesapDeposu.cikisYap()` üç deponun hepsinde uygulanmıştı ama
            // hiçbir ekrandan çağrılmıyordu: giriş yapan kullanıcı hesabını
            // değiştiremiyor, tek yol "Hesabımı sil" oluyordu -- ki o da
            // çöküyordu (`debug.md` Y-3, K-2).
            //
            // Misafirin çıkacağı bir hesap yok ama KURACAĞI var: `Oturum`
            // KDoc'u "misafir de Profil'den hesap oluşturabilir" diyordu ve
            // öyle bir satır hiç yazılmamıştı.
            Spacer(modifier = Modifier.height(Olcu.BolumArasi))
            BolumBasligi("Hesap")
            Spacer(modifier = Modifier.height(Bosluk.S))
            Kart(icBosluk = Olcu.Aralik) {
                if (durum.hesapVarMi) {
                    AyarSatiri(
                        Icons.AutoMirrored.Filled.Logout,
                        "Çıkış yap",
                        altMetin = durum.eposta,
                        onTikla = { cikisOnayi = true },
                    )
                } else {
                    AyarSatiri(
                        Icons.Filled.PersonAdd,
                        "Hesap oluştur",
                        altMetin = "Misafir olarak kullanıyorsun",
                        onTikla = onHesapOlustur,
                    )
                }
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
                    Icons.Filled.SupportAgent,
                    "YEDAM hakkında",
                    onTikla = { yedamAcik = true },
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
        AltSayfa(onKapat = { toplananVerilerAcik = false }) {
            ToplananVerilerIcerik(onKapat = { toplananVerilerAcik = false })
        }
    }

    if (gecmisiIndirAcik) {
        AltSayfa(onKapat = { gecmisiIndirAcik = false }) {
            GecmisiIndirIcerik(
                onKapat = { gecmisiIndirAcik = false },
                onDisaAktar = {
                    viewModel.gecmisiDisaAktar { niyet ->
                        context.startActivity(
                            android.content.Intent.createChooser(niyet, "Verilerini paylaş"),
                        )
                        gecmisiIndirAcik = false
                    }
                },
            )
        }
    }

    if (verileriSilAcik) {
        AltSayfa(onKapat = { verileriSilAcik = false }) {
            VerileriSilIcerik(
                durum = durum,
                onOnayla = viewModel::verileriSil,
                onVazgec = { verileriSilAcik = false },
            )
        }
    }

    if (hesabiSilAcik) {
        AltSayfa(onKapat = { hesabiSilAcik = false }) {
            HesabiSilIcerik(
                durum = durum,
                onOnayla = viewModel::hesabiSil,
                onVazgec = { hesabiSilAcik = false },
                onUyariyiKapat = viewModel::uyariyiKapat,
            )
        }
    }

    if (cikisOnayi) {
        AltSayfa(onKapat = { cikisOnayi = false }) {
            CikisOnayi(
                eposta = durum.eposta,
                onOnayla = {
                    viewModel.cikisYap()
                    cikisOnayi = false
                },
                onVazgec = { cikisOnayi = false },
            )
        }
    }

    if (yardimAcik) {
        AltSayfa(onKapat = { yardimAcik = false }) {
            YardimIcerik(onKapat = { yardimAcik = false })
        }
    }

    if (hakkindaAcik) {
        AltSayfa(onKapat = { hakkindaAcik = false }) {
            HakkindaIcerik(onKapat = { hakkindaAcik = false })
        }
    }

    if (yedamAcik) {
        AltSayfa(onKapat = { yedamAcik = false }) {
            YedamHakkindaIcerik(onKapat = { yedamAcik = false })
        }
    }

    if (bildirimSaatleriAcik) {
        AltSayfa(onKapat = { bildirimSaatleriAcik = false }) {
            BildirimSaatleriIcerik(
                durum = durum,
                onDegis = { saatler ->
                    // Hatırlatma AÇILIRKEN bildirim izni isteniyor.
                    // Reddedilirse defter yine çalışıyor (Bildirimler
                    // ekranında görünüyor), yalnızca sistem bildirimi
                    // gelmiyor — HatirlatmaWorker bunu kendi içinde
                    // kontrol ediyor.
                    if (saatler.acik && !durum.bildirimSaatleri.acik) {
                        bildirimIzni?.invoke()
                    }
                    viewModel.bildirimSaatleriAyarla(saatler)
                },
                onKapat = { bildirimSaatleriAcik = false },
            )
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
private fun GecmisiIndirIcerik(onKapat: () -> Unit, onDisaAktar: () -> Unit) {
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

        // Tasarım 24'te tek seçenek "PDF raporu" var; PDF üretimi backend
        // işi ve çevrimdışı çalışmıyor. Ham veri dışa aktarımı KVKK
        // taşınabilirliğini zaten karşılıyor ve dosya gerçekten
        // taşınabilir oluyor (Excel'de açılıyor).
        SecenekKarti(
            ikon = Icons.Filled.Description,
            baslik = "Ham veri (JSON + CSV)",
            aciklama = "Cihazda üretilir, hemen paylaşılır",
            secili = true,
            onayIkonuMu = true,
            onTikla = {},
        )

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        GizlilikNotu(
            "Dosyalar telefonunda üretilir; istediğin uygulamayla paylaşabilirsin. " +
                "Uygulama kullanım olayların (hangi uygulama, ne zaman) dosyaya " +
                "dahil edilmez.",
            ikon = androidx.compose.material.icons.Icons.Filled.Sync,
        )

        Spacer(modifier = Modifier.height(Olcu.Aralik))
        BirincilButon(metin = "Dosyaları oluştur ve paylaş", onTikla = onDisaAktar)
    }
}

/**
 * Çıkış onayı.
 *
 * Yıkıcı bir işlem değil -- veri cihazda kalıyor -- o yüzden silme
 * akışlarındaki kırmızı ve onay kutusu YOK. Ama tek dokunuşla da
 * yapılmıyor: yanlışlıkla çıkmak, kullanıcıyı şifresini hatırlamak
 * zorunda bırakır.
 */
@Composable
private fun CikisOnayi(eposta: String, onOnayla: () -> Unit, onVazgec: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IkonRozeti(
            ikon = Icons.AutoMirrored.Filled.Logout,
            zemin = YesilNane,
            renk = YesilKoyu,
            olcu = Olcu.AvatarBuyuk,
            sekil = androidx.compose.foundation.shape.CircleShape,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        Text(
            text = "Çıkış yapılsın mı?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Bosluk.S))
        Text(
            text = "$eposta hesabından çıkacaksın. Cihazındaki geçmişin " +
                "silinmiyor; aynı hesapla girdiğinde her şey yerinde olacak.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        BirincilButon(metin = "Çıkış yap", onTikla = onOnayla)
        MetinBaglanti(metin = "Vazgeç", onTikla = onVazgec)
    }
}

/** Alt sayfa 25 — Verilerimi kalıcı sil. CLAUDE.md §12 K10: KirmiziIstisna. */
@Composable
private fun VerileriSilIcerik(
    durum: ProfilDurum,
    onOnayla: () -> Unit,
    onVazgec: () -> Unit,
) {
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
        TehlikeliButon(
            metin = if (durum.siliniyor) "Siliniyor…" else "Verilerimi kalıcı olarak sil",
            onTikla = onOnayla,
            etkin = onayli && !durum.siliniyor,
        )
        MetinBaglanti(metin = "Vazgeç", onTikla = onVazgec)
    }
}

/**
 * Silme bitti ama sunucudaki kopya duruyor.
 *
 * Kullanıcıyı sessizce dışarı atmak yerine bunu söylüyoruz: "kalıcı sil"
 * dedikten sonra sunucuda veri kalmışsa bilmeye hakkı var (`debug.md` K-2).
 */
@Composable
private fun SunucudaKaldiUyarisi(onKapat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Yaricap.Buton))
            .background(color = KirmiziIstisnaZemin)
            .padding(Olcu.Aralik),
    ) {
        Text(
            text = "Cihazındaki her şey silindi.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MetinBirincil,
        )
        Spacer(modifier = Modifier.height(Bosluk.Xs))
        Text(
            text = "Sunucuya şu an ulaşılamadığı için oradaki kaydın " +
                "silinemedi. Bağlantın olduğunda tekrar giriş yapıp " +
                "silme işlemini yenileyebilirsin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        TehlikeliButon(metin = "Anladım", onTikla = onKapat)
    }
}

/**
 * Alt sayfa 26 — Hesabımı kalıcı sil.
 *
 * "Verilerimi sil"den farkı: cihazın yanında **sunucudaki hesap da**
 * siliniyor (`DELETE /auth/hesap`). Eskiden ikisi aynı fonksiyonu çağırıyor,
 * sunucuya hiç uğramıyordu (`debug.md` K-2).
 */
@Composable
private fun HesabiSilIcerik(
    durum: ProfilDurum,
    onOnayla: () -> Unit,
    onVazgec: () -> Unit,
    onUyariyiKapat: () -> Unit,
) {
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

        if (durum.sunucudaKaldiUyarisi) {
            SunucudaKaldiUyarisi(onKapat = onUyariyiKapat)
            return@Column
        }

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
            enabled = !durum.siliniyor,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))
        TehlikeliButon(
            metin = if (durum.siliniyor) "Siliniyor…" else "Hesabımı kalıcı olarak sil",
            onTikla = onOnayla,
            etkin = onayli && !durum.siliniyor,
        )
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

/**
 * Alt sayfa 27 — YEDAM hakkında.
 *
 * **Sadece bilgilendirme.** Yönlendirme/rıza akışı ayrı bir ekran
 * (`ui/ekran/yedam/`), yalnızca yüksek bantta ve kullanıcı onayıyla açılıyor
 * (`docs/tasarim-referans.md §2 / 27` notu).
 */
@Composable
private fun YedamHakkindaIcerik(onKapat: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
    ) {
        AltSayfaBasligi("YEDAM hakkında", onKapat)
        Spacer(modifier = Modifier.height(Bosluk.S))

        IkonRozeti(
            ikon = Icons.Filled.SupportAgent,
            zemin = YesilKoyu,
            renk = Beyaz,
            sekil = RoundedCornerShape(Yaricap.Cip),
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Text(
            text = "YEDAM (Yeşilay Danışmanlık Merkezi), bağımlılık alanında " +
                "ücretsiz danışmanlık veriyor. Başvurmak için bir tanı almış " +
                "olman gerekmiyor.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Yaricap.Buton))
                .background(YesilNane)
                .padding(Olcu.KartIci),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.SupportAgent,
                contentDescription = null,
                tint = YesilAna,
            )
            Spacer(modifier = Modifier.width(Olcu.Aralik))
            Text(
                text = "115 — YEDAM Hattı, 7/24 ücretsiz ve gizli",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MetinBirincil,
            )
        }
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Text(
            text = "Görüşmeler gizlidir; kimseyle paylaşılmaz.",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
        Text(
            text = "Görüşmeyi uzman psikologlar yürütür.",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        // ACTION_DIAL: numarayı çevirici uygulamasına yazıyor ama kendisi
        // aramıyor. ACTION_CALL izin isterdi ve kullanıcının onayı olmadan
        // arama başlatmak bu ekranın işi değil.
        BirincilButon(
            metin = "115'i ara",
            onTikla = {
                context.startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:115"),
                    ),
                )
            },
        )
    }
}

/** Alt sayfa — Bildirim saatleri (Profil › Hedeflerim). */
@Composable
private fun BildirimSaatleriIcerik(
    durum: ProfilDurum,
    onDegis: (BildirimSaatleri) -> Unit,
    onKapat: () -> Unit,
) {
    val saatler = durum.bildirimSaatleri

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Olcu.EkranKenari)
            .padding(bottom = Olcu.BolumArasi),
    ) {
        AltSayfaBasligi("Bildirim saatleri", onKapat)
        Spacer(modifier = Modifier.height(Bosluk.S))

        Text(
            text = "Günde en fazla iki kısa hatırlatma. İstediğin zaman " +
                "kapatabilirsin.",
            style = MaterialTheme.typography.bodyLarge,
            color = MetinIkincil,
        )
        Spacer(modifier = Modifier.height(Olcu.Aralik))

        Kart(icBosluk = Olcu.Aralik) {
            AnahtarSatiri(
                ikon = Icons.Filled.Notifications,
                etiket = "Günlük hatırlatma",
                acik = saatler.acik,
                onDegis = { acik -> onDegis(saatler.copy(acik = acik)) },
            )
        }

        // Saat seçimi yalnızca hatırlatma açıkken — kapalıyken seçim
        // sunmak, ayarın çalıştığı izlenimini verirdi.
        if (saatler.acik) {
            Spacer(modifier = Modifier.height(Olcu.Aralik))
            BolumBasligi("Sabah")
            Spacer(modifier = Modifier.height(Bosluk.S))
            SaatSeridi(
                secenekler = BildirimSaatleri.SABAH_SECENEKLERI,
                secili = saatler.sabahSaati,
                onSec = { saatDegeri -> onDegis(saatler.copy(sabahSaati = saatDegeri)) },
            )

            Spacer(modifier = Modifier.height(Olcu.Aralik))
            BolumBasligi("Akşam")
            Spacer(modifier = Modifier.height(Bosluk.S))
            SaatSeridi(
                secenekler = BildirimSaatleri.AKSAM_SECENEKLERI,
                secili = saatler.aksamSaati,
                onSec = { saatDegeri -> onDegis(saatler.copy(aksamSaati = saatDegeri)) },
            )
        }
    }
}

@Composable
private fun SaatSeridi(secenekler: List<Int>, secili: Int, onSec: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Bosluk.S)) {
        secenekler.forEach { saatDegeri ->
            Cip(
                metin = "%02d:00".format(saatDegeri),
                zemin = if (saatDegeri == secili) YesilAna else Yesil50,
                renk = if (saatDegeri == secili) Beyaz else MetinBirincil,
                onTikla = { onSec(saatDegeri) },
            )
        }
    }
}


/**
 * Bildirim izni isteği (API 33+).
 *
 * `null` dönerse izin gerekmiyor (API 32 ve altı) ya da zaten verilmiş.
 * İzin **yalnızca kullanıcı hatırlatmayı açtığında** isteniyor; açılışta
 * sormak, henüz istemediği bir şey için rahatsız etmek olurdu.
 */
@Composable
private fun bildirimIzniIstegi(): (() -> Unit)? {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return null

    val baslatici = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { /* Reddedilse de defter çalışmaya devam ediyor. */ }

    return { baslatici.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
}
