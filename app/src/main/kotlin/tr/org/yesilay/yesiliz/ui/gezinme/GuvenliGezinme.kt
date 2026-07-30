package tr.org.yesilay.yesiliz.ui.gezinme

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

/**
 * Çift gezinme koruması.
 *
 * Aynı yerde duran butonlarda (onboarding'in her ekranında birincil buton
 * tam olarak aynı noktada) tek bir dokunuş iki ekranı birden geçebiliyor:
 * ilk `navigate()` yeni ekranı yerleştiriyor, aynı dokunuşun devamı yeni
 * ekranın butonuna düşüyor. Emülatörde 1. adımdan 3. adıma atlarken yakalandı.
 *
 * Çözüm: yalnızca **ekran gerçekten öndeyken** (RESUMED) gezin. Geçiş
 * sürerken gelen ikinci çağrı sessizce düşer.
 */
fun NavBackStackEntry.guvenliGit(navController: NavHostController, rota: String) {
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        navController.navigate(rota)
    }
}

/** [guvenliGit] ile aynı koruma, geri dönüş için. */
fun NavBackStackEntry.guvenliGeri(navController: NavHostController) {
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        navController.popBackStack()
    }
}
