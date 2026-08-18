package tr.org.yesilay.yesiliz.ui.ekran.lig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import retrofit2.HttpException
import java.io.IOException
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import tr.org.yesilay.yesiliz.data.depo.SenkronDeposu
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.domain.lig.DavetKodu
import tr.org.yesilay.yesiliz.domain.lig.LigDeposu
import javax.inject.Inject

/**
 * Lig durumu.
 *
 * Sıralama artık [LigDeposu] arayüzünden geliyor (CLAUDE.md §12 K7); gömülü
 * sabit liste `YerelSahteLigDeposu`'na taşındı. Faz 2'de `RetrofitLigDeposu`
 * bağlanınca bu sınıf **hiç değişmiyor**.
 */
@HiltViewModel
class LigViewModel @Inject constructor(
    private val depo: LigDeposu,
    private val tercihler: Tercihler,
    private val senkronDeposu: SenkronDeposu,
) : ViewModel() {

    /**
     * Elle tazeleme tetikleyicisi.
     *
     * [LigDeposu.ligAkisi] bir `flow { }`: sunucuyu abonelik başına **bir
     * kez** yokluyor. İlk denemede ağ yoksa ekran, yeniden abone olunana
     * kadar (sekme değişimi ya da uygulamayı kapatıp açma) çevrimdışı
     * kalıyordu — "çık gir yapınca güncelleniyor" şikâyetinin lig ayağı.
     * Yenile butonu bu akışı yeniden kuruyor.
     */
    private val tazelemeIsareti = MutableStateFlow(0)

    private val _katilimHatasi = MutableStateFlow<String?>(null)
    val katilimHatasi: StateFlow<String?> = _katilimHatasi.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val durum: StateFlow<LigDurum> = tazelemeIsareti
        .flatMapLatest {
            flow {
                // Sıralamayı istemeden ÖNCE puanı gönder. Puan sunucuya
                // yalnızca `POST /ozet` ile gidiyordu ve onu da gecelik
                // worker çağırıyordu: lige katılan kullanıcı ertesi sabaha
                // kadar kendini 0 puanla görüyordu. Çağrı rıza kapısını
                // kendi içinde kontrol ediyor ve hatayı yutuyor.
                senkronDeposu.senkronEt()
                emitAll(depo.ligAkisi())
            }
        }
        .combine(tercihler.yesilayPaylasimi) { ozet, paylasim ->
            ozet to paylasim
        }
        .combine(tercihler.oturum) { (ozet, paylasim), oturum ->
            // Misafir ve hiç giriş yapmamış kullanıcı sunucuya gitmeden
            // "oturum yok" durumuna düşüyor: lig uçları zorunlu jeton
            // istiyor, çağrı zaten 401 dönecek.
            val girisYok = oturum.misafirMi || !oturum.girisYapildiMi
            val duzeltilmis =
                if (girisYok) ozet.copy(oturumYok = true, cevrimdisi = false) else ozet
            // Paylaşım kapalıyken puan gönderilemiyor; bunu yalnızca gerçekten
            // ligde olan ve giriş yapmış kullanıcıya söylemek anlamlı.
            duzeltilmis to (!paylasim && !girisYok && duzeltilmis.katildiMi)
        }
        .map { (ozet, paylasimKapali) ->
            LigDurum(
                ligAdi = ozet.ligAdi,
                kalanGun = ozet.kalanGun,
                yukselen = ozet.yukselen,
                dusen = ozet.dusen,
                siralama = ozet.siralama.map {
                    LigSatiriModel(
                        sira = it.sira,
                        isim = it.isim,
                        puan = it.puan,
                        benMiyim = it.benMiyim,
                        arkadasMi = it.arkadasMi,
                    )
                },
                katildiMi = ozet.katildiMi,
                gorunuyorum = ozet.gorunuyorum,
                cevrimdisi = ozet.cevrimdisi,
                oturumYok = ozet.oturumYok,
                puanPaylasimiKapali = paylasimKapali,
                yukleniyor = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
            initialValue = LigDurum(),
        )

    fun ligeKatil() {
        viewModelScope.launch {
            _katilimHatasi.value = when (depo.ligeKatil()) {
                // Katılım üyeliği 0 puanla açıyor; puan ayrı bir uçtan
                // gidiyor. Tazeleme onu da tetikliyor.
                LigDeposu.KatilimSonuc.BASARILI -> {
                    tazele()
                    null
                }
                LigDeposu.KatilimSonuc.OTURUM_YOK ->
                    "Lige katılmak için giriş yapman gerekiyor."
                LigDeposu.KatilimSonuc.ULASILAMADI ->
                    "Sunucuya ulaşılamadı. Birazdan tekrar dene."
            }
        }
    }

    /** Yenile butonu — akışı baştan kurar, sunucu yeniden yoklanır. */
    fun tazele() {
        _katilimHatasi.value = null
        tazelemeIsareti.update { it + 1 }
    }

    /**
     * "Paylaşımı aç ve puanımı gönder" — lig kartındaki eylem.
     *
     * Rızayı **kullanıcının açık dokunuşuyla** açıyor; kart neyin
     * gönderileceğini yazıyor. Profil'deki anahtarla aynı tercih,
     * yalnızca kullanıcının sorunu yaşadığı yerde.
     */
    fun paylasimiAcVePuaniGonder() {
        viewModelScope.launch {
            tercihler.yesilayPaylasimiAyarla(true)
            tazele()
        }
    }

    fun katilimHatasiGoruldu() {
        _katilimHatasi.value = null
    }

    private companion object {
        const val AKIS_BEKLEME_MS = 5_000L
    }
}

/** Arkadaş ekle — tasarım 36. */
@HiltViewModel
class ArkadasEkleViewModel @Inject constructor(
    private val depo: LigDeposu,
) : ViewModel() {

    private val _durum = MutableStateFlow(ArkadasEkleDurum())
    val durum: StateFlow<ArkadasEkleDurum> = _durum.asStateFlow()

    init {
        yenile()
    }

    /**
     * Davet kodunu tazeler.
     *
     * Askıya alan çağrı `update` bloğunun DIŞINDA: `MutableStateFlow`
     * lambdayı yarışma durumunda yeniden çalıştırıyor ve içeride ağ
     * çağrısı olsaydı ikinci kez istek gidebilirdi.
     */
    private fun yenile() {
        viewModelScope.launch {
            val kod = depo.davetKodu()
            _durum.update { it.copy(davetKodu = kod, yukleniyor = false) }
        }
    }

    fun davetKodunuDegistir(kod: String) {
        _durum.update {
            it.copy(
                girilenKod = kod.uppercase().take(DAVET_KODU_AZAMI),
                kodHatasi = null,
                kodlaEklendi = false,
            )
        }
    }

    /** Paylaşılan gerçek kodu backend'de karşılıklı arkadaşlığa çevirir. */
    fun kodlaArkadasEkle() {
        if (_durum.value.kodIslemde) return
        val kod = _durum.value.girilenKod.trim().uppercase()
        val dogrulamaHatasi = davetKodunuDogrula(kod, _durum.value.davetKodu)
        if (dogrulamaHatasi != null) {
            _durum.update { it.copy(kodHatasi = dogrulamaHatasi) }
            return
        }

        _durum.update { it.copy(kodIslemde = true, kodHatasi = null, kodlaEklendi = false) }
        viewModelScope.launch {
            try {
                depo.davetGonder(kod)
                _durum.update {
                    it.copy(kodIslemde = false, kodlaEklendi = true, girilenKod = "")
                }
            } catch (iptal: CancellationException) {
                throw iptal
            } catch (hata: Exception) {
                _durum.update {
                    it.copy(kodIslemde = false, kodHatasi = davetHatasi(hata))
                }
            }
        }
    }

    private fun davetHatasi(hata: Exception): String = when {
        hata is HttpException && hata.code() == 404 -> "Bu davet kodu bulunamadı."
        hata is HttpException && hata.code() == 400 -> "Bu kod kendi hesabına ait ya da kullanılamıyor."
        hata is HttpException && hata.code() == 401 -> "Davet eklemek için yeniden giriş yapmalısın."
        hata is IOException -> "Sunucuya ulaşılamıyor. Bağlantını kontrol edip tekrar dene."
        else -> "Arkadaş eklenemedi. Biraz sonra tekrar deneyebilirsin."
    }

    /** Panoya kopyalama Composable'da; burada yalnızca geri bildirim. */
    fun kopyalandiIsaretle() {
        _durum.update { it.copy(kopyalandi = true) }
    }

    private companion object {
        const val DAVET_KODU_AZAMI = 4 + DavetKodu.UZUNLUK
    }
}

/** Davet kodu biçimi ve kendi-kodunu-kullanma tuzağı için saf doğrulama. */
internal fun davetKodunuDogrula(kod: String, kendiKodu: String): String? = when {
    !DavetKodu.gecerliMi(kod) -> "Kod ${DavetKodu.ORNEK} biçiminde olmalı."
    kod.trim().uppercase() == kendiKodu.trim().uppercase() ->
        "Kendi davet kodunu kullanamazsın."
    else -> null
}
