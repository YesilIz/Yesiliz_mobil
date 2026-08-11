package tr.org.yesilay.yesiliz.data.yerel

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.arkaplan.gunlukZinciriAtomik
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.KullanimOlayEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.RiskSonucEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.BildirimEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.ArkadasEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.GorevTamamlamaEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.PuanGunlukEntity
import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
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

    @Test
    fun esitZamanliListelerKararliSiralanir() = runTest {
        val an = Instant.parse("2026-08-10T21:00:00Z")
        val bildirimDao = db.bildirimDao()
        listOf("Bir", "İki", "Üç").forEach { baslik ->
            bildirimDao.ekle(
                BildirimEntity(
                    tur = "hatirlatma",
                    baslik = baslik,
                    govde = "Gövde",
                    zaman = an,
                ),
            )
        }

        assertEquals(
            listOf("Üç", "İki", "Bir"),
            bildirimDao.sonlarAkisi(10).first().map { it.baslik },
        )

        val tarih = LocalDate.of(2026, 8, 11)
        val arkadasDao = db.arkadasDao()
        arkadasDao.kaydet(ArkadasEntity("Zeynep", ArkadasEntity.DURUM_EKLENDI, tarih))
        arkadasDao.kaydet(ArkadasEntity("Ali", ArkadasEntity.DURUM_EKLENDI, tarih))

        assertEquals(
            listOf("Ali", "Zeynep"),
            arkadasDao.hepsiAkisi().first().map { it.isim },
        )
    }

    @Test
    fun aktifGunGorevVeGercekKullanimKanitiBirlikteIster() = runTest {
        val puanDao = db.puanGunlukDao()
        val gorevDao = db.gorevTamamlamaDao()
        val ozetDao = db.gunlukOzetDao()
        val gunler = (1..4).map { LocalDate.of(2026, 8, it) }

        gunler.forEach { gun ->
            puanDao.kaydet(PuanGunlukEntity(gun, 10, 10, 1, 1.0))
        }
        // 1: yalnızca görev, 2: yalnızca kullanım, 3: ikisi de,
        // 4: görev + telafi için yazılmış boş özet.
        listOf(gunler[0], gunler[2], gunler[3]).forEach { gun ->
            gorevDao.ekle(GorevTamamlamaEntity(tarih = gun, gorevId = "nefes", puan = 5, dogrulamaTuru = "aktivite"))
        }
        ozetDao.kaydet(GunlukOzetEntity(tarih = gunler[1], olayVarMi = true))
        ozetDao.kaydet(GunlukOzetEntity(tarih = gunler[2], olayVarMi = true))
        ozetDao.kaydet(GunlukOzetEntity(tarih = gunler[3], olayVarMi = false))

        assertEquals(1, puanDao.aktifGunAkisi().first())
    }

    @Test
    fun esZamanliGorevTamamlamaGunlukLimitiAsamaz() = runTest {
        val depo = PuanDeposu(
            db = db,
            gorevDao = db.gorevTamamlamaDao(),
            puanDao = db.puanGunlukDao(),
            ozetDao = db.gunlukOzetDao(),
            tercihler = Tercihler(baglam),
        )
        val gun = LocalDate.of(2026, 8, 11)

        val sonuclar = List(8) { async { depo.gorevTamamla(Gorev.NEFES, gun) } }.awaitAll()

        assertEquals(Gorev.NEFES.gunlukLimit, sonuclar.count { it })
        assertEquals(Gorev.NEFES.gunlukLimit, db.gorevTamamlamaDao().gununSayisi(gun, Gorev.NEFES.id))
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
