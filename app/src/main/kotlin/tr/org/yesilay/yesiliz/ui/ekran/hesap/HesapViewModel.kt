package tr.org.yesilay.yesiliz.ui.ekran.hesap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.domain.hesap.HesapAlani
import tr.org.yesilay.yesiliz.domain.hesap.HesapDeposu
import tr.org.yesilay.yesiliz.domain.hesap.HesapDogrula
import tr.org.yesilay.yesiliz.domain.hesap.HesapSonuc
import javax.inject.Inject

@HiltViewModel
class HesapViewModel @Inject constructor(
    private val depo: HesapDeposu,
) : ViewModel() {

    private val _durum = MutableStateFlow(HesapDurum())
    val durum: StateFlow<HesapDurum> = _durum.asStateFlow()

    // Alan güncellemeleri ilgili hatayı temizliyor: kullanıcı düzeltmeye
    // başladığı anda eski hata metni yanıltıcı hâle geliyor.
    fun adDegisti(deger: String) = guncelle(HesapAlani.AD) { it.copy(ad = deger) }

    fun epostaDegisti(deger: String) = guncelle(HesapAlani.EPOSTA) { it.copy(eposta = deger) }

    fun sifreDegisti(deger: String) = guncelle(HesapAlani.SIFRE) { it.copy(sifre = deger) }

    fun sifreTekrarDegisti(deger: String) =
        guncelle(HesapAlani.SIFRE_TEKRAR) { it.copy(sifreTekrar = deger) }

    fun kosullariDegistir(onay: Boolean) =
        guncelle(HesapAlani.KOSULLAR) { it.copy(kosullarOnaylandi = onay) }

    fun sifreGorunurlugu() {
        _durum.update { it.copy(sifreGorunur = !it.sifreGorunur) }
    }

    fun girisYap(onBasarili: () -> Unit) {
        val d = _durum.value
        if (d.islemde) return

        // Alan doğrulaması AĞDAN ÖNCE. Eskiden bu kontroller yalnızca depo
        // katmanındaydı ve `YedekliHesapDeposu` önce ağı deniyordu: boş bir
        // formda "Hesap oluştur"a basınca "Adını yazman gerekiyor." hatası
        // 5+ saniye sonra çıkıyordu (`debug.md` Y-12). Tamamen istemci
        // tarafı olması gereken bir kontrol, ağ gecikmesinin arkasındaydı.
        HesapDogrula.eposta(d.eposta)?.let { return hataYaz(HesapAlani.EPOSTA, it) }
        HesapDogrula.sifre(d.sifre)?.let { return hataYaz(HesapAlani.SIFRE, it) }

        if (!_durum.islemiSahiplen()) return
        viewModelScope.launch {
            sonucuIsle(depo.girisYap(d.eposta, d.sifre), onBasarili)
        }
    }

    fun kayitOl(onBasarili: () -> Unit) {
        val d = _durum.value
        if (d.islemde) return

        // Alan doğrulamalarının tamamı depoya gitmeden burada (`debug.md`
        // Y-12): hiçbiri sunucu bilgisi istemiyor ve ağ zaman aşımının
        // arkasında beklemeleri için sebep yok.
        HesapDogrula.ad(d.ad)?.let { return hataYaz(HesapAlani.AD, it) }
        HesapDogrula.eposta(d.eposta)?.let { return hataYaz(HesapAlani.EPOSTA, it) }
        HesapDogrula.sifre(d.sifre)?.let { return hataYaz(HesapAlani.SIFRE, it) }
        HesapDogrula.sifreTekrar(d.sifre, d.sifreTekrar)?.let {
            return hataYaz(HesapAlani.SIFRE_TEKRAR, it)
        }
        if (!d.kosullarOnaylandi) {
            return hataYaz(HesapAlani.KOSULLAR, "Devam etmek için koşulları onaylaman gerekiyor.")
        }

        if (!_durum.islemiSahiplen()) return
        viewModelScope.launch {
            sonucuIsle(depo.kayitOl(d.ad, d.eposta, d.sifre), onBasarili)
        }
    }

    fun sifreSifirla() {
        val d = _durum.value
        if (d.islemde) return
        HesapDogrula.eposta(d.eposta)?.let { return hataYaz(HesapAlani.EPOSTA, it) }

        if (!_durum.islemiSahiplen()) return
        viewModelScope.launch {
            when (val sonuc = depo.sifreSifirla(d.eposta)) {
                is HesapSonuc.Basarili ->
                    _durum.update { it.copy(islemde = false, sifirlamaGonderildi = true) }
                is HesapSonuc.Hata -> hataYaz(sonuc.alan, sonuc.mesaj)
            }
        }
    }

    fun misafirDevam(onBasarili: () -> Unit) {
        if (!_durum.islemiSahiplen()) return
        viewModelScope.launch {
            depo.misafirDevam()
            onBasarili()
        }
    }

    private fun sonucuIsle(sonuc: HesapSonuc, onBasarili: () -> Unit) {
        when (sonuc) {
            is HesapSonuc.Basarili -> {
                _durum.update { it.copy(islemde = false) }
                onBasarili()
            }
            is HesapSonuc.Hata -> hataYaz(sonuc.alan, sonuc.mesaj)
        }
    }

    private fun hataYaz(alan: HesapAlani, mesaj: String) {
        _durum.update { it.copy(islemde = false, hatalar = it.hatalar + (alan to mesaj)) }
    }

    private fun guncelle(alan: HesapAlani, donusum: (HesapDurum) -> HesapDurum) {
        _durum.update { donusum(it).copy(hatalar = it.hatalar - alan) }
    }
}

/** Hızlı çift dokunuşta yalnızca bir coroutine ağ işlemini sahiplenir. */
internal fun MutableStateFlow<HesapDurum>.islemiSahiplen(): Boolean {
    while (true) {
        val mevcut = value
        if (mevcut.islemde) return false
        if (compareAndSet(mevcut, mevcut.copy(islemde = true))) return true
    }
}
