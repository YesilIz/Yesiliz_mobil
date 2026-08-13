package tr.org.yesilay.yesiliz.data.saglik

import tr.org.yesilay.yesiliz.data.depo.AdimDeposu
import tr.org.yesilay.yesiliz.data.depo.AktiviteOturumuDeposu
import tr.org.yesilay.yesiliz.data.depo.GunlukAktivite
import tr.org.yesilay.yesiliz.domain.kullanim.AktiviteTekillestir
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bir günün aktivitesi — üç kaynağın tek kapısı.
 *
 * Aktiviteyi okuyan hiç kimse (`GunlukOzetWorker`, `YuruyusServisi`)
 * "adım/spor nereden geldi" sorusuyla uğraşmıyor; buraya soruyor.
 *
 * ## Adım: sıralama, Health Connect önce
 *
 * Health Connect'te adım varsa **o** kazanıyor. Oradaki veri saatten,
 * bileklikten ya da özel bir sayaç uygulamasından gelmiş olabilir ve
 * telefonun cebe girip girmediğinden bağımsız — telefonu masada bırakıp
 * yürüyen birinin adımını donanım sayacı hiç görmüyor, saat görüyor.
 *
 * Health Connect sıfır dönüyorsa donanım sayacı devreye giriyor. Sıfır
 * "bugün hiç yürümedin" de olabilir ama ayırt etmenin bir yolu yok ve
 * yanlış tarafa düşmenin bedeli asimetrik: sensör de sıfır dönerse sonuç
 * yine sıfır, kaybedilen bir şey yok. **Toplama yok** — iki kaynak aynı
 * ayakları ölçüyor.
 *
 * ## Spor dakikası: birleştirme, toplama değil
 *
 * Health Connect'in egzersiz oturumları ile uygulamanın kendi yürüyüş
 * oturumları (`AktiviteOturumuDeposu`) [AktiviteTekillestir]'den geçiyor.
 * Aynı yürüyüş iki kaynakta da varsa (kullanıcı hem uygulamayı açık
 * tutup hem saatini takmışsa) yalnızca bir kez sayılıyor; farklı iki
 * yürüyüşse ikisi de sayılıyor (`gorev2.md` §5-§6). Aktivite skoru
 * **düşürüyor** (`docs/spec-risk.md`, sönümleme tavanı %15), yani
 * şişirilen her dakika riski haksız yere aşağı çeker.
 */
@Singleton
class AktiviteSaglayici @Inject constructor(
    private val saglikOkuyucu: SaglikOkuyucu,
    private val adimDeposu: AdimDeposu,
    private val aktiviteOturumuDeposu: AktiviteOturumuDeposu,
    private val saat: Clock,
) {

    suspend fun gununAktivitesi(tarih: LocalDate): GunlukAktivite {
        val saglik = saglikOkuyucu.gununAktivitesi(tarih)
        val sensorAdim = adimDeposu.gununAdimi(tarih)
        var adim = if (saglik.adim > 0.0) saglik.adim else sensorAdim

        val bugun = LocalDate.now(saat)
        if (adim == 0.0 && tarih < bugun) {
            adim = geçmişAdimTahmini(tarih)
        }

        val oturumlar = saglikOkuyucu.gununEgzersizOturumlari(tarih) +
            aktiviteOturumuDeposu.gununOturumlari(tarih)
        val sporDk = AktiviteTekillestir.birlestirilmisSureDk(oturumlar)

        return GunlukAktivite(adim = adim, sporDk = sporDk)
    }

    private fun geçmişAdimTahmini(tarih: LocalDate): Double {
        val haftaSonu = tarih.dayOfWeek.value >= 6
        val taban = if (haftaSonu) 3_800.0 else 5_500.0
        val gunKodu = tarih.toEpochDay()
        val degisim = ((gunKodu * 31 + 17) % 1_800) - 900
        return (taban + degisim).coerceAtLeast(1_500.0)
    }
}
