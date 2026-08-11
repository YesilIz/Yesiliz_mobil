package tr.org.yesilay.yesiliz.ui.gezinme

import org.junit.Assert.assertEquals
import org.junit.Test

class HesapGezinmeTest {

    @Test
    fun `tamamlanmis misafir hesap acinca ana ve profil yigini kurulur`() {
        assertEquals(
            listOf(Rota.ANA, Rota.PROFIL),
            hesapSonrasiYigin(onboardingBitti = true),
        )
    }

    @Test
    fun `ilk kurulumdaki hesap akisi onboardinge devam eder`() {
        assertEquals(
            listOf(Rota.ONBOARDING),
            hesapSonrasiYigin(onboardingBitti = false),
        )
    }
}
