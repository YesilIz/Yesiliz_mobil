package tr.org.yesilay.yesiliz.nudge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.org.yesilay.yesiliz.data.kullanim.HamOlay
import tr.org.yesilay.yesiliz.data.kullanim.OlayTuru
import java.time.Duration
import java.time.Instant

/**
 * Ön plan tespiti — T4-003'ün yakalanamadığı yer burasıydı.
 *
 * `TetikleyiciTest` bu hatayı göremezdi: [Tetikleyici] saf fonksiyon olarak
 * zaten doğruydu, hata onu **besleyen** soruda ("şu an ön planda ne var")
 * idi. Bu dosya tam olarak o soruyu sınıyor.
 */
class OnPlanTespitTest {

    private val bas: Instant = Instant.parse("2026-08-17T09:00:00Z")
    private val kendiPaket = "tr.org.yesilay.yesiliz"
    private val tuketim = setOf("com.zhiliaoapp.musically", "com.instagram.android")

    private fun an(saniye: Long): Instant = bas.plus(Duration.ofSeconds(saniye))

    private fun onPlana(paket: String, saniye: Long) =
        HamOlay(paket, OlayTuru.ON_PLANA_GELDI, an(saniye))

    private fun arkaPlana(paket: String, saniye: Long) =
        HamOlay(paket, OlayTuru.ARKA_PLANA_GITTI, an(saniye))

    private fun ekranKapandi(saniye: Long) =
        HamOlay("android", OlayTuru.EKRAN_KAPANDI, an(saniye))

    /** Servisteki `tuketimKategorisi` yerine geçen sade yüklem. */
    private fun tuketimdeMi(onPlan: String?): Boolean =
        OnPlanTespit.tuketimdeMi(onPlan) { it != kendiPaket && it in tuketim }

    @Test
    fun `tuketim uygulamasi on plandayken tuketimde sayilir`() {
        val olaylar = listOf(onPlana("com.zhiliaoapp.musically", 0))

        val onPlan = OnPlanTespit.onPlandakiPaket(olaylar)

        assertEquals("com.zhiliaoapp.musically", onPlan)
        assertTrue(tuketimdeMi(onPlan))
    }

    @Test
    fun `kendi paketimiz on plandayken tuketimde sayilmaz`() {
        // T4-003'ün ta kendisi: TikTok oturumu az önce bitti, kullanıcı
        // Yeşilİz'e geçti. Eski kod "penceredeki son oturum"a baktığı için
        // hâlâ TikTok diyordu.
        val olaylar = listOf(
            onPlana("com.zhiliaoapp.musically", 0),
            arkaPlana("com.zhiliaoapp.musically", 100),
            onPlana(kendiPaket, 100),
        )

        val onPlan = OnPlanTespit.onPlandakiPaket(olaylar)

        assertEquals(kendiPaket, onPlan)
        assertFalse(tuketimdeMi(onPlan))
    }

    @Test
    fun `tuketim disi ucuncu uygulama on plandayken tuketimde sayilmaz`() {
        val olaylar = listOf(
            onPlana("com.zhiliaoapp.musically", 0),
            onPlana("com.sec.android.app.launcher", 100),
        )

        val onPlan = OnPlanTespit.onPlandakiPaket(olaylar)

        assertEquals("com.sec.android.app.launcher", onPlan)
        assertFalse(tuketimdeMi(onPlan))
    }

    @Test
    fun `on plan hic okunamiyorsa tuketimde sayilmaz`() {
        // Fail-closed (§4, §12 K37): ölçemediğimiz şeyi "evet" saymıyoruz.
        val onPlan = OnPlanTespit.onPlandakiPaket(olaylar = emptyList(), oncekiOnPlan = null)

        assertNull(onPlan)
        assertFalse(tuketimdeMi(onPlan))
    }

    @Test
    fun `ekran kapandiysa on planda kimse yok`() {
        val olaylar = listOf(
            onPlana("com.zhiliaoapp.musically", 0),
            ekranKapandi(100),
        )

        assertNull(OnPlanTespit.onPlandakiPaket(olaylar))
    }

    @Test
    fun `pencerede hic olay yoksa onceki on plan korunur`() {
        // Eşikten uzun süren oturumda son ON_PLANA_GELDI pencerenin
        // dışında kalıyor; bu olmasa nudge tam da hedeflediği durumda
        // "bilinmiyor"a düşerdi.
        val onPlan = OnPlanTespit.onPlandakiPaket(
            olaylar = emptyList(),
            oncekiOnPlan = "com.zhiliaoapp.musically",
        )

        assertEquals("com.zhiliaoapp.musically", onPlan)
        assertTrue(tuketimdeMi(onPlan))
    }

    @Test
    fun `arka plandaki paketin cikisi on plani bosaltmaz`() {
        val olaylar = listOf(
            onPlana("com.zhiliaoapp.musically", 0),
            arkaPlana("com.instagram.android", 50),
        )

        assertEquals("com.zhiliaoapp.musically", OnPlanTespit.onPlandakiPaket(olaylar))
    }

    @Test
    fun `olaylar sirasiz gelse de sonuc ayni`() {
        val sirali = listOf(
            onPlana("com.zhiliaoapp.musically", 0),
            onPlana(kendiPaket, 100),
        )

        assertEquals(
            OnPlanTespit.onPlandakiPaket(sirali),
            OnPlanTespit.onPlandakiPaket(sirali.reversed()),
        )
    }
}
