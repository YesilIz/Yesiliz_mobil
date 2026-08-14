package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb
import tr.org.yesilay.yesiliz.data.yerel.dao.GorevTamamlamaDao
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.PuanGunlukDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GorevTamamlamaEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.PuanGunlukEntity
import tr.org.yesilay.yesiliz.domain.gorev.DogrulamaTuru
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.domain.puan.PuanHesapla
import tr.org.yesilay.yesiliz.domain.puan.PuanSabitleri
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Görev tamamlama ve günlük puan — `docs/spec-puan.md §7`.
 *
 * **Yalnızca kazanç.** Hiçbir yol negatif puan yazmıyor, hiçbir yol var olan
 * puanı silmiyor (CLAUDE.md §4: "Ceza suçluluk üretir, uygulamayı sildirir").
 * Lig düşmesi bile puan silmiyor — yalnızca grup değiştiriyor.
 *
 * Puan sistemi **risk skorundan bağımsız**: risk düşük olmalı, puan yüksek.
 * Bu yüzden `RiskDeposu` ile arasında bağ yok; ortak nokta yalnızca günlük
 * özetten okunan adım/spor/gece değerleri.
 */
@Singleton
class PuanDeposu @Inject constructor(
    private val db: YesilizDb,
    private val gorevDao: GorevTamamlamaDao,
    private val puanDao: PuanGunlukDao,
    private val ozetDao: GunlukOzetDao,
    private val tercihler: Tercihler,
    private val saat: Clock,
) {

    val toplamPuan: Flow<Int> = puanDao.toplamPuanAkisi()
    val aktifGunSayisi: Flow<Int> = puanDao.aktifGunAkisi()

    fun gununGorevleri(tarih: LocalDate): Flow<List<GorevTamamlamaEntity>> =
        gorevDao.gununAkisi(tarih)

    fun sonKayitlar(adet: Int = GECMIS_ADET): Flow<List<GorevTamamlamaEntity>> =
        gorevDao.sonKayitlarAkisi(adet)

    /**
     * Görevi tamamlar ve o günün puanını yeniden hesaplar.
     *
     * Günlük limit aşılırsa **sessizce yok sayılır** — hata göstermek
     * "yanlış yaptın" mesajı verir; görev zaten tamamlanmış görünüyor.
     *
     * [DogrulamaTuru.OTOMATIK] görevler buradan **yazılamaz**: puanlarını
     * `gunuYenidenHesapla` zaten günlük özetten veriyor. İkinci kez
     * yazılmaları aynı gün için çift sayım demekti (`debug.md` K-9). UI
     * artık o butonu göstermiyor; bu kontrol ikinci savunma hattı.
     *
     * @return kayıt yapıldıysa true
     */
    suspend fun gorevTamamla(gorev: Gorev, tarih: LocalDate): Boolean = db.withTransaction {
        if (gorev.dogrulama == DogrulamaTuru.OTOMATIK) return@withTransaction false
        val eklendi = gorevDao.limitAltindaEkle(
            tarih = tarih,
            gorevId = gorev.id,
            puan = gorev.puan,
            dogrulamaTuru = gorev.dogrulama.anahtar,
            limit = gorev.gunlukLimit,
        )
        if (eklendi < 0L) return@withTransaction false

        gunuYenidenHesaplaIc(tarih)
        true
    }

    /**
     * Bir günün puanını baştan hesaplar.
     *
     * Worker günlük özeti güncelledikten sonra da çağrılıyor: adım, spor ve
     * gece kullanımı değiştiğinde puan da değişiyor. `Upsert` olduğu için
     * tekrar güvenli.
     */
    suspend fun gunuYenidenHesapla(tarih: LocalDate) = db.withTransaction {
        gunuYenidenHesaplaIc(tarih)
    }

    /** Çağıran zaten [YesilizDb.withTransaction] içindeyken ortak hesap. */
    private suspend fun gunuYenidenHesaplaIc(tarih: LocalDate) {
        val ozet = ozetDao.getir(tarih)
        val hedefDk = tercihler.anlik().gunlukHedefDk

        // Gün BAŞINDAN İTİBAREN ölçüldü mü.
        //
        // `KullanimDeposu` bugünü `enErken = veriBaslangicAni` ile işliyor,
        // yani kurulum/rıza anından öncesi hiç okunmuyor. Saat 14:00'te
        // kurulan kullanıcının o günkü "toplam ekran süresi" 8 dakika
        // çıkıyor ve bu, TÜM GÜN için konmuş 285 dakikalık hedefle
        // karşılaştırılınca hedef tam puanla tutturulmuş sayılıyordu:
        // misafir kullanıcı uygulamayı açar açmaz 27 puan ve 1 günlük seri
        // görüyordu. Yarım ölçülen gün hedef için geçerli bir taban değil.
        val veriBaslangici = tercihler.veriBaslangicAni.first()
        val gunBasi = tarih.atStartOfDay(saat.zone).toInstant()
        val tamGunOlculdu = veriBaslangici == null || !veriBaslangici.isAfter(gunBasi)

        val toplamEkranDk = ozet?.let {
            it.socialDk + it.entertainmentDk + it.gamesDk + it.communicationDk +
                it.productivityDk + it.educationDk + it.healthDk + it.otherDk
        } ?: 0.0

        // Ölçüm YOKSA hedef "tutturuldu" sayılmıyor: veri olmadan ödül
        // vermek, izni vermemiş kullanıcıya avantaj sağlardı.
        //
        // Buradaki koşul `ozet != null` idi ve tam da engellemek istediği
        // şeyi kaçırıyordu: worker'ın telafi ettiği boş günlere de sıfır
        // dolu bir satır yazılıyor, `0.0 <= hedef` ve `0.0 < gece eşiği`
        // ikisi de doğru çıkıyordu. Temiz kurulumdan 10 dakika sonra
        // kullanıcı tek bir görev yapmadan "8 gün seri, 404 puan" görüyordu
        // (`debug.md` K-4). Doğru ölçüt satırın varlığı değil, o gün
        // gerçekten kullanım ölçülmüş olması.
        val olcumVar = ozet != null && ozet.olayVarMi
        val hedefeUygunGun = olcumVar && tamGunOlculdu

        // Seri artık hedefe bağlı (bkz. PuanHesapla.yeniSeri). Önceki gün
        // "puan aldığı gün" değil, takvimde bir önceki gün — seri puandan
        // bağımsızlaştığı için "aktif gün" araması anlamını yitirdi.
        val onceki = puanDao.getir(tarih.minusDays(1))

        // Seri kararı GÜN BİTİMİNDE veriliyor (§12 K40). Süren gün için
        // "hedefi tutturdun" demek, akşam hedef aşılınca geri alınacak bir
        // seri göstermek olurdu; kullanıcı bunu bozulma sanır.
        val gunKapandi = tarih < LocalDate.now(saat)
        val hedefTutturuldu = hedefeUygunGun && gunKapandi &&
            hedefDk > 0 && toplamEkranDk <= hedefDk
        val jokerHakkiVar = puanDao.jokerKullanilanGunSayisi(
            baslangic = tarih.minusDays(PuanSabitleri.KORUMA_PENCERESI_GUN - 1L),
            bitis = tarih.minusDays(1),
        ) == 0
        val seri = PuanHesapla.yeniSeri(
            oncekiSeri = onceki?.seriGun ?: 0,
            // Süren gün ve yarım ölçülen gün seriyi ne artırır ne kırar;
            // önceki günün değeri korunur.
            olculduMu = hedefeUygunGun && gunKapandi,
            hedefTutturuldu = hedefTutturuldu,
            jokerHakkiVar = jokerHakkiVar,
        )

        val puan = PuanHesapla.gunlukPuan(
            gorevPuani = gorevDao.gununPuani(tarih),
            sporDk = ozet?.sporDk ?: 0.0,
            adim = ozet?.adim ?: 0.0,
            // Oransal (`debug.md` İ-4): hedefin altındaysan tam puan,
            // aştıkça doğrusal iniyor, iki katında sıfırlanıyor.
            // Puan gün içinde CANLI birikiyor (seri gibi gün sonuna
            // bırakılmıyor) ama yarım ölçülen günde verilmiyor: 8 dakikalık
            // bir ölçümü 285 dakikalık hedefle karşılaştırmak ölçüm değil.
            hedefOrani = if (hedefeUygunGun) {
                PuanHesapla.hedefeYakinlik(toplamEkranDk, hedefDk)
            } else {
                0.0
            },
            // Gece ÖLÇÜLEMEYEN günde (olay geçmişi tükenmiş eski gün; orada
            // yalnızca paket başına toplam süre okunabiliyor, saat bilgisi
            // yok) `geceKullanimDk = 0` "gece temiz" demek değil "bilmiyoruz"
            // demek. Puan yazmak, ölçmediğimiz bir davranışı ödüllendirmek
            // olurdu.
            geceOrani = if (hedefeUygunGun && ozet.geceOlculduMu) {
                PuanHesapla.geceTemizligi(ozet.geceKullanimDk)
            } else {
                0.0
            },
            seriGun = seri.seri,
        )

        puanDao.kaydet(
            PuanGunlukEntity(
                tarih = tarih,
                puanHam = puan.ham,
                puanNet = puan.net,
                seriGun = puan.seriGun,
                carpani = puan.carpan,
                hedefTutturuldu = hedefTutturuldu,
                jokerKullanildi = seri.jokerHarcandi,
            ),
        )
    }

    /** Ana Sayfa'daki alev çipi — kaç gündür seri sürüyor. */
    suspend fun seriGunu(tarih: LocalDate): Int =
        puanDao.getir(tarih)?.seriGun ?: puanDao.oncekiGun(tarih)?.seriGun ?: 0

    private companion object {
        /** Puan geçmişi alt sayfasında gösterilen kayıt sayısı. */
        const val GECMIS_ADET = 30
    }
}
