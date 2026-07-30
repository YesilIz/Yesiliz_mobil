package tr.org.yesilay.yesiliz.ui.bilesen.veri

import org.junit.Assert.assertEquals
import org.junit.Test

class CubukSecimTest {

    @Test
    fun `yuvanin ortasina dokunmak o cubugu secer`() {
        // 7 çubuk, 700px → her yuva 100px. 250px = 3. yuva (indeks 2).
        assertEquals(2, CubukSecim.indeks(x = 250f, toplamGenislik = 700f, cubukSayisi = 7))
    }

    @Test
    fun `ilk ve son cubuk kenarlarda secilebiliyor`() {
        assertEquals(0, CubukSecim.indeks(x = 0f, toplamGenislik = 700f, cubukSayisi = 7))
        assertEquals(6, CubukSecim.indeks(x = 699f, toplamGenislik = 700f, cubukSayisi = 7))
    }

    @Test
    fun `tam sag kenar son cubuga kirpiliyor`() {
        // x == genişlik: taşma yerine son çubuk. Kırpma olmasaydı indeks 7
        // dönerdi ve çağıran listeyi taşırdı.
        assertEquals(6, CubukSecim.indeks(x = 700f, toplamGenislik = 700f, cubukSayisi = 7))
    }

    @Test
    fun `alan disi dokunma secim yapmiyor`() {
        assertEquals(-1, CubukSecim.indeks(x = -5f, toplamGenislik = 700f, cubukSayisi = 7))
        assertEquals(-1, CubukSecim.indeks(x = 750f, toplamGenislik = 700f, cubukSayisi = 7))
    }

    @Test
    fun `bos grafik secim yapmiyor`() {
        assertEquals(-1, CubukSecim.indeks(x = 10f, toplamGenislik = 700f, cubukSayisi = 0))
        assertEquals(-1, CubukSecim.indeks(x = 10f, toplamGenislik = 0f, cubukSayisi = 7))
    }

    @Test
    fun `cubuklar arasindaki bosluga dokunmak da secim yapiyor`() {
        // Çubuk yuvanın %46'sı; yuva 100px ise çubuk 46px, 27–73 arasında.
        // 5px (çubuğun solundaki boşluk) yine de ilk çubuğu seçmeli.
        assertEquals(0, CubukSecim.indeks(x = 5f, toplamGenislik = 700f, cubukSayisi = 7))
        assertEquals(0, CubukSecim.indeks(x = 95f, toplamGenislik = 700f, cubukSayisi = 7))
    }

    @Test
    fun `ay araliginda 31 cubuk da dogru bolunuyor`() {
        val genislik = 620f
        assertEquals(0, CubukSecim.indeks(x = 5f, toplamGenislik = genislik, cubukSayisi = 31))
        assertEquals(15, CubukSecim.indeks(x = 310f, toplamGenislik = genislik, cubukSayisi = 31))
        assertEquals(30, CubukSecim.indeks(x = 615f, toplamGenislik = genislik, cubukSayisi = 31))
    }
}
