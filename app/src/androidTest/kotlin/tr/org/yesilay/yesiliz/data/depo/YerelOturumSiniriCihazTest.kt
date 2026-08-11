package tr.org.yesilay.yesiliz.data.depo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.JetonDeposu
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb
import tr.org.yesilay.yesiliz.data.yerel.entity.OdulSecimiEntity
import java.time.LocalDate
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class YerelOturumSiniriCihazTest {

    private val baglam = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var db: YesilizDb
    private lateinit var tercihler: Tercihler
    private lateinit var jetonlar: JetonDeposu
    private lateinit var sinir: YerelOturumSiniri

    @Before
    fun kur() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(baglam, YesilizDb::class.java)
            .allowMainThreadQueries()
            .build()
        tercihler = Tercihler(baglam)
        jetonlar = JetonDeposu(baglam)
        tercihler.sifirla()
        jetonlar.temizle()
        sinir = YerelOturumSiniri(
            db = db,
            tercihler = tercihler,
            jetonDeposu = jetonlar,
            kategoriDeposu = KategoriDeposu.olustur(baglam, db.kategoriEslemeDao()),
            saat = Clock.fixed(Instant.parse("2026-08-11T01:02:03Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun kapat() = runBlocking {
        tercihler.sifirla()
        jetonlar.temizle()
        db.close()
    }

    @Test
    fun farkliHesapRoomTercihVeJetonSiniriniTemizler() = runTest {
        sinir.hesabaGec("a@example.com")
        db.odulSecimiDao().kaydet(
            OdulSecimiEntity("kupon_market", "fidan_bagisi", LocalDate.of(2026, 8, 11)),
        )
        jetonlar.kaydet("eski-erisim", "eski-yenileme")

        // Aynı hesap yerel geçmişi korur.
        sinir.hesabaGec("A@example.com")
        assertNotNull(db.odulSecimiDao().getir("kupon_market"))

        // Farklı hesap önceki kullanıcının davranış verisini göremez.
        sinir.hesabaGec("b@example.com")
        assertNull(db.odulSecimiDao().getir("kupon_market"))
        assertNull(jetonlar.erisim())
        assertEquals("b@example.com", tercihler.veriSahibi.first())
        assertEquals(Instant.parse("2026-08-11T01:02:03Z"), tercihler.veriBaslangicAni.first())
        assertTrue(db.kategoriEslemeDao().sayi() > 0)
    }

    @Test
    fun misafirdenHesabaDonusumYerelOduluKorur() = runTest {
        sinir.misafireGec()
        db.odulSecimiDao().kaydet(
            OdulSecimiEntity("kupon_market", "market_kuponu", LocalDate.of(2026, 8, 11)),
        )

        sinir.hesabaGec("yeni@example.com")

        assertNotNull(db.odulSecimiDao().getir("kupon_market"))
        assertEquals("yeni@example.com", tercihler.veriSahibi.first())
    }

    @Test
    fun eskiSurumdeOturumsuzYerelVeriYeniHesabaTasinmaz() = runTest {
        tercihler.kurulumuIsaretle(LocalDate.of(2026, 8, 1), gecmisGun = 4)
        db.odulSecimiDao().kaydet(
            OdulSecimiEntity("kupon_market", "fidan_bagisi", LocalDate.of(2026, 8, 2)),
        )

        sinir.baslat()
        assertEquals(BILINMEYEN_SAHIP, tercihler.veriSahibi.first())

        sinir.hesabaGec("yeni@example.com")

        assertNull(db.odulSecimiDao().getir("kupon_market"))
        assertEquals("yeni@example.com", tercihler.veriSahibi.first())
        assertEquals(0, tercihler.gecmisGunSayisi.first())
    }
}
