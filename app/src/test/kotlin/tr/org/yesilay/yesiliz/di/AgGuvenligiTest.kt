package tr.org.yesilay.yesiliz.di

import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class AgGuvenligiTest {

    @Test
    fun debugGunluguIstekVeYanitGovdesiniYazmaz() {
        assertEquals(HttpLoggingInterceptor.Level.BASIC, guvenliHttpGunlugu().level)
    }

    @Test
    fun sunucuTabanininAltYoluVeIsteginSorgusuKorunur() {
        val istek = "http://10.0.2.2:8000/ozet?bas=2026-08-01&bit=2026-08-11".toHttpUrl()
        val taban = "https://ornek.test/api/v1/?taban=silinmeli".toHttpUrl()

        assertEquals(
            "https://ornek.test/api/v1/ozet?bas=2026-08-01&bit=2026-08-11",
            tabaniUygula(istek, taban).toString(),
        )
    }
}
