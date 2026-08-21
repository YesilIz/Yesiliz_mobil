package tr.org.yesilay.yesiliz.data.depo

import android.util.Log
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.AnketYanitDto
import tr.org.yesilay.yesiliz.data.uzak.EbeveynYedamOnayIstek
import tr.org.yesilay.yesiliz.data.uzak.SurukleyiciDto
import tr.org.yesilay.yesiliz.data.uzak.SurukleyiciIstek
import tr.org.yesilay.yesiliz.data.uzak.YedamBasvuruIstek
import tr.org.yesilay.yesiliz.data.uzak.YedamBasvuruYanit
import tr.org.yesilay.yesiliz.data.uzak.YedamDurumYanit
import tr.org.yesilay.yesiliz.data.uzak.YedamGecmisIstek
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import tr.org.yesilay.yesiliz.data.yerel.dao.AnketYanitDao
import tr.org.yesilay.yesiliz.data.yerel.dao.RiskSonucDao
import tr.org.yesilay.yesiliz.data.yerel.entity.RiskSonucEntity
import tr.org.yesilay.yesiliz.domain.yedam.YedamKapsam
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YEDAM köprüsünün cihaz tarafı.
 *
 * ## İKİ RIZA BAĞIMSIZ
 *
 * [SenkronDeposu] "Yeşilay ile paylaşım" rızasına bağlı (CLAUDE.md §12
 * K19) ve o kapalıysa **ilk satırda dönüyor**. YEDAM rızası ayrı bir
 * karar: paylaşımı kapatmış bir kullanıcı YEDAM desteği isteyebilir ve o
 * durumda sürükleyicileri gider, günlük özeti gitmez.
 *
 * Bu yüzden sürükleyici gönderimi `POST /ozet` gövdesine **eklenmedi**,
 * ayrı bir uca gitti (`docs/panel-plani.md §1.1 U1`): `/ozet` o kullanıcıda
 * hiç çağrılmadığı için özellik tam da korumak istediği kişide sessizce
 * ölü kalırdı.
 *
 * ## HATA YUTULMUYOR
 *
 * Başvuru ve geri çekme kullanıcının **gördüğü** eylemler; sonuçları
 * [YedamSonuc] ile dönüyor. Sessizce başarısız olup "gönderildi" demek
 * §4'ün yasakladığı şey (K37). Arka plandaki sürükleyici gönderimi ise
 * yan iş — orada hata yalnızca loglanıyor.
 */
@Singleton
class YedamDeposu @Inject constructor(
    private val api: YesilizApi,
    private val tercihler: Tercihler,
    private val riskDao: RiskSonucDao,
    private val anketDao: AnketYanitDao,
) {

    sealed interface YedamSonuc {
        data class Basarili(val yanit: YedamBasvuruYanit) : YedamSonuc

        /** Sunucuya ulaşılamadı — kullanıcı yeniden deneyebilir. */
        data object Cevrimdisi : YedamSonuc

        /** Sunucu reddetti; [mesaj] kullanıcıya gösterilecek gerekçe. */
        data class Reddedildi(val mesaj: String) : YedamSonuc

        /** Oturum yok: misafir kullanıcı ya da jetonu düşmüş hesap. */
        data object OturumYok : YedamSonuc
    }

    /**
     * Rızayı ve başvuruyu tek çağrıda gönderir.
     *
     * Başarılıysa geçmiş yüklemesi de **hemen** deneniyor (M3): 30 günlük
     * risk ve anket geçmişi gitmezse danışman ilk gün hiçbir şey görmüyor
     * ve "son 30 günün bandı" sözü karşılanmıyor. Yükleme başarısız olursa
     * başvuru yine de açık — geçmiş, başvurunun ön koşulu değil.
     */
    suspend fun basvur(
        metinSurumu: String,
        kapsam: Set<String>,
        iletisimTuru: String,
        iletisimDegeri: String,
        kullaniciNotu: String,
        beyanYas: Int?,
    ): YedamSonuc = try {
        val yanit = api.yedamBasvur(
            YedamBasvuruIstek(
                metinSurumu = metinSurumu,
                kapsam = kapsam.sorted(),
                iletisimTuru = iletisimTuru,
                iletisimDegeri = iletisimDegeri.trim(),
                kullaniciNotu = kullaniciNotu.trim(),
                beyanYas = beyanYas,
            ),
        )
        gecmisiYukle(kapsam)
        YedamSonuc.Basarili(yanit)
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: HttpException) {
        when (hata.code()) {
            KOD_YETKISIZ -> YedamSonuc.OturumYok
            // Sunucunun gerekçesi kullanıcıya gösteriliyor: "başvuru
            // açılamadı" deyip susmak, ne yapması gerektiğini bilmemesi.
            else -> YedamSonuc.Reddedildi(hataMesaji(hata))
        }
    } catch (hata: IOException) {
        Log.w(ETIKET, "YEDAM başvurusu gönderilemedi", hata)
        YedamSonuc.Cevrimdisi
    }

    /**
     * Rıza anındaki tek seferlik geçmiş yüklemesi (M3).
     *
     * Hata yutuluyor **ama loglanıyor** (§9): başvuru açıldı, geçmiş
     * gitmedi — kullanıcıya gösterilecek bir şey yok, danışman da bir
     * sonraki günlük gönderimden itibaren veri görmeye başlıyor.
     */
    private suspend fun gecmisiYukle(kapsam: Set<String>) {
        val surukleyiciler =
            if (YedamKapsam.SURUKLEYICI in kapsam) surukleyicileriTopla() else emptyList()
        val anketler = if (YedamKapsam.ANKET in kapsam) anketleriTopla() else emptyList()
        if (surukleyiciler.isEmpty() && anketler.isEmpty()) return

        try {
            api.yedamGecmis(YedamGecmisIstek(surukleyiciler, anketler))
        } catch (iptal: CancellationException) {
            throw iptal
        } catch (hata: Exception) {
            Log.w(ETIKET, "YEDAM geçmişi yüklenemedi", hata)
        }
    }

    /**
     * Günlük sürükleyici gönderimi (M2) — yan iş, sessiz.
     *
     * Rıza yoksa sunucu 403 dönüyor ve bu **beklenen** durum: her
     * kullanıcının YEDAM rızası yok. Gürültü yapmamak için 403 ayrıca
     * loglanmıyor.
     */
    suspend fun surukleyicileriGonder(): Boolean {
        val kayitlar = surukleyicileriTopla()
        if (kayitlar.isEmpty()) return false
        return try {
            api.yedamSurukleyici(SurukleyiciIstek(kayitlar))
            true
        } catch (iptal: CancellationException) {
            throw iptal
        } catch (hata: HttpException) {
            if (hata.code() != KOD_YASAK) {
                Log.w(ETIKET, "Sürükleyiciler gönderilemedi", hata)
            }
            false
        } catch (hata: Exception) {
            Log.w(ETIKET, "Sürükleyiciler gönderilemedi", hata)
            false
        }
    }

    /** Durum takibi (M4). Sunucuya ulaşılamazsa `null` — uydurma durum yok. */
    suspend fun durum(): YedamDurumYanit? = sessizce("YEDAM durumu alınamadı") {
        api.yedamDurumu()
    }

    /** Şeffaflık kaydı (M5). */
    suspend fun erisimKaydim() = sessizce("Erişim kaydı alınamadı") {
        api.yedamErisimKaydim()
    }

    /**
     * Rızayı geri çeker (M6).
     *
     * Yerel bir bayrak **yok**: rızanın tek otoritesi sunucu. Cihazda
     * "geri çektim" diye işaretleyip sunucuya ulaşamamak, erişimin sürdüğü
     * hâlde kesildiğini söylemek olurdu (K25 ailesi).
     */
    suspend fun rizayiGeriCek(): Boolean = try {
        api.yedamRizasiniGeriCek()
        true
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: Exception) {
        Log.w(ETIKET, "YEDAM rızası geri çekilemedi", hata)
        false
    }

    /** 18 altı çocuğun başvurusuna ebeveyn onayı. */
    suspend fun ebeveynOnayi(rizaId: Int, onay: Boolean): Boolean = try {
        api.yedamOnayi(EbeveynYedamOnayIstek(rizaId, onay))
        true
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: Exception) {
        Log.w(ETIKET, "YEDAM ebeveyn onayı gönderilemedi", hata)
        false
    }

    /**
     * Son [GECMIS_GUN] günün sürükleyicileri, **sırasıyla**.
     *
     * `RiskSonucEntity.surukleyiciler` zaten sıralı (en çok katkı yapan
     * önce); sıra o listedeki indeksten geliyor. Katkı DEĞERİ cihazda
     * saklanmıyor ve uydurulmuyor.
     *
     * Ölçümü yetersiz günler dışarıda: `veriDurumu != HESAPLANDI` olan bir
     * günün sürükleyicisi "o gün en çok şunu kullandı" demiyor.
     */
    private suspend fun surukleyicileriTopla(): List<SurukleyiciDto> =
        riskDao.sonGunler(GECMIS_GUN)
            .filter { it.veriDurumu == HESAPLANDI }
            .flatMap { gun -> gun.surukleyiciDtolari() }

    private fun RiskSonucEntity.surukleyiciDtolari(): List<SurukleyiciDto> =
        surukleyiciler.mapIndexed { indeks, faktor ->
            SurukleyiciDto(tarih = tarih.toString(), faktor = faktor, sira = indeks + 1)
        }

    private suspend fun anketleriTopla(): List<AnketYanitDto> =
        anketDao.hepsi().map { yanit ->
            AnketYanitDto(
                tarih = yanit.tarih.toString(),
                maddePuanlari = listOf(
                    yanit.madde1, yanit.madde2, yanit.madde3, yanit.madde4, yanit.madde5,
                    yanit.madde6, yanit.madde7, yanit.madde8, yanit.madde9, yanit.madde10,
                ),
                hamPuan = yanit.hamPuan,
                rS = yanit.rS,
            )
        }

    private fun hataMesaji(hata: HttpException): String =
        runCatching { hata.response()?.errorBody()?.string() }
            .getOrNull()
            ?.let { govde -> DETAY.find(govde)?.groupValues?.getOrNull(1) }
            ?: VARSAYILAN_HATA

    private suspend fun <T> sessizce(mesaj: String, blok: suspend () -> T): T? = try {
        blok()
    } catch (iptal: CancellationException) {
        throw iptal
    } catch (hata: Exception) {
        Log.w(ETIKET, mesaj, hata)
        null
    }

    private companion object {
        const val ETIKET = "YedamDepo"
        const val KOD_YETKISIZ = 401
        const val KOD_YASAK = 403
        const val HESAPLANDI = "HESAPLANDI"

        /** Rıza metni "son 30 günün risk bandın" diyor. */
        const val GECMIS_GUN = 30

        const val VARSAYILAN_HATA = "Başvuru gönderilemedi. Daha sonra tekrar dene."

        /** FastAPI hata gövdesi: `{"detail": "..."}`. */
        val DETAY = """"detail"\s*:\s*"([^"]*)"""".toRegex()
    }
}
