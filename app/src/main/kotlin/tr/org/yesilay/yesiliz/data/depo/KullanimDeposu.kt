package tr.org.yesilay.yesiliz.data.depo

import tr.org.yesilay.yesiliz.data.kullanim.HamOturum
import tr.org.yesilay.yesiliz.data.kullanim.KullanimOkuyucu
import tr.org.yesilay.yesiliz.data.saglik.AdimKaynagiTuru
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
import java.time.Clock
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
    private val saat: Clock,
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
        val olaylar = kategorilendir(oturumlar).filter { it.kategori.hesabaGirer }

        // Gün yeniden işlenebilir (worker iki kez koşabilir) — önce temizle.
        olayDao.gununuSil(tarih)
        if (olaylar.isNotEmpty()) {
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
        }

        val cikarim = if (olaylar.isNotEmpty()) {
            OruntuCikar.cikar(olaylar, tarih, saat.zone)
        } else {
            agregadanCikar(tarih)
        }

        val gecmis = ozetDao.araliktakiler(
            tarih.minusDays(YER_DEGIS_GUN - 1L),
            tarih.minusDays(1),
        )
        val yerDegis = yerDegisDegeri(gecmis, cikarim.toplamDk, aktivite.adim)
        // Ölçüm bayrağı YALNIZCA gerçek kullanım ölçümüne bakar. Adım tek
        // başına "veri var" yapmaz: adım verisi telefon kullanılmadan da
        // (saatten, bileklikten) gelebiliyor ve `olayVarMi` puan/seri/alpha
        // için "bu gün ölçüldü mü" sorusunun cevabı (CLAUDE.md §9).
        val olayVarMi = cikarim.toplamDk > 0.0

        ozetDao.kaydet(
            ozetKur(
                tarih = tarih,
                c = cikarim,
                aktivite = aktivite,
                olayVarMi = olayVarMi,
                yerDegis = yerDegis,
            ),
        )
        return cikarim
    }

    /**
     * Olay geçmişi tükenmiş geçmiş günler için yedek yol.
     *
     * Android yalnızca paket başına **toplam süre** veriyor — saat bilgisi yok.
     * Bu yüzden örüntüler (gece, oturum, açılış, sabah, uyku öncesi) burada
     * hesaplanamıyor ve `geceOlculduMu = false` işaretleniyor: "gece 0 dakika"
     * ile "geceyi ölçemedik" karışırsa `GECE_TEMIZ` görevi ölçülemeyen her
     * güne puan yazar.
     *
     * Güvenilir kova yoksa ([AgregaSuz] `null` döndürürse) gün **boş** kalıyor
     * ve `olayVarMi = false` yazılıyor — uydurma değer üretilmiyor (§4).
     */
    private suspend fun agregadanCikar(tarih: LocalDate): GunlukCikarim {
        val bos = GunlukCikarim(
            kategoriDakika = emptyMap(),
            oruntu = emptyMap(),
            toplamDk = 0.0,
            geceKullanimDk = 0.0,
            acilisSayisi = 0,
            uykuPenceresi = null,
            geceOlculduMu = false,
        )
        val paketDakikalari = okuyucu.gunlukPaketDakikalari(tarih) ?: return bos

        val esleyici = kategoriDeposu.esleyiciAl()
        val kategoriDakikaMap = mutableMapOf<String, Double>()

        paketDakikalari.forEach { (paket, dakika) ->
            val kat = esleyici.bul(paket)
            if (kat.hesabaGirer) {
                val mevcut = kategoriDakikaMap[kat.anahtar] ?: 0.0
                kategoriDakikaMap[kat.anahtar] = mevcut + dakika
            }
        }
        if (kategoriDakikaMap.isEmpty()) return bos

        return GunlukCikarim(
            kategoriDakika = kategoriDakikaMap,
            oruntu = emptyMap(),
            toplamDk = kategoriDakikaMap.values.sum(),
            geceKullanimDk = 0.0,
            acilisSayisi = 0,
            uykuPenceresi = null,
            geceOlculduMu = false,
        )
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
            geceOlculduMu = c.geceOlculduMu,
            adimKaynagi = aktivite.adimKaynagi.name,
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
    /** [adim] hangi kaynaktan geldi — ekranda gösteriliyor, bkz. AdimKaynagiTuru. */
    val adimKaynagi: AdimKaynagiTuru = AdimKaynagiTuru.YOK,
)
