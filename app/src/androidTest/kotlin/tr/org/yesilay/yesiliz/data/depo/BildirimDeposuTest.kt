package tr.org.yesilay.yesiliz.data.depo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Bildirim defterinin silme/geri alma yolu.
 *
 * Kaydırarak silme cihazda görüldü ama "Geri al" bağlantısını ekran
 * görüntüsüyle yakalamak snackbar süresine bağlı ve güvenilmez; asıl
 * garanti burada. Geri alınamayan bir silme, yanlışlıkla kaydıran
 * kullanıcıya kaydı kalıcı kaybettirirdi.
 */
@RunWith(AndroidJUnit4::class)
class BildirimDeposuTest {

    private lateinit var db: YesilizDb
    private lateinit var depo: BildirimDeposu

    private val saat: Clock = Clock.fixed(
        Instant.parse("2026-08-14T09:00:00Z"),
        ZoneId.of("Europe/Istanbul"),
    )

    @Before
    fun kur() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            YesilizDb::class.java,
        ).build()
        depo = BildirimDeposu(db.bildirimDao(), saat)
    }

    @After
    fun kapat() {
        db.close()
    }

    @Test
    fun silinenBildirimGeriYuklenebiliyor() = runTest {
        depo.gorevTamamlandi(Gorev.RUH_HALI)
        val kayit = depo.sonlar().first().single()

        depo.sil(kayit.id)
        assertTrue("silinmemiş", depo.sonlar().first().isEmpty())

        depo.geriYukle(kayit)

        val geriGelen = depo.sonlar().first().single()
        assertEquals("aynı satır dönmedi", kayit.id, geriGelen.id)
        assertEquals(kayit.baslik, geriGelen.baslik)
        assertEquals(kayit.govde, geriGelen.govde)
        assertEquals("okundu durumu korunmadı", kayit.okundu, geriGelen.okundu)
        assertEquals("zaman damgası korunmadı", kayit.zaman, geriGelen.zaman)
    }

    @Test
    fun silinen_bildirim_getir_ile_okunabiliyor() = runTest {
        // Ekran, silmeden ÖNCE satırı okuyup saklıyor; okunamazsa geri
        // alınacak bir şey de kalmıyor.
        depo.gorevTamamlandi(Gorev.NEFES)
        val kayit = depo.sonlar().first().single()

        assertNotNull(depo.getir(kayit.id))
        depo.sil(kayit.id)
        assertNull("silinen satır hâlâ okunuyor", depo.getir(kayit.id))
    }

    @Test
    fun hepsiniSil_defteri_bosaltiyor() = runTest {
        depo.gorevTamamlandi(Gorev.RUH_HALI)
        depo.gorevTamamlandi(Gorev.NEFES)
        assertEquals(2, depo.sonlar().first().size)

        depo.hepsiniSil()

        assertTrue(depo.sonlar().first().isEmpty())
        assertEquals(0, depo.okunmamisSayisi.first())
    }

    @Test
    fun tek_silme_digerlerine_dokunmuyor() = runTest {
        depo.gorevTamamlandi(Gorev.RUH_HALI)
        depo.gorevTamamlandi(Gorev.NEFES)
        val hepsi = depo.sonlar().first()

        depo.sil(hepsi.first().id)

        val kalan = depo.sonlar().first()
        assertEquals(1, kalan.size)
        assertEquals(hepsi.last().id, kalan.single().id)
    }
}
