package tr.org.yesilay.yesiliz.data.yerel

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tr.org.yesilay.yesiliz.data.depo.KategoriDeposu
import tr.org.yesilay.yesiliz.arkaplan.gunlukZinciriAtomik
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.KullanimOlayEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.RiskSonucEntity
import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import java.time.Instant
import java.time.LocalDate

/**
 * Veritabanının **gerçekten** oluştuğunu ve çalıştığını doğrular.
 *
 * Birim testi değil — emülatörde koşar (`./gradlew :app:connectedDebugAndroidTest`),
 * çünkü Room'un SQLite'ı Android'in kendi motoru. Bellekte veritabanı
 * kuruluyor, disk kirlenmiyor.
 */
@RunWith(AndroidJUnit4::class)
class YesilizDbTest {

    private lateinit var db: YesilizDb
    private val baglam = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun kur() {
        db = Room.inMemoryDatabaseBuilder(baglam, YesilizDb::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun kapat() = db.close()

    // -----------------------------------------------------------------
    @Test
    fun tumTablolarOlusuyor() = runTest {
        // Her DAO'dan bir okuma yap — tablo yoksa SQLite patlar
        assertEquals(0, db.kullanimOlayDao().gununSayisi(LocalDate.now()))
        assertNull(db.gunlukOzetDao().getir(LocalDate.now()))
        assertNull(db.riskSonucDao().getir(LocalDate.now()))
        assertNull(db.anketYanitDao().sonuncu())
        assertEquals(0, db.kategoriEslemeDao().sayi())
        assertEquals(0, db.gorevTamamlamaDao().gununPuani(LocalDate.now()))
        assertNull(db.puanGunlukDao().sonuncu())
    }

    @Test
    fun tarihVeZamanDonusturuculeriCalisiyor() = runTest {
        val tarih = LocalDate.of(2026, 7, 28)
        val bas = Instant.ofEpochMilli(1_785_000_000_000)

        db.kullanimOlayDao().ekle(
            listOf(
                KullanimOlayEntity(
                    paketAdi = "com.instagram.android",
                    kategori = "social",
                    baslangic = bas,
                    bitis = bas.plusSeconds(600),
                    tarih = tarih,
                ),
            ),
        )

        val geri = db.kullanimOlayDao().gunun(tarih)
        assertEquals(1, geri.size)
        assertEquals(bas, geri[0].baslangic)
        assertEquals(tarih, geri[0].tarih)
        assertEquals("com.instagram.android", geri[0].paketAdi)
    }

    @Test
    fun listeDonusturucusuSurukleyicileriKoruyor() = runTest {
        val tarih = LocalDate.of(2026, 7, 28)
        db.riskSonucDao().kaydet(
            RiskSonucEntity(
                tarih = tarih, S = 240.0, rB = 42.8, rS = null, alpha = 0.0, R = 42.8,
                adayBant = "ORTA RİSKLİ", gosterilenBant = "ORTA RİSKLİ",
                guvenlikKurali = null, trend = "sabit",
                surukleyiciler = listOf("social", "entertainment", "games"),
            ),
        )
        val geri = db.riskSonucDao().getir(tarih)
        assertNotNull(geri)
        assertEquals(listOf("social", "entertainment", "games"), geri!!.surukleyiciler)
        assertNull(geri.rS)
    }

    @Test
    fun ayniTariheIkinciYazmaUzerineYazar() = runTest {
        val tarih = LocalDate.of(2026, 7, 28)
        val dao = db.gunlukOzetDao()
        dao.kaydet(GunlukOzetEntity(tarih = tarih, socialDk = 60.0))
        dao.kaydet(GunlukOzetEntity(tarih = tarih, socialDk = 90.0))

        assertEquals(1, dao.sonGunler(10).size)
        assertEquals(90.0, dao.getir(tarih)!!.socialDk, 0.0)
    }

    @Test
    fun gunlukZincirOrtadaBozulursaKismiYazmaGeriAlinir() = runTest {
        val tarih = LocalDate.of(2026, 8, 10)
        val dao = db.gunlukOzetDao()

        try {
            gunlukZinciriAtomik(db) {
                dao.kaydet(GunlukOzetEntity(tarih = tarih, socialDk = 45.0))
                error("risk hesabı benzetimli olarak bozuldu")
            }
        } catch (_: IllegalStateException) {
            // Beklenen: işlem geri alınacak.
        }

        assertNull("kısmi özet satırı kalmamalı", dao.getir(tarih))
    }

    @Test
    fun eskiOlaylarSilinebiliyor() = runTest {
        val dao = db.kullanimOlayDao()
        val eski = LocalDate.of(2026, 7, 1)
        val yeni = LocalDate.of(2026, 7, 28)
        val an = Instant.ofEpochMilli(1_785_000_000_000)

        dao.ekle(
            listOf(eski, yeni).map {
                KullanimOlayEntity(
                    paketAdi = "com.whatsapp", kategori = "communication",
                    baslangic = an, bitis = an.plusSeconds(60), tarih = it,
                )
            },
        )
        val silinen = dao.eskileriSil(LocalDate.of(2026, 7, 15))
        assertEquals(1, silinen)
        assertEquals(1, dao.gununSayisi(yeni))
        assertEquals(0, dao.gununSayisi(eski))
    }

    // -----------------------------------------------------------------
    // Kategori tablosunun assets'ten Room'a yüklenmesi
    // -----------------------------------------------------------------
    @Test
    fun kategoriTablosuAssetsdenYukleniyor() = runTest {
        val depo = KategoriDeposu.olustur(baglam, db.kategoriEslemeDao())

        val eklenen = depo.seedEt()
        assertTrue("3000'den fazla paket bekleniyordu, gelen: $eklenen", eklenen > 3000)
        assertEquals(eklenen, db.kategoriEslemeDao().sayi())

        // İkinci çağrı hiçbir şey yapmamalı
        assertEquals(0, depo.seedEt())
    }

    @Test
    fun yuklenenTablodanEsleyiciKuruluyor() = runTest {
        val depo = KategoriDeposu.olustur(baglam, db.kategoriEslemeDao())
        depo.seedEt()

        val esleyici = depo.esleyiciAl()
        assertEquals(Kategori.SOCIAL, esleyici.bul("com.instagram.android"))
        assertEquals(Kategori.ENTERTAINMENT, esleyici.bul("com.zhiliaoapp.musically"))
        assertEquals(Kategori.OTHER, esleyici.bul("com.bilinmeyen.uygulama"))
    }

    @Test
    fun kullaniciSecimiSeedTarafindanEzilmiyor() = runTest {
        val dao = db.kategoriEslemeDao()
        val depo = KategoriDeposu.olustur(baglam, dao)
        depo.seedEt()

        depo.kullaniciSecti("com.instagram.android", Kategori.PRODUCTIVITY)
        assertEquals(Kategori.PRODUCTIVITY, depo.esleyiciAl().bul("com.instagram.android"))

        // Tablo yeniden seed edilse bile kullanıcı seçimi kalmalı
        depo.seedEt(zorla = true)
        assertEquals(
            "seed kullanıcı seçimini ezmemeli",
            Kategori.PRODUCTIVITY,
            depo.esleyiciAl().bul("com.instagram.android"),
        )
    }
}
