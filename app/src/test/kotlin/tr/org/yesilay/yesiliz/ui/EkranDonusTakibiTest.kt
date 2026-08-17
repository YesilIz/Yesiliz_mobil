package tr.org.yesilay.yesiliz.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EkranDonusTakibiTest {

    @Test
    fun `gozlemci eklenirken gelen ilk donus seansi bozmaz`() {
        val takip = EkranDonusTakibi()

        assertFalse(takip.dondu())
    }

    @Test
    fun `ekrandan ayrildiktan sonraki donus seansi bozar`() {
        val takip = EkranDonusTakibi()

        takip.ayrildi()

        assertTrue(takip.dondu())
        assertFalse("aynı dönüş iki kez sayılmamalı", takip.dondu())
    }

    @Test
    fun `birden fazla ayrilis tek donus sayilir`() {
        val takip = EkranDonusTakibi()

        takip.ayrildi()
        takip.ayrildi()

        assertTrue(takip.dondu())
        assertFalse(takip.dondu())
    }
}
