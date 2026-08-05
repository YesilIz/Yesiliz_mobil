package tr.org.yesilay.yesiliz.data.depo

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tr.org.yesilay.yesiliz.domain.hesap.HesapAlani
import tr.org.yesilay.yesiliz.domain.hesap.HesapDeposu
import tr.org.yesilay.yesiliz.domain.hesap.HesapSonuc
import tr.org.yesilay.yesiliz.domain.hesap.Oturum
import tr.org.yesilay.yesiliz.domain.lig.Arkadas
import tr.org.yesilay.yesiliz.domain.lig.LigDeposu
import tr.org.yesilay.yesiliz.domain.lig.LigOzeti
import tr.org.yesilay.yesiliz.domain.lig.OnerilenKisi
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Sunucu-yalnız depolar — **release** derlemesinin bağladıkları.
 *
 * [YedekliLigDeposu] / [YedekliHesapDeposu] ağ hatasında `YerelSahte*`
 * depolara düşüyor. O davranış bilinçli (CLAUDE.md §12 K18) ama **demo
 * güvencesi**: sunum salonunda backend kapalı kalırsa lig sekmesi boş
 * kalmasın diye. Yayınlanan sürümde aynı davranış kullanıcıya uydurulmuş
 * bir sıralama göstermek olurdu.
 *
 * Bu yüzden bağlama kaynak setine ayrıldı (`DemoVeri` ile aynı desen):
 * `src/debug` → `Yedekli*`, `src/release` → buradaki `Sunucu*`. Sahte veri
 * sınıfları release'de **hiç örneklenmiyor**.
 *
 * **Hata yutulmuyor, indirgeniyor.** Ne çağıran ViewModel'ler istisna
 * bekliyor (`viewModelScope.launch { depo.ligeKatil() }` — fırlarsa
 * uygulama çöker) ne de kullanıcıya sessizce eski/uydurma veri
 * gösterilebilir. Ortası: boş sonuç + `cevrimdisi` bayrağı + logcat izi.
 * Ekran "sunucuya ulaşılamıyor" diyor, veri uydurmuyor.
 */
private const val ETIKET = "SunucuDepo"

@Singleton
class SunucuLigDeposu @Inject constructor(
    private val uzak: RetrofitLigDeposu,
) : LigDeposu {

    override fun ligAkisi(): Flow<LigOzeti> = flow {
        val ulasildi = try {
            uzak.tazele()
            true
        } catch (hata: Exception) {
            Log.w(ETIKET, "lig tazelenemedi", hata)
            false
        }
        if (ulasildi) {
            uzak.ligAkisi().collect { emit(it) }
        } else {
            // Boş sıralama + bayrak: ekran "sunucuya ulaşılamıyor" notunu
            // gösteriyor ve altında hiçbir satır olmuyor.
            emit(BOS_LIG)
        }
    }

    override fun arkadaslarAkisi(): Flow<List<Arkadas>> = flow {
        val ulasildi = try {
            uzak.tazele()
            true
        } catch (hata: Exception) {
            Log.w(ETIKET, "arkadaş listesi alınamadı", hata)
            false
        }
        if (ulasildi) uzak.arkadaslarAkisi().collect { emit(it) } else emit(emptyList())
    }

    /** Kod sunucuda üretiliyor; ulaşılamıyorsa uydurulmuş kod verilmiyor. */
    override suspend fun davetKodu(): String = sessizce("davet kodu alınamadı", "") {
        uzak.davetKodu()
    }

    override suspend fun ligeKatil() {
        sessizce("lige katılınamadı", Unit) { uzak.ligeKatil() }
    }

    override suspend fun davetGonder(isim: String) {
        sessizce("davet gönderilemedi", Unit) { uzak.davetGonder(isim) }
    }

    /** Öneri listesi sunucuda yok; rehber de okunmuyor (§12 K14). */
    override suspend fun onerilenler(): List<OnerilenKisi> = emptyList()

    private companion object {
        val BOS_LIG = LigOzeti(
            ligAdi = "",
            kalanGun = 0,
            yukselen = 0,
            dusen = 0,
            siralama = emptyList(),
            katildiMi = false,
            cevrimdisi = true,
        )
    }
}

/**
 * Hesap işlemleri — sunucu ne diyorsa o.
 *
 * Ağ hatası yerel sahte hesaba **düşmüyor**: sunucuya ulaşılamazken
 * "giriş yaptın" demek, kimlik doğrulamayı tamamen anlamsızlaştırırdı.
 * Kullanıcı bağlantı hatasını okuyup tekrar deniyor.
 */
@Singleton
class SunucuHesapDeposu @Inject constructor(
    private val uzak: RetrofitHesapDeposu,
) : HesapDeposu {

    override val oturum: Flow<Oturum> = uzak.oturum

    override suspend fun girisYap(eposta: String, sifre: String): HesapSonuc =
        agHatasiniCevir { uzak.girisYap(eposta, sifre) }

    override suspend fun kayitOl(ad: String, eposta: String, sifre: String): HesapSonuc =
        agHatasiniCevir { uzak.kayitOl(ad, eposta, sifre) }

    override suspend fun sifreSifirla(eposta: String): HesapSonuc =
        agHatasiniCevir { uzak.sifreSifirla(eposta) }

    override suspend fun misafirDevam() = uzak.misafirDevam()

    override suspend fun cikisYap() = uzak.cikisYap()

    override suspend fun hesabiSil(): Boolean = uzak.hesabiSil()

    /**
     * Ağ hatasını forma yazılabilir bir hataya çeviriyor.
     *
     * `RetrofitHesapDeposu` yalnızca `HttpException`'ı yakalıyor (iş
     * hataları); `IOException` ondan geçip `viewModelScope`'u çökertirdi.
     */
    private suspend fun agHatasiniCevir(blok: suspend () -> HesapSonuc): HesapSonuc = try {
        blok()
    } catch (hata: IOException) {
        Log.w(ETIKET, "sunucuya ulaşılamadı", hata)
        HesapSonuc.Hata(
            HesapAlani.GENEL,
            "Sunucuya ulaşılamıyor. İnternet bağlantını kontrol edip tekrar dene.",
        )
    }
}

/** Ağ hatasında [bosDeger] döner ama **iz bırakır** (CLAUDE.md §9). */
private suspend fun <T> sessizce(mesaj: String, bosDeger: T, blok: suspend () -> T): T = try {
    blok()
} catch (hata: Exception) {
    Log.w(ETIKET, mesaj, hata)
    bosDeger
}
