package tr.org.yesilay.yesiliz.ui.ekran.lig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
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
) : ViewModel() {

    val durum: StateFlow<LigDurum> = depo.ligAkisi()
        .map { ozet ->
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
                yukleniyor = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AKIS_BEKLEME_MS),
            initialValue = LigDurum(),
        )

    fun ligeKatil() {
        viewModelScope.launch { depo.ligeKatil() }
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
     * Kod ve öneri listesini tazeler.
     *
     * Askıya alan çağrılar `update` bloğunun DIŞINDA: `MutableStateFlow`
     * lambdayı yarışma durumunda yeniden çalıştırıyor ve içeride ağ
     * çağrısı olsaydı ikinci kez istek gidebilirdi.
     */
    private fun yenile() {
        viewModelScope.launch {
            val kod = depo.davetKodu()
            val oneriler = depo.onerilenler()
            _durum.update {
                it.copy(davetKodu = kod, onerilenler = oneriler, yukleniyor = false)
            }
        }
    }

    /**
     * Davet gönderir.
     *
     * Dokunuş veritabanına yazılıyordu ama ekranda hiçbir iz bırakmıyordu:
     * çip hâlâ "Ekle" diyor, kişi listeden çıkmıyor, "Davet gönderildi"
     * durumu görünmüyordu. Kullanıcı işe yaramadı sanıp tekrar dokunuyordu
     * (`debug.md` Y-10).
     *
     * Asıl sebep gecikmeydi: sunucuya ulaşılamayan bir telefonda hem
     * `davetGonder` hem `yenile` beşer saniye zaman aşımına giriyor,
     * yaklaşık on saniye boyunca hiçbir şey değişmiyordu. Artık dokunuş
     * ANINDA "Gönderiliyor…" olarak görünüyor.
     */
    fun davetGonder(isim: String) {
        if (isim in _durum.value.gonderiliyor) return
        _durum.update { it.copy(gonderiliyor = it.gonderiliyor + isim) }

        viewModelScope.launch {
            depo.davetGonder(isim)
            val oneriler = depo.onerilenler()
            _durum.update {
                it.copy(onerilenler = oneriler, gonderiliyor = it.gonderiliyor - isim)
            }
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
        const val DAVET_KODU_AZAMI = 8
    }
}

/** Davet kodu biçimi ve kendi-kodunu-kullanma tuzağı için saf doğrulama. */
internal fun davetKodunuDogrula(kod: String, kendiKodu: String): String? = when {
    !DavetKodu.gecerliMi(kod) -> "Kod YSZ-7K2Q biçiminde olmalı."
    kod.trim().uppercase() == kendiKodu.trim().uppercase() ->
        "Kendi davet kodunu kullanamazsın."
    else -> null
}
