package tr.org.yesilay.yesiliz

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tr.org.yesilay.yesiliz.ui.gezinme.Rota

class DisHedefRotasiTest {

    @Test
    fun uygulamaninUrettigiNefesVeYuruyusHedefleriKabulEdilir() {
        assertEquals(Rota.NEFES, disHedefRotasiniDogrula(null, Rota.NEFES))
        assertEquals(Rota.YURUYUS, disHedefRotasiniDogrula(null, Rota.YURUYUS))
    }

    @Test
    fun grafikteOlmayanVeyaIzinVerilmeyenRotaReddedilir() {
        assertNull(disHedefRotasiniDogrula(null, "not/a/route"))
        assertNull(disHedefRotasiniDogrula(null, Rota.PROFIL))
    }

    @Test
    fun healthConnectEylemiHariciExtraYerineIzinEkraniniAcar() {
        assertEquals(
            Rota.SAGLIK_IZIN,
            disHedefRotasiniDogrula(MainActivity.GEREKCE_EYLEMI, "not/a/route"),
        )
        assertEquals(
            Rota.SAGLIK_IZIN,
            disHedefRotasiniDogrula(Intent.ACTION_VIEW_PERMISSION_USAGE, Rota.PROFIL),
        )
    }
}
