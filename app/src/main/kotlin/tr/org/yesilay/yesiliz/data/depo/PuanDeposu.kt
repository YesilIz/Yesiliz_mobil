package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.flow.Flow
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.dao.GorevTamamlamaDao
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.PuanGunlukDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GorevTamamlamaEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.PuanGunlukEntity
import tr.org.yesilay.yesiliz.domain.gorev.DogrulamaTuru
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.domain.puan.PuanHesapla
import tr.org.yesilay.yesiliz.domain.puan.PuanSabitleri
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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
    private val gorevDao: GorevTamamlamaDao,
    private val puanDao: PuanGunlukDao,
    private val ozetDao: GunlukOzetDao,
    private val tercihler: Tercihler,
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
    suspend fun gorevTamamla(gorev: Gorev, tarih: LocalDate): Boolean {
        if (gorev.dogrulama == DogrulamaTuru.OTOMATIK) return false
        if (gorevDao.gununSayisi(tarih, gorev.id) >= gorev.gunlukLimit) return false

        gorevDao.ekle(
            GorevTamamlamaEntity(
                tarih = tarih,
                gorevId = gorev.id,
                puan = gorev.puan,
                dogrulamaTuru = gorev.dogrulama.anahtar,
            ),
        )
        gunuYenidenHesapla(tarih)
        return true
    }

    /**
     * Bir günün puanını baştan hesaplar.
     *
     * Worker günlük özeti güncelledikten sonra da çağrılıyor: adım, spor ve
     * gece kullanımı değiştiğinde puan da değişiyor. `Upsert` olduğu için
     * tekrar güvenli.
     */
    suspend fun gunuYenidenHesapla(tarih: LocalDate) {
        val ozet = ozetDao.getir(tarih)
        val hedefDk = tercihler.anlik().gunlukHedefDk

        val onceki = puanDao.oncekiAktifGun(tarih)
        val seri = PuanHesapla.yeniSeri(
            oncekiSeri = onceki?.seriGun ?: 0,
            aradakiBosGun = onceki
                ?.let { ChronoUnit.DAYS.between(it.tarih, tarih).toInt() - 1 }
                ?: 0,
        )

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

        val puan = PuanHesapla.gunlukPuan(
            gorevPuani = gorevDao.gununPuani(tarih),
            sporDk = ozet?.sporDk ?: 0.0,
            adim = ozet?.adim ?: 0.0,
            // Oransal (`debug.md` İ-4): hedefin altındaysan tam puan,
            // aştıkça doğrusal iniyor, iki katında sıfırlanıyor.
            hedefOrani = if (olcumVar) PuanHesapla.hedefeYakinlik(toplamEkranDk, hedefDk) else 0.0,
            geceOrani = if (olcumVar) PuanHesapla.geceTemizligi(ozet.geceKullanimDk) else 0.0,
            seriGun = seri,
        )

        puanDao.kaydet(
            PuanGunlukEntity(
                tarih = tarih,
                puanHam = puan.ham,
                puanNet = puan.net,
                seriGun = puan.seriGun,
                carpani = puan.carpan,
            ),
        )
    }

    /** Ana Sayfa'daki alev çipi — kaç gündür seri sürüyor. */
    suspend fun seriGunu(tarih: LocalDate): Int =
        puanDao.getir(tarih)?.seriGun ?: puanDao.oncekiAktifGun(tarih)?.seriGun ?: 0

    private companion object {
        /** Puan geçmişi alt sayfasında gösterilen kayıt sayısı. */
        const val GECMIS_ADET = 30
    }
}
