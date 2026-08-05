package tr.org.yesilay.yesiliz.ui.gezinme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import tr.org.yesilay.yesiliz.data.kullanim.KullanimIzni
import tr.org.yesilay.yesiliz.ui.ekran.onboarding.AnketEkran
import tr.org.yesilay.yesiliz.ui.ekran.onboarding.HedefEkran
import tr.org.yesilay.yesiliz.ui.ekran.onboarding.HedefOzelEkran
import tr.org.yesilay.yesiliz.ui.ekran.onboarding.IlkSonucEkran
import tr.org.yesilay.yesiliz.ui.ekran.onboarding.EbeveynOnayiEkran
import tr.org.yesilay.yesiliz.ui.ekran.onboarding.IzinEkran
import tr.org.yesilay.yesiliz.ui.ekran.onboarding.NasilCalisirEkran
import tr.org.yesilay.yesiliz.ui.ekran.onboarding.KarsilamaEkran
import tr.org.yesilay.yesiliz.ui.ekran.onboarding.OnboardingViewModel
import tr.org.yesilay.yesiliz.ui.ekran.onboarding.ProfilKurulumEkran

/**
 * Onboarding alt grafiği — `docs/tasarim-referans.md §3`.
 *
 * 7 ekran tek bir `OnboardingViewModel` paylaşıyor: ViewModel grafiğin
 * kendi `NavBackStackEntry`'sine bağlanıyor, böylece yaş/anket/hedef değerleri
 * ekranlar arasında elle taşınmıyor ve akış bitince otomatik temizleniyor.
 *
 * Her gezinme [guvenliGit] üzerinden — birincil buton her ekranda aynı
 * noktada durduğu için tek dokunuş iki ekran birden geçebiliyor.
 */
fun NavGraphBuilder.onboardingAkisi(navController: NavHostController) {
    navigation(startDestination = Rota.KARSILAMA, route = Rota.ONBOARDING) {

        composable(Rota.KARSILAMA) { girdi ->
            KarsilamaEkran(
                onBasla = { girdi.guvenliGit(navController, Rota.IZIN) },
                // İki geri çağrı BİREBİR aynı hedefe gidiyordu; nasıl
                // çalıştığını anlatan bir ekran hiç yoktu (`debug.md` O-3).
                onNasilCalisir = { girdi.guvenliGit(navController, Rota.NASIL_CALISIR) },
            )
        }

        composable(Rota.NASIL_CALISIR) { girdi ->
            NasilCalisirEkran(
                onDevam = { girdi.guvenliGit(navController, Rota.IZIN) },
                onGeri = { navController.popBackStack() },
            )
        }

        composable(Rota.IZIN) { girdi ->
            val vm = girdi.onboardingViewModel(navController)
            val durum by vm.durum.collectAsStateWithLifecycle()
            val context = LocalContext.current

            // Ayarlar'dan dönüşte izin durumunu tazele — bu izin türü için
            // sistem uygulamaya geri bildirim vermiyor, `ON_RESUME` tek yol.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val gozlemci = LifecycleEventObserver { _, olay ->
                    if (olay == Lifecycle.Event.ON_RESUME) vm.izinKontrolEt(context)
                }
                lifecycleOwner.lifecycle.addObserver(gozlemci)
                onDispose { lifecycleOwner.lifecycle.removeObserver(gozlemci) }
            }

            LaunchedEffect(durum.izinVerildiMi) {
                if (durum.izinVerildiMi) girdi.guvenliGit(navController, Rota.PROFIL_KURULUM)
            }

            IzinEkran(
                onIzinVer = {
                    if (KullanimIzni.ayarEkraniVarMi(context)) {
                        context.startActivity(KullanimIzni.ayarIntent())
                    } else {
                        girdi.guvenliGit(navController, Rota.PROFIL_KURULUM)
                    }
                },
                onAtla = { girdi.guvenliGit(navController, Rota.PROFIL_KURULUM) },
            )
        }

        composable(Rota.PROFIL_KURULUM) { girdi ->
            val vm = girdi.onboardingViewModel(navController)
            val durum by vm.durum.collectAsStateWithLifecycle()
            // 18 yaş altı ayrı dala giriyor. `ebeveynOnayiGerekir` yazılmış
            // ama hiç okunmamıştı: yaş 15 girilse bile "Devam" doğrudan
            // ankete geçiyordu (`debug.md` Y-11).
            val sonrakiRota = if (durum.ebeveynOnayiGerekir) {
                Rota.EBEVEYN_ONAYI
            } else {
                Rota.ANKET
            }

            ProfilKurulumEkran(
                durum = durum,
                onYas = vm::yasDegisti,
                onCinsiyet = vm::cinsiyetSecildi,
                onDevam = { girdi.guvenliGit(navController, sonrakiRota) },
                onAtla = { girdi.guvenliGit(navController, sonrakiRota) },
            )
        }

        composable(Rota.EBEVEYN_ONAYI) { girdi ->
            val vm = girdi.onboardingViewModel(navController)
            val durum by vm.durum.collectAsStateWithLifecycle()
            EbeveynOnayiEkran(
                yas = durum.yas,
                onayli = durum.ebeveynOnayiVerildi,
                onOnayDegisti = vm::ebeveynOnayiDegisti,
                onDevam = { girdi.guvenliGit(navController, Rota.ANKET) },
                onGeri = { navController.popBackStack() },
                onSozlesme = { id -> navController.navigate(Rota.sozlesme(id)) },
            )
        }

        composable(Rota.ANKET) { girdi ->
            val vm = girdi.onboardingViewModel(navController)
            val durum by vm.durum.collectAsStateWithLifecycle()
            AnketEkran(
                durum = durum,
                onYanit = vm::yanitVerildi,
                onBitti = { girdi.guvenliGit(navController, Rota.ILK_SONUC) },
                onAtla = { girdi.guvenliGit(navController, Rota.ILK_SONUC) },
            )
        }

        composable(Rota.ILK_SONUC) { girdi ->
            val vm = girdi.onboardingViewModel(navController)
            val durum by vm.durum.collectAsStateWithLifecycle()
            IlkSonucEkran(
                onPlanimiGor = { girdi.guvenliGit(navController, Rota.HEDEF) },
                // `vm.tamamla()` eksikti: `onboardingBitti` yalnızca burada
                // yazılıyor, HEDEF/HEDEF_OZEL'deki gibi. Eksik olunca
                // kullanıcı ana sayfayı görüyordu ama uygulama kapanıp
                // açılınca `onboardingBitti == false` görülüp onboarding
                // baştan başlıyordu — "Atla" sonsuz döngüsü
                // (`debug.md` üçüncü tarama A3). Hedef seçilmediği için
                // `tamamla()` varsayılan hedefi (120 dk) yazar.
                onAtla = {
                    vm.tamamla()
                    girdi.anaSayfayaGec(navController)
                },
                // Anket atlandıysa kullanım hakkında iddia edilmiyor
                // (`debug.md` O-2).
                anketDolduruldu = durum.anketTamam,
            )
        }

        composable(Rota.HEDEF) { girdi ->
            val vm = girdi.onboardingViewModel(navController)
            val durum by vm.durum.collectAsStateWithLifecycle()
            HedefEkran(
                durum = durum,
                onBasla = {
                    vm.tamamla()
                    girdi.anaSayfayaGec(navController)
                },
                onKendiHedefi = { girdi.guvenliGit(navController, Rota.HEDEF_OZEL) },
            )
        }

        composable(Rota.HEDEF_OZEL) { girdi ->
            val vm = girdi.onboardingViewModel(navController)
            val durum by vm.durum.collectAsStateWithLifecycle()
            HedefOzelEkran(
                durum = durum,
                onHedef = vm::hedefDegisti,
                onHizli = vm::hizliHedef,
                onKaydet = {
                    vm.tamamla()
                    girdi.anaSayfayaGec(navController)
                },
                onVazgec = { girdi.guvenliGeri(navController) },
            )
        }
    }
}

/**
 * Grafiğe bağlı ortak ViewModel.
 *
 * `viewModel()` çağrısı varsayılan olarak ekranın kendi girdisine bağlanır ve
 * her ekranda yeni bir örnek üretir — yaş girildikten sonra ankete geçince
 * sıfırlanırdı. Grafik girdisini `viewModelStoreOwner` vererek 7 ekran aynı
 * örneği paylaşıyor.
 */
@Composable
private fun NavBackStackEntry.onboardingViewModel(
    navController: NavHostController,
): OnboardingViewModel {
    val grafikGirdisi = remember(this) { navController.getBackStackEntry(Rota.ONBOARDING) }
    return hiltViewModel(viewModelStoreOwner = grafikGirdisi)
}

/** Onboarding bitti — geri tuşuyla akışa dönülmesin. */
private fun NavBackStackEntry.anaSayfayaGec(navController: NavHostController) {
    if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
    navController.navigate(Rota.ANA) {
        popUpTo(Rota.ONBOARDING) { inclusive = true }
    }
}
