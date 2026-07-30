package tr.org.yesilay.yesiliz.ui.gezinme

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
import tr.org.yesilay.yesiliz.ui.ekran.istatistik.IstatistikEkran
import tr.org.yesilay.yesiliz.ui.ekran.lig.LigEkran
import tr.org.yesilay.yesiliz.ui.ekran.nefes.NefesEkran
import tr.org.yesilay.yesiliz.ui.ekran.odul.OdulEkran
import tr.org.yesilay.yesiliz.ui.ekran.profil.ProfilEkran
import tr.org.yesilay.yesiliz.ui.ekran.riskdetay.RiskDetayEkran
import tr.org.yesilay.yesiliz.ui.ekran.test.TestEkran
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
    val sekmede = Sekme.entries.any { it.rota == rota }

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
            modifier = Modifier.padding(bosluk),
        ) {
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
            composable(Rota.LIG) { LigEkran() }
            composable(Rota.PROFIL) {
                ProfilEkran(onEbeveyn = { navController.navigate(Rota.EBEVEYN) })
            }
            composable(Rota.ODUL) {
                OdulEkran(onGeri = { navController.popBackStack() })
            }
            composable(Rota.EBEVEYN) {
                EbeveynEkran(onGeri = { navController.popBackStack() })
            }
            composable(Rota.RISK_DETAY) {
                RiskDetayEkran(onGeri = { navController.popBackStack() })
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
private fun NavHostController.sekmeyeGit(sekme: Sekme) {
    navigate(sekme.rota) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
