package tr.org.yesilay.yesiliz.nudge

import org.junit.Assert.assertEquals
import org.junit.Test
import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import tr.org.yesilay.yesiliz.domain.kullanim.KullanimOlay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Nudge tetikleyicisi — `OturumCikarTest.kt`'nin senteti-olay yaklaşımını
 * izliyor: sistem yok, yalnızca [KullanimOlay] girdisi.
 */
class TetikleyiciTest {

    private val zon: ZoneId = ZoneId.of("Europe/Istanbul")
    private val tarih: LocalDate = LocalDate.of(2026, 7, 30)

    private fun an(saat: Int, dakika: Int): Instant =
        tarih.atTime(LocalTime.of(saat, dakika)).atZone(zon).toInstant()

    private fun olay(baslamaSaat: Int, baslamaDk: Int, bitisSaat: Int, bitisDk: Int) = KullanimOlay(
        paketAdi = "com.instagram.android",
        kategori = Kategori.SOCIAL,
        baslangic = an(baslamaSaat, baslamaDk),
        bitis = an(bitisSaat, bitisDk),
    )

    @Test
    fun `esigin altindaki blok tetiklemez`() {
        // 08:00-08:40, 40 dk — esik 45 dk
        val olaylar = listOf(olay(8, 0, 8, 40))
        val sonuc = Tetikleyici.degerlendir(olaylar, an(8, 40), sonEylem = null)
        assertEquals(TetikSonucu.GOSTERME, sonuc)
    }

    @Test
    fun `esigi asan kesintisiz blok tetikler`() {
        // 08:00-08:50, 50 dk — esik 45 dk asildi
        val olaylar = listOf(olay(8, 0, 8, 50))
        val sonuc = Tetikleyici.degerlendir(olaylar, an(8, 50), sonEylem = null)
        assertEquals(TetikSonucu.GOSTER, sonuc)
    }

    @Test
    fun `tolerans icindeki gecisler blogu bozmaz`() {
        // 08:00-08:20, 1 dk boşluk (tolerans 2dk), 08:21-08:50 devam — toplam 50dk
        val olaylar = listOf(
            olay(8, 0, 8, 20),
            olay(8, 21, 8, 50),
        )
        val sonuc = Tetikleyici.degerlendir(olaylar, an(8, 50), sonEylem = null)
        assertEquals(TetikSonucu.GOSTER, sonuc)
    }

    @Test
    fun `toleransi asan bosluk blogu sifirlar`() {
        // 08:00-08:20 (20dk), 5dk boşluk (tolerans 2dk aşıldı), 08:25-08:50 (25dk) — hiçbiri esigi asmiyor
        val olaylar = listOf(
            olay(8, 0, 8, 20),
            olay(8, 25, 8, 50),
        )
        val sonuc = Tetikleyici.degerlendir(olaylar, an(8, 50), sonEylem = null)
        assertEquals(TetikSonucu.GOSTERME, sonuc)
    }

    @Test
    fun `simdiden uzak biten blok acik sayilmaz`() {
        // Blok 08:00-08:50 sürdü ama şimdi 09:10 — tolerans 2dk çok aşıldı
        val olaylar = listOf(olay(8, 0, 8, 50))
        val sonuc = Tetikleyici.degerlendir(olaylar, an(9, 10), sonEylem = null)
        assertEquals(TetikSonucu.GOSTERME, sonuc)
    }

    @Test
    fun `mola sonum penceresinde tetiklenmez`() {
        val olaylar = listOf(olay(8, 0, 8, 50))
        val simdi = an(8, 50)
        val sonEylem = SonEylem(EylemTuru.MOLA_VERILDI, simdi.minusSeconds(60))
        val sonuc = Tetikleyici.degerlendir(olaylar, simdi, sonEylem)
        assertEquals(TetikSonucu.GOSTERME, sonuc)
    }

    @Test
    fun `mola sonumu bitince tekrar tetiklenir`() {
        val olaylar = listOf(olay(8, 0, 8, 50))
        val simdi = an(8, 50)
        val sonEylem = SonEylem(EylemTuru.MOLA_VERILDI, simdi.minus(NudgeSabitleri.SONUM_MOLA).minusSeconds(1))
        val sonuc = Tetikleyici.degerlendir(olaylar, simdi, sonEylem)
        assertEquals(TetikSonucu.GOSTER, sonuc)
    }

    @Test
    fun `devam et sonumu moladan daha uzun`() {
        val olaylar = listOf(olay(8, 0, 8, 50))
        val simdi = an(8, 50)
        // Mola sönümü geçmiş olurdu ama Devam et sönümü hâlâ sürüyor.
        val sonEylem = SonEylem(EylemTuru.DEVAM_EDILDI, simdi.minus(NudgeSabitleri.SONUM_MOLA).minusSeconds(1))
        val sonuc = Tetikleyici.degerlendir(olaylar, simdi, sonEylem)
        assertEquals(TetikSonucu.GOSTERME, sonuc)
    }
}
