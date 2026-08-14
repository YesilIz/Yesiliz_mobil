package tr.org.yesilay.yesiliz.domain.gorev

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RuhHaliPenceresiTest {

    private val gun = LocalDate.of(2026, 8, 14)

    @Test
    fun `pencere sinirlari`() {
        assertEquals(RuhHaliPenceresi.SABAH, RuhHaliPenceresi.bul(LocalTime.of(5, 0)))
        assertEquals(RuhHaliPenceresi.SABAH, RuhHaliPenceresi.bul(LocalTime.of(11, 59)))
        assertEquals(RuhHaliPenceresi.OGLEN, RuhHaliPenceresi.bul(LocalTime.of(12, 0)))
        assertEquals(RuhHaliPenceresi.OGLEN, RuhHaliPenceresi.bul(LocalTime.of(17, 59)))
        assertEquals(RuhHaliPenceresi.AKSAM, RuhHaliPenceresi.bul(LocalTime.of(18, 0)))
        assertEquals(RuhHaliPenceresi.AKSAM, RuhHaliPenceresi.bul(LocalTime.of(23, 59)))
    }

    @Test
    fun `gece yarisi sonrasi hala aksam penceresi`() {
        assertEquals(RuhHaliPenceresi.AKSAM, RuhHaliPenceresi.bul(LocalTime.of(0, 30)))
        assertEquals(RuhHaliPenceresi.AKSAM, RuhHaliPenceresi.bul(LocalTime.of(4, 59)))
    }

    @Test
    fun `gece yarisi sonrasi kayit DUNUN aksamina yaziliyor`() {
        // Yoksa 00:30'da uygulamayı açan kullanıcı yeni günün akşam
        // kaydını saatler önce yapmış olurdu.
        val (kayitGunu, pencere) = RuhHaliPenceresi.gunuVeDilimi(gun, LocalTime.of(0, 30))
        assertEquals(gun.minusDays(1), kayitGunu)
        assertEquals(RuhHaliPenceresi.AKSAM, pencere)
    }

    @Test
    fun `aksam penceresi gun icinde ayni gune yaziliyor`() {
        val (kayitGunu, pencere) = RuhHaliPenceresi.gunuVeDilimi(gun, LocalTime.of(21, 0))
        assertEquals(gun, kayitGunu)
        assertEquals(RuhHaliPenceresi.AKSAM, pencere)
    }

    @Test
    fun `sabah ve oglen gunu kaydirmiyor`() {
        assertEquals(gun, RuhHaliPenceresi.gunuVeDilimi(gun, LocalTime.of(8, 0)).first)
        assertEquals(gun, RuhHaliPenceresi.gunuVeDilimi(gun, LocalTime.of(14, 0)).first)
    }

    @Test
    fun `anahtarlar benzersiz ve gorev limitiyle tutarli`() {
        val anahtarlar = RuhHaliPenceresi.entries.map { it.anahtar }
        assertEquals(anahtarlar.size, anahtarlar.toSet().size)
        assertEquals(RuhHaliPenceresi.GUNLUK_PENCERE_SAYISI, RuhHaliPenceresi.entries.size)
        assertEquals(Gorev.RUH_HALI.gunlukLimit, RuhHaliPenceresi.entries.size)
    }

    @Test
    fun `anahtardan geri cevrilebiliyor`() {
        RuhHaliPenceresi.entries.forEach {
            assertEquals(it, RuhHaliPenceresi.anahtardan(it.anahtar))
        }
    }
}
