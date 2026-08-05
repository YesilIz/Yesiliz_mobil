package tr.org.yesilay.yesiliz.data.yerel

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Şema geçişleri.
 *
 * `fallbackToDestructiveMigration` kaldırıldı; artık geçiş yazılmazsa
 * uygulama açılışta çöküyor. Bu test o çöküşü **kullanıcıdan önce**
 * yakalıyor: sürüm 1 veritabanı kurulup 2'ye taşınıyor ve Room'un
 * doğrulaması (`validateDroppedTables = true`) şemanın beklenenle birebir
 * olduğunu kontrol ediyor.
 *
 * Yeni bir sürüm eklendiğinde buraya bir test daha eklenmesi gerekiyor.
 *
 * **Metod adları camelCase** — emülatör testlerinde backtick'li boşluklu
 * ad DEX'te yasak ("Space characters in SimpleName ... are not allowed").
 * Birim testlerde (`src/test/`) serbest, burada değil.
 */
@RunWith(AndroidJUnit4::class)
class GecisTest {

    @get:Rule
    val yardimci = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        YesilizDb::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun gecisSemaDogrulamasindanGeciyor() {
        yardimci.createDatabase(TEST_DB, 1).close()

        // `validateDroppedTables = true`: Room şemanın beklenenle birebir
        // olduğunu doğruluyor. Eksik bir sütun ya da indeks burada patlıyor.
        yardimci.runMigrationsAndValidate(TEST_DB, 2, true, GECIS_1_2)
    }

    @Test
    @Throws(IOException::class)
    fun gecisVarOlanVeriyiKoruyor() {
        yardimci.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO gunluk_ozet (tarih, socialDk, entertainmentDk, gamesDk, " +
                    "communicationDk, productivityDk, educationDk, healthDk, otherDk, " +
                    "gece, oturum, acilis, sabah, uykuOncesi, yerDegis, adim, sporDk, " +
                    "bTuketim, bIslevsel, P, A, D, S, geceKullanimDk, acilisSayisi) " +
                    "VALUES ('2026-07-30', 120.0, 0,0,0,0,0,0,0, 0,0,0,0,0,0, 0,0, " +
                    "0,0,1,0,1,0, 0,0)",
            )
            close()
        }

        val db = yardimci.runMigrationsAndValidate(TEST_DB, 2, true, GECIS_1_2)

        db.query("SELECT tarih, socialDk FROM gunluk_ozet").use { imlec ->
            assertTrue("Geçişten sonra satır kaybolmuş", imlec.moveToFirst())
            assertEquals("2026-07-30", imlec.getString(0))
            assertEquals(120.0, imlec.getDouble(1), 0.001)
        }
    }

    @Test
    @Throws(IOException::class)
    fun yeniTablolarOlusuyor() {
        yardimci.createDatabase(TEST_DB, 1).close()
        val db = yardimci.runMigrationsAndValidate(TEST_DB, 2, true, GECIS_1_2)

        listOf("ruh_hali", "uyku_beyani", "farkindalik_testi", "bildirim", "arkadas").forEach { ad ->
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(ad))
                .use { imlec ->
                    assertTrue("$ad tablosu oluşmamış", imlec.moveToFirst())
                }
        }
    }

    @Test
    @Throws(IOException::class)
    fun gecis2den3eSemaDogrulamasindanGeciyor() {
        yardimci.createDatabase(TEST_DB, 1).close()
        yardimci.runMigrationsAndValidate(TEST_DB, 3, true, GECIS_1_2, GECIS_2_3)
    }

    /**
     * `olayVarMi` geriye dönük doldurma.
     *
     * Var olan satırlar `0` ile başlayıp ölçülmüş olanlar `1`'e çekiliyor.
     * Ölçüt `acilisSayisi > 0`: `sistem` kategorisi risk hesabına girmediği
     * için (CLAUDE.md §12 K9) gerçekten kullanılmış bir günün kategori
     * toplamı sıfır olabiliyor, ama açılış sayılıyor.
     */
    @Test
    @Throws(IOException::class)
    fun gecis2den3eOlculmusGunleriIsaretliyor() {
        yardimci.createDatabase(TEST_DB, 2).apply {
            execSQL(satir("2026-07-29", acilis = 45))
            execSQL(satir("2026-07-30", acilis = 0))
            close()
        }

        val db = yardimci.runMigrationsAndValidate(TEST_DB, 3, true, GECIS_1_2, GECIS_2_3)

        db.query("SELECT tarih, olayVarMi FROM gunluk_ozet ORDER BY tarih").use { imlec ->
            assertTrue(imlec.moveToFirst())
            assertEquals("2026-07-29", imlec.getString(0))
            assertEquals("ölçülmüş gün işaretlenmemiş", 1, imlec.getInt(1))

            assertTrue(imlec.moveToNext())
            assertEquals("2026-07-30", imlec.getString(0))
            assertEquals("boş telafi günü işaretlenmiş", 0, imlec.getInt(1))
        }
    }

    /**
     * 3 → 4: `adim_sayac` tablosu (`debug.md` K-0).
     *
     * Var olan hiçbir tablo değişmediği için veri taşıma iddiası yok; test
     * ettiği şey geçişin **1'den 4'e kadar** zincirlenmesi ve Room'un beklediği
     * şemayla birebir çıkması.
     */
    @Test
    @Throws(IOException::class)
    fun gecis3ten4eSemaDogrulamasindanGeciyor() {
        yardimci.createDatabase(TEST_DB, 1).close()
        yardimci.runMigrationsAndValidate(TEST_DB, 4, true, GECIS_1_2, GECIS_2_3, GECIS_3_4)
    }

    @Test
    @Throws(IOException::class)
    fun gecis3ten4eAdimSayaciEskiVeriyiKorumuyor() {
        yardimci.createDatabase(TEST_DB, 3).apply {
            // Sürüm 3'te `olayVarMi` var ve `NOT NULL`; sürüm 2 için yazılan
            // [satir] onu bilmiyor, o yüzden burada açıkça veriliyor.
            execSQL(satir("2026-08-02", acilis = 12, olayVarMi = 1))
            close()
        }

        val db = yardimci.runMigrationsAndValidate(TEST_DB, 4, true, GECIS_1_2, GECIS_2_3, GECIS_3_4)

        // Yeni tablo boş açılıyor; eski özet satırı yerinde duruyor.
        db.query("SELECT COUNT(*) FROM adim_sayac").use { imlec ->
            assertTrue(imlec.moveToFirst())
            assertEquals(0, imlec.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM gunluk_ozet").use { imlec ->
            assertTrue(imlec.moveToFirst())
            assertEquals("geçiş var olan özeti silmiş", 1, imlec.getInt(0))
        }
    }

    /**
     * [olayVarMi] yalnızca sürüm 3 ve üstünde verilebiliyor — kolon 2 → 3
     * geçişinde eklendi ve `NOT NULL`. Sürüm 2 satırı yazan testler `null`
     * bırakıyor, kolon INSERT listesine hiç girmiyor.
     */
    /**
     * 4 → 5: `odul_secimi` tablosu.
     *
     * Var olan hiçbir tablo değişmediği için veri taşıma iddiası yok; test
     * ettiği şey geçişin **1'den 5'e kadar** zincirlenmesi ve Room'un
     * beklediği şemayla birebir çıkması.
     */
    @Test
    @Throws(IOException::class)
    fun gecis4ten5eSemaDogrulamasindanGeciyor() {
        yardimci.createDatabase(TEST_DB, 1).close()
        yardimci.runMigrationsAndValidate(
            TEST_DB, 5, true, GECIS_1_2, GECIS_2_3, GECIS_3_4, GECIS_4_5,
        )
    }

    private fun satir(tarih: String, acilis: Int, olayVarMi: Int? = null): String {
        val ekKolon = if (olayVarMi == null) "" else ", olayVarMi"
        val ekDeger = if (olayVarMi == null) "" else ", $olayVarMi"
        return "INSERT INTO gunluk_ozet (tarih, socialDk, entertainmentDk, gamesDk, " +
            "communicationDk, productivityDk, educationDk, healthDk, otherDk, " +
            "gece, oturum, acilis, sabah, uykuOncesi, yerDegis, adim, sporDk, " +
            "bTuketim, bIslevsel, P, A, D, S, geceKullanimDk, acilisSayisi$ekKolon) " +
            "VALUES (\'$tarih\', 0,0,0,0,0,0,0,0, 0,0,0,0,0,0, 0,0, " +
            "0,0,1,0,1,0, 0,$acilis$ekDeger)"
    }

    private companion object {
        const val TEST_DB = "gecis_testi.db"
    }
}
