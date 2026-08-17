package tr.org.yesilay.yesiliz.ui.ekran.nefes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nefes seansının duraklat/devam mantığı — T4-010.
 *
 * Seans arka planda tamamlanıp puan yazıyordu; kullanıcı ekranı açıp
 * HOME'a basarak 5 puan alabiliyordu. Buradaki değişmez: **ön planda
 * geçmemiş saniye tamamlanmaya sayılmaz.**
 */
class NefesSayacTest {

    private val seans = 120

    /** Seansı [saniye] kadar ilerlet; duraklamış sayaçta hiçbir şey saymaz. */
    private fun NefesSayac.calistir(saniye: Int) = repeat(saniye) { tik() }

    @Test
    fun `on planda tam sure sayilinca seans tamamlanir`() {
        val sayac = NefesSayac()

        sayac.calistir(seans)

        assertEquals(seans, sayac.gecenSaniye)
        assertTrue(sayac.tamamlandiMi(seans))
    }

    @Test
    fun `ortada ayrilan seans tamamlanmaz`() {
        val sayac = NefesSayac()

        sayac.calistir(30)
        sayac.arkaPlanaGitti()
        // Arka planda tam seans süresi kadar bekle: hiçbiri sayılmamalı.
        sayac.calistir(seans)

        assertEquals(30, sayac.gecenSaniye)
        assertFalse("arka planda geçen süre tamamlamaz", sayac.tamamlandiMi(seans))
        assertFalse(sayac.calisiyorMu)
    }

    @Test
    fun `donus kalan sureden devam eder`() {
        val sayac = NefesSayac()

        sayac.calistir(30)
        sayac.arkaPlanaGitti()
        sayac.calistir(60)
        sayac.onPlanaDondu()

        assertTrue(sayac.calisiyorMu)
        assertEquals("sayaç kaldığı yerden devam etmeli", 30, sayac.gecenSaniye)

        sayac.calistir(seans - 30)

        assertEquals(seans, sayac.gecenSaniye)
        assertTrue(sayac.tamamlandiMi(seans))
    }

    @Test
    fun `elle duraklatilan seans on plana donunce kendiliginden baslamaz`() {
        val sayac = NefesSayac()

        sayac.calistir(30)
        sayac.duraklat()
        sayac.arkaPlanaGitti()
        sayac.onPlanaDondu()

        assertFalse("kullanıcının duraklat kararı ezilmemeli", sayac.calisiyorMu)

        sayac.calistir(60)
        assertEquals(30, sayac.gecenSaniye)
    }

    @Test
    fun `elle devam ettirilen seans sayar`() {
        val sayac = NefesSayac()

        sayac.duraklat()
        sayac.calistir(10)
        sayac.devamEt()
        sayac.calistir(10)

        assertEquals(10, sayac.gecenSaniye)
    }

    @Test
    fun `tik duraklamis sayacta false doner`() {
        val sayac = NefesSayac()

        assertTrue(sayac.tik())
        sayac.arkaPlanaGitti()
        assertFalse(sayac.tik())
    }

    @Test
    fun `sifirla sayaci ve elle duraklatmayi temizler`() {
        val sayac = NefesSayac()

        sayac.calistir(40)
        sayac.duraklat()
        sayac.sifirla()

        assertEquals(0, sayac.gecenSaniye)
        assertTrue(sayac.calisiyorMu)
    }

    @Test
    fun `arka planda sifirlanan seans yine de arka planda saymaz`() {
        val sayac = NefesSayac()

        sayac.arkaPlanaGitti()
        sayac.sifirla()
        sayac.calistir(seans)

        assertEquals(0, sayac.gecenSaniye)
        assertFalse(sayac.calisiyorMu)
    }
}
