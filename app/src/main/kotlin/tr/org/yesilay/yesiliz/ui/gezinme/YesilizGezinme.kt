package tr.org.yesilay.yesiliz.ui.gezinme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import tr.org.yesilay.yesiliz.ui.ekran.ana.AnaEkran
import tr.org.yesilay.yesiliz.ui.ekran.ebeveyn.EbeveynEkran
import tr.org.yesilay.yesiliz.ui.ekran.istatistik.IstatistikEkran
import tr.org.yesilay.yesiliz.ui.ekran.lig.LigEkran
import tr.org.yesilay.yesiliz.ui.ekran.odul.OdulEkran
import tr.org.yesilay.yesiliz.ui.ekran.profil.ProfilEkran
import tr.org.yesilay.yesiliz.ui.ekran.riskdetay.RiskDetayEkran
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
                    onGorev = { /* görev detayı alt sayfası — sıradaki iş */ },
                )
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
            // Yer tutucu — gerçek nefes ekranı CLAUDE.md §11 sıradaki iş #3.
            // Yalnızca nudge'ın "Evet" derin bağlantısının çökmemesi için var.
            composable(Rota.NEFES) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nefes egzersizi — yakında", style = MaterialTheme.typography.bodyLarge)
                }
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
