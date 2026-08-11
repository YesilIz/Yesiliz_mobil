package tr.org.yesilay.yesiliz.data.depo

import tr.org.yesilay.yesiliz.data.kullanim.HamOturum
import tr.org.yesilay.yesiliz.data.kullanim.KullanimOkuyucu
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.KullanimOlayDao
import tr.org.yesilay.yesiliz.data.yerel.entity.GunlukOzetEntity
import tr.org.yesilay.yesiliz.data.yerel.entity.KullanimOlayEntity
import tr.org.yesilay.yesiliz.domain.kategori.Kategori
import tr.org.yesilay.yesiliz.domain.kullanim.GunlukCikarim
import tr.org.yesilay.yesiliz.domain.kullanim.KullanimOlay
import tr.org.yesilay.yesiliz.domain.kullanim.OruntuCikar
import tr.org.yesilay.yesiliz.domain.kullanim.YerDegisHesapla
import tr.org.yesilay.yesiliz.risk.GunlukGirdi
import tr.org.yesilay.yesiliz.risk.RiskHesapla
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sistemden okuma → kategorilendirme → Room → günlük özet zinciri.
 *
 * CLAUDE.md §6 veri akışının ilk üç adımını birleştiriyor:
 * ```
 * UsageStatsManager → data/kullanim → Room → data/depo (günlük özet)
 * ```
 *
 * Risk hesabı burada **yapılmıyor**, yalnızca özetin ara ürünleri
 * (P, A, D, S) doldurulurken `core-risk` çağrılıyor. Bant kararı çok günlü
 * olduğu için ayrı bir katmanda kalacak.
 */
@Singleton
class KullanimDeposu @Inject constructor(
    private val okuyucu: KullanimOkuyucu,
    private val kategoriDeposu: KategoriDeposu,
    private val olayDao: KullanimOlayDao,
    private val ozetDao: GunlukOzetDao,
    private val zon: ZoneId,
) {

    /**
     * Bir günü baştan sona işler: sistemden oku → kategorile → kaydet → özetle.
     *
     * @param aktivite o günün adım/spor değerleri; verilmezse sönümleme
     *   uygulanmaz (`D = 1.0`)
     * @return üretilen çıkarım; gün boşsa alanları sıfır
     */
    suspend fun gunuIsle(
        tarih: LocalDate,
        aktivite: GunlukAktivite = GunlukAktivite(),
        enErken: Instant? = null,
    ): GunlukCikarim {
        val oturumlar = okuyucu.gununOturumlari(tarih, enErken = enErken)

        // Sistem paketleri (launcher, SystemUI, Ayarlar) HİÇ saklanmıyor.
        // Risk hesabına zaten girmiyorlar; saklamak hem yer israfı hem de
        // gereksiz veri toplamak olurdu (CLAUDE.md §4 veri minimizasyonu).
        // Emülatörde 14 oturumun 11'i sistemdi — oran düşük değil.
        val olaylar = kategorilendir(oturumlar).filter { it.kategori.hesabaGirer }

        // Gün yeniden işlenebilir (worker iki kez koşabilir) — önce temizle.
        // `eskileriSil` yalnızca tarihten ÖNCEsini siler, bu yüzden aynı günü
        // ayrıca silmek gerekiyor.
        olayDao.gununuSil(tarih)
        olayDao.ekle(
            olaylar.map {
                KullanimOlayEntity(
                    paketAdi = it.paketAdi,
                    kategori = it.kategori.anahtar,
                    baslangic = it.baslangic,
                    bitis = it.bitis,
                    tarih = tarih,
                )
            },
        )

        val cikarim = OruntuCikar.cikar(olaylar, tarih, zon)
        val gecmis = ozetDao.araliktakiler(
            tarih.minusDays(YER_DEGIS_GUN - 1L),
            tarih.minusDays(1),
        )
        val yerDegis = yerDegisDegeri(gecmis, cikarim.toplamDk, aktivite.adim)
        // Satır boş günde de yazılıyor (telefonsuz geçen gerçek bir gün de
        // kayıt), ama `olayVarMi` ile işaretleniyor: satırın varlığı veri
        // olduğu anlamına gelmiyordu ve puan/seri/alpha bunu "veri var"
        // sanıyordu (`debug.md` K-4, K-5).
        ozetDao.kaydet(
            ozetKur(
                tarih = tarih,
                c = cikarim,
                aktivite = aktivite,
                olayVarMi = olaylar.isNotEmpty(),
                yerDegis = yerDegis,
            ),
        )
        return cikarim
    }

    /** Ham oturumları kategorili domain modeline çevirir. */
    private suspend fun kategorilendir(oturumlar: List<HamOturum>): List<KullanimOlay> {
        val esleyici = kategoriDeposu.esleyiciAl()
        return oturumlar.map { oturum ->
            KullanimOlay(
                paketAdi = oturum.paketAdi,
                kategori = esleyici.bul(
                    oturum.paketAdi,
                    okuyucu.androidKategorisi(oturum.paketAdi),
                ),
                baslangic = oturum.baslangic,
                bitis = oturum.bitis,
            )
        }
    }

    /**
     * Çıkarımı `gunluk_ozet` satırına dönüştürür.
     *
     * **Skorlama burada YAPILMIYOR.** Risk çok günlü (alpha kaç gün veri
     * biriktiğine, güvenlik tabanı son 5 güne, histerezis ardışık güne bakıyor);
     * tek günlük bu fonksiyonun içinde kurulamaz. `RiskDeposu.gunuHesapla`
     * bu satırı okuyup skoru ve bandı üretiyor.
     *
     * Ara ürünler (`bTuketim`, `bIslevsel`, `P`, `A`, `D`, `S`) yine burada
     * dolduruluyor: risk detayı ekranını tek sorguya indiriyorlar
     * (`GunlukOzetEntity` kdoc'u) ve hepsi **tek güne** bağlı.
     */
    private fun ozetKur(
        tarih: LocalDate,
        c: GunlukCikarim,
        aktivite: GunlukAktivite,
        olayVarMi: Boolean,
        yerDegis: Double,
    ): GunlukOzetEntity {
        val oruntu = c.oruntu + ("yer_degis" to yerDegis)
        val girdi = GunlukGirdi(
            kategoriDakika = c.kategoriDakika,
            oruntu = oruntu,
            adim = aktivite.adim,
            sporDk = aktivite.sporDk,
        )
        val araUrun = RiskHesapla.araUrunler(girdi)
        fun dk(k: Kategori) = c.kategoriDakika[k.anahtar] ?: 0.0
        fun o(ad: String) = oruntu[ad] ?: 0.0

        return GunlukOzetEntity(
            tarih = tarih,
            socialDk = dk(Kategori.SOCIAL),
            entertainmentDk = dk(Kategori.ENTERTAINMENT),
            gamesDk = dk(Kategori.GAMES),
            communicationDk = dk(Kategori.COMMUNICATION),
            productivityDk = dk(Kategori.PRODUCTIVITY),
            educationDk = dk(Kategori.EDUCATION),
            healthDk = dk(Kategori.HEALTH),
            otherDk = dk(Kategori.OTHER),
            gece = o("gece"),
            oturum = o("oturum"),
            acilis = o("acilis"),
            sabah = o("sabah"),
            uykuOncesi = o("uyku_oncesi"),
            yerDegis = o("yer_degis"),
            adim = aktivite.adim,
            sporDk = aktivite.sporDk,
            bTuketim = araUrun.bTuketim,
            bIslevsel = araUrun.bIslevsel,
            P = araUrun.P,
            A = araUrun.A,
            D = araUrun.D,
            S = araUrun.S,
            geceKullanimDk = c.geceKullanimDk,
            acilisSayisi = c.acilisSayisi,
            olayVarMi = olayVarMi,
        )
    }

    private companion object {
        const val YER_DEGIS_GUN = 7L
    }
}

/** Son yedi günlük ekran/adım dizisini gerçek çok-günlü örüntüye bağlar. */
internal fun yerDegisDegeri(
    gecmis: List<GunlukOzetEntity>,
    bugunEkranDk: Double,
    bugunAdim: Double,
): Double = YerDegisHesapla.hesapla(
    ekranDk = gecmis.map { it.toplamEkranDk() } + bugunEkranDk,
    adim = gecmis.map { it.adim } + bugunAdim,
)

private fun GunlukOzetEntity.toplamEkranDk(): Double =
    socialDk + entertainmentDk + gamesDk + communicationDk +
        productivityDk + educationDk + healthDk + otherDk

/**
 * O güne ait aktivite — Health Connect'ten gelir.
 *
 * Sağlık verisi yoksa (izin verilmedi, Health Connect kurulu değil) sıfır
 * kalır: `A = 0` → `D = 1.0`, yani **sönümleme uygulanmaz**. Bu doğru
 * varsayılan; uydurma aktivite skoru düşürürdü.
 */
data class GunlukAktivite(
    val adim: Double = 0.0,
    val sporDk: Double = 0.0,
)
