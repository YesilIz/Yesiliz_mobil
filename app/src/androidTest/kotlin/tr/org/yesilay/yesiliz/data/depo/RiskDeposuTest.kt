package tr.org.yesilay.yesiliz.data.depo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb
import tr.org.yesilay.yesiliz.data.yerel.entity.AnketYanitEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.RiskHesapla
import tr.org.yesilay.yesiliz.risk.Sabitler
import java.time.LocalDate

/**
 * `RiskDeposu` — zincirin en kritik halkası.
 *
 * Bu sınıf yazılmadan önce `risk_sonuc` tablosuna **hiç yazılmıyordu**:
 * `KullanimDeposu` skoru hesaplayıp atıyordu, bant/histerezis/trend
 * production'da hiç çalışmıyordu. Testler o boşluğun kapandığını doğruluyor.
 */
@RunWith(AndroidJUnit4::class)
class RiskDeposuTest {

    private lateinit var db: YesilizDb
    private lateinit var depo: RiskDeposu

    private val bugun: LocalDate = LocalDate.of(2026, 7, 29)

    @Before
    fun kur() {
        val baglam = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(baglam, YesilizDb::class.java)
            .allowMainThreadQueries()
            .build()
        depo = RiskDeposu(
            ozetDao = db.gunlukOzetDao(),
            riskDao = db.riskSonucDao(),
            anketDao = db.anketYanitDao(),
            tercihler = Tercihler(baglam),
        )
    }

    @After
    fun kapat() = db.close()

    /** Ağır tüketim → yüksek skor. Kategori ağırlıkları `Sabitler`'den. */
    private fun agirGun(tarih: LocalDate, sosyalDk: Double = 400.0) = GunlukOzetEntity(
        tarih = tarih,
        socialDk = sosyalDk,
        gece = 0.3,
        geceKullanimDk = sosyalDk * 0.3,
    )

    private fun hafifGun(tarih: LocalDate) = GunlukOzetEntity(
        tarih = tarih,
        socialDk = 20.0,
        productivityDk = 30.0,
    )

    @Test
    fun ozetYoksaHesaplamaz() = runTest {
        assertNull("özet olmadan risk satırı yazılmamalı", depo.gunuHesapla(bugun))
    }

    @Test
    fun riskSonucSatiriYazilir() = runTest {
        db.gunlukOzetDao().kaydet(agirGun(bugun))

        val sonuc = depo.gunuHesapla(bugun)

        assertNotNull("risk_sonuc satırı oluşmalı", sonuc)
        assertEquals(bugun, db.riskSonucDao().getir(bugun)?.tarih)
        assertTrue("ham skor pozitif olmalı", sonuc!!.S > 0.0)
        assertTrue("davranış skoru 0-100 arasında", sonuc.rB in 0.0..100.0)
    }

    @Test
    fun surukleyicilerHepDolu() = runTest {
        // §4.6 md.5: her hesap sürükleyici döndürmeli — "neden bu skor?"
        db.gunlukOzetDao().kaydet(agirGun(bugun))
        val sonuc = depo.gunuHesapla(bugun)!!

        assertTrue("sürükleyiciler boş olmamalı", sonuc.surukleyiciler.isNotEmpty())
        assertEquals("en çok katkı sosyal medyadan", "social", sonuc.surukleyiciler.first())
    }

    @Test
    fun anketYokkenAlphaSifirVeRiskDavranistanGelir() = runTest {
        db.gunlukOzetDao().kaydet(agirGun(bugun))
        val sonuc = depo.gunuHesapla(bugun)!!

        assertEquals("anket yoksa alpha 0", 0.0, sonuc.alpha, 1e-9)
        assertNull("anket skoru null olmalı", sonuc.rS)
        assertEquals("R tamamen davranıştan", sonuc.rB, sonuc.R, 1e-9)
    }

    @Test
    fun anketVarkenAlphaDevreyeGirer() = runTest {
        // Aynı gün, aynı davranış — tek fark anketin varlığı.
        db.gunlukOzetDao().kaydet(agirGun(bugun))
        val anketsiz = depo.gunuHesapla(bugun)!!

        val hamPuan = 55.0 // kesme puanı 29,5'in üstünde
        db.anketYanitDao().ekle(
            AnketYanitEntity(
                tarih = bugun,
                madde1 = 6, madde2 = 6, madde3 = 5, madde4 = 6, madde5 = 6,
                madde6 = 6, madde7 = 5, madde8 = 5, madde9 = 5, madde10 = 5,
                hamPuan = hamPuan,
                rS = RiskHesapla.anketSkoru(hamPuan),
            ),
        )
        val anketli = depo.gunuHesapla(bugun)!!

        assertNotNull("anket skoru dolmalı", anketli.rS)
        assertTrue("alpha sıfırdan büyük olmalı", anketli.alpha > 0.0)
        assertTrue(
            "R artık yalnızca davranıştan gelmemeli",
            kotlin.math.abs(anketli.R - anketli.rB) > 1e-6,
        )
        assertTrue("anket eklenince R değişmeli", anketli.R != anketsiz.R)
    }

    @Test
    fun gelecektekiAnketGecmisGunuEtkilemez() = runTest {
        // Geçmiş bir günü yeniden işlerken o gün henüz var olmayan anket
        // kullanılmamalı — yoksa geçmiş çarpıtılır.
        val dun = bugun.minusDays(1)
        db.gunlukOzetDao().kaydet(agirGun(dun))
        db.anketYanitDao().ekle(
            AnketYanitEntity(
                tarih = bugun,
                madde1 = 6, madde2 = 6, madde3 = 6, madde4 = 6, madde5 = 6,
                madde6 = 6, madde7 = 6, madde8 = 6, madde9 = 6, madde10 = 6,
                hamPuan = 60.0,
                rS = RiskHesapla.anketSkoru(60.0),
            ),
        )

        val sonuc = depo.gunuHesapla(dun)!!
        assertNull("dünün hesabı yarınki anketi görmemeli", sonuc.rS)
    }

    @Test
    fun histerezisGunlerArasiSurer() = runTest {
        // ASIL İDDİA: `BantTakipci` bellekte yaşıyor ama geçmiş yeniden
        // oynatıldığı için sayaç günler arası korunuyor.
        // Önce AZ bandına yerleş, sonra ağır günlerle ÇOK adayına geç.
        val baslangic = bugun.minusDays(9)

        // 2 hafif gün → AZ yerleşir
        repeat(2) { i ->
            val gun = baslangic.plusDays(i.toLong())
            db.gunlukOzetDao().kaydet(hafifGun(gun))
            depo.gunuHesapla(gun)
        }
        assertEquals(
            Bant.AZ.name,
            db.riskSonucDao().getir(baslangic.plusDays(1))?.gosterilenBant,
        )

        // Ağır günler: aday ÇOK'a çıkar ama GUN_YUKARI=3 dolana kadar geçmez
        val agirBas = baslangic.plusDays(2)
        repeat(Sabitler.GUN_YUKARI) { i ->
            val gun = agirBas.plusDays(i.toLong())
            db.gunlukOzetDao().kaydet(agirGun(gun, sosyalDk = 900.0))
            depo.gunuHesapla(gun)
        }

        val ilkAgir = db.riskSonucDao().getir(agirBas)!!
        assertEquals("aday hemen yükselmeli", Bant.COK.name, ilkAgir.adayBant)
        assertEquals(
            "gösterilen bant ilk gün DEĞİŞMEMELİ — histerezis",
            Bant.AZ.name,
            ilkAgir.gosterilenBant,
        )

        val ucuncuAgir = db.riskSonucDao().getir(agirBas.plusDays(2))!!
        assertEquals(
            "3. ardışık günde geçmeli",
            Bant.COK.name,
            ucuncuAgir.gosterilenBant,
        )
    }

    @Test
    fun gunYenidenIslenirseGecmisIkiKezSayilmaz() = runTest {
        // Worker aynı günü birden çok kez koşabilir; sonuç aynı kalmalı.
        db.gunlukOzetDao().kaydet(agirGun(bugun))

        val ilk = depo.gunuHesapla(bugun)!!
        val ikinci = depo.gunuHesapla(bugun)!!

        assertEquals(ilk.R, ikinci.R, 1e-9)
        assertEquals(ilk.gosterilenBant, ikinci.gosterilenBant)
        assertEquals(ilk.adayBant, ikinci.adayBant)
    }

    /** Ham puanı verilen bir SAS-SV satırı — maddeler eşit dağıtılıyor. */
    private fun anket(tarih: LocalDate, hamPuan: Double): AnketYanitEntity {
        val madde = (hamPuan / 10).toInt()
        return AnketYanitEntity(
            tarih = tarih,
            madde1 = madde, madde2 = madde, madde3 = madde, madde4 = madde,
            madde5 = madde, madde6 = madde, madde7 = madde, madde8 = madde,
            madde9 = madde, madde10 = madde,
            hamPuan = hamPuan,
            rS = RiskHesapla.anketSkoru(hamPuan),
        )
    }

    /**
     * `tarih` gün çözünürlüğünde; aynı gün iki anket varsa `sonuncu()`
     * **yeni** olanı vermeli.
     *
     * Eşitlik bozucusu (`id DESC`) olmadan SQLite `rowid` sırasını, yani
     * eskisini veriyordu — kullanıcı anketi yenilediğinde risk motoru eski
     * puanı okuyor ve skor hiç değişmiyordu.
     */
    @Test
    fun TUZAK_ayniGunIkinciAnketSonuncuSayilir() = runTest {
        db.anketYanitDao().ekle(anket(bugun, hamPuan = 10.0))
        db.anketYanitDao().ekle(anket(bugun, hamPuan = 60.0))

        val sonuncu = db.anketYanitDao().sonuncu()!!

        assertEquals("aynı gün doldurulan İKİNCİ anket geçerli olmalı", 60.0, sonuncu.hamPuan, 1e-9)
        assertEquals(
            "geçmiş listesi de kronolojik olmalı",
            listOf(10.0, 60.0),
            db.anketYanitDao().hepsi().map { it.hamPuan },
        )
    }

    /**
     * Kullanıcının bildirdiği kusur: "anketi tekrar doldurunca risk seviyesi
     * değişmiyor." Aynı gün, aynı davranış, yalnızca anket puanı farklı.
     */
    @Test
    fun ayniGunAnketYenilenirseRiskDegisir() = runTest {
        db.gunlukOzetDao().kaydet(agirGun(bugun))

        db.anketYanitDao().ekle(anket(bugun, hamPuan = 10.0))
        val dusukAnketle = depo.gunuHesapla(bugun)!!

        db.anketYanitDao().ekle(anket(bugun, hamPuan = 60.0))
        val yuksekAnketle = depo.gunuHesapla(bugun)!!

        assertTrue(
            "anket skoru yükselmeli: ${dusukAnketle.rS} → ${yuksekAnketle.rS}",
            yuksekAnketle.rS!! > dusukAnketle.rS!!,
        )
        assertTrue(
            "bileşik skor da yükselmeli: ${dusukAnketle.R} → ${yuksekAnketle.R}",
            yuksekAnketle.R > dusukAnketle.R,
        )
    }
}
