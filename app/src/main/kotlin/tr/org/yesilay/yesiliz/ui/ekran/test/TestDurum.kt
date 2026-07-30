package tr.org.yesilay.yesiliz.ui.ekran.test

import tr.org.yesilay.yesiliz.domain.test.FarkindalikTesti
import tr.org.yesilay.yesiliz.domain.test.TestSecenegi
import tr.org.yesilay.yesiliz.domain.test.TestSonuc
import java.time.LocalDate

/**
 * Kısa farkındalık testinin durumu — `ui/ekran/test/`.
 *
 * Tek ekran iki aşamalı: sorular ve sonuç. Ayrı rota açmak yerine [sonuc]
 * dolduğunda ekranın içeriği değişiyor; geri tuşu her iki aşamada da
 * doğrudan ana sayfaya dönüyor (yarım kalmış test kaydedilmiyor).
 */
data class TestDurum(
    /** Soru sırasıyla yanıtlar; cevaplanmamışlar `null`. */
    val yanitlar: List<TestSecenegi?> = List(FarkindalikTesti.SORULAR.size) { null },
    val sonuc: TestSonuc? = null,
    /** Daha önce çözülmüşse sonuç ekranında karşılaştırma satırı çıkıyor. */
    val oncekiPuan: Int? = null,
    val oncekiTarih: LocalDate? = null,
) {
    val tamamMi: Boolean get() = yanitlar.all { it != null }

    /** İlerleme çubuğu — cevaplanan soru oranı. */
    val ilerleme: Float
        get() = yanitlar.count { it != null }.toFloat() / yanitlar.size
}
