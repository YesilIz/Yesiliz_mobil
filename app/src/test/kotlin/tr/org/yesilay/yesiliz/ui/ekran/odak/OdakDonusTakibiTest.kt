package tr.org.yesilay.yesiliz.ui.ekran.odak

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OdakDonusTakibiTest {

    @Test
    fun `gozlemci eklenirken gelen ilk donus seansi bozmaz`() {
        val takip = OdakDonusTakibi()

        assertFalse(takip.dondu())
    }

    @Test
    fun `ekrandan ayrildiktan sonraki donus seansi bozar`() {
        val takip = OdakDonusTakibi()

        takip.ayrildi()

        assertTrue(takip.dondu())
        assertFalse("aynı dönüş iki kez sayılmamalı", takip.dondu())
    }

    @Test
    fun `birden fazla ayrilis tek donus sayilir`() {
        val takip = OdakDonusTakibi()

        takip.ayrildi()
        takip.ayrildi()

        assertTrue(takip.dondu())
        assertFalse(takip.dondu())
    }
}
