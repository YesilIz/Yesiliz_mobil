package tr.org.yesilay.yesiliz.arkaplan

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.room.withTransaction
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import tr.org.yesilay.yesiliz.data.depo.BildirimDeposu
import tr.org.yesilay.yesiliz.data.depo.KullanimDeposu
import tr.org.yesilay.yesiliz.data.depo.SenkronDeposu
import tr.org.yesilay.yesiliz.data.depo.PuanDeposu
import tr.org.yesilay.yesiliz.data.depo.RiskDeposu
import tr.org.yesilay.yesiliz.data.kullanim.KullanimIzni
import tr.org.yesilay.yesiliz.data.depo.AdimDeposu
import tr.org.yesilay.yesiliz.data.saglik.AktiviteSaglayici
import tr.org.yesilay.yesiliz.data.yerel.dao.GunlukOzetDao
import tr.org.yesilay.yesiliz.data.yerel.dao.KullanimOlayDao
import tr.org.yesilay.yesiliz.data.yerel.dao.PuanGunlukDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.yerel.YesilizDb
import tr.org.yesilay.yesiliz.widget.widgetlariTazele
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Günlük özet + risk hesabı — CLAUDE.md §11 md.1.
 *
 * Zincirin tamamı burada koşuyor:
 * ```
 * Health Connect → adım/spor
 * UsageStatsManager → oturumlar → kategori → Room → günlük özet
 * günlük özet → core-risk → risk_sonuc (skor + bant + trend)
 * eski ham olayları sil
 * ```
 *
 * ## Telafi
 *
 * Yalnızca dünü işlemek yetmiyor: kullanıcı telefonu kapatmış, uygulama
 * kısıtlanmış ya da worker ertelenmiş olabilir. Her koşuda son
 * [TELAFI_GUN] günden **işlenmemiş** olanlar da işleniyor. Zaten işlenmiş
 * günler atlanıyor, yani ek maliyet yok.
 *
 * Bugün her koşuda yeniden işleniyor: gün henüz bitmedi, veri artmaya devam
 * ediyor. `gunuIsle` günü silip yeniden yazdığı için tekrar güvenli.
 */
@HiltWorker
class GunlukOzetWorker @AssistedInject constructor(
    @Assisted baglam: Context,
    @Assisted parametreler: WorkerParameters,
    private val kullanimDeposu: KullanimDeposu,
    private val riskDeposu: RiskDeposu,
    private val puanDeposu: PuanDeposu,
    private val aktiviteSaglayici: AktiviteSaglayici,
    private val adimDeposu: AdimDeposu,
    private val db: YesilizDb,
    private val ozetDao: GunlukOzetDao,
    private val riskDao: RiskSonucDao,
    private val puanDao: PuanGunlukDao,
    private val olayDao: KullanimOlayDao,
    private val bildirimDeposu: BildirimDeposu,
    private val senkronDeposu: SenkronDeposu,
    private val tercihler: tr.org.yesilay.yesiliz.data.tercih.Tercihler,
    private val saat: Clock,
    private val isPlanlayici: IsPlanlayici,
) : CoroutineWorker(baglam, parametreler) {

    override suspend fun doWork(): Result {
        // İzin yoksa yapacak bir şey yok. `retry` DEĞİL: izin Ayarlar'dan
        // elle veriliyor, tekrar denemek pili boşuna yakar. İzin verildiğinde
        // uygulama açılışı zaten telafiyi tetikliyor.
        if (!KullanimIzni.verildiMi(applicationContext)) {
            isPlanlayici.gecelikPlanla()
            return Result.success()
        }

        val sonuc = try {
            val bugun = LocalDate.now(saat)
            val baslangic = bugun.minusDays(TELAFI_GUN)
            val islenmis = tamamlanmisGunler(
                ozetDao.islenmisGunler(baslangic, bugun),
                riskDao.islenmisGunler(baslangic, bugun),
                puanDao.islenmisGunler(baslangic, bugun),
            )

            // Kurulumdan ÖNCEsi telafi edilmiyor. UsageStats kurulumdan önceki
            // günler için de veri verebiliyor ama uygulama o günlerde kullanıcıyı
            // izlemiyordu; o günleri işlemek geçmişi geriye doğru uydurmak olurdu.
            // Telafinin asıl işi, uygulama birkaç gün açılmadığında oluşan
            // BOŞLUKLARI kapatmak (`debug.md` K-4).
            val kurulum = tercihler.anlik().ilkKurulumTarihi
            val veriBaslangici = tercihler.veriBaslangicAni.first()

            // Donanım sayacı özet hesaplanmadan HEMEN ÖNCE örnekleniyor. Sensör
            // yalnızca açılıştan beri biriken kümülatif sayıyı veriyor; son örnek
            // ile gece yarısı arasındaki adımlar örneklenmezse kaybolur.
            adimDeposu.ornekle()

            // Bir kereye mahsus tam tarama: eski bir sürüm geçmiş günlere
            // UYDURMA adım sayısı yazmıştı (hafta içi ~5.500 / hafta sonu
            // ~3.800, gün numarasından türetilmiş). O satırlar "adım var"
            // göründüğü için aşağıdaki atlama koşuluna takılıp sonsuza kadar
            // kalırdı. Bayrak `Tercihler`'de ve bir kez tüketiliyor —
            // `eskiSunucuAdresiniTemizle` ile aynı desen (§12 K24).
            val tamTarama = tercihler.uydurmaAdimTemizligiGerekli.first()

            (TELAFI_GUN downTo 0).forEach { geriGun ->
                val gun = bugun.minusDays(geriGun)
                // Bugün hep yeniden; geçmiş günler yalnızca eksikse.
                if (!tamTarama && gun != bugun && gun in islenmis) return@forEach

                // Kurulumdan ÖNCEKİ günler için ekran süresi geçmişi
                // yazılıyor (UsageStats o günleri gerçekten ölçmüş, §10
                // Android'in avantajı) ama **puan ve risk yazılmıyor**:
                // kullanıcı o günlerde uygulamayı kullanmıyordu, geriye
                // dönük seri/puan vermek hiçbir şey yapmadan ödül dağıtmak
                // olurdu (`debug.md` K-4). İstatistik geçmişi gösteriyor,
                // ödül zinciri göstermiyor.
                val kurulumOncesi = kurulum != null && gun < kurulum

                val aktivite = aktiviteSaglayici.gununAktivitesi(gun)
                gunlukZinciriAtomik(db) {
                    val enErken = if (gun == bugun) veriBaslangici else null
                    kullanimDeposu.gunuIsle(gun, aktivite, enErken = enErken)
                    if (!kurulumOncesi) {
                        riskDeposu.gunuHesapla(gun)
                        // Adım/spor/gece değerleri değişti; günün puanı da değişir.
                        puanDeposu.gunuYenidenHesapla(gun)
                    }
                }
            }

            if (tamTarama) tercihler.uydurmaAdimTemizligiBitti()

            // Kapanan günün kazanımları bildirim defterine.
            kapananGunuBildir(bugun)

            // Ham olaylar sonsuza kadar tutulmuyor (veri minimizasyonu,
            // CLAUDE.md §4). Özet zaten çıkarıldı.
            olayDao.eskileriSil(bugun.minusDays(HAM_OLAY_SAKLAMA_GUN))
            adimDeposu.eskileriSil(bugun.minusDays(HAM_OLAY_SAKLAMA_GUN))

            // Bildirim defteri de sınırsız büyümüyor.
            bildirimDeposu.eskileriTemizle()

            // Widget'lar özet yazıldıktan sonra tazeleniyor. Kendi periyodik
            // güncellemelerini beklemek, ana ekranda saatlerce eski sayı
            // göstermek demek (updatePeriodMillis zaten 0).
            widgetlariTazele(applicationContext)

            // Sunucuya yükleme EN SON: rıza kapalıysa `senkronEt` hiçbir şey
            // göndermeden dönüyor, hata da sessizce yutuluyor. Senkron
            // başarısız diye günlük özet hesabı geri alınmamalı.
            senkronDeposu.senkronEt()
            // Gizlilik tercihi davranış verisi rızasından bağımsız ve
            // ağ kesintisinden sonra da sonunda uygulanmalı.
            senkronDeposu.ligGorunurlugunuSenkronEt()
            senkronDeposu.heartbeat()

            Result.success()
        } catch (iptal: CancellationException) {
            // WorkManager işi durdurduysa yerel yazma/senkron zincirini
            // sürdürme ve yeni bir retry planlama.
            throw iptal
        } catch (hata: Exception) {
            Log.w(ETIKET, "Günlük özet zinciri tamamlanamadı; yeniden denenecek", hata)
            Result.retry()
        }
        // WorkManager'ın 24 saatlik periyodu duvar saati değildir. Sonraki
        // 03:00 anını her tamamlanan/yeniden denenecek koşudan sonra güncelle.
        isPlanlayici.gecelikPlanla()
        return sonuc
    }

    /**
     * Bugünün olumlu sonuçlarını bildirim defterine yazar.
     *
     * **Yalnızca kazanımlar.** "Hedefini aştın" diye bir satır bilinçli
     * olarak yok — CLAUDE.md §4: ceza suçluluk üretir. Kötü giden bir günü
     * kullanıcıya bildirimle hatırlatmak, uygulamayı sildiren davranış.
     */
    /**
     * Kapanan günün kutlaması.
     *
     * **Bugünü değil DÜNÜ bildiriyor.** Eskiden süren gün için
     * "Ekran süresi hedefine ulaştın" yazıyordu: sabah 8 dakikayken hedefin
     * altında olmak gün sonunda hedefin altında kalmak demek değil, üstelik
     * kurulum gününde yalnızca kurulum sonrası ölçülen birkaç dakika
     * karşılaştırılıyordu. Puan ve seri zaten gün bitiminde karar veriyor
     * (§12 K40); kutlama da aynı anda gelmeli, yoksa uygulama puan
     * vermediği bir başarıyı ilan ediyor (§12 K37).
     *
     * `birKezGunde` koruması sayesinde worker gün içinde tekrar koşsa da
     * satır katlanmıyor.
     */
    private suspend fun kapananGunuBildir(bugun: LocalDate) {
        val gun = bugun.minusDays(1)
        val ozet = ozetDao.getir(gun) ?: return
        // Ölçülmemiş gün kutlanmaz: sıfır dakika "hedefin altında" değil
        // "ölçemedik" demek (CLAUDE.md §9).
        if (!ozet.olayVarMi) return

        // Gün başından itibaren ölçülmediyse hedef karşılaştırması geçersiz
        // — `PuanDeposu` ile aynı kural.
        val veriBaslangici = tercihler.veriBaslangicAni.first()
        val gunBasi = gun.atStartOfDay(saat.zone).toInstant()
        if (veriBaslangici != null && veriBaslangici.isAfter(gunBasi)) return

        val hedef = tercihler.anlik().gunlukHedefDk
        val ekranDk = ozet.let {
            it.socialDk + it.entertainmentDk + it.gamesDk + it.communicationDk +
                it.productivityDk + it.educationDk + it.healthDk + it.otherDk
        }.roundToInt()

        if (hedef > 0 && ekranDk <= hedef) {
            bildirimDeposu.hedefTutturuldu(ekranDk, hedef)
        }

        val seri = puanDeposu.seriGunu(gun)
        if (seri in SERI_ESIKLERI) {
            bildirimDeposu.seriIlerledi(seri)
        }
    }

    companion object {
        /** Serinin kutlandığı gün eşikleri — her gün bildirim göndermiyoruz. */
        val SERI_ESIKLERI = setOf(3, 7, 14, 30, 60, 100)

        /** Kaçırılan günler için geriye dönük tarama penceresi. */
        const val TELAFI_GUN = 30L

        /** Ham olay saklama süresi; özet kalıcı, ayrıntı değil. */
        const val HAM_OLAY_SAKLAMA_GUN = 30L

        const val IS_ADI = "gunluk_ozet"

        private const val ETIKET = "GunlukOzetWorker"
    }
}

/**
 * Bir gün ancak üç kalıcı çıktısı da varsa tamamlanmıştır. Kesişim yerine
 * yalnızca özet satırına bakmak, risk/puan aşamasındaki kısmi hatayı kalıcı
 * bir veri boşluğuna dönüştürür.
 */
internal fun tamamlanmisGunler(
    ozetGunleri: List<LocalDate>,
    riskGunleri: List<LocalDate>,
    puanGunleri: List<LocalDate>,
): Set<LocalDate> = ozetGunleri.toSet()
    .intersect(riskGunleri.toSet())
    .intersect(puanGunleri.toSet())

/** Bir günün ham olay + özet + risk + puan yazılarını tek işlem yapar. */
internal suspend fun <T> gunlukZinciriAtomik(db: YesilizDb, blok: suspend () -> T): T =
    db.withTransaction { blok() }
