package tr.org.yesilay.yesiliz.arkaplan

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class GunlukOzetTamamlamaTest {

    @Test
    fun yalnizcaUcCiktisiDaOlanGunTamamlanmisSayilir() {
        val tam = LocalDate.of(2026, 8, 8)
        val riskEksik = LocalDate.of(2026, 8, 9)
        val puanEksik = LocalDate.of(2026, 8, 10)

        val sonuc = tamamlanmisGunler(
            ozetGunleri = listOf(tam, riskEksik, puanEksik),
            riskGunleri = listOf(tam, puanEksik),
            puanGunleri = listOf(tam, riskEksik),
            kurulum = LocalDate.of(2026, 8, 1),
        )

        assertEquals(setOf(tam), sonuc)
    }

    /**
     * Kurulum öncesi güne risk/puan **bilerek** yazılmıyor (`debug.md` K-4).
     * Üçlü kesişim onları da arayınca hiçbir zaman tamamlanmış sayılmıyor ve
     * telafi penceresi boyunca her gece yeniden işleniyorlardı.
     */
    @Test
    fun kurulumOncesiGunOzetleTamamlanmisSayilir() {
        val kurulum = LocalDate.of(2026, 8, 10)
        val once = LocalDate.of(2026, 8, 7)

        val sonuc = tamamlanmisGunler(
            ozetGunleri = listOf(once),
            riskGunleri = emptyList(),
            puanGunleri = emptyList(),
            kurulum = kurulum,
        )

        assertEquals(setOf(once), sonuc)
    }

    /** Kurulum gününün kendisi "öncesi" değil: risk ve puan orada yazılıyor. */
    @Test
    fun kurulumGunuHalaUcCiktiyiIstiyor() {
        val kurulum = LocalDate.of(2026, 8, 10)

        val eksik = tamamlanmisGunler(
            ozetGunleri = listOf(kurulum),
            riskGunleri = emptyList(),
            puanGunleri = emptyList(),
            kurulum = kurulum,
        )
        assertEquals(emptySet<LocalDate>(), eksik)

        val tam = tamamlanmisGunler(
            ozetGunleri = listOf(kurulum),
            riskGunleri = listOf(kurulum),
            puanGunleri = listOf(kurulum),
            kurulum = kurulum,
        )
        assertEquals(setOf(kurulum), tam)
    }

    /** Özet satırı olmayan kurulum öncesi gün tamamlanmış sayılmaz. */
    @Test
    fun ozetiOlmayanKurulumOncesiGunTamamlanmisDegil() {
        val sonuc = tamamlanmisGunler(
            ozetGunleri = emptyList(),
            riskGunleri = emptyList(),
            puanGunleri = emptyList(),
            kurulum = LocalDate.of(2026, 8, 10),
        )

        assertEquals(emptySet<LocalDate>(), sonuc)
    }

    /** Kurulum tarihi henüz yazılmamışsa davranış üçlü kesişimin aynısı. */
    @Test
    fun kurulumBilinmiyorsaEskiDavranis() {
        val gun = LocalDate.of(2026, 8, 7)

        val sonuc = tamamlanmisGunler(
            ozetGunleri = listOf(gun),
            riskGunleri = emptyList(),
            puanGunleri = emptyList(),
            kurulum = null,
        )

        assertEquals(emptySet<LocalDate>(), sonuc)
    }
}
