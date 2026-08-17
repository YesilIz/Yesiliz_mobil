package tr.org.yesilay.yesiliz.ui.ekran.riskdetay

import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.Sabitler

/**
 * Risk halkasının doluluk oranı — **SAF FONKSİYON**.
 *
 * Halka ve ortasındaki etiket **tek bir iddia** olmalı. Eskiden halka ham
 * skordan (`R / 100`), etiket ise histerezisten geçmiş banttan geliyordu:
 * aynı dairenin içinde %66 dolu bir halka ve "Düşük" yazısı yan yana
 * duruyordu, altındaki cümle de "sağlıklı bir düzeyde görünüyor" diyordu.
 *
 * Çözüm skoru atmak değil, **gösterilen bandın aralığına kırpmak**: bant ile
 * skor uyuştuğu sürece (normal durum) halka skorun bant içindeki gerçek
 * yerini göstermeye devam ediyor; yalnızca histerezis bandı geride tuttuğunda
 * halka bandın sınırında duruyor ve etiketle çelişmiyor.
 *
 * Eşikler [Sabitler]'den; aralıkların kaynağı `Bantlama.esiktenBant` ve bu
 * fonksiyon onun tersi — ikisi aynı sabitleri okuduğu için ayrışamaz.
 */
internal object RiskHalkaOrani {

    /** Bandın skor aralığı — `0 ── 40 ── 65 ── 100`. */
    private fun araligi(bant: Bant): ClosedFloatingPointRange<Double> = when (bant) {
        Bant.AZ -> 0.0..Sabitler.BANT_AZ_UST
        Bant.ORTA -> Sabitler.BANT_AZ_UST..Sabitler.BANT_ORTA_UST
        Bant.COK -> Sabitler.BANT_ORTA_UST..100.0
    }

    /**
     * @param R ham risk skoru (0-100)
     * @param bant kullanıcıya **gösterilen** bant (histerezisten geçmiş)
     * @return halkanın doluluk oranı, 0f..1f
     */
    fun hesapla(R: Double, bant: Bant): Float {
        val aralik = araligi(bant)
        return (R.coerceIn(aralik) / 100.0).toFloat().coerceIn(0f, 1f)
    }
}
