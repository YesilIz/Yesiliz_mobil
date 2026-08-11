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

    /**
     * 5 → 6: `risk_sonuc.veriDurumu` (`gorev2.md` §8/§10 — dolu veritabanı ile).
     *
     * Var olan hiçbir tablo değişmediği için veri taşıma iddiası yok; test
     * ettiği şey geçişin **1'den 6'ya kadar** zincirlenmesi ve Room'un
     * beklediği şemayla birebir çıkması.
     */
    @Test
    @Throws(IOException::class)
    fun gecis5ten6yaSemaDogrulamasindanGeciyor() {
        yardimci.createDatabase(TEST_DB, 1).close()
        yardimci.runMigrationsAndValidate(
            TEST_DB, 6, true, GECIS_1_2, GECIS_2_3, GECIS_3_4, GECIS_4_5, GECIS_5_6,
        )
    }

    /**
     * Dolu veritabanı: iki günlük gerçek geçmiş (biri ölçülmüş, biri boş
     * telafi günü) + ikisinin `risk_sonuc` satırı — `gorev2.md` §10
     * "gerçek kullanıcı geçmişine benzeyen dolu bir veritabanı". Geçiş
     * sonrası eski satırlar kaybolmamalı ve `veriDurumu`
     * `gunluk_ozet.olayVarMi`'ye göre doğru ayrılmalı.
     */
    @Test
    @Throws(IOException::class)
    fun gecis5ten6yaYetersizGunleriIsaretliyor() {
        yardimci.createDatabase(TEST_DB, 5).apply {
            execSQL(satir("2026-08-03", acilis = 20, olayVarMi = 1))
            execSQL(satir("2026-08-04", acilis = 0, olayVarMi = 0))
            execSQL(riskSonucSatiri("2026-08-03", bant = "ORTA"))
            execSQL(riskSonucSatiri("2026-08-04", bant = "AZ"))
            close()
        }

        val db = yardimci.runMigrationsAndValidate(
            TEST_DB, 6, true, GECIS_1_2, GECIS_2_3, GECIS_3_4, GECIS_4_5, GECIS_5_6,
        )

        db.query("SELECT tarih, veriDurumu FROM risk_sonuc ORDER BY tarih").use { imlec ->
            assertTrue("satır kaybolmuş", imlec.moveToFirst())
            assertEquals("2026-08-03", imlec.getString(0))
            assertEquals("ölçülmüş gün HESAPLANDI kalmalı", "HESAPLANDI", imlec.getString(1))

            assertTrue(imlec.moveToNext())
            assertEquals("2026-08-04", imlec.getString(0))
            assertEquals("boş telafi günü YETERSİZ işaretlenmemiş", "YETERSIZ", imlec.getString(1))
        }
    }

    /**
     * 6 → 7: `aktivite_oturumu` tablosu (`gorev2.md` §4).
     *
     * Var olan hiçbir tablo değişmediği için veri taşıma iddiası yok; test
     * ettiği şey geçişin **1'den 7'ye kadar** zincirlenmesi ve Room'un
     * beklediği şemayla birebir çıkması.
     */
    @Test
    @Throws(IOException::class)
    fun gecis6dan7yeSemaDogrulamasindanGeciyor() {
        yardimci.createDatabase(TEST_DB, 1).close()
        yardimci.runMigrationsAndValidate(
            TEST_DB, 7, true, GECIS_1_2, GECIS_2_3, GECIS_3_4, GECIS_4_5, GECIS_5_6, GECIS_6_7,
        )
    }

    @Test
    @Throws(IOException::class)
    fun gecis7den8eSemaDogrulamasindanGeciyor() {
        yardimci.createDatabase(TEST_DB, 7).close()
        yardimci.runMigrationsAndValidate(TEST_DB, 8, true, GECIS_7_8)
    }

    @Test
    @Throws(IOException::class)
    fun gecis7den8eYalnizcaSahteEbeveynBildiriminiTemizler() {
        yardimci.createDatabase(TEST_DB, 7).apply {
            execSQL(
                "INSERT INTO bildirim (tur, baslik, govde, zaman, okundu) VALUES " +
                    "('ebeveyn', 'Gönderildi', 'Gerçekte gönderilmedi', 1, 0), " +
                    "('puan', '10 puan', 'Gerçek bildirim', 2, 0)",
            )
            close()
        }

        val db = yardimci.runMigrationsAndValidate(TEST_DB, 8, true, GECIS_7_8)

        db.query("SELECT tur, baslik FROM bildirim ORDER BY zaman").use { imlec ->
            assertTrue("gerçek bildirim de silinmiş", imlec.moveToFirst())
            assertEquals("puan", imlec.getString(0))
            assertEquals("10 puan", imlec.getString(1))
            assertTrue("sahte bildirim kalmış", imlec.isLast)
        }
    }

    /**
     * Kapsamlı dolu veritabanı — `gorev2.md` §10 "gerçek kullanıcı geçmişine
     * benzeyen dolu bir veritabanı": üç günlük geçmiş, dört tablo birden
     * (`gunluk_ozet`, `risk_sonuc`, `adim_sayac`) — sürüm 4'te (adım sayacı
     * eklendikten sonra) kurulup 7'ye kadar TÜM geçişlerden (5→6 ve 6→7
     * dahil) geçiriliyor. Doğrulanan üç şey: hiçbir satır kaybolmuyor, yeni
     * kolon/tablo doğru dolduruluyor, hiçbir geçiş veriyi mükerrer yazmıyor
     * (satır sayıları girdiyle birebir).
     */
    @Test
    @Throws(IOException::class)
    fun doluVeritabaniTumGecislerdenVeriKaybiOlmadanGeciyor() {
        val gunler = listOf(
            Triple("2026-08-01", 30, 1), // ölçülmüş gün
            Triple("2026-08-02", 0, 0), // boş telafi günü
            Triple("2026-08-03", 45, 1), // ölçülmüş gün
        )

        yardimci.createDatabase(TEST_DB, 4).apply {
            gunler.forEach { (tarih, acilis, olayVarMi) ->
                execSQL(satir(tarih, acilis = acilis, olayVarMi = olayVarMi))
                execSQL(
                    riskSonucSatiri(
                        tarih,
                        bant = if (olayVarMi == 1) "ORTA" else "AZ",
                    ),
                )
                execSQL(adimSayacSatiri(tarih, toplam = acilis * 100))
            }
            close()
        }

        val db = yardimci.runMigrationsAndValidate(
            TEST_DB, 7, true, GECIS_1_2, GECIS_2_3, GECIS_3_4, GECIS_4_5, GECIS_5_6, GECIS_6_7,
        )

        db.query("SELECT COUNT(*) FROM gunluk_ozet").use { imlec ->
            imlec.moveToFirst()
            assertEquals("3 günlük özet kaybolmuş/çoğalmış", 3, imlec.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM risk_sonuc").use { imlec ->
            imlec.moveToFirst()
            assertEquals("3 risk sonucu kaybolmuş/çoğalmış", 3, imlec.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM adim_sayac").use { imlec ->
            imlec.moveToFirst()
            assertEquals("3 adım sayacı kaybolmuş/çoğalmış", 3, imlec.getInt(0))
        }
        // Yeni tablo boş açılıyor — veri taşıma iddiası yok, yalnızca varlığı.
        db.query("SELECT COUNT(*) FROM aktivite_oturumu").use { imlec ->
            imlec.moveToFirst()
            assertEquals(0, imlec.getInt(0))
        }

        db.query("SELECT tarih, veriDurumu FROM risk_sonuc ORDER BY tarih").use { imlec ->
            gunler.forEach { (tarih, _, olayVarMi) ->
                assertTrue("$tarih satırı kaybolmuş", imlec.moveToNext())
                assertEquals(tarih, imlec.getString(0))
                assertEquals(
                    "$tarih için veriDurumu olayVarMi'ye göre yanlış",
                    if (olayVarMi == 1) "HESAPLANDI" else "YETERSIZ",
                    imlec.getString(1),
                )
            }
        }

        db.query("SELECT tarih, toplam FROM adim_sayac ORDER BY tarih").use { imlec ->
            gunler.forEach { (tarih, acilis, _) ->
                assertTrue(imlec.moveToNext())
                assertEquals(tarih, imlec.getString(0))
                assertEquals("adım birikimi bozulmuş", (acilis * 100).toLong(), imlec.getLong(1))
            }
        }
    }

    /** Sürüm 4 `adim_sayac` şeması. */
    private fun adimSayacSatiri(tarih: String, toplam: Int): String =
        "INSERT INTO adim_sayac (tarih, toplam, sonSayac) VALUES ('$tarih', $toplam, $toplam)"

    /**
     * [olayVarMi] yalnızca sürüm 3 ve üstünde verilebiliyor — kolon 2 → 3
     * geçişinde eklendi ve `NOT NULL`. Sürüm 2 satırı yazan testler `null`
     * bırakıyor, kolon INSERT listesine hiç girmiyor.
     */
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

    /** Sürüm 5 `risk_sonuc` şeması — `surukleyiciler` boş listeyle (`""`). */
    private fun riskSonucSatiri(tarih: String, bant: String): String =
        "INSERT INTO risk_sonuc (tarih, S, rB, rS, alpha, R, adayBant, gosterilenBant, " +
            "guvenlikKurali, trend, surukleyiciler) VALUES " +
            "('$tarih', 0,0, NULL, 0,0, '$bant', '$bant', NULL, 'sabit', '')"

    private companion object {
        const val TEST_DB = "gecis_testi.db"
    }
}
