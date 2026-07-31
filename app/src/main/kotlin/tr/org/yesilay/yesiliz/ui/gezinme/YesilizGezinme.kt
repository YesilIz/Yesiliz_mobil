package tr.org.yesilay.yesiliz.ui.gezinme

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tr.org.yesilay.yesiliz.ui.ekran.ana.AnaEkran
import tr.org.yesilay.yesiliz.ui.ekran.anket.AnketYenileEkran
import tr.org.yesilay.yesiliz.ui.ekran.bildirim.BildirimEkran
import tr.org.yesilay.yesiliz.ui.ekran.bilgi.BilgiEkran
import tr.org.yesilay.yesiliz.ui.ekran.bilgi.BilgiYaziEkran
import tr.org.yesilay.yesiliz.ui.ekran.bilgi.BilgiYaziViewModel
import tr.org.yesilay.yesiliz.ui.ekran.ebeveyn.EbeveynEkran
import tr.org.yesilay.yesiliz.ui.ekran.hedef.HedefEkran
import tr.org.yesilay.yesiliz.ui.ekran.hesap.GirisEkran
import tr.org.yesilay.yesiliz.ui.ekran.hesap.KayitEkran
import tr.org.yesilay.yesiliz.ui.ekran.hesap.SifreEkran
import tr.org.yesilay.yesiliz.ui.ekran.kategori.KategoriEkran
import tr.org.yesilay.yesiliz.ui.ekran.kategori.KategorizeEtEkran
import tr.org.yesilay.yesiliz.ui.ekran.istatistik.IstatistikEkran
import tr.org.yesilay.yesiliz.ui.ekran.lig.ArkadasEkleEkran
import tr.org.yesilay.yesiliz.ui.ekran.lig.LigEkran
import tr.org.yesilay.yesiliz.ui.ekran.nefes.NefesEkran
import tr.org.yesilay.yesiliz.ui.ekran.odul.OdulEkran
import tr.org.yesilay.yesiliz.ui.ekran.profil.ProfilEkran
import tr.org.yesilay.yesiliz.ui.ekran.riskdetay.RiskDetayEkran
import tr.org.yesilay.yesiliz.ui.ekran.saglik.SaglikIzinEkran
import tr.org.yesilay.yesiliz.ui.ekran.sozlesme.SozlesmeEkran
import tr.org.yesilay.yesiliz.ui.ekran.test.TestEkran
import tr.org.yesilay.yesiliz.ui.ekran.yedam.YedamOnayEkran
import tr.org.yesilay.yesiliz.ui.ekran.yuruyus.YuruyusEkran
import tr.org.yesilay.yesiliz.ui.tema.Yesil50

/**
 * Uygulamanın tek NavHost'u — `docs/tasarim-referans.md §3`.
 *
 * Alt gezinme çubuğu **yalnızca** 4 sekmede görünüyor; Ödüller, nefes ve
 * onboarding tam ekran. `Scaffold`'un `bottomBar` slotu bu yüzden koşullu.
 */
@Composable
fun YesilizGezinme(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    acilisViewModel: AcilisViewModel = hiltViewModel(),
    hedefRota: String? = null,
    onHedefRotaTuketildi: () -> Unit = {},
) {
    val baslangic by acilisViewModel.baslangicRotasi.collectAsStateWithLifecycle()

    // Bayrak DataStore'dan okunana kadar NavHost KURULMUYOR. Varsayılan bir
    // rotayla kurup sonra atlamak, onboarding'i bitirmiş kullanıcıya bir kare
    // boyunca karşılama ekranını gösterirdi.
    val baslangicRotasi = baslangic ?: return

    // Nudge overlay'in "Nefes egzersizi yapmak ister misin? → Evet" derin
    // bağlantısı — `MainActivity`'nin dışarıdan aldığı Intent'i tüketir.
    LaunchedEffect(hedefRota) {
        hedefRota?.let {
            navController.navigate(it)
            onHedefRotaTuketildi()
        }
    }

    val girdi by navController.currentBackStackEntryAsState()
    val rota = girdi?.destination?.route
    // Sorgu argümanı rotanın parçası ("profil?acilacakSayfa={...}"); sekme
    // eşlemesi yalnızca yola bakıyor, yoksa argümanlı rota sekme
    // sayılmayıp alt çubuk kaybolurdu.
    val sekmede = Sekme.entries.any { it.rota == rota?.substringBefore("?") }

    Scaffold(
        modifier = modifier,
        containerColor = Yesil50,
        // Yeşil başlık status bar'ın altına kadar uzanıyor; içerik boşluğunu
        // ekranlar kendi `windowInsetsTopHeight`'ı ile veriyor.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (sekmede) {
                AltGezinme(
                    seciliRota = rota,
                    onSekme = { sekme -> navController.sekmeyeGit(sekme) },
                )
            }
        },
    ) { bosluk ->
        NavHost(
            navController = navController,
            startDestination = baslangicRotasi,
            // `consumeWindowInsets` şart: sekme ekranlarında alt çubuk sistem
            // gezinme payını ZATEN kapsıyor (bkz. `AltGezinme`), o pay burada
            // tüketilmezse ekranların kendi `navigationBarsPadding()`'i aynı
            // boşluğu bir kez daha eklerdi. Alt çubuğun olmadığı tam ekran
            // rotalarda tüketilecek bir şey yok, pay ekranın kendisinden geliyor.
            modifier = Modifier
                .padding(bosluk)
                .consumeWindowInsets(bosluk),
        ) {
            // Hesap akışı — uygulama akışının dışında, yalnızca ilk açılışta.
            composable(Rota.GIRIS) {
                GirisEkran(
                    onBasarili = { navController.hesaptanCik(Rota.ONBOARDING) },
                    onKayit = { navController.navigate(Rota.KAYIT) },
                    onSifremiUnuttum = { navController.navigate(Rota.SIFRE) },
                )
            }
            composable(Rota.KAYIT) {
                KayitEkran(
                    onBasarili = {
                        // Profil'den gelen misafir onboarding'i zaten
                        // bitirmiş; onu baştan geçirmek yerine geri
                        // dönülüyor. İlk kurulumda akış onboarding'e devam
                        // ediyor.
                        if (navController.previousBackStackEntry
                                ?.destination?.route == Rota.PROFIL
                        ) {
                            navController.popBackStack()
                        } else {
                            navController.hesaptanCik(Rota.ONBOARDING)
                        }
                    },
                    onGiris = { navController.popBackStack(Rota.GIRIS, inclusive = false) },
                    onGeri = { navController.popBackStack() },
                    onSozlesme = { id -> navController.navigate(Rota.sozlesme(id)) },
                )
            }
            composable(Rota.SIFRE) {
                SifreEkran(
                    onGeri = { navController.popBackStack() },
                    onGiris = { navController.popBackStack(Rota.GIRIS, inclusive = false) },
                )
            }

            onboardingAkisi(navController)

            composable(Rota.ANA) {
                AnaEkran(
                    onOdul = { navController.navigate(Rota.ODUL) },
                    onNefesBaslat = { navController.navigate(Rota.NEFES) },
                    onBilgiBankasi = { navController.navigate(Rota.BILGI_BANKASI) },
                    onKendiniTest = { navController.navigate(Rota.KENDINI_TEST) },
                    onBildirimler = { navController.navigate(Rota.BILDIRIM) },
                    onYuruyusBaslat = { navController.navigate(Rota.YURUYUS) },
                    onDegerlendirmeYenile = { navController.navigate(Rota.ANKET_YENILE) },
                    onProfil = { navController.navigate(Rota.PROFIL) },
                    onIstatistik = { navController.sekmeyeGit(Sekme.ISTATISTIK) },
                )
            }
            composable(Rota.BILGI_BANKASI) {
                BilgiEkran(
                    onGeri = { navController.popBackStack() },
                    onYazi = { id -> navController.navigate(Rota.bilgiYazisi(id)) },
                )
            }
            composable(
                route = Rota.BILGI_YAZI,
                arguments = listOf(navArgument(BilgiYaziViewModel.YAZI_ID) { type = NavType.StringType }),
            ) {
                BilgiYaziEkran(onGeri = { navController.popBackStack() })
            }
            composable(
                route = Rota.SOZLESME,
                arguments = listOf(
                    navArgument(Rota.SOZLESME_ARG) { type = NavType.StringType },
                ),
            ) {
                SozlesmeEkran(onGeri = { navController.popBackStack() })
            }
            composable(Rota.KENDINI_TEST) {
                TestEkran(
                    onGeri = { navController.popBackStack() },
                    onBilgiYazisi = { id -> navController.navigate(Rota.bilgiYazisi(id)) },
                    onNefes = { navController.navigate(Rota.NEFES) },
                )
            }
            composable(Rota.BILDIRIM) {
                BildirimEkran(onGeri = { navController.popBackStack() })
            }
            composable(Rota.ISTATISTIK) {
                IstatistikEkran(onRiskDetay = { navController.navigate(Rota.RISK_DETAY) })
            }
            composable(Rota.LIG) {
                LigEkran(onArkadasEkle = { navController.navigate(Rota.ARKADAS_EKLE) })
            }
            composable(Rota.ARKADAS_EKLE) {
                ArkadasEkleEkran(onGeri = { navController.popBackStack() })
            }
            composable(
                route = Rota.PROFIL_SAYFALI,
                arguments = listOf(
                    navArgument(Rota.PROFIL_SAYFA_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { girdiP ->
                ProfilEkran(
                    // Risk detayından gelen "Hatırlatma saatlerini
                    // ayarla → Aç" doğrudan ilgili alt sayfayı açıyor
                    // (`debug.md` O-8).
                    acilacakSayfa = girdiP.arguments
                        ?.getString(Rota.PROFIL_SAYFA_ARG),
                    onEbeveyn = { navController.navigate(Rota.EBEVEYN) },
                    onGunlukHedef = { navController.navigate(Rota.GUNLUK_HEDEF) },
                    onKategoriler = { navController.navigate(Rota.KATEGORI) },
                    onAnketYenile = { navController.navigate(Rota.ANKET_YENILE) },
                    onSaglikIzni = { navController.navigate(Rota.SAGLIK_IZIN) },
                    // Veri/hesap silindi: geri yığınının tamamı atılıyor.
                    // Silinmiş veriye geri tuşuyla dönmek boş ekranlar
                    // gösterirdi.
                    onSilindi = { navController.bastanBasla() },
                    onHesapOlustur = { navController.navigate(Rota.KAYIT) },
                    onSozlesme = { id -> navController.navigate(Rota.sozlesme(id)) },
                )
            }
            composable(Rota.GUNLUK_HEDEF) {
                HedefEkran(onGeri = { navController.popBackStack() })
            }
            composable(Rota.KATEGORI) {
                KategoriEkran(
                    onGeri = { navController.popBackStack() },
                    onRiskDetay = { navController.navigate(Rota.RISK_DETAY) },
                    onKategorizeEt = { navController.navigate(Rota.KATEGORIZE_ET) },
                )
            }
            composable(Rota.KATEGORIZE_ET) {
                KategorizeEtEkran(onGeri = { navController.popBackStack() })
            }
            composable(Rota.SAGLIK_IZIN) {
                SaglikIzinEkran(onGeri = { navController.popBackStack() })
            }
            composable(Rota.YEDAM_ONAY) {
                YedamOnayEkran(onGeri = { navController.popBackStack() })
            }
            composable(Rota.ODUL) {
                OdulEkran(onGeri = { navController.popBackStack() })
            }
            composable(Rota.EBEVEYN) {
                EbeveynEkran(onGeri = { navController.popBackStack() })
            }
            composable(Rota.RISK_DETAY) {
                RiskDetayEkran(
                    onGeri = { navController.popBackStack() },
                    // Öneri kartları gerçek ekranlara gidiyor. "Bildirim
                    // saatleri" Profil'in alt sayfası olduğu için sekmeye
                    // götürülüyor; ayrı bir rota açmak o alt sayfayı iki
                    // yerde tanımlamak olurdu.
                    onEylem = { id ->
                        when (id) {
                            "bildirim_saatleri" ->
                        navController.navigate(
                            Rota.profilSayfasi(Rota.SAYFA_BILDIRIM_SAATLERI),
                        )
                            "hedef_azalt" -> navController.navigate(Rota.GUNLUK_HEDEF)
                            "nefes" -> navController.navigate(Rota.NEFES)
                            "yedam" -> navController.navigate(Rota.YEDAM_ONAY)
                        }
                    },
                )
            }
            composable(Rota.NEFES) {
                NefesEkran(onKapat = { navController.popBackStack() })
            }
            composable(Rota.YURUYUS) {
                YuruyusEkran(onKapat = { navController.popBackStack() })
            }
            composable(Rota.ANKET_YENILE) {
                AnketYenileEkran(onGeri = { navController.popBackStack() })
            }
        }
    }
}

/**
 * Sekme değiştirme.
 *
 * `launchSingleTop` + start hedefine `popUpTo`: sekmeler arasında gidip
 * gelince geri yığını şişmesin, geri tuşu uygulamadan çıksın. `saveState`
 * ile her sekme kendi kaydırma konumunu koruyor.
 */
/**
 * Hesap akışından çıkış.
 *
 * `popUpTo(GIRIS, inclusive)` şart: geri tuşuyla giriş ekranına dönmek,
 * onboarding'i yarıda bırakıp kimliksiz bir duruma düşürürdü.
 */
private fun NavHostController.hesaptanCik(hedef: String) {
    navigate(hedef) {
        popUpTo(Rota.GIRIS) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * "Verilerimi/Hesabımı sil" sonrası sıfırdan başlatma.
 *
 * `popUpTo(0)` geri yığınının tamamını atıyor: silinmiş veriye ait hiçbir
 * ekran geride kalmamalı.
 */
private fun NavHostController.bastanBasla() {
    navigate(Rota.GIRIS) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.sekmeyeGit(sekme: Sekme) {
    navigate(sekme.rota) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
