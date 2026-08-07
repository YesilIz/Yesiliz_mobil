package tr.org.yesilay.yesiliz.data.depo

import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.yerel.dao.AnketYanitDao
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.RiskSonucEntity
import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import tr.org.yesilay.yesiliz.risk.Alpha
import tr.org.yesilay.yesiliz.risk.AnketGirdi
import tr.org.yesilay.yesiliz.risk.Bant
import tr.org.yesilay.yesiliz.risk.Bantlama
import tr.org.yesilay.yesiliz.risk.BantTakipci
import tr.org.yesilay.yesiliz.risk.EtkileyenFaktor
import tr.org.yesilay.yesiliz.risk.EtkileyenFaktorler
import tr.org.yesilay.yesiliz.risk.GecmisPencere
import tr.org.yesilay.yesiliz.risk.GunlukGirdi
import tr.org.yesilay.yesiliz.risk.GunlukOzet
import tr.org.yesilay.yesiliz.risk.RiskHesapla
import tr.org.yesilay.yesiliz.risk.Sabitler
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Günlük özeti risk skoruna ve **gösterilecek banda** çevirir.
 *
 * CLAUDE.md §6 veri akışının dördüncü adımı:
 * ```
 * data/depo (günlük özet) → core-risk (saf fonksiyon) → risk_sonuc
 * ```
 *
 * `KullanimDeposu` yalnızca *ham girdiyi* üretiyor; skorlama burada çünkü
 * skor **çok günlü**: alpha eğrisi kaç gün veri biriktiğine, güvenlik tabanı
 * son 5 güne, histerezis ardışık gün sayısına, trend son 14 güne bakıyor.
 * Tek günlük bir sınıfın içinde bunlar kurulamaz.
 *
 * ## Histerezis kalıcılığı
 *
 * `BantTakipci` bellekte durum taşıyor ve geri yükleme API'si yok. Her hesaptan
 * önce `risk_sonuc`'taki geçmiş **aday** bantlar taze bir takipçiye yeniden
 * oynatılıyor ([takipciyiKur]). Böylece core-risk'in saflık garantisi bozulmadan
 * sayaç günler arası sürüyor — CLAUDE.md §11'deki "bant takibi kalıcılığı"
 * maddesinin karşılığı.
 */
@Singleton
class RiskDeposu @Inject constructor(
    private val ozetDao: GunlukOzetDao,
    private val riskDao: RiskSonucDao,
    private val anketDao: AnketYanitDao,
    private val tercihler: Tercihler,
) {

    /**
     * Bir günün riskini hesaplar ve `risk_sonuc`'a yazar.
     *
     * @return yazılan satır; o güne ait özet yoksa null (hesaplanacak bir şey yok)
     */
    suspend fun gunuHesapla(tarih: LocalDate): RiskSonucEntity? {
        val ozet = ozetDao.getir(tarih) ?: return null

        val sonuc = RiskHesapla.hesapla(
            gun = ozet.girdiyeCevir(),
            anket = anketGirdisi(tarih),
            d = dEfektif(tarih),
        )

        val takipci = takipciyiKur(tarih)
        val gunSonuc = Bantlama.gunuIsle(
            skor = sonuc.R,
            // İkisi de ARTAN kronolojik sıra bekliyor (`takeLast`) ve
            // hesaplanan günü içeriyor — SenaryoTest'in çağrı biçimi bu.
            gunlukVeri = guvenlikPenceresi(tarih),
            skorGecmisi = skorGecmisi(tarih) + sonuc.R,
            takipci = takipci,
        )

        val satir = RiskSonucEntity(
            tarih = tarih,
            S = sonuc.S,
            rB = sonuc.rB,
            rS = sonuc.rS,
            alpha = sonuc.alpha,
            R = sonuc.R,
            adayBant = gunSonuc.adayBant.name,
            gosterilenBant = gunSonuc.gosterilenBant.name,
            guvenlikKurali = gunSonuc.guvenlikKurali,
            trend = gunSonuc.trend.name,
            surukleyiciler = sonuc.surukleyiciler,
            veriDurumu = sonuc.veriDurumu.name,
        )
        riskDao.kaydet(satir)
        return satir
    }

    /**
     * Risk detayı ekranının "Etkileyen faktörler" bölümü — o güne ait özet
     * yoksa `null` (henüz veri yok).
     */
    suspend fun etkileyenFaktorler(tarih: LocalDate): List<EtkileyenFaktor>? =
        ozetDao.getir(tarih)?.girdiyeCevir()?.let { EtkileyenFaktorler.hesapla(it) }

    /**
     * Geçmiş aday bantları yeniden oynatarak histerezis sayacını kurar.
     *
     * [Sabitler.GUN_ASAGI] gün aşağı yönde şart koşuluyor; 30 gün fazlasıyla
     * yeterli ve tek sorgu.
     */
    private suspend fun takipciyiKur(tarih: LocalDate): BantTakipci {
        val takipci = BantTakipci()
        riskDao.oncekiler(tarih, GECMIS_PENCERE_GUN).forEach {
            takipci.guncelle(Bant.valueOf(it.adayBant))
        }
        return takipci
    }

    /** Güvenlik tabanının baktığı pencere — hesaplanan gün dahil. */
    private suspend fun guvenlikPenceresi(tarih: LocalDate): List<GunlukOzet> =
        ozetDao.buGuneKadar(tarih, Sabitler.GUVENLIK_GUN).map {
            GunlukOzet(tuketimDk = it.tuketimDk(), geceDk = it.geceKullanimDk)
        }

    /** Trend penceresi — hesaplanan günün skoru çağıran tarafından ekleniyor. */
    private suspend fun skorGecmisi(tarih: LocalDate): List<Double> =
        riskDao.oncekiler(tarih, 2 * Sabitler.PENCERE_GUN).map { it.R }

    /**
     * Anket girdisi — en son doldurulan anket, üzerinden geçen günle birlikte.
     *
     * Anket **hesaplanan günden sonra** doldurulduysa yok sayılıyor: geçmiş bir
     * günü yeniden işlerken o gün henüz var olmayan bir anketi kullanmak
     * geçmişi çarpıtırdı.
     */
    private suspend fun anketGirdisi(tarih: LocalDate): AnketGirdi? {
        val anket = anketDao.sonuncu() ?: return null
        if (anket.tarih.isAfter(tarih)) return null
        return AnketGirdi(
            hamPuan = anket.hamPuan,
            kacGunOnce = ChronoUnit.DAYS.between(anket.tarih, tarih).toInt(),
        )
    }

    /**
     * Alpha eğrisinin `d` girdisi (spec-risk.md §4.5).
     *
     * `canliGun` = biriken özet satırı sayısı. `gecmisGun` kurulumda geriye
     * dönük çekilen gün — 0,6 ağırlıkla sayılıyor, şimdilik 0.
     */
    private suspend fun dEfektif(tarih: LocalDate): Int = Alpha.dEfektif(
        GecmisPencere(
            canliGun = ozetDao.gunSayisiEKadar(tarih),
            gecmisGun = tercihler.anlik().gecmisGunSayisi,
        ),
    )

    private companion object {
        /** Histerezis yeniden oynatma penceresi — GUN_ASAGI'dan fazlasıyla geniş. */
        const val GECMIS_PENCERE_GUN = 30
    }
}

/**
 * Room satırını motorun girdisine çevirir.
 *
 * **Ekleme sırası önemli** (`GunlukGirdi` kdoc'u): sürükleyiciler eşit katkıda
 * bu sıraya göre belirleniyor ve referans Python'un dict sırasıyla birebir
 * olması gerekiyor. Bu yüzden `linkedMapOf` ve sabit sıra.
 */
private fun GunlukOzetEntity.girdiyeCevir() = GunlukGirdi(
    kategoriDakika = linkedMapOf(
        Kategori.SOCIAL.anahtar to socialDk,
        Kategori.ENTERTAINMENT.anahtar to entertainmentDk,
        Kategori.GAMES.anahtar to gamesDk,
        Kategori.COMMUNICATION.anahtar to communicationDk,
        Kategori.PRODUCTIVITY.anahtar to productivityDk,
        Kategori.EDUCATION.anahtar to educationDk,
        Kategori.HEALTH.anahtar to healthDk,
        Kategori.OTHER.anahtar to otherDk,
    ),
    oruntu = linkedMapOf(
        "gece" to gece,
        "oturum" to oturum,
        "acilis" to acilis,
        "sabah" to sabah,
        "uyku_oncesi" to uykuOncesi,
        "yer_degis" to yerDegis,
    ),
    adim = adim,
    sporDk = sporDk,
)

/** Güvenlik tabanının baktığı ham tüketim dakikası (skordan bağımsız). */
private fun GunlukOzetEntity.tuketimDk(): Double =
    listOf(
        Kategori.SOCIAL.anahtar to socialDk,
        Kategori.ENTERTAINMENT.anahtar to entertainmentDk,
        Kategori.GAMES.anahtar to gamesDk,
        Kategori.COMMUNICATION.anahtar to communicationDk,
        Kategori.PRODUCTIVITY.anahtar to productivityDk,
        Kategori.EDUCATION.anahtar to educationDk,
        Kategori.HEALTH.anahtar to healthDk,
        Kategori.OTHER.anahtar to otherDk,
    ).filter { it.first in Sabitler.TUKETIM }.sumOf { it.second }
