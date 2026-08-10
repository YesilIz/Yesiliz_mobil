package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tr.org.yesilay.yesiliz.data.saglik.AdimDurumu
import tr.org.yesilay.yesiliz.data.saglik.AdimKaynagi
import tr.org.yesilay.yesiliz.data.saglik.adimBirikimGuncelle
import tr.org.yesilay.yesiliz.data.yerel.dao.AdimSayacDao
import tr.org.yesilay.yesiliz.data.yerel.entity.AdimSayacEntity
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Donanım adım sayacının günlük birikimi.
 *
 * Sensör yalnızca **açılıştan beri** biriken kümülatif sayıyı veriyor; güne
 * düşen payı çıkarmak için örneklemek ve farkını yazmak gerekiyor
 * ([adimBirikimGuncelle]). Bu depo o örneklemeyi yapıyor.
 *
 * ## Kim örnekliyor
 *
 * | Yer | Ne zaman |
 * |---|---|
 * | `MainActivity.onResume` | kullanıcı uygulamayı her açtığında |
 * | `GunlukOzetWorker` | gecelik özet hazırlanmadan hemen önce |
 * | `YuruyusServisi` | yürüyüş sayacının her tikinde |
 *
 * Sık örneklemenin bedeli yok — okuma tek bir tam sayı, sensör zaten
 * donanımda sayıyor. Seyrek örneklemenin bedeli **var**: son örnek ile gece
 * yarısı arasındaki adımlar kayboluyor. O yüzden mümkün olan her yerde
 * örnekleniyor.
 */
@Singleton
class AdimDeposu @Inject constructor(
    private val dao: AdimSayacDao,
    private val okuyucu: AdimKaynagi,
    private val saat: Clock,
) {

    /**
     * Ana ekran, gece işi ve yürüyüş servisi aynı anda örnekleyebilir.
     * Sayaç okuma → son satırı okuma → yeni toplamı yazma tek zincir
     * olmalı; iki zincir yarışırsa daha eski sayaç sonradan yazılıp
     * günlük toplam geçici olarak gerileyebilirdi.
     */
    private val orneklemeKilidi = Mutex()

    /**
     * Sayacı okuyup bugünün birikimine işler. Sensör yoksa, izin yoksa ya da
     * okuma başarısızsa sessizce hiçbir şey yapmıyor — çağıranların hepsi
     * "olursa iyi" konumunda.
     */
    suspend fun ornekle() = orneklemeKilidi.withLock {
        withContext(Dispatchers.IO) {
            val sayac = try {
                okuyucu.anlikSayac()
            } catch (iptal: CancellationException) {
                throw iptal
            } catch (_: SecurityException) {
                // İzin, `izinVarMi` kontrolü ile SensorManager kaydı arasında
                // sistem Ayarları'ndan geri alınabilir. Bu beklenen yarış mevcut
                // satırı bozmamalı ve ana ekran işini düşürmemeli.
                null
            } ?: return@withContext
            val bugun = LocalDate.now(saat)
            val onceki = dao.gunun(bugun)?.let { AdimDurumu(it.toplam, it.sonSayac) }
            val yeni = adimBirikimGuncelle(onceki, sayac)
            dao.kaydet(AdimSayacEntity(bugun, yeni.toplam, yeni.sonSayac))
        }
    }

    /** O günün biriktirilmiş adımı; kayıt yoksa sıfır. */
    suspend fun gununAdimi(tarih: LocalDate): Double = withContext(Dispatchers.IO) {
        (dao.gunun(tarih)?.toplam ?: 0L).toDouble()
    }

    /** Sayaç birikimi geçmişe dönük anlam taşımıyor; eskiler temizlenir. */
    suspend fun eskileriSil(sinir: LocalDate) = withContext(Dispatchers.IO) {
        dao.eskileriSil(sinir)
    }
}
