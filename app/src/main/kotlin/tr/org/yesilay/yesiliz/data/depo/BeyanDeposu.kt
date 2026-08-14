package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.flow.Flow
import tr.org.yesilay.yesiliz.data.yerel.dao.RuhHaliDao
import tr.org.yesilay.yesiliz.data.yerel.dao.UykuBeyaniDao
import tr.org.yesilay.yesiliz.data.yerel.entity.RuhHaliEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.UykuBeyaniEntity
import tr.org.yesilay.yesiliz.domain.gorev.Gorev
import tr.org.yesilay.yesiliz.domain.gorev.RuhHaliPenceresi
import tr.org.yesilay.yesiliz.domain.gorev.UykuAraligi
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kullanıcının kendi beyanları: ruh hali ve uyku.
 *
 * İkisi de **risk skoruna girmiyor** — `core-risk` bu değerleri hiç görmüyor.
 * Beyanı skora katmak, kullanıcıyı "iyi görünmek için" yanlış cevaplamaya
 * iterdi; ölçüm davranıştan yapılıyor, beyan yalnızca farkındalık ve görev
 * puanı için (`docs/spec-puan.md §7.1`: ikisi de "beyan" doğrulamalı görev).
 *
 * Puanı bu depo veriyor ki hem ekran hem widget aynı yoldan geçsin.
 */
@Singleton
class BeyanDeposu @Inject constructor(
    private val ruhHaliDao: RuhHaliDao,
    private val uykuDao: UykuBeyaniDao,
    private val puanDeposu: PuanDeposu,
    private val bildirimDeposu: BildirimDeposu,
) {

    /** Günün üç penceresindeki kayıtlar; yazılmamış pencere listede yok. */
    fun ruhHaliAkisi(tarih: LocalDate): Flow<List<RuhHaliEntity>> = ruhHaliDao.gununAkisi(tarih)

    fun uykuAkisi(tarih: LocalDate): Flow<UykuBeyaniEntity?> = uykuDao.gununAkisi(tarih)

    /**
     * Ruh halini o anki pencereye kaydeder; pencere ilk kez doluyorsa görev
     * puanı verir.
     *
     * Aynı pencerede tekrar sürüklemek değeri günceller ama **ikinci kez puan
     * vermez** — [PuanDeposu.gorevTamamla] günlük limiti (3) atomik olarak
     * uyguluyor. Ama limit tek başına yetmiyor: sabah kaydını yapan kullanıcı
     * sabah penceresinde tekrar sürüklerse limit hâlâ 3'ün altında kalır ve
     * ikinci puan yazılırdı. Bu yüzden pencerenin daha önce yazılıp
     * yazılmadığı **ayrıca** soruluyor.
     *
     * @param tarih ve pencere [RuhHaliPenceresi.gunuVeDilimi]'nden gelir —
     *   gece yarısı ile 05:00 arası hâlâ dünün akşam penceresi
     */
    suspend fun ruhHaliKaydet(tarih: LocalDate, dilim: RuhHaliPenceresi, deger: Float) {
        val ilkKez = ruhHaliDao.getir(tarih, dilim.anahtar) == null
        ruhHaliDao.kaydet(
            RuhHaliEntity(tarih = tarih, dilim = dilim.anahtar, deger = deger),
        )
        if (ilkKez && puanDeposu.gorevTamamla(Gorev.RUH_HALI, tarih)) {
            bildirimDeposu.gorevTamamlandi(Gorev.RUH_HALI)
        }
    }

    /** Uyku beyanı — görev listesinden ya da widget'tan gelir. */
    suspend fun uykuKaydet(tarih: LocalDate, aralik: UykuAraligi) {
        uykuDao.kaydet(UykuBeyaniEntity(tarih = tarih, aralik = aralik.anahtar))
        if (puanDeposu.gorevTamamla(Gorev.UYKU_BEYANI, tarih)) {
            bildirimDeposu.gorevTamamlandi(Gorev.UYKU_BEYANI)
        }
    }

    suspend fun uykuGetir(tarih: LocalDate): UykuAraligi? =
        uykuDao.getir(tarih)?.let { UykuAraligi.anahtardan(it.aralik) }
}
