package tr.org.yesilay.yesiliz.data.saglik

import tr.org.yesilay.yesiliz.data.depo.AdimDeposu
import tr.org.yesilay.yesiliz.data.depo.GunlukAktivite
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bir günün aktivitesi — iki kaynağın tek kapısı.
 *
 * Aktiviteyi okuyan hiç kimse (`GunlukOzetWorker`, `YuruyusServisi`)
 * "adım nereden geldi" sorusuyla uğraşmıyor; buraya soruyor.
 *
 * ## Sıralama: Health Connect önce
 *
 * Health Connect'te adım varsa **o** kazanıyor. Oradaki veri saatten,
 * bileklikten ya da özel bir sayaç uygulamasından gelmiş olabilir ve
 * telefonun cebe girip girmediğinden bağımsız — telefonu masada bırakıp
 * yürüyen birinin adımını donanım sayacı hiç görmüyor, saat görüyor.
 *
 * Health Connect sıfır dönüyorsa donanım sayacı devreye giriyor. Sıfır
 * "bugün hiç yürümedin" de olabilir ama ayırt etmenin bir yolu yok ve
 * yanlış tarafa düşmenin bedeli asimetrik: sensör de sıfır dönerse sonuç
 * yine sıfır, kaybedilen bir şey yok.
 *
 * **Toplama yok.** İki kaynak aynı adımları iki kez sayardı; ikisi de aynı
 * ayakları ölçüyor. Aktivite skoru **düşürüyor** (`docs/spec-risk.md`,
 * sönümleme tavanı %15), yani şişirilen her adım riski haksız yere aşağı
 * çeker.
 *
 * ## Egzersiz süresi
 *
 * Yalnızca Health Connect'ten. Donanım sayacında egzersiz oturumu diye bir
 * kavram yok; adımdan egzersiz uydurmak ölçüm değil tahmin olurdu.
 */
@Singleton
class AktiviteSaglayici @Inject constructor(
    private val saglikOkuyucu: SaglikOkuyucu,
    private val adimDeposu: AdimDeposu,
) {

    suspend fun gununAktivitesi(tarih: LocalDate): GunlukAktivite {
        val saglik = saglikOkuyucu.gununAktivitesi(tarih)
        if (saglik.adim > 0.0) return saglik
        return saglik.copy(adim = adimDeposu.gununAdimi(tarih))
    }
}
