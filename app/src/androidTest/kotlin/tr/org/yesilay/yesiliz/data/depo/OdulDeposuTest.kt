package tr.org.yesilay.yesiliz.data.depo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.ArkadasDto
import tr.org.yesilay.yesiliz.data.uzak.DavetIstek
import tr.org.yesilay.yesiliz.data.uzak.EbeveynOzetDto
import tr.org.yesilay.yesiliz.data.uzak.EslestirIstek
import tr.org.yesilay.yesiliz.data.uzak.EslestirmeKoduIstek
import tr.org.yesilay.yesiliz.data.uzak.GirisIstek
import tr.org.yesilay.yesiliz.data.uzak.GunlukOzetDto
import tr.org.yesilay.yesiliz.data.uzak.JetonlarDto
import tr.org.yesilay.yesiliz.data.uzak.KayitIstek
import tr.org.yesilay.yesiliz.data.uzak.KodYanit
import tr.org.yesilay.yesiliz.data.uzak.LigYanit
import tr.org.yesilay.yesiliz.data.uzak.OdulSecimIstek
import tr.org.yesilay.yesiliz.data.uzak.OdulSecimiDto
import tr.org.yesilay.yesiliz.data.uzak.SenkronIstek
import tr.org.yesilay.yesiliz.data.uzak.SenkronYanit
import tr.org.yesilay.yesiliz.data.uzak.SifreSifirlaIstek
import tr.org.yesilay.yesiliz.data.uzak.YenileIstek
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb

/**
 * `OdulDeposu` — seçim kalıcılığı ve tekrar seçilememe kuralı.
 *
 * Taze [Tercihler] varsayılan olarak `girisYapildiMi = false` döndürüyor
 * (bkz. `Tercihler.oturum`), yani bu testlerde sunucu hiç çağrılmıyor —
 * [sahteApi] her metotta patlıyor, çağrılırsa test bunu yakalar.
 */
@RunWith(AndroidJUnit4::class)
class OdulDeposuTest {

    private lateinit var db: YesilizDb
    private lateinit var depo: OdulDeposu

    @Before
    fun kur() {
        val baglam = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(baglam, YesilizDb::class.java)
            .allowMainThreadQueries()
            .build()
        depo = OdulDeposu(
            dao = db.odulSecimiDao(),
            api = sahteApi,
            tercihler = Tercihler(baglam),
            saat = java.time.Clock.fixed(
                java.time.Instant.parse("2026-08-04T12:00:00Z"),
                java.time.ZoneOffset.UTC,
            ),
        )
    }

    @After
    fun kapat() = db.close()

    @Test
    fun ilkSecimKaydediliyor() = runTest {
        val sonuc = depo.sec("kupon_market", "fidan_bagisi")

        assertTrue(sonuc)
        assertEquals(mapOf("kupon_market" to "fidan_bagisi"), depo.secimlerAkisi.first())
    }

    @Test
    fun ayniKademeIcinIkinciSecimReddediliyor() = runTest {
        depo.sec("kupon_market", "market_kuponu")

        val ikinci = depo.sec("kupon_market", "fidan_bagisi")

        assertFalse(ikinci)
        // İlk seçim korunuyor, ikinci deneme onu ezmiyor.
        assertEquals(mapOf("kupon_market" to "market_kuponu"), depo.secimlerAkisi.first())
    }

    @Test
    fun farkliKademelerAyriSeciliyor() = runTest {
        depo.sec("kupon_market", "fidan_bagisi")
        depo.sec("kupon_spor", "kupon_spor_kitap")

        val secimler = depo.secimlerAkisi.first()

        assertEquals(2, secimler.size)
        assertEquals("fidan_bagisi", secimler["kupon_market"])
        assertEquals("kupon_spor_kitap", secimler["kupon_spor"])
    }

    private companion object {
        /** Hiçbir metodu çağrılmamalı — misafir/girişsiz varsayılanda ağa hiç çıkılmıyor. */
        val sahteApi = object : YesilizApi {
            private fun patla(): Nothing = error("Ağa çıkılmamalıydı")

            override suspend fun saglik() = patla()
            override suspend fun kayit(istek: KayitIstek): JetonlarDto = patla()
            override suspend fun giris(istek: GirisIstek): JetonlarDto = patla()
            override suspend fun yenile(istek: YenileIstek): JetonlarDto = patla()
            override suspend fun sifreSifirla(istek: SifreSifirlaIstek) = patla()
            override suspend fun hesabiSil() = patla()
            override suspend fun ozetGonder(istek: SenkronIstek): SenkronYanit = patla()
            override suspend fun ozetleriGetir(bas: String, bit: String): List<GunlukOzetDto> = patla()
            override suspend fun heartbeat() = patla()
            override suspend fun lig(): LigYanit = patla()
            override suspend fun ligeKatil(): LigYanit = patla()
            override suspend fun arkadaslar(): List<ArkadasDto> = patla()
            override suspend fun davetKodu(): KodYanit = patla()
            override suspend fun davetGonder(istek: DavetIstek): List<ArkadasDto> = patla()
            override suspend fun odulSec(istek: OdulSecimIstek): OdulSecimiDto = patla()
            override suspend fun odulSecimlerim(): List<OdulSecimiDto> = patla()
            override suspend fun eslestirmeKodu(istek: EslestirmeKoduIstek): KodYanit = patla()
            override suspend fun eslestir(istek: EslestirIstek) = patla()
            override suspend fun eslesmeyiKaldir() = patla()
            override suspend fun ebeveynOzeti(): EbeveynOzetDto = patla()
        }
    }
}
