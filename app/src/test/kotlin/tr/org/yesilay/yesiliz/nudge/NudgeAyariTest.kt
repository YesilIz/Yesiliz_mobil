package tr.org.yesilay.yesiliz.nudge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

/**
 * Kullanıcının seçtiği nudge eşiği ve ondan türeyen süreler.
 *
 * Asıl korunan şey **sınır**: ekrandaki `−`/`+` ve çipler zaten aralık içinde
 * kalıyor, ama DataStore'daki sayı yedekten, eski bir sürümden ya da elle
 * gelebilir; okuyan taraf her durumda kırpmak zorunda.
 */
class NudgeAyariTest {

    @Test
    fun `aralik disindaki secim sinira cekilir`() {
        assertEquals(NudgeAyari.EN_COK_DK, NudgeAyari.kirp(720))
        assertEquals(NudgeAyari.EN_AZ_DK, NudgeAyari.kirp(1))
        assertEquals(NudgeAyari.EN_AZ_DK, NudgeAyari.kirp(-30))
    }

    @Test
    fun `aralik icindeki secim degismeden gecer`() {
        assertEquals(45, NudgeAyari.kirp(45))
        assertEquals(NudgeAyari.EN_AZ_DK, NudgeAyari.kirp(NudgeAyari.EN_AZ_DK))
        assertEquals(NudgeAyari.EN_COK_DK, NudgeAyari.kirp(NudgeAyari.EN_COK_DK))
    }

    @Test
    fun `12 saatlik esik saklanamaz`() {
        // "Hatırlatıcı hiç çıkmasın" isteyen kullanıcı onu kapatır; eşiği
        // gizlice sonsuza itmek, açık sandığı bir özellikle kalması olurdu.
        val ayar = NudgeAyari.dakikadan(12 * 60)
        assertEquals(Duration.ofMinutes(NudgeAyari.EN_COK_DK.toLong()), ayar.esik)
    }

    @Test
    fun `secim yoksa varsayilana dusulur`() {
        assertEquals(NudgeAyari.VARSAYILAN, NudgeAyari.dakikadan(null))
        assertEquals(NudgeSabitleri.ESIK, NudgeAyari.dakikadan(null).esik)
    }

    @Test
    fun `secilen dakika esige donusur`() {
        assertEquals(Duration.ofMinutes(30), NudgeAyari.dakikadan(30).esik)
    }

    @Test
    fun `poll araligi esigin yarisini ve 20 saniyeyi asmaz`() {
        val ayar = NudgeAyari(Duration.ofMinutes(45))
        assertEquals(Duration.ofSeconds(20), ayar.pollAraligi)

        // Bölme saniyeye yuvarlanmıyor: 15 sn / 2 = 7,5 sn.
        val kisa = NudgeAyari(Duration.ofSeconds(15))
        assertEquals(Duration.ofMillis(7_500), kisa.pollAraligi)
        assertTrue(kisa.pollAraligi <= kisa.esik.dividedBy(2))
    }

    @Test
    fun `poll araligi 3 saniyenin altina inmez`() {
        // Aksi hâlde çok kısa bir eşikte servis saniyede birkaç kez
        // UsageStats sorgular; pil bütçesi bunu kaldırmaz.
        val ayar = NudgeAyari(Duration.ofSeconds(2))
        assertEquals(Duration.ofSeconds(3), ayar.pollAraligi)
    }

    @Test
    fun `sonum sureleri esigi asmaz`() {
        val kisa = NudgeAyari(Duration.ofMinutes(15))
        assertEquals(Duration.ofMinutes(5), kisa.sonumMola)
        // 10 dakikalık devam sönümü 15 dakikalık eşiğin altında kalıyor.
        assertEquals(Duration.ofMinutes(10), kisa.sonumDevam)

        val cokKisa = NudgeAyari(Duration.ofMinutes(3))
        assertEquals(cokKisa.esik, cokKisa.sonumMola)
        assertEquals(cokKisa.esik, cokKisa.sonumDevam)
    }

    @Test
    fun `gercek esikte devam sonumu moladan uzun`() {
        val ayar = NudgeAyari(Duration.ofMinutes(45))
        assertTrue(ayar.sonumDevam > ayar.sonumMola)
    }

    @Test
    fun `hizli secim degerleri araligin icinde ve adimin kati`() {
        NudgeAyari.HIZLI_SECIM.forEach { dakika ->
            assertEquals(dakika, NudgeAyari.kirp(dakika))
            assertEquals(0, dakika % NudgeAyari.ADIM_DK)
        }
    }
}
