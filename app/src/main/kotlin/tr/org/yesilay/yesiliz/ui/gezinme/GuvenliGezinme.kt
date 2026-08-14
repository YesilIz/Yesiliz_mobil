package tr.org.yesilay.yesiliz.ui.gezinme

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.withResumed
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

/**
 * Ekran öne dönene kadar **bekleyip** gezinir.
 *
 * [guvenliGit] geçişi sessizce düşürüyor ve bu, çift dokunuş için doğru
 * davranış ama **sistem diyaloğundan dönen** bir geri çağrı için yanlış:
 * izin diyaloğu açıkken Activity duraklıyor, diyalog kapanırken geri çağrı
 * ekran henüz `RESUMED` olmadan çalışıyor ve tek gezinme isteği düşüyor.
 * Kullanıcı izin ekranında kalıyordu — hareket izni adımında emülatörde
 * tam olarak bu görüldü.
 *
 * Burada istek düşmüyor, sıraya giriyor: `withResumed` ekran öne dönene
 * kadar askıya alıyor. Çift gezinme riski yok çünkü çağrı bir kerelik bir
 * sonuca (izin diyaloğunun kapanmasına) bağlı.
 */
suspend fun NavBackStackEntry.bekleyipGit(navController: NavHostController, rota: String) {
    lifecycle.withResumed { navController.navigate(rota) }
}
