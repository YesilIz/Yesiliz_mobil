package tr.org.yesilay.yesiliz.data.demo

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.data.depo.RiskDeposu
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.risk.RiskHesapla
import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import tr.org.yesilay.yesiliz.risk.GunlukGirdi
import java.time.Clock
import java.time.LocalDate
import javax.inject.Singleton
import kotlin.random.Random

/**
 * **YALNIZCA DEBUG.** Bu dosya `src/debug/` altında; release derlemesine
 * hiç girmiyor (karşılığı `src/release/DemoVeriModulu.kt`, no-op).
 */
@Module
@InstallIn(SingletonComponent::class)
object DemoVeriModulu {

    @Provides
    @Singleton
    fun demoVeri(
        ozetDao: GunlukOzetDao,
        riskDeposu: RiskDeposu,
        puanDeposu: PuanDeposu,
        saat: Clock,
    ): DemoVeri = DebugDemoVeri(ozetDao, riskDeposu, puanDeposu, saat)
}

/**
 * Emülatör için gerçekçi örnek veri.
 *
 * **Motoru atlamıyor:** yalnızca `gunluk_ozet` *girdilerini* (kategori
 * dakikaları, örüntüler, adım) yazıyor; skor, bant, histerezis, trend ve puan
 * gerçek `RiskDeposu` / `PuanDeposu` tarafından üretiliyor. Yani ekranda
 * görünen sayı motorun gerçek çıktısı, uydurulmuş bir sonuç değil.
 *
 * Tohum **belirlenimci** ([TOHUM] sabit): aynı ekran görüntüsü her koşuda
 * aynı çıksın, demo sırasında sürpriz olmasın.
 */
private class DebugDemoVeri(
    private val ozetDao: GunlukOzetDao,
    private val riskDeposu: RiskDeposu,
    private val puanDeposu: PuanDeposu,
    private val saat: Clock,
) : DemoVeri {

    override val kullanilabilir = true

    override suspend fun tohumla(gun: Int) {
        val bugun = LocalDate.now(saat)
        val rastgele = Random(TOHUM)

        for (geri in gun - 1 downTo 0) {
            val tarih = bugun.minusDays(geri.toLong())
            val ozet = ornekGun(tarih, rastgele)

            // Ara ürünler gerçek motordan — elle doldurulmuyor.
            val ara = RiskHesapla.araUrunler(ozet.girdi)
            ozetDao.kaydet(
                ozet.entity.copy(
                    bTuketim = ara.bTuketim,
                    bIslevsel = ara.bIslevsel,
                    P = ara.P,
                    A = ara.A,
                    D = ara.D,
                    S = ara.S,
                ),
            )

            riskDeposu.gunuHesapla(tarih)
            puanDeposu.gunuYenidenHesapla(tarih)
        }
    }

    /** Hafif azalan bir eğilim — "iyileşiyor" trendi görünsün. */
    private fun ornekGun(tarih: LocalDate, rastgele: Random): OrnekGun {
        val haftaSonu = tarih.dayOfWeek.value >= CUMARTESI
        val taban = if (haftaSonu) HAFTA_SONU_TABAN else HAFTA_ICI_TABAN
        val gurultu = rastgele.nextDouble(-SAPMA, SAPMA)

        val sosyal = taban * SOSYAL_PAY + gurultu
        val eglence = taban * EGLENCE_PAY + gurultu / 2
        val iletisim = taban * ILETISIM_PAY
        val oyun = taban * OYUN_PAY
        val uretkenlik = taban * URETKENLIK_PAY
        val diger = taban * DIGER_PAY

        val geceOran = rastgele.nextDouble(0.04, 0.18)
        val toplam = sosyal + eglence + iletisim + oyun + uretkenlik + diger

        val entity = GunlukOzetEntity(
            tarih = tarih,
            socialDk = sosyal,
            entertainmentDk = eglence,
            communicationDk = iletisim,
            gamesDk = oyun,
            productivityDk = uretkenlik,
            otherDk = diger,
            gece = geceOran,
            oturum = rastgele.nextDouble(0.3, 0.7),
            acilis = rastgele.nextDouble(0.2, 0.6),
            sabah = rastgele.nextDouble(0.1, 0.5),
            uykuOncesi = rastgele.nextDouble(0.2, 0.6),
            yerDegis = rastgele.nextDouble(0.1, 0.4),
            adim = rastgele.nextDouble(2_500.0, 8_500.0),
            sporDk = if (rastgele.nextInt(3) == 0) rastgele.nextDouble(15.0, 40.0) else 0.0,
            geceKullanimDk = geceOran * toplam,
            acilisSayisi = rastgele.nextInt(40, 120),
        )

        return OrnekGun(
            entity = entity,
            girdi = GunlukGirdi(
                kategoriDakika = linkedMapOf(
                    Kategori.SOCIAL.anahtar to sosyal,
                    Kategori.ENTERTAINMENT.anahtar to eglence,
                    Kategori.GAMES.anahtar to oyun,
                    Kategori.COMMUNICATION.anahtar to iletisim,
                    Kategori.PRODUCTIVITY.anahtar to uretkenlik,
                    Kategori.OTHER.anahtar to diger,
                ),
                oruntu = linkedMapOf(
                    "gece" to entity.gece,
                    "oturum" to entity.oturum,
                    "acilis" to entity.acilis,
                    "sabah" to entity.sabah,
                    "uyku_oncesi" to entity.uykuOncesi,
                    "yer_degis" to entity.yerDegis,
                ),
                adim = entity.adim,
                sporDk = entity.sporDk,
            ),
        )
    }

    private data class OrnekGun(val entity: GunlukOzetEntity, val girdi: GunlukGirdi)

    private companion object {
        const val TOHUM = 20260729L
        const val CUMARTESI = 6

        const val HAFTA_ICI_TABAN = 250.0
        const val HAFTA_SONU_TABAN = 330.0
        const val SAPMA = 45.0

        const val SOSYAL_PAY = 0.34
        const val EGLENCE_PAY = 0.20
        const val ILETISIM_PAY = 0.16
        const val OYUN_PAY = 0.14
        const val URETKENLIK_PAY = 0.09
        const val DIGER_PAY = 0.07
    }
}
