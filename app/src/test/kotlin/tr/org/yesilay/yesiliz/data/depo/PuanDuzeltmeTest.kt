package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Puan düzeltmesinin cihazdaki saf kuralları (`CLAUDE.md §12 K56`).
 *
 * İki garanti korunuyor:
 *
 * 1. **Bakiye negatife düşmüyor.** "-40 puanın var" ekranı, K56'nın
 *    kaçınmak istediği ceza duygusunun ta kendisi.
 * 2. **Kazanılmış geri gitmiyor.** Ağaç evresi (K2) ve ödül kademesi
 *    (K28) yüksek-su-seviyesinden türüyor; bir düşürme bakiyeyi
 *    değiştiriyor ama ağacı kurutmuyor.
 *
 * Hesabın kendisi tek satır ama yanlış tarafa düşmesi kullanıcıya en
 * pahalıya patlayan yerde — bu yüzden ayrı sınanıyor.
 */
class PuanDuzeltmeTest {

    /** `PuanDeposu.toplamPuan`'daki birleştirme. */
    private fun bakiye(kazanc: Int, delta: Int): Int = (kazanc + delta).coerceAtLeast(0)

    /** `PuanDeposu.kazanilmisPuan`'daki birleştirme. */
    private fun kazanilmis(guncel: Int, enYuksek: Int): Int = maxOf(guncel, enYuksek)

    @Test
    fun `pozitif duzeltme bakiyeye ekleniyor`() {
        assertEquals(450, bakiye(kazanc = 300, delta = 150))
    }

    @Test
    fun `negatif duzeltme bakiyeden dusuyor`() {
        assertEquals(180, bakiye(kazanc = 300, delta = -120))
    }

    @Test
    fun `bakiye negatife dusmuyor`() {
        assertEquals(0, bakiye(kazanc = 50, delta = -500))
    }

    @Test
    fun `duzeltme yoksa bakiye kazancin kendisi`() {
        assertEquals(300, bakiye(kazanc = 300, delta = 0))
    }

    @Test
    fun `dusurme kazanilmisi geri almiyor`() {
        // Kullanıcı 5.000'e ulaşmıştı; 1.000 düşürüldü. Ağaç evresi ve
        // ödül kademesi hâlâ 5.000'i okuyor (U6).
        val guncel = bakiye(kazanc = 5_000, delta = -1_000)
        assertEquals(4_000, guncel)
        assertEquals(5_000, kazanilmis(guncel, enYuksek = 5_000))
    }

    @Test
    fun `ekleme kazanilmisi ilerletiyor`() {
        val guncel = bakiye(kazanc = 5_000, delta = 500)
        assertEquals(5_500, kazanilmis(guncel, enYuksek = 5_000))
    }

    @Test
    fun `yuksek su seviyesi hic yazilmamissa guncel gecerli`() {
        assertEquals(300, kazanilmis(guncel = 300, enYuksek = 0))
    }

    @Test
    fun `bakiye sifira dusse de kazanilmis duruyor`() = runTest {
        val guncel = bakiye(kazanc = 100, delta = -5_000)
        assertEquals(0, guncel)
        assertEquals(10_000, kazanilmis(guncel, enYuksek = 10_000))
    }
}
