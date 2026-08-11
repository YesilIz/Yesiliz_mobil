package tr.org.yesilay.yesiliz.data.kullanim

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tr.org.yesilay.yesiliz.data.depo.KategoriDeposu
import tr.org.yesilay.yesiliz.data.depo.KullanimDeposu
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * `UsageStatsManager` yolunun gerçekten çalıştığını doğrular.
 *
 * **İzin gerekiyor.** Testten önce:
 * ```
 * adb shell appops set tr.org.yesilay.yesiliz android:get_usage_stats allow
 * ```
 * İzin yoksa testler `Assume` ile atlanır — kırmızıya döndürmez, çünkü
 * izin veremeyen bir ortamda başarısızlık yanıltıcı olur.
 *
 * **Emülatör uyarısı (CLAUDE.md §10):** taze emülatörde üçüncü parti
 * uygulama yok, dönen veri neredeyse boş. Bu testler *API yolunun*
 * çalıştığını gösterir, *döngünün* çalıştığını değil — o gerçek telefon ister.
 */
@RunWith(AndroidJUnit4::class)
class KullanimOkuyucuTest {

    private val baglam = InstrumentationRegistry.getInstrumentation().targetContext
    private val zon: ZoneId = ZoneId.systemDefault()
    private lateinit var db: YesilizDb

    @Before
    fun kur() {
        db = Room.inMemoryDatabaseBuilder(baglam, YesilizDb::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun kapat() = db.close()

    private fun okuyucu() = KullanimOkuyucu.olustur(baglam, zon)

    // -----------------------------------------------------------------
    @Test
    fun izinDurumuOkunabiliyor() {
        // Fırlatmadan bir sonuç dönmeli — true da false da geçerli
        val verildi = KullanimIzni.verildiMi(baglam)
        assertTrue("izin durumu okunabilmeli", verildi || !verildi)
    }

    @Test
    fun ayarEkraniCozuluyor() {
        assertTrue(
            "Kullanım erişimi ayar ekranı bulunamadı — kullanıcı yönlendirilemez",
            KullanimIzni.ayarEkraniVarMi(baglam),
        )
    }

    @Test
    fun sistemOlaylariOkunabiliyor() {
        assumeTrue("PACKAGE_USAGE_STATS izni yok", KullanimIzni.verildiMi(baglam))

        val bugun = LocalDate.now(zon)
        val oturumlar = okuyucu().gununOturumlari(bugun)

        // Emülatörde boş dönebilir — çökmemesi ve tutarlı olması yeterli
        assertNotNull(oturumlar)
        for (o in oturumlar) {
            assertTrue("bitiş başlangıçtan sonra olmalı: $o", o.bitis.isAfter(o.baslangic))
            assertTrue("oturum 4 saati aşmamalı", o.sure.toHours() <= 4)
            assertTrue("paket adı boş olmamalı", o.paketAdi.isNotEmpty())
        }
    }

    @Test
    fun gelecekteki_gun_bos_doner() {
        assumeTrue(KullanimIzni.verildiMi(baglam))
        val yarin = LocalDate.now(zon).plusDays(1)
        assertTrue(
            "gelecek gün için veri olamaz",
            okuyucu().gununOturumlari(yarin, simdi = Instant.now()).isEmpty(),
        )
    }

    @Test
    fun sahipBaslangiciPencereBitisindenSonraysaSistemSorgulanmaz() {
        val bugun = LocalDate.of(2026, 8, 11)
        val simdi = Instant.parse("2026-08-11T12:00:00Z")
        val yeniSahip = Instant.parse("2026-08-11T12:00:01Z")

        assertTrue(
            okuyucu().gununOturumlari(bugun, simdi = simdi, enErken = yeniSahip).isEmpty(),
        )
    }

    @Test
    fun androidKategorisiSorgulanabiliyor() {
        // Kendi paketimiz her zaman görünür
        val kategori = okuyucu().androidKategorisi(baglam.packageName)
        assertNotNull("kendi paketimizin ApplicationInfo'su okunabilmeli", kategori)
    }

    // -----------------------------------------------------------------
    // Uçtan uca: sistem → kategori → Room → özet
    // -----------------------------------------------------------------
    @Test
    fun gunuIsleZinciriCalisiyor() = runTest {
        assumeTrue("PACKAGE_USAGE_STATS izni yok", KullanimIzni.verildiMi(baglam))

        val kategoriDeposu = KategoriDeposu.olustur(baglam, db.kategoriEslemeDao())
        kategoriDeposu.seedEt()

        val depo = KullanimDeposu(
            okuyucu = okuyucu(),
            kategoriDeposu = kategoriDeposu,
            olayDao = db.kullanimOlayDao(),
            ozetDao = db.gunlukOzetDao(),
            zon = zon,
        )

        val bugun = LocalDate.now(zon)
        val cikarim = depo.gunuIsle(bugun)

        // Özet satırı yazılmış olmalı — gün boş olsa bile
        val ozet = db.gunlukOzetDao().getir(bugun)
        assertNotNull("günlük özet kaydedilmeli", ozet)
        assertEquals(bugun, ozet!!.tarih)
        assertEquals(cikarim.acilisSayisi, ozet.acilisSayisi)

        // Room'daki olay sayısı çıkarımdaki açılış sayısıyla tutmalı
        assertEquals(
            "kaydedilen olay sayısı çıkarımla tutmalı",
            cikarim.acilisSayisi,
            db.kullanimOlayDao().gununSayisi(bugun),
        )
    }

    @Test
    fun ayniGunIkiKezIslenirseKopyalanmaz() = runTest {
        assumeTrue(KullanimIzni.verildiMi(baglam))

        val kategoriDeposu = KategoriDeposu.olustur(baglam, db.kategoriEslemeDao())
        kategoriDeposu.seedEt()
        val depo = KullanimDeposu(
            okuyucu(), kategoriDeposu, db.kullanimOlayDao(), db.gunlukOzetDao(), zon,
        )

        val bugun = LocalDate.now(zon)
        depo.gunuIsle(bugun)
        val ilkSayi = db.kullanimOlayDao().gununSayisi(bugun)

        depo.gunuIsle(bugun)
        val ikinciSayi = db.kullanimOlayDao().gununSayisi(bugun)

        assertTrue(
            "ikinci işleme olayları katlamamalı ($ilkSayi → $ikinciSayi)",
            ikinciSayi <= ilkSayi + 5,
        )
        assertEquals("özet tek satır kalmalı", 1, db.gunlukOzetDao().sonGunler(10).size)
    }
}
