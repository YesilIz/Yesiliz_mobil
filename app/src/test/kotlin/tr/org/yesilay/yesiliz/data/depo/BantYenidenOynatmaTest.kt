package tr.org.yesilay.yesiliz.data.depo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.BantTakipci
import tr.org.yesilay.yesiliz.risk.Sabitler

/**
 * `RiskDeposu.takipciyiKur`'un dayandığı varsayımın testi.
 *
 * Histerezis sayacı bellekte yaşıyor ve `BantTakipci`'nin geri yükleme API'si
 * yok. Kalıcılık, `risk_sonuc`'taki geçmiş **aday** bantları taze bir
 * takipçiye yeniden oynatarak sağlanıyor. Bu testler o çözümün üç kırılma
 * noktasını kapatıyor:
 *
 *  1. yeniden oynatma, kesintisiz çalışmayla **aynı** sonucu vermeli
 *  2. sıra ters olursa sonuç **değişmeli** (yoksa test hiçbir şey kanıtlamaz)
 *  3. §5.4 tuzağı yeniden oynatmada da korunmalı
 */
class BantYenidenOynatmaTest {

    /** `RiskDeposu.takipciyiKur` ile aynı iş: aday bantları sırayla ver. */
    private fun oynat(adaylar: List<Bant>): BantTakipci =
        BantTakipci().also { takipci -> adaylar.forEach { takipci.guncelle(it) } }

    @Test
    fun `yeniden oynatma kesintisiz calismayla ayni sonucu verir`() {
        // Kesintisiz: tek takipçi, 10 gün boyunca güncelleniyor
        // AZ yerleşir → 3 gün COK (GUN_YUKARI) → COK'a geçer
        // → 7 gün AZ (GUN_ASAGI) → AZ'a geri döner
        val adaylar = listOf(Bant.AZ) +
            List(Sabitler.GUN_YUKARI) { Bant.COK } +
            List(Sabitler.GUN_ASAGI) { Bant.AZ }
        val kesintisiz = oynat(adaylar)

        // Yeniden oynatma: her gün için takipçi SIFIRDAN kurulup o güne kadarki
        // geçmiş yeniden oynatılıyor — `RiskDeposu.gunuHesapla`'nın yaptığı bu.
        // Her adımda kesintisiz çalışmayla aynı bandı vermeli.
        val adimAdim = BantTakipci()
        adaylar.forEachIndexed { i, aday ->
            val beklenen = adimAdim.guncelle(aday)

            val taze = oynat(adaylar.take(i))
            val bulunan = taze.guncelle(aday)

            assertEquals(
                "gün ${i + 1}: yeniden oynatma kesintisizle aynı bandı vermeli",
                beklenen,
                bulunan,
            )
        }
        assertEquals("dizi AZ'a dönmüş olmalı", Bant.AZ, kesintisiz.mevcutBant)
    }

    @Test
    fun `sira tersse sonuc degisir`() {
        // Bu test ordering hatasının YAKALANABİLİR olduğunu kanıtlıyor:
        // DAO'lar `ORDER BY tarih DESC` döndürüyor, depo ters çevirmeyi
        // unutursa sonuç bozulmalı ki hata görünsün.
        //
        // Artan:  AZ yerleşir, 2 gün COK — GUN_YUKARI=3 dolmadı → AZ kalır
        // Azalan: COK yerleşir, 2 gün AZ — GUN_ASAGI=7 dolmadı → COK kalır
        // Histerezis asimetrik olduğu için sıra hatası bandı tersine çeviriyor.
        val artan = listOf(Bant.AZ, Bant.AZ, Bant.COK, Bant.COK)
        val azalan = artan.reversed()

        assertEquals(Bant.AZ, oynat(artan).mevcutBant)
        assertEquals(Bant.COK, oynat(azalan).mevcutBant)
        assertNotEquals(
            "ters sıra farklı bant vermeli — yoksa sıralama testi anlamsız",
            oynat(artan).mevcutBant,
            oynat(azalan).mevcutBant,
        )
    }

    @Test
    fun `TUZAK derinlesen iyilesme yeniden oynatmada da sayaci sifirlamaz`() {
        // CLAUDE.md §9 bilinen tuzağı: sayaç yalnızca YÖN değişince sıfırlanır.
        // ÇOK'tan inerken aday ORTA iken AZ'a derinleşirse sayaç SIFIRLANMAMALI.
        val adaylar = listOf(Bant.COK) +
            List(3) { Bant.ORTA } +
            List(3) { Bant.AZ }

        val takipci = oynat(adaylar)
        assertEquals("7 gün dolmadan geçmemeli", Bant.COK, takipci.mevcutBant)

        takipci.guncelle(Bant.AZ)
        assertEquals("aynı yönde 7. gün doldu, geçmeli", Bant.AZ, takipci.mevcutBant)
    }

    @Test
    fun `bos gecmiste ilk aday dogrudan yerlesir`() {
        // Uygulamanın ilk günü: risk_sonuc boş, oynatılacak geçmiş yok.
        val takipci = oynat(emptyList())
        takipci.guncelle(Bant.ORTA)
        assertEquals(Bant.ORTA, takipci.mevcutBant)
    }
}
