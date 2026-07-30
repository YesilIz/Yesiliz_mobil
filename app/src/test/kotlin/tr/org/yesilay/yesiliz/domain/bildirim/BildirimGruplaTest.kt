package tr.org.yesilay.yesiliz.domain.bildirim

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class BildirimGruplaTest {

    private val zon: ZoneId = ZoneId.of("Europe/Istanbul")

    /** 2026-07-31 Cuma. Haftanın pazartesisi 27 Temmuz. */
    private val bugun = LocalDate.of(2026, 7, 31)

    private fun an(yil: Int, ay: Int, gun: Int, saat: Int = 12, dakika: Int = 0) =
        LocalDateTime.of(yil, ay, gun, saat, dakika).atZone(zon).toInstant()

    @Test
    fun `bugun dun ve bu hafta ayriliyor`() {
        assertEquals(BildirimGrubu.BUGUN, BildirimGrupla.grubu(an(2026, 7, 31), bugun, zon))
        assertEquals(BildirimGrubu.DUN, BildirimGrupla.grubu(an(2026, 7, 30), bugun, zon))
        assertEquals(BildirimGrubu.BU_HAFTA, BildirimGrupla.grubu(an(2026, 7, 29), bugun, zon))
    }

    @Test
    fun `haftanin pazartesisi bu haftaya dahil`() {
        assertEquals(BildirimGrubu.BU_HAFTA, BildirimGrupla.grubu(an(2026, 7, 27), bugun, zon))
    }

    @Test
    fun `onceki pazar daha onceye dusuyor`() {
        // Takvim haftası kullanılıyor, kayan 7 gün değil: 26 Temmuz Pazar,
        // bugünden 5 gün önce olmasına rağmen geçen haftaya ait.
        assertEquals(BildirimGrubu.DAHA_ONCE, BildirimGrupla.grubu(an(2026, 7, 26), bugun, zon))
    }

    @Test
    fun `gun sinirinda gruplama yerel gune gore`() {
        // 30 Temmuz 23:59 yerel saatte dün; UTC'de 31'e taşmış olsa bile.
        assertEquals(
            BildirimGrubu.DUN,
            BildirimGrupla.grubu(an(2026, 7, 30, 23, 59), bugun, zon),
        )
    }

    @Test
    fun `zaman etiketi dakika ve saat esiklerini ayiriyor`() {
        val simdi = an(2026, 7, 31, 14, 0)

        assertEquals("Az önce", BildirimGrupla.zamanEtiketi(simdi, simdi, zon))
        assertEquals(
            "30 dakika önce",
            BildirimGrupla.zamanEtiketi(an(2026, 7, 31, 13, 30), simdi, zon),
        )
        assertEquals(
            "2 saat önce",
            BildirimGrupla.zamanEtiketi(an(2026, 7, 31, 12, 0), simdi, zon),
        )
    }

    @Test
    fun `dun icin saat gosteriliyor`() {
        val simdi = an(2026, 7, 31, 14, 0)

        assertEquals(
            "Dün, 21:14",
            BildirimGrupla.zamanEtiketi(an(2026, 7, 30, 21, 14), simdi, zon),
        )
    }

    @Test
    fun `hafta icindeki gunler ad ile gosteriliyor`() {
        val simdi = an(2026, 7, 31, 14, 0)

        assertEquals(
            "Pazartesi",
            BildirimGrupla.zamanEtiketi(an(2026, 7, 27, 9, 0), simdi, zon),
        )
    }

    @Test
    fun `bir haftadan eski kayitlar tarih ile gosteriliyor`() {
        val simdi = an(2026, 7, 31, 14, 0)

        assertEquals(
            "3 Temmuz",
            BildirimGrupla.zamanEtiketi(an(2026, 7, 3, 9, 0), simdi, zon),
        )
    }
}
