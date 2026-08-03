package tr.org.yesilay.yesiliz.data.saglik

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Donanım adım sayacının güne düşen payı.
 *
 * `Sensor.TYPE_STEP_COUNTER` gün başına adım vermiyor; açılıştan beri biriken
 * kümülatif bir sayı veriyor ve yeniden başlatmada sıfırlanıyor. Bu testler
 * o sayının farkını almanın üç hâlini sabitliyor — hesap saf olduğu için
 * emülatör gerekmiyor (CLAUDE.md §6).
 */
class AdimBirikimTest {

    @Test
    fun `gunun ilk ornegi sifirdan baslar, sayaci sahiplenmez`() {
        val sonuc = adimBirikimGuncelle(onceki = null, sayac = 12_500)

        // Sayaç günlerdir sayıyor olabilir; 12.500'ü bugüne yazmak günün
        // başına toplu bir sıçrama uydurmak olurdu.
        assertEquals(0L, sonuc.toplam)
        assertEquals(12_500L, sonuc.sonSayac)
    }

    @Test
    fun `ardisik ornekler yalnizca farki ekler`() {
        var durum = adimBirikimGuncelle(null, 12_500)
        durum = adimBirikimGuncelle(durum, 12_800)
        durum = adimBirikimGuncelle(durum, 13_450)

        assertEquals(950L, durum.toplam)
        assertEquals(13_450L, durum.sonSayac)
    }

    @Test
    fun `sayac degismediyse toplam da degismez`() {
        val ilk = adimBirikimGuncelle(null, 900)
        val sonra = adimBirikimGuncelle(adimBirikimGuncelle(ilk, 1_100), 1_100)

        assertEquals(200L, sonra.toplam)
        assertEquals(1_100L, sonra.sonSayac)
    }

    @Test
    fun `TUZAK yeniden baslatmada sayac sifirlanir, fark negatife dusmez`() {
        var durum = adimBirikimGuncelle(null, 9_000)
        durum = adimBirikimGuncelle(durum, 9_400) // +400

        // Cihaz yeniden başladı: sayaç sıfırdan sayıyor. Çıplak fark
        // (120 - 9.400) negatif olurdu ve günün toplamını silerdi.
        durum = adimBirikimGuncelle(durum, 120)

        assertEquals(520L, durum.toplam)
        assertEquals(120L, durum.sonSayac)
    }

    @Test
    fun `yeniden baslatmadan sonra birikim normal surer`() {
        var durum = adimBirikimGuncelle(null, 9_000)
        durum = adimBirikimGuncelle(durum, 120) // yeniden başlatma: +120
        durum = adimBirikimGuncelle(durum, 500) // +380

        assertEquals(500L, durum.toplam)
    }

    @Test
    fun `sifir sayacla acilan gun sifir kalir`() {
        val durum = adimBirikimGuncelle(adimBirikimGuncelle(null, 0), 0)

        assertEquals(0L, durum.toplam)
    }
}
