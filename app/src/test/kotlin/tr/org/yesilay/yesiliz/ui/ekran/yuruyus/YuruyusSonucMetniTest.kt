package tr.org.yesilay.yesiliz.ui.ekran.yuruyus

import org.junit.Assert.assertTrue
import org.junit.Test

class YuruyusSonucMetniTest {

    @Test
    fun `erken bitis puan eklendi iddiasi yapmaz`() {
        val metin = yuruyusSonucMetni(
            YuruyusDurum(tamamlandiMi = true, hedefTamamlandiMi = false, kazanilanPuan = 12),
        )

        assertTrue(metin.contains("puan yazılmadı"))
    }

    @Test
    fun `hedef bitti ama gunluk puan alinmissa tekrar eklendi demez`() {
        val metin = yuruyusSonucMetni(
            YuruyusDurum(
                tamamlandiMi = true,
                hedefTamamlandiMi = true,
                puanVerildiMi = false,
                kazanilanPuan = 45,
            ),
        )

        assertTrue(metin.contains("zaten alınmış"))
    }

    @Test
    fun `yalnizca gercek puan yaziminda eklendi denir`() {
        val metin = yuruyusSonucMetni(
            YuruyusDurum(
                tamamlandiMi = true,
                hedefTamamlandiMi = true,
                puanVerildiMi = true,
                kazanilanPuan = 45,
            ),
        )

        assertTrue(metin.contains("+45 puan"))
    }
}
