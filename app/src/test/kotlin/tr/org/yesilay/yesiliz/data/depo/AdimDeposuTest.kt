package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import tr.org.yesilay.yesiliz.data.saglik.AdimKaynagi
import tr.org.yesilay.yesiliz.data.yerel.dao.AdimSayacDao
import tr.org.yesilay.yesiliz.data.yerel.entity.AdimSayacEntity
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * `AdimDeposu.ornekle()` — `gorev2.md` §3'ün `AdimBirikimTest`'te
 * (saf `adimBirikimGuncelle` fonksiyonu) test edilemeyen, ÇAĞRILAR ARASI
 * senaryoları: gün/saat dilimi değişimi, sensör erişiminin kesilip
 * geri gelmesi. `AdimSayacOkuyucu` somut ve Android'e bağlı olduğu için
 * `AdimKaynagi` arayüzü çıkarıldı — mocking kütüphanesi yok, sahteler
 * elle yazılıyor (projede yerleşik desen, bkz. core-risk saf fonksiyon
 * testleri).
 */
class AdimDeposuTest {

    private class SahteDao : AdimSayacDao {
        val satirlar = mutableMapOf<LocalDate, AdimSayacEntity>()

        override suspend fun kaydet(kayit: AdimSayacEntity) {
            satirlar[kayit.tarih] = kayit
        }

        override suspend fun gunun(tarih: LocalDate): AdimSayacEntity? = satirlar[tarih]

        override suspend fun eskileriSil(tarih: LocalDate): Int {
            val silinecek = satirlar.keys.filter { it < tarih }
            silinecek.forEach { satirlar.remove(it) }
            return silinecek.size
        }
    }

    private class SahteKaynak(
        var kullanilabilirMi: Boolean = true,
        var sonrakiSayac: Long? = null,
    ) : AdimKaynagi {
        override fun kullanilabilir(): Boolean = kullanilabilirMi
        override suspend fun anlikSayac(): Long? = sonrakiSayac
    }

    /** Testler arası `an`/`bolge`'yi değiştirip aynı `Clock`'u yeniden kullanabilsin. */
    private class DegisebilirSaat(var an: Instant, var bolge: ZoneId) : Clock() {
        override fun getZone(): ZoneId = bolge
        override fun withZone(bolge: ZoneId): Clock = DegisebilirSaat(an, bolge)
        override fun instant(): Instant = an
    }

    @Test
    fun `gece yarisinda gun degisince yeni gun sifirdan baslar`() = runBlocking {
        val dao = SahteDao()
        val kaynak = SahteKaynak(sonrakiSayac = 1_000L)
        val saat = DegisebilirSaat(
            Instant.parse("2026-08-06T23:00:00Z"),
            ZoneOffset.UTC,
        )
        val depo = AdimDeposu(dao, kaynak, saat)

        depo.ornekle()
        assertEquals(0L, dao.gunun(LocalDate.of(2026, 8, 6))!!.toplam)

        // Gece yarısını geçildi — aynı sayaç artmaya devam ediyor.
        saat.an = Instant.parse("2026-08-07T00:30:00Z")
        kaynak.sonrakiSayac = 1_050L
        depo.ornekle()

        // Yeni gün SIFIRDAN başlar (taban bu örnekte kuruluyor), önceki
        // günün 6 Ağustos satırı bozulmuyor.
        assertEquals(
            "yeni gün taban kurmalı, önceki güne fark eklenmemeli",
            0L,
            dao.gunun(LocalDate.of(2026, 8, 7))!!.toplam,
        )
        assertEquals(
            "önceki günün satırı değişmemeli",
            0L,
            dao.gunun(LocalDate.of(2026, 8, 6))!!.toplam,
        )
    }

    @Test
    fun `saat dilimi degisince gun siniri yeni bolgeye gore hesaplanir`() = runBlocking {
        val dao = SahteDao()
        val kaynak = SahteKaynak(sonrakiSayac = 500L)
        // 23:30 UTC = 6 Ağustos UTC'de, ama UTC+2'de zaten 7 Ağustos.
        val an = Instant.parse("2026-08-06T23:30:00Z")
        val saat = DegisebilirSaat(an, ZoneOffset.UTC)
        val depo = AdimDeposu(dao, kaynak, saat)

        depo.ornekle()
        assertEquals(
            "UTC'de gün hâlâ 6 Ağustos",
            LocalDate.of(2026, 8, 6),
            dao.satirlar.keys.single(),
        )

        // Aynı an, saat dilimi değişti (kullanıcı seyahat etti / cihaz TZ'i değişti).
        saat.bolge = ZoneOffset.ofHours(2)
        kaynak.sonrakiSayac = 520L
        depo.ornekle()

        assertEquals(
            "yeni bölgede aynı an 7 Ağustos'a düşmeli",
            setOf(LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 7)),
            dao.satirlar.keys,
        )
    }

    @Test
    fun `sensor erisimi kesilince ornekleme sessizce hicbir sey yazmaz`() = runBlocking {
        val dao = SahteDao()
        val kaynak = SahteKaynak(sonrakiSayac = 200L)
        val saat = DegisebilirSaat(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC)
        val depo = AdimDeposu(dao, kaynak, saat)

        depo.ornekle()
        val ilkKayit = dao.gunun(LocalDate.of(2026, 8, 6))
        assertEquals(200L, ilkKayit!!.sonSayac)

        // İzin geri alındı / sensöre erişilemiyor — `anlikSayac()` null dönüyor.
        kaynak.sonrakiSayac = null
        depo.ornekle()

        assertEquals(
            "erişim kesilince var olan kayıt korunmalı, üzerine yazılmamalı",
            ilkKayit,
            dao.gunun(LocalDate.of(2026, 8, 6)),
        )
    }

    @Test
    fun `izin yeniden verilince kaldigi yerden devam eder`() = runBlocking {
        val dao = SahteDao()
        val kaynak = SahteKaynak(sonrakiSayac = 200L)
        val saat = DegisebilirSaat(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC)
        val depo = AdimDeposu(dao, kaynak, saat)

        depo.ornekle() // taban: 200
        kaynak.sonrakiSayac = null
        depo.ornekle() // izin kesildi, hiçbir şey değişmedi

        // İzin geri verildi, sensör bu arada 260'a ilerlemiş.
        kaynak.sonrakiSayac = 260L
        depo.ornekle()

        assertEquals(
            "kesinti sırasında kaybolan adım yok, kaldığı yerden fark eklenmeli",
            60L,
            dao.gunun(LocalDate.of(2026, 8, 6))!!.toplam,
        )
    }

    @Test
    fun `ayni sayacla tekrar orneklemek toplami degistirmez`() = runBlocking {
        val dao = SahteDao()
        val kaynak = SahteKaynak(sonrakiSayac = 300L)
        val saat = DegisebilirSaat(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC)
        val depo = AdimDeposu(dao, kaynak, saat)

        depo.ornekle()
        val birinci = dao.gunun(LocalDate.of(2026, 8, 6))!!.toplam

        // `GunlukOzetWorker` ve `YuruyusServisi` aynı dakikada iki kez
        // örnekleyebilir; sensör henüz ilerlemediyse toplam ikiye katlanmamalı.
        depo.ornekle()
        val ikinci = dao.gunun(LocalDate.of(2026, 8, 6))!!.toplam

        assertEquals(birinci, ikinci)
    }

    @Test
    fun `birkac gun acilmama sonrasi yeni gun sifirdan basliyor, eski gun kaybolmuyor`() = runBlocking {
        val dao = SahteDao()
        val kaynak = SahteKaynak(sonrakiSayac = 400L)
        val saat = DegisebilirSaat(Instant.parse("2026-08-01T10:00:00Z"), ZoneOffset.UTC)
        val depo = AdimDeposu(dao, kaynak, saat)

        depo.ornekle()
        kaynak.sonrakiSayac = 450L
        depo.ornekle()
        val eskiGun = dao.gunun(LocalDate.of(2026, 8, 1))!!

        // Uygulama 5 gün açılmadı.
        saat.an = Instant.parse("2026-08-06T10:00:00Z")
        kaynak.sonrakiSayac = 9_000L
        depo.ornekle()

        assertEquals(
            "aradaki günler için satır YOK (telafi bu depoyu bağlamıyor)",
            null,
            dao.gunun(LocalDate.of(2026, 8, 3)),
        )
        assertEquals(
            "yeni gün taban kurar, eski sayaçtan sıçrama devralmaz",
            0L,
            dao.gunun(LocalDate.of(2026, 8, 6))!!.toplam,
        )
        assertEquals("1 Ağustos satırı yerinde duruyor", eskiGun, dao.gunun(LocalDate.of(2026, 8, 1)))
    }
}
