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
        viewModelScope.launch {
            _durum.update { it.copy(islemde = true) }
            sonucuIsle(depo.girisYap(d.eposta, d.sifre), onBasarili)
        }
    }

    fun kayitOl(onBasarili: () -> Unit) {
        val d = _durum.value

        // Şifre tekrarı ve koşullar sunucunun değil formun işi; depoya
        // gitmeden burada kontrol ediliyor.
        HesapDogrula.sifreTekrar(d.sifre, d.sifreTekrar)?.let {
            return hataYaz(HesapAlani.SIFRE_TEKRAR, it)
        }
        if (!d.kosullarOnaylandi) {
            return hataYaz(HesapAlani.KOSULLAR, "Devam etmek için koşulları onaylaman gerekiyor.")
        }

        viewModelScope.launch {
            _durum.update { it.copy(islemde = true) }
            sonucuIsle(depo.kayitOl(d.ad, d.eposta, d.sifre), onBasarili)
        }
    }

    fun sifreSifirla() {
        val d = _durum.value
        viewModelScope.launch {
            _durum.update { it.copy(islemde = true) }
            when (val sonuc = depo.sifreSifirla(d.eposta)) {
                is HesapSonuc.Basarili ->
                    _durum.update { it.copy(islemde = false, sifirlamaGonderildi = true) }
                is HesapSonuc.Hata -> hataYaz(sonuc.alan, sonuc.mesaj)
            }
        }
    }

    fun misafirDevam(onBasarili: () -> Unit) {
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
