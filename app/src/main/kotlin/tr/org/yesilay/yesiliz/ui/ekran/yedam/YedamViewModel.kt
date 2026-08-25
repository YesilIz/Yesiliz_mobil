package tr.org.yesilay.yesiliz.ui.ekran.yedam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tr.org.yesilay.yesiliz.data.depo.YedamDeposu
import tr.org.yesilay.yesiliz.data.tercih.Tercihler
import tr.org.yesilay.yesiliz.data.uzak.YedamDurumYanit
import tr.org.yesilay.yesiliz.domain.yedam.YedamKapsam
import javax.inject.Inject

/**
 * YEDAM köprüsü — rıza, başvuru ve durum takibi.
 *
 * Ekran açılışta **sunucudan** durumu soruyor: cihazda "başvurdum" diye
 * bir bayrak tutulmuyor. Rızanın ve başvurunun tek otoritesi sunucu;
 * yerel bir kopya, sunucuda kapanmış bir başvuruyu açık göstermeye yol
 * açardı (CLAUDE.md §12 K25 ailesi).
 */
@HiltViewModel
class YedamViewModel @Inject constructor(
    private val depo: YedamDeposu,
    private val tercihler: Tercihler,
) : ViewModel() {

    private val _durum = MutableStateFlow(YedamDurum())
    val durum: StateFlow<YedamDurum> = _durum.asStateFlow()

    init {
        tazele()
    }

    fun tazele() {
        viewModelScope.launch {
            _durum.value = _durum.value.copy(yukleniyor = true, cevrimdisi = false)
            val yanit = depo.durum()
            _durum.value = _durum.value.copy(
                yukleniyor = false,
                // `null` "sunucuya ulaşılamadı" demek; boş bir başvuru
                // görünümü çizmek "başvurun yok" demek olurdu ve ikisi
                // aynı şey değil.
                cevrimdisi = yanit == null,
                basvuru = yanit?.gorunume(),
            )
        }
    }

    fun onayDegisti(secili: Boolean) {
        _durum.value = _durum.value.copy(onayVerildi = secili, hata = null)
    }

    fun iletisimTuruDegisti(tur: IletisimTuru) {
        _durum.value = _durum.value.copy(iletisimTuru = tur, iletisimDegeri = "", hata = null)
    }

    fun iletisimDegeriDegisti(deger: String) {
        _durum.value = _durum.value.copy(iletisimDegeri = deger, hata = null)
    }

    fun notDegisti(metin: String) {
        _durum.value = _durum.value.copy(not = metin)
    }

    /**
     * Başvuruyu gönderir.
     *
     * Yaş **kalıcı tercihten** okunuyor (`Tercihler.beyanYas`): karşılamada
     * beyan edilen değer artık saklanıyor. Girilmemişse `null` gidiyor ve
     * sunucu reşit saymıyor — bilinmeyeni "18 üstü" kabul etmek korumayı
     * tam da korunması gerekenlerde kapatırdı (§9).
     */
    fun gonder() {
        val mevcut = _durum.value
        if (!mevcut.gonderilebilir) return

        _durum.value = mevcut.copy(gonderiliyor = true, hata = null, cevrimdisi = false)
        viewModelScope.launch {
            val yas = tercihler.beyanYas.first()
            val sonuc = depo.basvur(
                metinSurumu = YedamKapsam.METIN_SURUMU,
                kapsam = YedamKapsam.VARSAYILAN,
                iletisimTuru = mevcut.iletisimTuru.anahtar,
                iletisimDegeri = mevcut.iletisimDegeri,
                kullaniciNotu = mevcut.not,
                beyanYas = yas,
            )
            _durum.value = when (sonuc) {
                is YedamDeposu.YedamSonuc.Basarili -> _durum.value.copy(
                    gonderiliyor = false,
                    basvuru = BasvuruGorunumu(
                        id = sonuc.yanit.basvuruId,
                        durum = sonuc.yanit.durum,
                        ebeveynOnayiBekleniyor = sonuc.yanit.ebeveynOnayiBekleniyor,
                        rizaBitisi = sonuc.yanit.rizaSonGecerlilik,
                        temaslar = emptyList(),
                    ),
                )

                YedamDeposu.YedamSonuc.Cevrimdisi ->
                    _durum.value.copy(gonderiliyor = false, cevrimdisi = true)

                is YedamDeposu.YedamSonuc.Reddedildi ->
                    _durum.value.copy(gonderiliyor = false, hata = sonuc.mesaj)

                YedamDeposu.YedamSonuc.OturumYok ->
                    _durum.value.copy(gonderiliyor = false, oturumYok = true)
            }
        }
    }
}

private fun YedamDurumYanit.gorunume(): BasvuruGorunumu? {
    val kimlik = basvuruId ?: return null
    return BasvuruGorunumu(
        id = kimlik,
        durum = durum,
        ebeveynOnayiBekleniyor = ebeveynOnayiBekleniyor,
        rizaBitisi = rizaSonGecerlilik,
        temaslar = temaslar.map { TemasGorunumu(it.tur, it.zaman) },
    )
}
