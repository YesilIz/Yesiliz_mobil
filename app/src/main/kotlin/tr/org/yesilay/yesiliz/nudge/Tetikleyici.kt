package tr.org.yesilay.yesiliz.nudge

import tr.org.yesilay.yesiliz.domain.kullanim.KullanimOlay
import tr.org.yesilay.yesiliz.domain.kullanim.OruntuCikar
import java.time.Duration
import java.time.Instant

/** Overlay'i kapatan eylem — ikisi de farklı sönüm süresi taşıyor (`NudgeSabitleri`). */
enum class EylemTuru { MOLA_VERILDI, DEVAM_EDILDI }

/** En son ne zaman, hangi eylemle kapatıldığı — sönüm penceresini belirler. */
data class SonEylem(val tur: EylemTuru, val an: Instant)

enum class TetikSonucu { GOSTER, GOSTERME }

/**
 * Nudge'ın canlı tetikleyicisi — **SAF FONKSİYON**.
 *
 * `OruntuCikar.oturumOrani` ile karıştırılmamalı: o bir *günün tamamı*
 * için "kesintisiz blok oranı" hesaplıyor (geriye dönük, günde bir kez).
 * Burada soru farklı — "şu an hâlâ açık olan blok kaç dakikadır sürüyor" —
 * bu yüzden ayrı, küçük bir birleştirme yürüyüşü var. Aynı tolerans
 * sabitini ([OruntuCikar.BLOK_TOLERANSI]) paylaşıyor ki "blok" tanımı
 * risk motoruyla tutarlı kalsın.
 */
object Tetikleyici {

    /**
     * @param tuketimOlaylari yalnızca `Sabitler.TUKETIM` kategorisi,
     *   kronolojik sırada gelmesi gerekmez (burada sıralanır)
     * @param simdi tetikleyicinin çalıştığı an
     * @param sonEylem overlay en son ne zaman/nasıl kapatıldı — `null` ise
     *   sönüm penceresi yok
     * @param ayar etkin eşik ve sönüm süreleri; kullanıcının seçtiği eşikten
     *   türüyor (`NudgeAyari`), verilmezse varsayılan
     */
    fun degerlendir(
        tuketimOlaylari: List<KullanimOlay>,
        simdi: Instant,
        sonEylem: SonEylem?,
        suAnTuketimdeMi: Boolean = true,
        ayar: NudgeAyari = NudgeAyari.VARSAYILAN,
    ): TetikSonucu {
        if (!suAnTuketimdeMi) return TetikSonucu.GOSTERME

        if (sonEylem != null) {
            val sonum = when (sonEylem.tur) {
                EylemTuru.MOLA_VERILDI -> ayar.sonumMola
                EylemTuru.DEVAM_EDILDI -> ayar.sonumDevam
            }
            if (Duration.between(sonEylem.an, simdi) < sonum) return TetikSonucu.GOSTERME
        }

        return if (acikBlokSuresi(tuketimOlaylari, simdi) >= ayar.esik) {
            TetikSonucu.GOSTER
        } else {
            TetikSonucu.GOSTERME
        }
    }

    /**
     * Son olaydan geriye, [OruntuCikar.BLOK_TOLERANSI] boşluğuna kadar
     * birleştirerek "şu an hâlâ açık olan" bloğun süresini döner.
     *
     * Blok [simdi]'ye [OruntuCikar.BLOK_TOLERANSI]'ndan daha uzak bir
     * boşlukla bitmişse (kullanıcı tüketim kategorisinden tamamen çıkmış)
     * `Duration.ZERO` döner — blok artık açık sayılmaz.
     *
     * Süre **ölçülen olaylardan** çıkıyor, duvar saatinden değil: son
     * tüketim olayı ile [simdi] arasındaki fark bloğa eklenmiyor. Eskiden
     * `blokBas → simdi` ölçülüyordu ve kullanıcı tüketim uygulamasından
     * çıktıktan sonra blok büyümeye devam ediyordu — "45 dakikadır
     * buradasın" diyen nudge'ın o 45 dakikanın bir kısmını başka bir
     * uygulamada geçirmiş kullanıcıya çıkmasının sebebi buydu (§12 K51).
     * Uygulama hâlâ ön plandayken olay akışı geciktiyse bitişi
     * `NudgeOverlayService.onPlanTuketimUzat` `simdi`'ye çekiyor.
     */
    private fun acikBlokSuresi(olaylar: List<KullanimOlay>, simdi: Instant): Duration {
        if (olaylar.isEmpty()) return Duration.ZERO
        val sirali = olaylar.sortedBy { it.baslangic }

        var blokBas = sirali.first().baslangic
        var blokBit = sirali.first().bitis

        for (olay in sirali.drop(1)) {
            val bosluk = Duration.between(blokBit, olay.baslangic)
            if (bosluk <= OruntuCikar.BLOK_TOLERANSI) {
                blokBit = maxOf(blokBit, olay.bitis)
            } else {
                blokBas = olay.baslangic
                blokBit = olay.bitis
            }
        }

        val simdiyeBosluk = Duration.between(blokBit, simdi)
        if (simdiyeBosluk > OruntuCikar.BLOK_TOLERANSI) return Duration.ZERO

        return Duration.between(blokBas, blokBit)
    }
}
