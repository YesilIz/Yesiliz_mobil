package tr.org.yesilay.yesiliz.domain.kullanim

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * CLAUDE.md §12 K31 tekilleştirme senaryoları — hepsi listede birebir var.
 */
class AktiviteTekillestirTest {

    private val t0: Instant = Instant.parse("2026-08-06T10:00:00Z")
    private fun dk(dakika: Long): Instant = t0.plusSeconds(dakika * 60)

    private fun uygulama(basDk: Long, bitDk: Long) = AktiviteOturumu(
        baslangic = dk(basDk),
        bitis = dk(bitDk),
        kaynak = AktiviteKaynagi.UYGULAMA_YURUYUSU,
    )

    private fun saglik(basDk: Long, bitDk: Long, disKimlik: String = "hc") = AktiviteOturumu(
        baslangic = dk(basDk),
        bitis = dk(bitDk),
        kaynak = AktiviteKaynagi.HEALTH_CONNECT,
        disKimlik = disKimlik,
    )

    @Test
    fun `bos liste sifir dondurur`() {
        assertEquals(0.0, AktiviteTekillestir.birlestirilmisSureDk(emptyList()), 1e-9)
    }

    @Test
    fun `yalnizca uygulama yuruyusu`() {
        val sonuc = AktiviteTekillestir.birlestirilmisSureDk(listOf(uygulama(0, 20)))
        assertEquals(20.0, sonuc, 1e-9)
    }

    @Test
    fun `yalnizca health connect kaydi`() {
        val sonuc = AktiviteTekillestir.birlestirilmisSureDk(listOf(saglik(0, 25)))
        assertEquals(25.0, sonuc, 1e-9)
    }

    @Test
    fun `iki kaynakta tamamen ayni yuruyus tek sayilir`() {
        val sonuc = AktiviteTekillestir.birlestirilmisSureDk(
            listOf(uygulama(0, 20), saglik(0, 20)),
        )
        // 40 DEĞİL: aynı yürüyüş iki kaynakta da var, toplanmıyor.
        assertEquals(20.0, sonuc, 1e-9)
    }

    @Test
    fun `baslangic saatleri birkac dakika farkli ayni yuruyus birlesir`() {
        // Uygulama 0-20 (20dk), HC 3-22 (19dk) — kesişim 3-20 = 17dk.
        // Kısa kayıt (HC, 19dk) üzerinden oran 17/19 ≈ 0.89 ≥ eşik → birleşir.
        val sonuc = AktiviteTekillestir.birlestirilmisSureDk(
            listOf(uygulama(0, 20), saglik(3, 22)),
        )
        // Birleşim (union): 0-22 = 22dk.
        assertEquals(22.0, sonuc, 1e-9)
    }

    @Test
    fun `kismen cakisan iki kayit esigin altindaysa ayri sayilir`() {
        // Uygulama 0-30 (30dk), HC 20-50 (30dk) — kesişim 20-30 = 10dk.
        // Oran 10/30 ≈ 0.33 < eşik (0.5) → ayrı aktivite, toplanır.
        val sonuc = AktiviteTekillestir.birlestirilmisSureDk(
            listOf(uygulama(0, 30), saglik(20, 50)),
        )
        assertEquals(60.0, sonuc, 1e-9)
    }

    @Test
    fun `cakisma orani tam esikte birlesir`() {
        // Kesişim / kısa süre TAM ESİK (0.5) ise "büyük ölçüde çakışıyor"
        // sayılır (>=), sınırın hemen altında sayılmaz.
        // Uygulama 0-20 (20dk), HC 10-30 (20dk) — kesişim 10-20 = 10dk, oran tam 0.5.
        val tamEsik = AktiviteTekillestir.birlestirilmisSureDk(
            listOf(uygulama(0, 20), saglik(10, 30)),
        )
        assertEquals("tam eşik birleşmeli", 30.0, tamEsik, 1e-9)

        // Bir dakika daha az örtüşme: kesişim 11-20 = 9dk, oran 9/20 = 0.45 < 0.5.
        val esikAltinda = AktiviteTekillestir.birlestirilmisSureDk(
            listOf(uygulama(0, 20), saglik(11, 30)),
        )
        assertEquals("eşiğin altı ayrı sayılmalı", 20.0 + 19.0, esikAltinda, 1e-9)
    }

    @Test
    fun `ayni gun yapilan iki farkli yuruyus toplanir`() {
        val sonuc = AktiviteTekillestir.birlestirilmisSureDk(
            listOf(uygulama(0, 20), uygulama(600, 615)),
        )
        assertEquals(35.0, sonuc, 1e-9)
    }

    @Test
    fun `health connect kaydinin sonradan gelmesi sira bagimsiz`() {
        // Aynı senaryo (başlangıç saatleri birkaç dakika farklı), liste
        // sırası TERS: HC kaydı önce ekleniyor — sonuç değişmemeli.
        val sonuc = AktiviteTekillestir.birlestirilmisSureDk(
            listOf(saglik(3, 22), uygulama(0, 20)),
        )
        assertEquals(22.0, sonuc, 1e-9)
    }

    @Test
    fun `ayni health connect kaydinin tekrar senkronize edilmesi ikiye katlamaz`() {
        // Aynı disKimlik iki kez geldi (worker aynı günü iki kez işledi) —
        // ikinci kopya atılır.
        val sonuc = AktiviteTekillestir.birlestirilmisSureDk(
            listOf(saglik(0, 20, disKimlik = "ayni-kayit"), saglik(0, 20, disKimlik = "ayni-kayit")),
        )
        assertEquals(20.0, sonuc, 1e-9)
    }

    @Test
    fun `negatif sure kabul edilmiyor`() {
        try {
            AktiviteOturumu(baslangic = dk(10), bitis = dk(5), kaynak = AktiviteKaynagi.UYGULAMA_YURUYUSU)
            org.junit.Assert.fail("bitiş başlangıçtan önce olamaz iddiası atlanmamalı")
        } catch (beklenen: IllegalArgumentException) {
            // beklenen
        }
    }
}
