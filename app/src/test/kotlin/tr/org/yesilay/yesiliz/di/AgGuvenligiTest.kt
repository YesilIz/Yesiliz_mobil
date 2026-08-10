package tr.org.yesilay.yesiliz.di

import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Test

class AgGuvenligiTest {

    @Test
    fun debugGunluguIstekVeYanitGovdesiniYazmaz() {
        assertEquals(HttpLoggingInterceptor.Level.BASIC, guvenliHttpGunlugu().level)
    }
}
