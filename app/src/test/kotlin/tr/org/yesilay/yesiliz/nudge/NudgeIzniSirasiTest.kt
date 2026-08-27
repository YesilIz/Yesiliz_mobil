package tr.org.yesilay.yesiliz.nudge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [siradakiIzin] — nudge izinlerinin sorulma sırası.
 *
 * Bu kararın kendi testi var çünkü asıl kusur mantıkta değil **çağrı
 * yerindeydi**: `MainActivity.onResume` her dönüşte soruyor ve üç izin de
 * verildiyse kullanıcının kapattığı anahtarı yeniden açıyordu (§12 K64).
 * Sıra fonksiyonu Context'e dokunmadığı için emülatörsüz sınanabiliyor;
 * çağrı yerinin doğruluğu ise elle doğrulanıyor.
 */
class NudgeIzniSirasiTest {

    @Test
    fun `hicbir izin yokken once overlay isteniyor`() {
        assertEquals(
            EksikNudgeIzni.OVERLAY,
            siradakiIzin(overlayVar = false, pilMuafiyetiVar = false),
        )
    }

    @Test
    fun `pil muafiyeti varken bile overlay once geliyor`() {
        // Overlay olmadan popup HİÇ çizilemiyor: özelliğin kendisi yok.
        // Pil muafiyeti yalnızca güvenilirliği artırıyor.
        assertEquals(
            EksikNudgeIzni.OVERLAY,
            siradakiIzin(overlayVar = false, pilMuafiyetiVar = true),
        )
    }

    @Test
    fun `overlay verildiyse sirada pil muafiyeti var`() {
        assertEquals(
            EksikNudgeIzni.PIL_MUAFIYETI,
            siradakiIzin(overlayVar = true, pilMuafiyetiVar = false),
        )
    }

    @Test
    fun `ikisi de verildiyse istenecek izin yok`() {
        // `null` = hiçbir sistem ekranı açılmıyor. Verilmiş izinler için
        // kullanıcıyı Ayarlar'a göndermek K45'in yasakladığı dürtme.
        assertNull(siradakiIzin(overlayVar = true, pilMuafiyetiVar = true))
    }
}
