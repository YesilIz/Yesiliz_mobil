package tr.org.yesilay.yesiliz.data.kullanim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

/**
 * Agrega kova süzmesi — istatistik ekranındaki "19 saat"in testi.
 *
 * `queryAndAggregateUsageStats` istenen aralığa kırpma yapmıyor: günlük
 * kovası kalmamış eski günler için haftalık/aylık kovayı döndürüyor ve o
 * kova tek güne yazılınca 19 saatlik günler çıkıyordu.
 */
class AgregaSuzTest {

    private val gunBas = 1_754_000_000_000L
    private val gunBit = gunBas + Duration.ofDays(1).toMillis()

    private fun kova(
        paket: String = "com.ornek.uygulama",
        basOfsetSaat: Long = 0,
        sureSaat: Long = 24,
        onPlanDk: Long = 60,
    ) = AgregaKova(
        paketAdi = paket,
        basMs = gunBas + Duration.ofHours(basOfsetSaat).toMillis(),
        bitMs = gunBas + Duration.ofHours(basOfsetSaat + sureSaat).toMillis(),
        onPlanMs = Duration.ofMinutes(onPlanDk).toMillis(),
    )

    @Test
    fun `gunluk kova kabul ediliyor`() {
        val sonuc = AgregaSuz.gunlukDakikalar(listOf(kova(onPlanDk = 90)), gunBas, gunBit)
        assertEquals(90.0, sonuc?.get("com.ornek.uygulama") ?: 0.0, 1e-6)
    }

    @Test
    fun `haftalik kova reddediliyor`() {
        // Asıl kusur: 7 günlük kovanın toplamı tek güne yazılıyordu.
        val haftalik = kova(sureSaat = 24 * 7, onPlanDk = 19 * 60)
        assertNull(AgregaSuz.gunlukDakikalar(listOf(haftalik), gunBas, gunBit))
    }

    @Test
    fun `aylik kova da reddediliyor`() {
        val aylik = kova(sureSaat = 24 * 30, onPlanDk = 60 * 60)
        assertNull(AgregaSuz.gunlukDakikalar(listOf(aylik), gunBas, gunBit))
    }

    @Test
    fun `komsu gune ait kova bu gune sayilmiyor`() {
        // Orta nokta kuralı: kova tam olarak bir güne ait. Yoksa aynı kova
        // iki komşu günde birden sayılır (çift sayma).
        val dunkuKova = kova(basOfsetSaat = -24, onPlanDk = 120)
        assertNull(AgregaSuz.gunlukDakikalar(listOf(dunkuKova), gunBas, gunBit))
    }

    @Test
    fun `sinirdaki kova en uzun kova esigine kadar kabul ediliyor`() {
        val tamSinir = kova(sureSaat = AgregaSuz.EN_UZUN_KOVA_SAAT, onPlanDk = 30)
        assertEquals(30.0, AgregaSuz.gunlukDakikalar(listOf(tamSinir), gunBas, gunBit)?.values?.sum() ?: 0.0, 1e-6)

        val sinirUstu = kova(sureSaat = AgregaSuz.EN_UZUN_KOVA_SAAT + 1, onPlanDk = 30)
        assertNull(AgregaSuz.gunlukDakikalar(listOf(sinirUstu), gunBas, gunBit))
    }

    @Test
    fun `toplam yirmi dort saati asamaz`() {
        // Savunma derinliği: sistem beklenmedik değer dönerse grafikte
        // absürt bir çubuk çıkmasın.
        val kovalar = listOf(
            kova(paket = "a", onPlanDk = 20 * 60),
            kova(paket = "b", onPlanDk = 20 * 60),
        )
        val toplam = AgregaSuz.gunlukDakikalar(kovalar, gunBas, gunBit)?.values?.sum() ?: 0.0
        assertEquals(AgregaSuz.GUN_DK_TAVANI, toplam, 1e-6)
    }

    @Test
    fun `tek paket bile yirmi dort saatle sinirli`() {
        val kovalar = listOf(kova(onPlanDk = 40 * 60))
        val sonuc = AgregaSuz.gunlukDakikalar(kovalar, gunBas, gunBit)
        assertTrue((sonuc?.values?.sum() ?: 0.0) <= AgregaSuz.GUN_DK_TAVANI)
    }

    @Test
    fun `bos liste veri yok demek`() {
        // Sıfır DEĞİL null: "hiç kullanmadın" ile "ölçemedik" farklı şeyler
        // (CLAUDE.md §9 "satır var ≠ veri var").
        assertNull(AgregaSuz.gunlukDakikalar(emptyList(), gunBas, gunBit))
    }

    @Test
    fun `on planda hic sure yoksa veri yok sayiliyor`() {
        val sonuc = AgregaSuz.gunlukDakikalar(listOf(kova(onPlanDk = 0)), gunBas, gunBit)
        assertNull(sonuc)
    }

    @Test
    fun `ayni paketin iki gunluk kovasi toplaniyor`() {
        val kovalar = listOf(
            kova(basOfsetSaat = 0, sureSaat = 12, onPlanDk = 30),
            kova(basOfsetSaat = 12, sureSaat = 12, onPlanDk = 45),
        )
        val sonuc = AgregaSuz.gunlukDakikalar(kovalar, gunBas, gunBit)
        assertEquals(75.0, sonuc?.get("com.ornek.uygulama") ?: 0.0, 1e-6)
    }

    @Test
    fun `bozuk kova negatif sure vermiyor`() {
        val bozuk = AgregaKova("com.ornek", basMs = gunBit, bitMs = gunBas, onPlanMs = 60_000)
        assertNull(AgregaSuz.gunlukDakikalar(listOf(bozuk), gunBas, gunBit))
    }
}
