package tr.org.yesilay.yesiliz.widget

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetManifestGuvenligiTest {
    @Test
    fun `widget alicilari baska uygulamalara acik degildir`() {
        val belge = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))

        val androidAlani = "http://schemas.android.com/apk/res/android"
        val widgetAlicilari = setOf(
            ".widget.EkranSuresiWidgetAlicisi",
            ".widget.GununSorusuWidgetAlicisi",
        )
        val alicilar = belge.getElementsByTagName("receiver")

        widgetAlicilari.forEach { beklenenAd ->
            val alici = (0 until alicilar.length)
                .map { alicilar.item(it) }
                .single { it.attributes.getNamedItemNS(androidAlani, "name").nodeValue == beklenenAd }

            assertEquals(
                "$beklenenAd dışarıya açık olmamalı",
                "false",
                alici.attributes.getNamedItemNS(androidAlani, "exported").nodeValue,
            )
        }
    }
}
