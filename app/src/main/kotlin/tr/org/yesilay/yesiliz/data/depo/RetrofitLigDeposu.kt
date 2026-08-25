package tr.org.yesilay.yesiliz.data.depo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.DavetIstek
import tr.org.yesilay.yesiliz.data.uzak.LigKatilIstek
import tr.org.yesilay.yesiliz.data.uzak.SenkronIstek
import tr.org.yesilay.yesiliz.data.uzak.YesilizApi
import tr.org.yesilay.yesiliz.domain.lig.Arkadas
import tr.org.yesilay.yesiliz.domain.lig.LigDeposu
import tr.org.yesilay.yesiliz.domain.lig.LigOzeti
import tr.org.yesilay.yesiliz.domain.lig.LigSatiri
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerçek lig — `backend/app/uclar/lig.py`.
 *
 * Sunucu tarafı `Flow` sunmuyor; akışlar bir `MutableStateFlow` üzerinden
 * yayınlanıyor ve her yazma işleminden sonra tazeleniyor. Uzun soluklu bir
 * bağlantı (WebSocket/SSE) lig için gereksiz: sıralama saatte bir değişiyor,
 * ekran her açıldığında zaten yeniden okunuyor.
 */
@Singleton
class RetrofitLigDeposu @Inject constructor(
    private val api: YesilizApi,
    private val tercihler: Tercihler,
) : LigDeposu {

    private val ozet = MutableStateFlow<LigOzeti?>(null)
    private val arkadaslar = MutableStateFlow<List<Arkadas>>(emptyList())

    override fun ligAkisi(): Flow<LigOzeti> = ozet.map { it ?: bos() }

    override fun arkadaslarAkisi(): Flow<List<Arkadas>> = arkadaslar

    /**
     * Sunucudan tazeler.
     *
     * `Yedekli*` sarmalayıcı bunu çağırıp hata alırsa yerele düşüyor;
     * bu yüzden burada hata yakalanmıyor — yutulursa sarmalayıcı
     * sunucunun kapalı olduğunu anlayamaz.
     */
    suspend fun tazele() {
        val yanit = api.lig()
        ozet.value = yanit.cevir()
        arkadaslar.value = api.arkadaslar().map { Arkadas(it.isim, it.davetBekliyorMu) }
    }

    override suspend fun davetKodu(): String = api.davetKodu().kod

    override suspend fun ligeKatil(): LigDeposu.KatilimSonuc {
        // Gizlilik tercihi veri paylaşım rızasından bağımsızdır ve yeni
        // haftanın üyeliği açılırken ilk andan uygulanmalıdır.
        val gorunur = tercihler.ligdeGorun.first()
        var yanit = api.ligeKatil(LigKatilIstek(gorunur = gorunur))
        if (yanit.gorunuyorMuyum != gorunur) {
            // Yuvarlanan dağıtımda eski sunucu katılım gövdesini yok
            // sayabilir. Var olan `/ozet` sözleşmesi yalnızca görünürlük
            // alanıyla çağrılır; özet, risk ve puan gönderilmez.
            api.ozetGonder(SenkronIstek(ligdeGorunur = gorunur))
            yanit = api.lig()
        }
        ozet.value = yanit.cevir()
        return LigDeposu.KatilimSonuc.BASARILI
    }

    /**
     * Sunucuda davet **koda göre** çalışıyor; arayüzdeki `isim` parametresi
     * öneri listesindeki kişiyi işaret ediyordu. Gerçek sistemde isim
     * yeterli bir kimlik değil, o yüzden bu çağrı kodu bekliyor —
     * `ArkadasEkleEkran`'daki "Ekle" butonu öneri listesinden geldiği için
     * yalnızca yerel depoda anlamlı; gerçek akış "davet kodu paylaş".
     */
    override suspend fun davetGonder(kod: String) {
        arkadaslar.value = api.davetGonder(DavetIstek(kod.trim().uppercase()))
            .map { Arkadas(it.isim, it.davetBekliyorMu) }
    }

    private fun tr.org.yesilay.yesiliz.data.uzak.LigYanit.cevir() = LigOzeti(
        ligAdi = ligAdi,
        kalanGun = kalanGun,
        yukselen = yukselen,
        dusen = dusen,
        siralama = siralama.map {
            LigSatiri(
                sira = it.sira,
                isim = it.isim,
                puan = it.puan,
                benMiyim = it.benMiyim,
                arkadasMi = it.arkadasMi,
            )
        },
        katildiMi = katildiMi,
        gorunuyorum = gorunuyorMuyum,
    )

    private fun bos() = LigOzeti(
        ligAdi = "",
        kalanGun = 0,
        yukselen = 0,
        dusen = 0,
        siralama = emptyList(),
        katildiMi = false,
    )
}
